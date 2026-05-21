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
 * <h3>存储桶选择策略：</h3>
 * <p>
 * 如果配置了独立的暂存桶（{@code zerx.oss.staging.bucket}），则以暂存前缀开头的对象键
 * （如 {@code _staging/xxx}）会被路由到暂存桶，其余操作使用主存储桶。
 * 未配置暂存桶时，所有对象存储在主存储桶中，通过目录前缀区分暂存与正式文件。
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

    /**
     * 上传对象到 MinIO 存储。
     * <p>
     * 使用 {@link MinioClient#putObject(PutObjectArgs)} 上传文件流。
     * 上传完成后通过 {@code statObject} 获取完整的对象元数据（大小、最后修改时间等），
     * ETag 从上传响应中提取。
     * </p>
     *
     * @param objectKey   对象键
     * @param input       文件内容输入流
     * @param contentType MIME 类型，可能为 {@code null}
     * @param metadata    自定义元数据（键已包含 {@code x-amz-meta-} 前缀）
     * @return 上传结果
     * @throws OssException 上传失败时抛出
     */
    @Override
    protected OssResult doPut(String objectKey, InputStream input, String contentType,
                              Map<String, String> metadata) {
        try {
            Map<String, String> userMetadata = stripMetaPrefix(metadata);

            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(resolveBucket(objectKey))
                    .object(objectKey)
                    .stream(input, -1, MIN_PART_SIZE)
                    .userMetadata(userMetadata);

            if (contentType != null && !contentType.isBlank()) {
                builder.contentType(contentType);
            }

            ObjectWriteResponse response = minioClient.putObject(builder.build());

            // Stat 以获取完整的元数据（size、lastModified 等）
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(resolveBucket(objectKey))
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

    /**
     * 获取对象元数据。
     * <p>
     * 使用 {@link MinioClient#statObject(StatObjectArgs)} 获取对象的
     * 大小、Content-Type、ETag、最后修改时间以及原始文件名（从用户元数据中提取）。
     * </p>
     *
     * @param objectKey 对象键
     * @return 对象元数据
     * @throws OssException 对象不存在或获取元数据失败时抛出
     */
    @Override
    protected OssObjectMeta doGetObjectMeta(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(resolveBucket(objectKey))
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

    /**
     * 获取对象内容和元数据。
     * <p>
     * 先通过 {@code statObject} 获取元数据，再通过 {@link MinioClient#getObject(GetObjectArgs)}
     * 获取内容流。返回的 {@link OssObject} 持有底层 HTTP 连接，调用方必须在使用后关闭。
     * </p>
     *
     * @param objectKey 对象键
     * @return 包含输入流和元数据的对象，调用方必须关闭
     * @throws OssException 对象不存在或获取失败时抛出
     */
    @Override
    protected OssObject doGet(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(resolveBucket(objectKey))
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
                            .bucket(resolveBucket(objectKey))
                            .object(objectKey)
                            .build());

            return new OssObject(response, meta);
        } catch (Exception e) {
            throw OssException.ossError("获取对象失败: " + objectKey, e);
        }
    }

    // ======================== 存在性检查 ========================

    /**
     * 判断对象是否存在。
     * <p>
     * 通过 {@link MinioClient#statObject(StatObjectArgs)} 检测对象是否存在：
     * 无异常抛出则表示对象存在，捕获异常则返回 {@code false}。
     * </p>
     *
     * @param objectKey 对象键
     * @return 存在返回 {@code true}
     */
    @Override
    protected boolean doExists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(resolveBucket(objectKey))
                            .object(objectKey)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ======================== 删除 ========================

    /**
     * 删除单个对象。
     * <p>
     * 使用 {@link MinioClient#removeObject(RemoveObjectArgs)} 删除对象。
     * 删除不存在的对象不会抛出异常（幂等操作）。
     * </p>
     *
     * @param objectKey 对象键
     * @throws OssException 删除过程中发生 I/O 或权限错误时抛出
     */
    @Override
    protected void doDelete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(resolveBucket(objectKey))
                            .object(objectKey)
                            .build());
        } catch (Exception e) {
            throw OssException.ossError("删除对象失败: " + objectKey, e);
        }
    }

    /**
     * 批量删除对象。
     * <p>
     * 使用 {@link MinioClient#removeObjects(RemoveObjectsArgs)} 批量删除。
     * MinIO 的批量删除 API 仅返回失败的结果（{@link DeleteError}），
     * 因此成功删除数 = 总数 - 失败数。
     * </p>
     *
     * @param objectKeys 对象键列表
     * @return 实际成功删除的数量
     * @throws OssException 批量删除过程中发生严重错误时抛出
     */
    @Override
    protected int doDeleteBatch(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return 0;
        }

        try {
            // 按存储桶分组批量删除
            Map<String, List<DeleteObject>> bucketGroups = new HashMap<>();

            for (String objectKey : objectKeys) {
                String bucket = resolveBucket(objectKey);
                bucketGroups.computeIfAbsent(bucket, k -> new ArrayList<>())
                        .add(new DeleteObject(objectKey));
            }

            int totalDeleted = 0;

            for (Map.Entry<String, List<DeleteObject>> entry : bucketGroups.entrySet()) {
                String bucket = entry.getKey();
                List<DeleteObject> deleteObjects = entry.getValue();

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

                totalDeleted += deleteObjects.size() - errorCount;
            }

            return totalDeleted;
        } catch (Exception e) {
            throw OssException.ossError("批量删除对象失败", e);
        }
    }

    // ======================== 复制 ========================

    /**
     * 复制对象。
     * <p>
     * 使用 {@link MinioClient#copyObject(CopyObjectArgs)} 将对象从源路径复制到目标路径。
     * 复制完成后通过 {@code statObject} 获取目标对象的完整元数据以构建结果。
     * 支持跨存储桶复制（源与目标可能在不同的存储桶中）。
     * </p>
     *
     * @param sourceKey 源对象键
     * @param targetKey 目标对象键
     * @return 复制结果，包含目标对象的路径、URL 和元数据
     * @throws OssException 源对象不存在或复制失败时抛出
     */
    @Override
    protected OssResult doCopy(String sourceKey, String targetKey) {
        try {
            String sourceBucket = resolveBucket(sourceKey);
            String targetBucket = resolveBucket(targetKey);

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .source(CopySource.builder()
                                    .bucket(sourceBucket)
                                    .object(sourceKey)
                                    .build())
                            .bucket(targetBucket)
                            .object(targetKey)
                            .build());

            // Stat 目标对象以获取完整元数据
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

    /**
     * 生成预签名上传 URL。
     * <p>
     * 使用 {@link MinioClient#getPresignedObjectUrl(GetPresignedObjectUrlArgs)} 生成
     * 带签名的 PUT URL。客户端必须在上传时携带 {@code headers} 中指定的请求头，
     * 否则签名校验将失败。
     * </p>
     *
     * @param objectKey 对象键
     * @param expiry    有效期
     * @param headers   客户端必须携带的请求头（键已包含 {@code x-amz-meta-} 前缀）
     * @return 预签名 URL
     * @throws OssException 生成预签名 URL 失败时抛出
     */
    @Override
    protected PresignedUrl doPresignPut(String objectKey, Duration expiry,
                                        Map<String, String> headers) {
        try {
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(resolveBucket(objectKey))
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

    /**
     * 生成预签名下载 URL。
     * <p>
     * 使用 {@link MinioClient#getPresignedObjectUrl(GetPresignedObjectUrlArgs)} 生成
     * 带签名的 GET URL，客户端可直接通过此 URL 下载对象。
     * </p>
     *
     * @param objectKey 对象键
     * @param expiry    有效期
     * @return 预签名 URL
     * @throws OssException 生成预签名 URL 失败时抛出
     */
    @Override
    protected PresignedUrl doPresignGet(String objectKey, Duration expiry) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(resolveBucket(objectKey))
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

    /**
     * 构建对象的访问 URL。
     * <p>
     * 如果配置了自定义域名（{@code customDomain}），则使用自定义域名拼接对象键；
     * 否则使用 {@code {endpoint}/{bucket}/{objectKey}} 格式构建。
     * </p>
     *
     * @param objectKey 对象键
     * @return 完整的访问 URL
     */
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

    // ======================== 存储桶解析 ========================

    /**
     * 获取暂存存储桶名称。
     * <p>
     * 如果配置了独立的暂存桶则返回暂存桶名称，否则返回主存储桶名称。
     * </p>
     *
     * @return 暂存存储桶名称
     */
    @Override
    protected String doGetStagingBucket() {
        String stagingBucket = properties.getStaging().getBucket();
        return StringUtil.isNotBlank(stagingBucket) ? stagingBucket : properties.getBucket();
    }

    // ======================== 用户元数据 ========================

    /**
     * 获取对象的自定义元数据（用户元数据）。
     * <p>
     * 通过 {@code statObject} 读取对象的用户元数据，并将 MinIO SDK 返回的
     * 不含 {@code x-amz-meta-} 前缀的键转换为带前缀的格式，以与基类的
     * {@link #META_FILENAME}、{@link #META_BASEPATH} 等常量保持一致。
     * </p>
     *
     * @param objectKey 对象键
     * @return 用户元数据映射（键含 {@code x-amz-meta-} 前缀），无元数据时返回空 Map
     * @throws OssException 获取元数据失败时抛出
     */
    @Override
    protected Map<String, String> doGetUserMetadata(String objectKey) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(resolveBucket(objectKey))
                            .object(objectKey)
                            .build());

            Map<String, String> raw = stat.userMetadata();
            return addMetaPrefix(raw);
        } catch (Exception e) {
            throw OssException.ossError("获取对象用户元数据失败: " + objectKey, e);
        }
    }

    // ======================== 过期暂存清理 ========================

    /**
     * 清理过期的暂存文件。
     * <p>
     * 扫描暂存存储桶中暂存前缀下的所有对象，筛选出最后修改时间早于
     * {@code now - olderThan} 的过期文件，然后批量删除。
     * </p>
     * <p>
     * 适用于定时任务定期清理未确认的过期暂存文件，释放存储空间。
     * </p>
     *
     * @param olderThan 清理阈值，删除最后修改时间早于此阈值的暂存文件
     * @return 实际清理的暂存文件数量
     * @throws OssException 批量删除过程中发生严重错误时抛出
     */
    @Override
    public int purgeExpiredStages(Duration olderThan) {
        try {
            String bucket = doGetStagingBucket();
            String prefix = properties.getStaging().getPrefix();
            Instant cutoff = Instant.now().minus(olderThan);

            // 列出暂存前缀下的所有对象
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

            // 批量删除过期暂存文件
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
     * 根据对象键解析目标存储桶。
     * <p>
     * 如果配置了独立的暂存桶，且对象键以暂存前缀开头，则返回暂存桶；
     * 否则返回主存储桶。
     * </p>
     *
     * @param objectKey 对象键
     * @return 目标存储桶名称
     */
    private String resolveBucket(String objectKey) {
        String stagingBucket = properties.getStaging().getBucket();
        String stagingPrefix = properties.getStaging().getPrefix();

        if (StringUtil.isNotBlank(stagingBucket)
                && objectKey != null
                && objectKey.startsWith(stagingPrefix)) {
            return stagingBucket;
        }
        return properties.getBucket();
    }

    /**
     * 从用户元数据中提取原始文件名。
     * <p>
     * MinIO SDK 返回的用户元数据键不含 {@code x-amz-meta-} 前缀。
     * 本方法查找 {@code "zerx-filename"} 键对应的值。
     * </p>
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
     * <p>
     * 基类传递的元数据键包含 {@code x-amz-meta-} 前缀（如 {@code "x-amz-meta-zerx-filename"}），
     * 而 MinIO SDK 的 {@code userMetadata()} 方法会自动添加该前缀。
     * 因此在写入前需去除前缀以避免重复（如 {@code "x-amz-meta-x-amz-meta-zerx-filename"}）。
     * </p>
     *
     * @param metadata 原始元数据映射（键含 {@code x-amz-meta-} 前缀），可为 {@code null}
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
     * <p>
     * MinIO SDK 读取的用户元数据键不含 {@code x-amz-meta-} 前缀，
     * 而基类的 {@link #META_FILENAME}、{@link #META_BASEPATH} 等常量使用带前缀的完整键名。
     * 因此在返回给基类前需添加前缀。
     * </p>
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
     * <p>
     * MinIO 返回的 ETag 通常为 {@code "d41d8cd98f00b204e9800998ecf8427e"}（含引号），
     * 去除引号后保持与其他 OSS 实现的兼容性。
     * </p>
     *
     * @param etag MinIO 原始 ETag 值
     * @return 去除首尾引号后的 ETag
     */
    private String stripQuotes(String etag) {
        if (etag == null) {
            return null;
        }
        if (etag.length() >= 2
                && etag.startsWith("\"")
                && etag.endsWith("\"")) {
            return etag.substring(1, etag.length() - 1);
        }
        return etag;
    }

    /**
     * 移除字符串末尾的斜杠。
     *
     * @param value 原始字符串
     * @return 去除末尾斜杠后的字符串
     */
    private String stripTrailingSlash(String value) {
        if (value != null && value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
