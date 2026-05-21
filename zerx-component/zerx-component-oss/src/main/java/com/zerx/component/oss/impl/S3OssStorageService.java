package com.zerx.component.oss.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.zerx.common.util.StringUtil;
import com.zerx.component.oss.OssException;
import com.zerx.component.oss.OssObject;
import com.zerx.component.oss.OssObjectMeta;
import com.zerx.component.oss.OssResult;
import com.zerx.component.oss.PresignedUrl;
import com.zerx.component.oss.properties.ZerxOssProperties;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AWS S3 存储服务实现。
 * <p>
 * 基于 {@link AbstractOssStorageService} 抽象模板，使用 AWS S3 SDK v1
 * 实现所有对象存储操作。兼容所有支持 S3 协议的对象存储服务
 * （如 AWS S3、DigitalOcean Spaces、MinIO（S3 模式）等）。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   oss:
 *     type: S3
 *     endpoint: "s3.us-east-1.amazonaws.com"      # 可选，自定义端点
 *     access-key: "AKIA..."
 *     secret-key: "..."
 *     bucket: "my-bucket"
 *     region: "us-east-1"                          # 默认 us-east-1
 *     custom-domain: "https://cdn.example.com"     # 可选
 *     staging:
 *       prefix: "_staging/"
 *       default-ttl: 24h
 * }</pre>
 *
 * <h3>URL 构建：</h3>
 * <p>
 * 未配置自定义域名时，使用 S3 路径式或虚拟托管式 URL：
 * {@code https://{bucket}.s3.{region}.amazonaws.com/{objectKey}}。
 * 如果 region 为空，则回退到 {@code {endpoint}/{bucket}/{objectKey}} 格式。
 * </p>
 *
 * <h3>用户元数据：</h3>
 * <p>
 * 用户自定义元数据使用 S3 原生的 {@code x-amz-meta-} 前缀，与模板层保持一致，无需前缀转换。
 * </p>
 *
 * @author zerx
 * @see AbstractOssStorageService
 * @see ZerxOssProperties
 */
public class S3OssStorageService extends AbstractOssStorageService {

    /** 统一元数据前缀（S3 原生前缀，无需转换） */
    private static final String META_PREFIX = "x-amz-meta-";

    /** AWS S3 客户端 */
    private final AmazonS3 s3Client;

    /**
     * 构造 AWS S3 存储服务。
     * <p>
     * 使用 {@link BasicAWSCredentials} 和 {@link AmazonS3ClientBuilder} 创建 S3 客户端实例。
     * 如果配置了 {@code region} 则使用指定区域，否则默认使用 {@link Regions#DEFAULT_REGION}。
     * 客户端在构造后立即可用，调用方需自行管理客户端的生命周期
     * （如通过 Spring {@code @PreDestroy} 关闭）。
     * </p>
     *
     * @param properties S3 配置属性，必须包含 {@code accessKey}、{@code secretKey}、{@code bucket}
     * @throws IllegalArgumentException 如果 {@code properties} 为 {@code null}
     * @throws OssException              如果创建 S3 客户端失败
     */
    public S3OssStorageService(ZerxOssProperties properties) {
        super(properties);
        try {
            String accessKey = properties.getAccessKey();
            String secretKey = properties.getSecretKey();
            String regionStr = properties.getRegion();

            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

            Regions region = Regions.DEFAULT_REGION;
            if (StringUtil.isNotBlank(regionStr)) {
                region = Regions.fromName(regionStr);
            }

            this.s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(region)
                    .build();
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("初始化 AWS S3 客户端失败: " + e.getMessage());
        }
    }

    // ======================== 上传 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code s3Client.putObject(PutObjectRequest)} 上传对象。
     * 自定义元数据条目直接使用 {@code x-amz-meta-} 前缀（S3 原生格式，无需转换）。
     * </p>
     */
    @Override
    protected OssResult doPut(String objectKey, InputStream input, String contentType,
                              Map<String, String> metadata) {
        try {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            if (contentType != null) {
                objectMetadata.setContentType(contentType);
            }
            if (metadata != null) {
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith(META_PREFIX)) {
                        key = key.substring(META_PREFIX.length());
                    }
                    objectMetadata.addUserMetadata(key, entry.getValue());
                }
            }

            String bucket = properties.getBucket();
            PutObjectRequest putRequest = new PutObjectRequest(bucket, objectKey, input, objectMetadata);
            s3Client.putObject(putRequest);

            ObjectMetaHolder holder = statObject(objectKey);
            return new OssResult(
                    objectKey,
                    doBuildUrl(objectKey),
                    holder.originalFilename,
                    holder.size,
                    holder.contentType,
                    holder.etag,
                    holder.lastModified
            );
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("上传对象到 AWS S3 失败: " + objectKey);
        }
    }

    // ======================== 读取 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.getObjectMetadata(bucket, key)} 获取对象元数据，
     * 并从用户自定义元数据中提取原始文件名。
     * </p>
     */
    @Override
    protected OssObjectMeta doGetObjectMeta(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            ObjectMetadata meta = s3Client.getObjectMetadata(bucket, objectKey);

            String etag = stripEtagQuotes(meta.getETag());
            String contentType = meta.getContentType();
            long size = meta.getContentLength();
            Instant lastModified = meta.getLastModified().toInstant();

            Map<String, String> userMeta = meta.getUserMetaData();
            String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

            return new OssObjectMeta(objectKey, size, contentType, lastModified, etag, originalFilename);
        } catch (OssException e) {
            throw e;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw OssException.ossError("对象不存在: " + objectKey);
            }
            throw OssException.ossError("获取 AWS S3 对象元数据失败: " + objectKey);
        } catch (Exception e) {
            throw OssException.ossError("获取 AWS S3 对象元数据失败: " + objectKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.getObject(bucket, key)} 获取对象，封装为 {@link OssObject}。
     * 调用方必须在使用完毕后关闭返回的对象以释放底层 HTTP 连接。
     * </p>
     */
    @Override
    protected OssObject doGet(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            S3Object s3Object = s3Client.getObject(bucket, objectKey);
            ObjectMetadata meta = s3Object.getObjectMetadata();

            String etag = stripEtagQuotes(meta.getETag());
            String contentType = meta.getContentType();
            long size = meta.getContentLength();
            Instant lastModified = meta.getLastModified().toInstant();

            Map<String, String> userMeta = meta.getUserMetaData();
            String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

            OssObjectMeta ossObjectMeta = new OssObjectMeta(objectKey, size, contentType, lastModified, etag, originalFilename);
            return new OssObject(s3Object.getObjectContent(), ossObjectMeta);
        } catch (OssException e) {
            throw e;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                throw OssException.ossError("对象不存在: " + objectKey);
            }
            throw OssException.ossError("获取 AWS S3 对象失败: " + objectKey);
        } catch (Exception e) {
            throw OssException.ossError("获取 AWS S3 对象失败: " + objectKey);
        }
    }

    // ======================== 判断存在 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.doesObjectExist(bucket, key)} 判断对象是否存在。
     * </p>
     */
    @Override
    protected boolean doExists(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            return s3Client.doesObjectExist(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("检查 AWS S3 对象是否存在失败: " + objectKey);
        }
    }

    // ======================== 删除 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.deleteObject(bucket, key)} 删除单个对象。
     * 删除不存在的对象不会抛出异常（幂等操作）。
     * </p>
     */
    @Override
    protected void doDelete(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            s3Client.deleteObject(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("删除 AWS S3 对象失败: " + objectKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.deleteObjects(DeleteObjectsRequest)} 批量删除对象。
     * 返回实际成功删除的对象数量（基于 {@link DeleteObjectsResult#getDeletedObjects()} 计数）。
     * </p>
     */
    @Override
    protected int doDeleteBatch(List<String> objectKeys) {
        try {
            if (objectKeys == null || objectKeys.isEmpty()) {
                return 0;
            }
            String bucket = resolveBucketForKey(objectKeys.get(0));
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket);
            deleteRequest.setKeys(objectKeys);
            DeleteObjectsResult deleteResult = s3Client.deleteObjects(deleteRequest);
            return deleteResult.getDeletedObjects().size();
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("批量删除 AWS S3 对象失败");
        }
    }

    // ======================== 复制 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.copyObject(CopyObjectRequest)} 复制对象，
     * 然后对目标对象执行 head 以获取最新元数据。
     * 支持跨桶复制（如从暂存桶到主桶）。
     * </p>
     */
    @Override
    protected OssResult doCopy(String sourceKey, String targetKey) {
        try {
            String sourceBucket = resolveBucketForKey(sourceKey);
            String targetBucket = resolveBucketForKey(targetKey);

            CopyObjectRequest copyRequest = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
            s3Client.copyObject(copyRequest);

            ObjectMetaHolder holder = statObject(targetKey);
            return new OssResult(
                    targetKey,
                    doBuildUrl(targetKey),
                    holder.originalFilename,
                    holder.size,
                    holder.contentType,
                    holder.etag,
                    holder.lastModified
            );
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("复制 AWS S3 对象失败: " + sourceKey + " → " + targetKey);
        }
    }

    // ======================== 预签名 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code s3Client.generatePresignedUrl(bucket, key, expiry, HttpMethod.PUT)}
     * 生成预签名上传 URL。自定义元数据头直接使用 {@code x-amz-meta-} 前缀
     * （S3 原生格式，无需转换），通过 {@code putCustomRequestHeader} 加入签名计算。
     * </p>
     */
    @Override
    protected PresignedUrl doPresignPut(String objectKey, Duration expiry, Map<String, String> headers) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            Date expiryDate = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(expiryDate);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    request.putCustomRequestHeader(entry.getKey(), entry.getValue());
                }
            }

            URL url = s3Client.generatePresignedUrl(request);
            return new PresignedUrl(url.toString(), headers, expiryDate.toInstant());
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("生成 AWS S3 预签名上传 URL 失败: " + objectKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code s3Client.generatePresignedUrl(bucket, key, expiry, HttpMethod.GET)}
     * 生成预签名下载 URL。
     * </p>
     */
    @Override
    protected PresignedUrl doPresignGet(String objectKey, Duration expiry) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            Date expiryDate = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiryDate);

            URL url = s3Client.generatePresignedUrl(request);
            return new PresignedUrl(url.toString(), null, expiryDate.toInstant());
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("生成 AWS S3 预签名下载 URL 失败: " + objectKey);
        }
    }

    // ======================== URL 构建 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 如果配置了自定义域名，则使用 {@code {customDomain}/{objectKey}} 格式。
     * 否则使用 S3 虚拟托管式 URL：
     * {@code https://{bucket}.s3.{region}.amazonaws.com/{objectKey}}。
     * 如果 region 为空，则回退到 {@code {endpoint}/{objectKey}} 格式。
     * </p>
     */
    @Override
    protected String doBuildUrl(String objectKey) {
        String customDomain = properties.getCustomDomain();
        if (StringUtil.isNotBlank(customDomain)) {
            String domain = stripTrailingSlash(customDomain);
            return domain + "/" + objectKey;
        }
        String bucket = properties.getBucket();
        String region = properties.getRegion();
        if (StringUtil.isNotBlank(region)) {
            return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
        }
        // Fallback to endpoint-based URL
        String endpoint = stripTrailingSlash(properties.getEndpoint());
        if (StringUtil.isBlank(endpoint)) {
            throw OssException.ossError("AWS S3 endpoint 和 region 不能同时为空");
        }
        return "https://" + endpoint + "/" + objectKey;
    }

    // ======================== 暂存桶 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 如果配置了独立的暂存桶则返回暂存桶名称，否则返回主存储桶名称。
     * </p>
     */
    @Override
    protected String doGetStagingBucket() {
        String stagingBucket = properties.getStaging().getBucket();
        return StringUtil.isNotBlank(stagingBucket) ? stagingBucket : properties.getBucket();
    }

    // ======================== 用户元数据 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code s3Client.getObjectMetadata(bucket, key).getUserMetaData()} 获取
     * S3 用户自定义元数据。返回的 Map 中键名自动添加 {@code x-amz-meta-} 前缀，
     * 以保持与模板层的一致性。
     * </p>
     */
    @Override
    protected Map<String, String> doGetUserMetadata(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            ObjectMetadata meta = s3Client.getObjectMetadata(bucket, objectKey);
            Map<String, String> userMeta = meta.getUserMetaData();
            Map<String, String> result = new HashMap<>();
            if (userMeta != null) {
                for (Map.Entry<String, String> entry : userMeta.entrySet()) {
                    result.put(META_PREFIX + entry.getKey(), entry.getValue());
                }
            }
            return result;
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("获取 AWS S3 用户元数据失败: " + objectKey);
        }
    }

    // ======================== 清理过期暂存 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code s3Client.listObjectsV2} 列出暂存前缀下的所有对象，
     * 过滤出最后修改时间早于阈值（{@code Instant.now() - olderThan}）的对象，
     * 分批调用 {@link #doDeleteBatch} 进行删除。
     * </p>
     */
    @Override
    public int purgeExpiredStages(Duration olderThan) {
        try {
            Instant cutoff = Instant.now().minus(olderThan);
            String bucket = doGetStagingBucket();
            String prefix = properties.getStaging().getPrefix();

            List<String> expiredKeys = new ArrayList<>();
            String continuationToken = null;

            do {
                ListObjectsV2Request listRequest = new ListObjectsV2Request();
                listRequest.setBucketName(bucket);
                listRequest.setPrefix(prefix);
                listRequest.setMaxKeys(1000);
                if (continuationToken != null) {
                    listRequest.setContinuationToken(continuationToken);
                }

                ListObjectsV2Result listResult = s3Client.listObjectsV2(listRequest);

                for (S3ObjectSummary summary : listResult.getObjectSummaries()) {
                    Instant lastModified = summary.getLastModified().toInstant();
                    if (lastModified.isBefore(cutoff)) {
                        expiredKeys.add(summary.getKey());
                    }
                }

                continuationToken = listResult.isTruncated() ? listResult.getNextContinuationToken() : null;
            } while (continuationToken != null);

            if (expiredKeys.isEmpty()) {
                return 0;
            }

            return doDeleteBatch(expiredKeys);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("清理 AWS S3 过期暂存记录失败");
        }
    }

    // ======================== 内部工具方法 ========================

    /**
     * 获取对象的元数据信息。
     * <p>
     * 查询主桶中指定对象键的元数据，提取 ETag、大小、内容类型、修改时间和原始文件名。
     * </p>
     *
     * @param objectKey 对象键
     * @return 元数据持有者对象
     */
    private ObjectMetaHolder statObject(String objectKey) {
        String bucket = properties.getBucket();
        ObjectMetadata meta = s3Client.getObjectMetadata(bucket, objectKey);

        String etag = stripEtagQuotes(meta.getETag());
        String contentType = meta.getContentType();
        long size = meta.getContentLength();
        Instant lastModified = meta.getLastModified().toInstant();

        Map<String, String> userMeta = meta.getUserMetaData();
        String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

        return new ObjectMetaHolder(etag, contentType, size, lastModified, originalFilename);
    }

    /**
     * 根据对象键解析应使用的存储桶。
     * <p>
     * 如果对象键以暂存前缀开头且配置了独立的暂存桶，则使用暂存桶；
     * 否则使用主存储桶。
     * </p>
     *
     * @param objectKey 对象键
     * @return 存储桶名称
     */
    private String resolveBucketForKey(String objectKey) {
        String stagingPrefix = properties.getStaging().getPrefix();
        String stagingBucket = properties.getStaging().getBucket();
        if (objectKey != null && objectKey.startsWith(stagingPrefix)
                && StringUtil.isNotBlank(stagingBucket)) {
            return stagingBucket;
        }
        return properties.getBucket();
    }

    /**
     * 去除 ETag 值两端的引号。
     * <p>
     * AWS S3 返回的 ETag 可能包含双引号（如 {@code "abc123def456"}），
     * 需要去除以便统一存储和使用。
     * </p>
     *
     * @param etag 原始 ETag 值
     * @return 去除引号后的 ETag 值，如果输入为 {@code null} 则返回 {@code null}
     */
    private static String stripEtagQuotes(String etag) {
        if (etag == null) {
            return null;
        }
        String trimmed = etag.trim();
        if (trimmed.length() >= 2
                && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 去除字符串末尾的斜杠。
     *
     * @param value 原始字符串
     * @return 去除末尾斜杠后的字符串
     */
    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * 对象元数据内部持有者。
     * <p>
     * 用于在方法间传递对象元数据的提取结果，避免多次查询。
     * </p>
     *
     * @param etag              ETag 值（已去除引号）
     * @param contentType       MIME 类型
     * @param size              文件大小（字节）
     * @param lastModified      最后修改时间（UTC）
     * @param originalFilename  原始文件名（从用户元数据中提取）
     */
    private record ObjectMetaHolder(String etag, String contentType, long size,
                                    Instant lastModified, String originalFilename) {
    }
}
