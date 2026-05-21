package com.zerx.component.oss.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.DeleteObjectsRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ListObjectsV2Request;
import com.aliyun.oss.model.ListObjectsV2Result;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectMetadata;
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
 * 阿里云 OSS 存储服务实现。
 * <p>
 * 基于 {@link AbstractOssStorageService} 抽象模板，使用阿里云 OSS SDK（aliyun-sdk-oss）
 * 实现所有对象存储操作。支持标准上传、预签名 URL、暂存流程、对象复制等全部功能。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   oss:
 *     type: ALIYUN
 *     endpoint: "oss-cn-hangzhou.aliyuncs.com"
 *     access-key: "LTAI..."
 *     secret-key: "..."
 *     bucket: "my-bucket"
 *     region: "cn-hangzhou"
 *     custom-domain: "https://cdn.example.com"
 *     staging:
 *       strategy: DIRECTORY
 *       prefix: "_staging/"
 *       default-ttl: 24h
 * }</pre>
 *
 * <h3>用户元数据：</h3>
 * <p>
 * 用户自定义元数据在存储时使用阿里云 {@code x-oss-meta-} 前缀，读取时自动适配回
 * 统一的 {@code x-amz-meta-} 前缀，对上层调用方透明。
 * </p>
 *
 * @author zerx
 * @see AbstractOssStorageService
 * @see ZerxOssProperties
 */
public class AliyunOssStorageService extends AbstractOssStorageService {

    /** 阿里云 OSS 用户元数据前缀 */
    private static final String VENDOR_META_PREFIX = "x-oss-meta-";

    /** 统一元数据前缀（模板层使用） */
    private static final String META_PREFIX = "x-amz-meta-";

    /** 阿里云 OSS 客户端 */
    private final OSS ossClient;

    /**
     * 构造阿里云 OSS 存储服务。
     *
     * @param properties 阿里云 OSS 配置属性
     * @throws IllegalArgumentException 如果 properties 为 {@code null}
     * @throws OssException              如果创建 OSS 客户端失败
     */
    public AliyunOssStorageService(ZerxOssProperties properties) {
        super(properties);
        try {
            String endpoint = properties.getEndpoint();
            String accessKey = properties.getAccessKey();
            String secretKey = properties.getSecretKey();
            this.ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        } catch (Exception e) {
            throw OssException.ossError("初始化阿里云 OSS 客户端失败: " + e.getMessage());
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

            ossClient.putObject(bucket, objectKey, input, objectMetadata);

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
            throw OssException.ossError("上传对象到阿里云 OSS 失败: " + objectKey);
        }
    }

    // ======================== 读取 ========================

    @Override
    protected OssObjectMeta doGetObjectMeta(String bucket, String objectKey) {
        try {
            ObjectMetadata meta = ossClient.getObjectMetadata(bucket, objectKey);

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
            throw OssException.ossError("获取阿里云 OSS 对象元数据失败: " + objectKey);
        }
    }

    @Override
    protected OssObject doGet(String bucket, String objectKey) {
        try {
            com.aliyun.oss.model.OSSObject aliOssObject = ossClient.getObject(bucket, objectKey);
            ObjectMetadata meta = aliOssObject.getObjectMetadata();

            String etag = stripEtagQuotes(meta.getETag());
            String contentType = meta.getContentType();
            long size = meta.getContentLength();
            Instant lastModified = meta.getLastModified().toInstant();

            Map<String, String> userMeta = meta.getUserMetaData();
            String originalFilename = (userMeta != null) ? userMeta.get("zerx-filename") : null;

            OssObjectMeta ossObjectMeta = new OssObjectMeta(objectKey, size, contentType, lastModified, etag, originalFilename);
            return new OssObject(aliOssObject.getObjectContent(), ossObjectMeta);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("获取阿里云 OSS 对象失败: " + objectKey);
        }
    }

    // ======================== 判断存在 ========================

