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
 *     endpoint: "s3.us-east-1.amazonaws.com"
 *     access-key: "AKIA..."
 *     secret-key: "..."
 *     bucket: "my-bucket"
 *     region: "us-east-1"
 *     custom-domain: "https://cdn.example.com"
 *     staging:
 *       strategy: DIRECTORY
 *       prefix: "_staging/"
 *       default-ttl: 24h
 * }</pre>
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
     *
     * @param properties S3 配置属性
     * @throws IllegalArgumentException 如果 properties 为 {@code null}
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

    @Override
    protected OssResult doPut(String bucket, String objectKey, InputStream input, String contentType,
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

            PutObjectRequest putRequest = new PutObjectRequest(bucket, objectKey, input, objectMetadata);
            s3Client.putObject(putRequest);

            ObjectMetaHolder holder = statObject(bucket, objectKey);
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

    @Override
    protected OssObjectMeta doGetObjectMeta(String bucket, String objectKey) {
        try {
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

    @Override
    protected OssObject doGet(String bucket, String objectKey) {
        try {
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

    @Override
    protected boolean doExists(String bucket, String objectKey) {
        try {
            return s3Client.doesObjectExist(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("检查 AWS S3 对象是否存在失败: " + objectKey);
        }
    }

    // ======================== 删除 ========================

    @Override
    protected void doDelete(String bucket, String objectKey) {
        try {
            s3Client.deleteObject(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("删除 AWS S3 对象失败: " + objectKey);
        }
    }

    @Override
    protected int doDeleteBatch(String bucket, List<String> objectKeys) {
        try {
            if (objectKeys == null || objectKeys.isEmpty()) {
                return 0;
            }
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

    @Override
    protected OssResult doCopy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            CopyObjectRequest copyRequest = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
            s3Client.copyObject(copyRequest);

            ObjectMetaHolder holder = statObject(targetBucket, targetKey);
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

    @Override
    protected PresignedUrl doPresignPut(String objectKey, Duration expiry, Map<String, String> headers) {
        return doPresignPut(objectKey, expiry, headers, resolveMainBucket());
    }

    @Override
    protected PresignedUrl doPresignPut(String objectKey, Duration expiry,
                                        Map<String, String> headers, String bucket) {
        try {
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

    @Override
    protected PresignedUrl doPresignGet(String objectKey, Duration expiry) {
        try {
            String bucket = resolveMainBucket();
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
        String endpoint = stripTrailingSlash(properties.getEndpoint());
        if (StringUtil.isBlank(endpoint)) {
            throw OssException.ossError("AWS S3 endpoint 和 region 不能同时为空");
        }
        return "https://" + endpoint + "/" + objectKey;
    }

    // ======================== 用户元数据 ========================

    @Override
    protected Map<String, String> doGetUserMetadata(String bucket, String objectKey) {
        try {
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

    @Override
    protected int doPurgeExpired(String bucket, String prefix, Instant cutoff) {
        try {
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

            return doDeleteBatch(bucket, expiredKeys);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("清理 AWS S3 过期暂存记录失败");
        }
    }

    // ======================== 内部工具方法 ========================

    private ObjectMetaHolder statObject(String bucket, String objectKey) {
        ObjectMetadata meta = s3Client.getObjectMetadata(bucket, objectKey);

        String etag = stripEtagQuotes(meta.getETag());
        String contentType = meta.getContentType();
        long size = meta.getContentLength();
        Instant lastModified = meta.getLastModified().toInstant();

        Map<String, String> userMeta = meta.getUserMetaData();
        String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

        return new ObjectMetaHolder(etag, contentType, size, lastModified, originalFilename);
    }

    private static String stripEtagQuotes(String etag) {
        if (etag == null) {
            return null;
        }
        String trimmed = etag.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record ObjectMetaHolder(String etag, String contentType, long size,
                                    Instant lastModified, String originalFilename) {
    }
}
