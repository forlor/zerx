package com.zerx.component.oss.impl;

import com.zerx.common.util.StringUtil;
import com.zerx.component.oss.OssException;
import com.zerx.component.oss.OssObject;
import com.zerx.component.oss.OssObjectMeta;
import com.zerx.component.oss.OssResult;
import com.zerx.component.oss.PresignedUrl;
import com.zerx.component.oss.properties.ZerxOssProperties;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.Item;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 MinIO 的对象存储服务实现。
 * <p>
 * 使用 <a href="https://min.io">MinIO</a> Java SDK ({@code io.minio}) 实现对象存储操作，
 * 适用于 MinIO Server 以及兼容 S3 协议的对象存储服务。通过 Spring Boot 自动配置
 * 在 {@code zerx.oss.type=MINIO} 时自动注册为 {@link com.zerx.component.oss.OssStorageService} Bean。
 * </p>
 *
 * <h3>依赖：</h3>
 * <ul>
 *   <li>MinIO Java SDK 8.x ({@code io.minio:minio})</li>
 *   <li>JDK 21+</li>
 * </ul>
 *
 * <h3>暂存策略：</h3>
 * <p>
 * 支持两种暂存隔离策略（通过 {@code zerx.oss.staging.strategy} 配置）：
 * <ul>
 *   <li><b>DIRECTORY</b>（默认）：同桶目录隔离，暂存文件在主桶的 {@code _staging/} 前缀下</li>
 *   <li><b>BUCKET</b>：独立桶隔离，暂存文件存放在独立的暂存桶中</li>
 * </ul>
 * </p>
 *
 * <h3>元数据处理：</h3>
 * <p>
 * MinIO SDK 的 {@code userMetadata()} 方法会自动添加/去除 {@code x-amz-meta-} 前缀：
 * <ul>
 *   <li><b>写入时</b>：传入不含前缀的键（如 {@code "zerx-filename"}），SDK 自动添加前缀</li>
 *   <li><b>读取时</b>：返回不含前缀的键（如 {@code "zerx-filename"}）</li>
 * </ul>
 * 本类负责在基类的带前缀格式（如 {@code "x-amz-meta-zerx-filename"}）与 MinIO SDK 的无前缀格式之间转换。
 * </p>
 *
 * <h3>ETag 格式：</h3>
 * <p>
 * MinIO 返回的 ETag 通常包含双引号（如 {@code "d41d8cd98f00b204e9800998ecf8427e"}），
 * 本类会自动去除首尾引号以保持与其他实现的兼容性。
 * </p>
 *
 * @author zerx
 * @see AbstractOssStorageService
 * @see com.zerx.component.oss.OssStorageService
 * @see ZerxOssProperties
 * @see MinioClient
 */
public class MinioOssStorageService extends AbstractOssStorageService {

    /** MinIO 元数据自动添加的 HTTP 头前缀 */
    private static final String X_AMZ_META_PREFIX = "x-amz-meta-";

    /** 多部分上传最小分片大小（5 MiB），MinIO SDK 常量 */
    private static final long MIN_PART_SIZE = 5 * 1024 * 1024;

    /** MinIO 客户端实例，线程安全 */
    private final MinioClient minioClient;

    /**
     * 构造 MinIO 对象存储服务。
     * <p>
     * 根据 {@link ZerxOssProperties} 中的配置初始化 {@link MinioClient}，
     * 包括 endpoint、accessKey、secretKey 和可选的 region。
     * </p>
     *
     * @param properties 对象存储配置属性，不能为 {@code null}
     * @throws IllegalArgumentException 如果 properties 为 {@code null}
     */
    public MinioOssStorageService(ZerxOssProperties properties) {
        super(properties);

        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey());

        if (StringUtil.isNotBlank(properties.getRegion())) {
            builder.region(properties.getRegion());
        }

