package com.zerx.component.oss.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.DeleteObjectsRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ListObjectsV2Request;
import com.qcloud.cos.model.ListObjectsV2Result;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.S3ObjectSummary;
import com.qcloud.cos.region.Region;
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
 * 腾讯云 COS 存储服务实现。
 * <p>
 * 基于 {@link AbstractOssStorageService} 抽象模板，使用腾讯云 COS SDK
 * 实现所有对象存储操作。支持标准上传、预签名 URL、暂存流程、对象复制等全部功能。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   oss:
 *     type: TENCENT
 *     endpoint: "cos.ap-guangzhou.myqcloud.com"
 *     access-key: "AKID..."
 *     secret-key: "..."
 *     bucket: "my-bucket-1250000000"
 *     region: "ap-guangzhou"
 *     custom-domain: "https://cdn.example.com"   # 可选
 *     staging:
 *       prefix: "_staging/"
 *       default-ttl: 24h
 * }</pre>
 *
 * <h3>URL 构建：</h3>
 * <p>
 * 未配置自定义域名时，使用腾讯云标准格式：
 * {@code https://{bucket}.cos.{region}.myqcloud.com/{objectKey}}。
 * 如果 region 为空，则回退到 {@code {endpoint}/{objectKey}} 格式。
 * </p>
 *
 * <h3>用户元数据：</h3>
 * <p>
 * 用户自定义元数据在存储时使用腾讯云 {@code x-cos-meta-} 前缀，读取时自动适配回
 * 统一的 {@code x-amz-meta-} 前缀，对上层调用方透明。
 * </p>
 *
 * @author zerx
 * @see AbstractOssStorageService
 * @see ZerxOssProperties
 */
public class TencentCosStorageService extends AbstractOssStorageService {

    /** 腾讯云 COS 用户元数据前缀 */
    private static final String VENDOR_META_PREFIX = "x-cos-meta-";

    /** 统一元数据前缀（模板层使用） */
    private static final String META_PREFIX = "x-amz-meta-";

    /** 腾讯云 COS 客户端 */
    private final COSClient cosClient;

    /**
     * 构造腾讯云 COS 存储服务。
     * <p>
     * 使用 {@link BasicCOSCredentials} 和 {@link ClientConfig} 创建 COS 客户端实例。
     * 必须在配置中提供 {@code region} 参数。客户端在构造后立即可用，
     * 调用方需自行管理客户端的生命周期（如通过 Spring {@code @PreDestroy} 关闭）。
     * </p>
     *
     * @param properties 腾讯云 COS 配置属性，必须包含 {@code endpoint}、{@code accessKey}、
     *                   {@code secretKey}、{@code bucket}、{@code region}
     * @throws IllegalArgumentException 如果 {@code properties} 为 {@code null}
     * @throws OssException              如果创建 COS 客户端失败（如 region 为空）
     */
    public TencentCosStorageService(ZerxOssProperties properties) {
        super(properties);
        try {
            String accessKey = properties.getAccessKey();
            String secretKey = properties.getSecretKey();
            String regionStr = properties.getRegion();

            if (StringUtil.isBlank(regionStr)) {
                throw OssException.ossError("腾讯云 COS region 不能为空");
            }

            COSCredentials cred = new BasicCOSCredentials(accessKey, secretKey);
            Region region = Region.valueOf(regionStr);
            ClientConfig clientConfig = new ClientConfig(region);
            this.cosClient = new COSClient(cred, clientConfig);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("初始化腾讯云 COS 客户端失败: " + e.getMessage());
        }
    }

    // ======================== 上传 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code cosClient.putObject(PutObjectRequest)} 上传对象。
     * 自定义元数据条目中 {@code x-amz-meta-} 前缀会自动转换为腾讯云的 {@code x-cos-meta-} 前缀。
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
            cosClient.putObject(putRequest);

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
            throw OssException.ossError("上传对象到腾讯云 COS 失败: " + objectKey);
        }
    }

    // ======================== 读取 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code cosClient.getObjectMetadata(bucket, key)} 获取对象元数据，
     * 并从用户自定义元数据中提取原始文件名。
     * </p>
     */
    @Override
    protected OssObjectMeta doGetObjectMeta(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            ObjectMetadata meta = cosClient.getObjectMetadata(bucket, objectKey);

            String etag = stripEtagQuotes(meta.getETag());
            String contentType = meta.getContentType();
            long size = meta.getContentLength();
            Instant lastModified = meta.getLastModified().toInstant();

            Map<String, String> userMeta = meta.getUserMetaData();
            String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

            return new OssObjectMeta(objectKey, size, contentType, lastModified, etag, originalFilename);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("获取腾讯云 COS 对象元数据失败: " + objectKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code cosClient.getObject(GetObjectRequest)} 获取对象，封装为 {@link OssObject}。
     * 调用方必须在使用完毕后关闭返回的对象以释放底层 HTTP 连接。
     * </p>
     */
    @Override
    protected OssObject doGet(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            GetObjectRequest getRequest = new GetObjectRequest(bucket, objectKey);
            COSObject cosObject = cosClient.getObject(getRequest);
            ObjectMetadata meta = cosObject.getObjectMetadata();

            String etag = stripEtagQuotes(meta.getETag());
            String contentType = meta.getContentType();
            long size = meta.getContentLength();
            Instant lastModified = meta.getLastModified().toInstant();

            Map<String, String> userMeta = meta.getUserMetaData();
            String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

            OssObjectMeta ossObjectMeta = new OssObjectMeta(objectKey, size, contentType, lastModified, etag, originalFilename);
            return new OssObject(cosObject.getObjectContent(), ossObjectMeta);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("获取腾讯云 COS 对象失败: " + objectKey);
        }
    }

    // ======================== 判断存在 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code cosClient.doesObjectExist(bucket, key)} 判断对象是否存在。
     * </p>
     */
    @Override
    protected boolean doExists(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            return cosClient.doesObjectExist(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("检查腾讯云 COS 对象是否存在失败: " + objectKey);
        }
    }

    // ======================== 删除 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code cosClient.deleteObject(bucket, key)} 删除单个对象。
     * 删除不存在的对象不会抛出异常（幂等操作）。
     * </p>
     */
    @Override
    protected void doDelete(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            cosClient.deleteObject(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("删除腾讯云 COS 对象失败: " + objectKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code cosClient.deleteObjects(DeleteObjectsRequest)} 批量删除对象。
     * 返回实际请求删除的对象数量。
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
            cosClient.deleteObjects(deleteRequest);
            return objectKeys.size();
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("批量删除腾讯云 COS 对象失败");
        }
    }

    // ======================== 复制 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 通过 {@code cosClient.copyObject(CopyObjectRequest)} 复制对象，
     * 然后对目标对象执行 head 以获取最新元数据。
     * 支持跨桶复制（如从暂存桶到主桶）。
     * </p>
     */
    @Override
    protected OssResult doCopy(String sourceKey, String targetKey) {
        try {
            String sourceBucket = resolveBucketForKey(sourceKey);
            String targetBucket = resolveBucketForKey(targetKey);

            CopyObjectRequest copyRequest = new CopyObjectRequest(
                    sourceBucket, sourceKey, targetBucket, targetKey);
            cosClient.copyObject(copyRequest);

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
            throw OssException.ossError("复制腾讯云 COS 对象失败: " + sourceKey + " → " + targetKey);
        }
    }

    // ======================== 预签名 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code cosClient.generatePresignedUrl(GeneratePresignedUrlRequest)} 生成
     * 预签名上传 URL。自定义元数据头的 {@code x-amz-meta-} 前缀会自动转换为
     * 腾讯云的 {@code x-cos-meta-} 前缀以保证签名校验通过。
     * </p>
     */
    @Override
    protected PresignedUrl doPresignPut(String objectKey, Duration expiry, Map<String, String> headers) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            Date expiryDate = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey, HttpMethodName.PUT);
            request.setExpiration(expiryDate);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith(META_PREFIX)) {
                        String vendorKey = VENDOR_META_PREFIX + key.substring(META_PREFIX.length());
                        request.putCustomHeader(vendorKey, entry.getValue());
                    } else {
                        request.putCustomHeader(key, entry.getValue());
                    }
                }
            }

            URL url = cosClient.generatePresignedUrl(request);
            return new PresignedUrl(url.toString(), headers, expiryDate.toInstant());
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("生成腾讯云 COS 预签名上传 URL 失败: " + objectKey);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code cosClient.generatePresignedUrl(GeneratePresignedUrlRequest)} 生成
     * 预签名下载 URL。
     * </p>
     */
    @Override
    protected PresignedUrl doPresignGet(String objectKey, Duration expiry) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            Date expiryDate = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey, HttpMethodName.GET);
            request.setExpiration(expiryDate);

            URL url = cosClient.generatePresignedUrl(request);
            return new PresignedUrl(url.toString(), null, expiryDate.toInstant());
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("生成腾讯云 COS 预签名下载 URL 失败: " + objectKey);
        }
    }

    // ======================== URL 构建 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 如果配置了自定义域名，则使用 {@code {customDomain}/{objectKey}} 格式。
     * 否则使用腾讯云标准格式：
     * {@code https://{bucket}.cos.{region}.myqcloud.com/{objectKey}}。
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
            return "https://" + bucket + ".cos." + region + ".myqcloud.com/" + objectKey;
        }
        // Fallback to endpoint-based URL
        String endpoint = stripTrailingSlash(properties.getEndpoint());
        if (StringUtil.isBlank(endpoint)) {
            throw OssException.ossError("腾讯云 COS endpoint 和 region 不能同时为空");
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
     * 通过 {@code cosClient.getObjectMetadata(bucket, key).getUserMetaData()} 获取
     * 腾讯云用户自定义元数据。返回的 Map 中键名自动添加 {@code x-amz-meta-} 前缀，
     * 以保持与模板层的一致性。
     * </p>
     */
    @Override
    protected Map<String, String> doGetUserMetadata(String objectKey) {
        try {
            String bucket = resolveBucketForKey(objectKey);
            ObjectMetadata meta = cosClient.getObjectMetadata(bucket, objectKey);
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
            throw OssException.ossError("获取腾讯云 COS 用户元数据失败: " + objectKey);
        }
    }

    // ======================== 清理过期暂存 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 使用 {@code cosClient.listObjectsV2} 列出暂存前缀下的所有对象，
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

                ListObjectsV2Result listResult = cosClient.listObjectsV2(listRequest);

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
            throw OssException.ossError("清理腾讯云 COS 过期暂存记录失败");
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
        ObjectMetadata meta = cosClient.getObjectMetadata(bucket, objectKey);

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
     * 腾讯云 COS 返回的 ETag 可能包含双引号（如 {@code "abc123def456"}），
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