    @Override
    protected boolean doExists(String bucket, String objectKey) {
        try {
            return ossClient.doesObjectExist(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("检查阿里云 OSS 对象是否存在失败: " + objectKey);
        }
    }

    // ======================== 删除 ========================

    @Override
    protected void doDelete(String bucket, String objectKey) {
        try {
            ossClient.deleteObject(bucket, objectKey);
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("删除阿里云 OSS 对象失败: " + objectKey);
        }
    }

    @Override
    protected int doDeleteBatch(String bucket, List<String> objectKeys) {
        try {
            if (objectKeys == null || objectKeys.isEmpty()) {
                return 0;
            }
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket).withKeys(objectKeys);
            ossClient.deleteObjects(deleteRequest);
            return objectKeys.size();
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("批量删除阿里云 OSS 对象失败");
        }
    }

    // ======================== 复制 ========================

    @Override
    protected OssResult doCopy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            CopyObjectRequest copyRequest = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
            ossClient.copyObject(copyRequest);

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
            throw OssException.ossError("复制阿里云 OSS 对象失败: " + sourceKey + " → " + targetKey);
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

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
            request.setMethod(com.aliyun.oss.common.utils.HttpMethod.PUT);
            request.setExpiration(expiryDate);

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    if (key.startsWith(META_PREFIX)) {
                        String vendorKey = VENDOR_META_PREFIX + key.substring(META_PREFIX.length());
                        request.addHeader(vendorKey, entry.getValue());
                    } else {
                        request.addHeader(key, entry.getValue());
                    }
                }
            }

            URL url = ossClient.generatePresignedUrl(request);
            return new PresignedUrl(url.toString(), headers, expiryDate.toInstant());
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("生成阿里云 OSS 预签名上传 URL 失败: " + objectKey);
        }
    }

    @Override
    protected PresignedUrl doPresignGet(String objectKey, Duration expiry) {
        try {
            String bucket = resolveMainBucket();
            Date expiryDate = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
            request.setMethod(com.aliyun.oss.common.utils.HttpMethod.GET);
            request.setExpiration(expiryDate);

            URL url = ossClient.generatePresignedUrl(request);
            return new PresignedUrl(url.toString(), null, expiryDate.toInstant());
        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            throw OssException.ossError("生成阿里云 OSS 预签名下载 URL 失败: " + objectKey);
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
        String endpoint = properties.getEndpoint();
        if (endpoint == null) {
            endpoint = "";
        }
        String cleanEndpoint = stripProtocolPrefix(stripTrailingSlash(endpoint));
        return "https://" + bucket + "." + cleanEndpoint + "/" + objectKey;
    }

    // ======================== 用户元数据 ========================

    @Override
    protected Map<String, String> doGetUserMetadata(String bucket, String objectKey) {
        try {
            ObjectMetadata meta = ossClient.getObjectMetadata(bucket, objectKey);
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
            throw OssException.ossError("获取阿里云 OSS 用户元数据失败: " + objectKey);
        }
    }

    // ======================== 清理过期暂存 ========================

    @Override
    protected int doPurgeExpired(String bucket, String prefix, Instant cutoff) {
        try {
            List<String> expiredKeys = new ArrayList<>();
            String continuationToken = null;

            do {
                ListObjectsV2Request listRequest = new ListObjectsV2Request(bucket);
                listRequest.setPrefix(prefix);
                listRequest.setMaxKeys(1000);
                if (continuationToken != null) {
                    listRequest.setContinuationToken(continuationToken);
                }

                ListObjectsV2Result listResult = ossClient.listObjectsV2(listRequest);

                for (OSSObjectSummary summary : listResult.getObjectSummaries()) {
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
            throw OssException.ossError("清理阿里云 OSS 过期暂存记录失败");
        }
    }

    // ======================== 内部工具方法 ========================

    private ObjectMetaHolder statObject(String bucket, String objectKey) {
        ObjectMetadata meta = ossClient.getObjectMetadata(bucket, objectKey);

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

    private static String stripProtocolPrefix(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        if (endpoint.startsWith("https://")) {
            return endpoint.substring("https://".length());
        }
        if (endpoint.startsWith("http://")) {
            return endpoint.substring("http://".length());
        }
        return endpoint;
    }

    private record ObjectMetaHolder(String etag, String contentType, long size,
                                    Instant lastModified, String originalFilename) {
    }
}