        this.minioClient = builder.build();
    }

    // ======================== 上传 ========================

    @Override
    protected OssResult doPut(String bucket, String objectKey, InputStream input, String contentType,
                              Map<String, String> metadata) {
        try {
            Map<String, String> userMetadata = stripMetaPrefix(metadata);

            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(input, -1, MIN_PART_SIZE)
                    .userMetadata(userMetadata);

            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }

            ObjectWriteResponse response = minioClient.putObject(builder.build());

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());

            String etag = stripQuotes(response.etag());
            String originalFilename = metadata != null ? metadata.get(META_FILENAME) : null;

            return new OssResult(
                    objectKey,
                    doBuildUrl(objectKey),
                    originalFilename,
                    stat.size(),
                    stat.contentType(),
                    etag,
                    stat.lastModified().toInstant()
            );
        } catch (Exception e) {
            throw OssException.ossError("上传对象失败: " + objectKey, e);
        }
    }

    // ======================== 元数据 ========================

    @Override
    protected OssObjectMeta doGetObjectMeta(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());

            String etag = stripQuotes(stat.etag());
            String originalFilename = extractOriginalFilename(stat.userMetadata());

            return new OssObjectMeta(
                    objectKey,
                    stat.size(),
                    stat.contentType(),
                    stat.lastModified().toInstant(),
                    etag,
                    originalFilename
            );
        } catch (Exception e) {
            throw OssException.ossError("获取对象元数据失败: " + objectKey, e);
        }
    }

    @Override
    protected OssObject doGet(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());

            String etag = stripQuotes(stat.etag());
            String originalFilename = extractOriginalFilename(stat.userMetadata());

            OssObjectMeta meta = new OssObjectMeta(
                    objectKey,
                    stat.size(),
                    stat.contentType(),
                    stat.lastModified().toInstant(),
                    etag,
                    originalFilename
            );

            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());

            return new OssObject(response, meta);
        } catch (Exception e) {
            throw OssException.ossError("获取对象失败: " + objectKey, e);
        }
    }

    // ======================== 存在性检查 ========================

    @Override
    protected boolean doExists(String bucket, String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== 删除 ========================

    @Override
    protected void doDelete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            throw OssException.ossError("删除对象失败: " + objectKey, e);
        }
    }

    @Override
    protected int doDeleteBatch(String bucket, List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return 0;
        }

        try {
            List<DeleteObject> deleteObjects = new ArrayList<>(objectKeys.size());
            for (String objectKey : objectKeys) {
                deleteObjects.add(new DeleteObject(objectKey));
            }

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucket)
                            .objects(deleteObjects)
                            .build());

            int errorCount = 0;
            for (Result<DeleteError> result : results) {
                try {
                    result.get();
                    errorCount++;
                } catch (Exception e) {
                    errorCount++;
                }
            }

            return deleteObjects.size() - errorCount;
        } catch (Exception e) {
            throw OssException.ossError("批量删除对象失败", e);
        }
    }

    // ======================== 复制 ========================

    @Override
    protected OssResult doCopy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(sourceBucket)
                                    .object(sourceKey)
                                    .build())
                            .bucket(targetBucket)
                            .object(targetKey)
                            .build());

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(targetBucket)
                            .object(targetKey)
                            .build());

            String etag = stripQuotes(stat.etag());
            String originalFilename = extractOriginalFilename(stat.userMetadata());

            return new OssResult(
                    targetKey,
                    doBuildUrl(targetKey),
                    originalFilename,
                    stat.size(),
                    stat.contentType(),
                    etag,
                    stat.lastModified().toInstant()
            );
        } catch (Exception e) {
            throw OssException.ossError("复制对象失败: " + sourceKey + " -> " + targetKey, e);
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
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(Math.max(1, (int) expiry.getSeconds()));

            if (headers != null && !headers.isEmpty()) {
                builder.extraHeaders(headers);
            }

            String url = minioClient.getPresignedObjectUrl(builder.build());
            Instant expiresAt = Instant.now().plus(expiry);

            return new PresignedUrl(url, headers, expiresAt);
        } catch (Exception e) {
            throw OssException.ossError("生成预签名上传 URL 失败: " + objectKey, e);
        }
    }

    @Override
    protected PresignedUrl doPresignGet(String objectKey, Duration expiry) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(resolveMainBucket())
                            .object(objectKey)
                            .expiry(Math.max(1, (int) expiry.getSeconds()))
                            .build());

            Instant expiresAt = Instant.now().plus(expiry);

            return new PresignedUrl(url, null, expiresAt);
        } catch (Exception e) {
            throw OssException.ossError("生成预签名下载 URL 失败: " + objectKey, e);
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

        String endpoint = stripTrailingSlash(properties.getEndpoint());
        return endpoint + "/" + properties.getBucket() + "/" + objectKey;
    }

    // ======================== 用户元数据 ========================

    @Override
    protected Map<String, String> doGetUserMetadata(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());

            Map<String, String> raw = stat.userMetadata();
            return addMetaPrefix(raw);
        } catch (Exception e) {
            throw OssException.ossError("获取对象用户元数据失败: " + objectKey, e);
        }
    }

    // ======================== 过期暂存清理 ========================

    @Override
    protected int doPurgeExpired(String bucket, String prefix, Instant cutoff) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .recursive(true)
                            .build());

            List<DeleteObject> toDelete = new ArrayList<>();

            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    if (item.lastModified() != null
                            && item.lastModified().toInstant().isBefore(cutoff)) {
                        toDelete.add(new DeleteObject(item.objectName()));
                    }
                } catch (Exception e) {
                    // 跳过无法读取的单个条目，继续处理其余对象
                }
            }

            if (toDelete.isEmpty()) {
                return 0;
            }

            Iterable<Result<DeleteError>> deleteResults = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucket)
                            .objects(toDelete)
                            .build());

            int errorCount = 0;
            for (Result<DeleteError> deleteResult : deleteResults) {
                try {
                    deleteResult.get();
                    errorCount++;
                } catch (Exception e) {
                    errorCount++;
                }
            }

            return toDelete.size() - errorCount;
        } catch (Exception e) {
            throw OssException.ossError("清理过期暂存文件失败", e);
        }
    }

    // ======================== 内部工具方法 ========================

    /**
     * 从用户元数据中提取原始文件名。
     *
     * @param userMetadata MinIO 返回的用户元数据（键不含前缀）
     * @return 原始文件名，未设置时返回 {@code null}
     */
    private String extractOriginalFilename(Map<String, String> userMetadata) {
        if (userMetadata == null) {
            return null;
        }
        return userMetadata.get("zerx-filename");
    }

    /**
     * 去除元数据键的 {@code x-amz-meta-} 前缀。
     *
     * @param metadata 原始元数据映射（键含前缀），可为 {@code null}
     * @return 去除前缀后的元数据映射
     */
    private Map<String, String> stripMetaPrefix(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>(metadata.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(X_AMZ_META_PREFIX)) {
                key = key.substring(X_AMZ_META_PREFIX.length());
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    /**
     * 为元数据键添加 {@code x-amz-meta-} 前缀。
     *
     * @param userMetadata MinIO 返回的用户元数据（键不含前缀），可为 {@code null}
     * @return 添加前缀后的元数据映射
     */
    private Map<String, String> addMetaPrefix(Map<String, String> userMetadata) {
        if (userMetadata == null || userMetadata.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> result = new HashMap<>(userMetadata.size());
        for (Map.Entry<String, String> entry : userMetadata.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(X_AMZ_META_PREFIX)) {
                key = X_AMZ_META_PREFIX + key;
            }
            result.put(key, entry.getValue());
        }
        return result;
    }

    /**
     * 去除 MinIO ETag 值的首尾双引号。
     */
    private String stripQuotes(String etag) {
        if (etag == null) {
            return null;
        }
        if (etag.length() >= 2 && etag.startsWith("\"") && etag.endsWith("\"")) {
            return etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    /**
     * 移除字符串末尾的斜杠。
     */
    private String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
