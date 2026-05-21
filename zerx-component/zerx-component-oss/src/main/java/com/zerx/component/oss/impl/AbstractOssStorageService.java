package com.zerx.component.oss.impl;

import com.zerx.common.util.StringUtil;
import com.zerx.common.util.UuidUtil;
import com.zerx.component.oss.OssConfirmResult;
import com.zerx.component.oss.OssException;
import com.zerx.component.oss.OssObject;
import com.zerx.component.oss.OssObjectMeta;
import com.zerx.component.oss.OssPresignPutRequest;
import com.zerx.component.oss.OssResult;
import com.zerx.component.oss.OssStageRequest;
import com.zerx.component.oss.OssStageResult;
import com.zerx.component.oss.OssStorageService;
import com.zerx.component.oss.OssUploadRequest;
import com.zerx.component.oss.PresignedUrl;
import com.zerx.component.oss.properties.ZerxOssProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对象存储服务抽象基类。
 * <p>
 * 实现 {@link OssStorageService} 接口，提供对象存储操作的通用模板逻辑，
 * 包括对象键生成、路径解析、URL 构建、暂存（预上传）流程等。
 * 具体的存储后端操作由子类通过 {@code do*} 抽象方法实现。
 * </p>
 *
 * <h3>职责划分：</h3>
 * <ul>
 *   <li><b>本类（模板）</b>：对象键生成规则、暂存流程编排、公共工具方法</li>
 *   <li><b>子类（实现）</b>：具体的存储后端交互（如 MinIO SDK、S3 SDK、阿里云 OSS SDK 等）</li>
 * </ul>
 *
 * <h3>对象键格式：</h3>
 * <pre>
 * {basePath}/{yyyy/MM/dd}/{uuid}.{extension}
 * </pre>
 *
 * <h3>暂存流程：</h3>
 * <p>
 * 暂存支持两种隔离策略（通过 {@code zerx.oss.staging.strategy} 配置）：
 * </p>
 * <ul>
 *   <li><b>DIRECTORY（默认）</b>：暂存文件存放在主桶的指定前缀目录下（如 {@code _staging/{uuid}}）</li>
 *   <li><b>BUCKET</b>：暂存文件存放在独立的暂存桶中（如 {@code my-bucket-staging/{uuid}}）</li>
 * </ul>
 * <ol>
 *   <li>客户端调用 {@link #presignStage(OssStageRequest)} 获取预签名 URL 和 stageToken</li>
 *   <li>客户端使用预签名 URL 上传文件到暂存区</li>
 *   <li>客户端调用 {@link #confirm(String)} 将文件移至正式存储，或 {@link #cancel(String)} 取消</li>
 * </ol>
 *
 * @author zerx
 * @see OssStorageService
 * @see ZerxOssProperties
 */
public abstract class AbstractOssStorageService implements OssStorageService {

    /** 自定义元数据键：原始文件名 */
    protected static final String META_FILENAME = "x-amz-meta-zerx-filename";

    /** 自定义元数据键：基础路径 */
    protected static final String META_BASEPATH = "x-amz-meta-zerx-basepath";

    /** 对象键中的日期路径格式：yyyy/MM/dd */
    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 默认文件扩展名（无法从文件名提取时使用） */
    private static final String DEFAULT_EXTENSION = "bin";

    /** 对象存储配置属性 */
    protected final ZerxOssProperties properties;

    /**
     * 构造抽象存储服务。
     *
     * @param properties 对象存储配置属性，不能为 {@code null}
     * @throws IllegalArgumentException 如果 properties 为 {@code null}
     */
    protected AbstractOssStorageService(ZerxOssProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
        this.properties = properties;
    }

    // ======================== 对象键生成 ========================

    /**
     * 使用默认基础路径生成对象键。
     * <p>
     * 格式：{@code {basePath}/{yyyy/MM/dd}/{uuid}.{extension}}
     * </p>
     *
     * @param extension 文件扩展名（不含点号，如 {@code "pdf"}）
     * @return 生成的对象键
     */
    protected String generateObjectKey(String extension) {
        return generateObjectKey(null, extension);
    }

    /**
     * 使用指定基础路径生成对象键。
     * <p>
     * 如果 {@code basePath} 为 {@code null} 或空白，则回退到配置中的默认基础路径。
     * 格式：{@code {basePath}/{yyyy/MM/dd}/{uuid}.{extension}}
     * </p>
     *
     * @param basePath   可选的基础路径前缀，为 {@code null} 时使用配置默认值
     * @param extension  文件扩展名（不含点号，如 {@code "pdf"}）
     * @return 生成的对象键
     */
    protected String generateObjectKey(String basePath, String extension) {
        String resolvedPath = resolveBasePath(basePath);
        String datePath = LocalDate.now().format(DATE_PATH_FORMATTER);
        String uuid = UuidUtil.fastUuidv7().toString();
        return resolvedPath + "/" + datePath + "/" + uuid + "." + extension;
    }

    // ======================== 存储桶与路径解析 ========================

    /**
     * 解析主存储桶名称。
     *
     * @return 主存储桶名称
     */
    protected String resolveMainBucket() {
        return properties.getBucket();
    }

    /**
     * 解析暂存存储桶名称。
     * <p>
     * 根据暂存策略返回：
     * <ul>
     *   <li>{@link ZerxOssProperties.Staging.Strategy#DIRECTORY}：返回主存储桶</li>
     *   <li>{@link ZerxOssProperties.Staging.Strategy#BUCKET}：返回独立暂存桶名</li>
     * </ul>
     * </p>
     *
     * @return 暂存存储桶名称
     */
    protected String resolveStagingBucket() {
        return properties.getStaging().resolveBucket(properties.getBucket());
    }

    /**
     * 基于配置构建对象访问 URL。
     * <p>
     * 如果配置了自定义域名（{@code customDomain}），则使用自定义域名拼接；
     * 否则使用 {@code endpoint}/{bucket}/{objectKey} 格式构建。
     * 正式对象的 URL 始终基于主桶生成。
     * </p>
     *
     * @param objectKey 对象键
     * @return 完整的访问 URL
     */
    protected String buildUrl(String objectKey) {
        String customDomain = properties.getCustomDomain();
        if (StringUtil.isNotBlank(customDomain)) {
            String domain = stripTrailingSlash(customDomain);
            return domain + "/" + objectKey;
        }
        String endpoint = stripTrailingSlash(properties.getEndpoint());
        return endpoint + "/" + properties.getBucket() + "/" + objectKey;
    }

    /**
     * 解析基础路径。
     * <p>
     * 如果 {@code basePath} 非空且非空白则直接使用，否则回退到配置中的默认基础路径。
     * </p>
     *
     * @param basePath 可选的基础路径
     * @return 解析后的基础路径（可能为空字符串）
     */
    protected String resolveBasePath(String basePath) {
        return StringUtil.isNotBlank(basePath) ? basePath : properties.getBasePath();
    }

    // ======================== 直接上传 ========================

    /**
     * {@inheritDoc}
     * <p>
     * 生成对象键后将上传委托给 {@link #doPut}，原始文件名存储在自定义元数据中。
     * </p>
     */
    @Override
    public OssResult put(OssUploadRequest request) {
        String extension = getExtension(request.filename());
        String objectKey = generateObjectKey(request.basePath(), extension);

        Map<String, String> metadata = new HashMap<>(2);
        metadata.put(META_FILENAME, request.filename());

        return doPut(objectKey, request.inputStream(), null, metadata);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 从文件名提取扩展名并生成对象键，然后将预签名委托给 {@link #doPresignPut}。
     * 原始文件名通过请求头传递给客户端，客户端上传时需携带该头。
     * </p>
     */
    @Override
    public PresignedUrl presignPut(OssPresignPutRequest request) {
        String extension = getExtension(request.filename());
        String objectKey = generateObjectKey(request.basePath(), extension);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(META_FILENAME, request.filename());

        Duration expiry = request.expiry() != null ? request.expiry() : properties.getSignedUrlExpiry();
        return doPresignPut(objectKey, expiry, headers);
    }

    // ======================== 暂存（预上传） ========================

    /**
     * {@inheritDoc}
     * <p>
     * 生成暂存令牌（UUID），根据暂存策略生成暂存键：
     * <ul>
     *   <li>DIRECTORY 模式：{@code {prefix}{stageToken}}（如 {@code _staging/550e8400-...}）</li>
     *   <li>BUCKET 模式：直接使用 {@code stageToken}（独立桶中无需目录前缀）</li>
     * </ul>
     * 预签名 PUT URL 在暂存桶上生成。
     * </p>
     */
    @Override
    public OssStageResult presignStage(OssStageRequest request) {
        String stageToken = UuidUtil.fastUuidv7().toString();
        String stagingKey = properties.getStaging().resolveStagingKey(stageToken);

        Duration ttl = request.ttl() != null ? request.ttl() : properties.getStaging().getDefaultTtl();
        String resolvedBasePath = resolveBasePath(request.basePath());

        Map<String, String> headers = new LinkedHashMap<>(2);
        headers.put(META_FILENAME, request.filename());
        headers.put(META_BASEPATH, resolvedBasePath);

        PresignedUrl presignedUrl = doPresignPut(stagingKey, ttl, headers, resolveStagingBucket());

        return new OssStageResult(stageToken, presignedUrl, stagingKey);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 从暂存桶读取暂存对象的元数据（原始文件名、基础路径），
     * 生成最终对象键后，将文件从暂存桶复制到主桶正式路径，最后删除暂存对象。
     * </p>
     */
    @Override
    public OssConfirmResult confirm(String stageToken) {
        String stagingKey = properties.getStaging().resolveStagingKey(stageToken);
        String stagingBucket = resolveStagingBucket();
        String mainBucket = resolveMainBucket();

        // 读取暂存对象的元数据（从暂存桶）
        OssObjectMeta stagingMeta = doGetObjectMeta(stagingBucket, stagingKey);
        Map<String, String> userMetadata = doGetUserMetadata(stagingBucket, stagingKey);

        String originalFilename = stagingMeta.originalFilename();
        String basePath = userMetadata != null ? userMetadata.get(META_BASEPATH) : null;

        // 生成最终对象键
        String extension = getExtension(originalFilename);
        String finalKey = generateObjectKey(basePath, extension);

        // 跨桶复制：暂存桶 → 主桶
        OssResult copyResult = doCopy(stagingBucket, stagingKey, mainBucket, finalKey);

        // 删除暂存对象
        doDelete(stagingBucket, stagingKey);

        return new OssConfirmResult(
                copyResult.objectKey(),
                copyResult.url(),
                copyResult.originalFilename(),
                copyResult.size(),
                copyResult.contentType(),
                copyResult.etag()
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * 删除指定暂存令牌对应的暂存对象。操作是幂等的。
     * </p>
     */
    @Override
    public void cancel(String stageToken) {
        String stagingKey = properties.getStaging().resolveStagingKey(stageToken);
        String stagingBucket = resolveStagingBucket();
        doDelete(stagingBucket, stagingKey);
    }

    // ======================== 读取 ========================

    /**
     * {@inheritDoc}
     */
    @Override
    public OssObjectMeta head(String objectKey) {
        return doGetObjectMeta(resolveMainBucket(), objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OssObject get(String objectKey) {
        return doGet(resolveMainBucket(), objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String url(String objectKey) {
        return doBuildUrl(objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PresignedUrl presignGet(String objectKey, Duration expiry) {
        return doPresignGet(objectKey, expiry);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 获取对象后将其内容流复制到输出流，使用 try-with-resources 确保资源释放。
     * </p>
     */
    @Override
    public void download(String objectKey, OutputStream out) {
        try (OssObject ossObject = doGet(resolveMainBucket(), objectKey)) {
            ossObject.getInputStream().transferTo(out);
        } catch (IOException e) {
            throw OssException.ossError("下载对象失败: " + objectKey);
        }
    }

    // ======================== 管理 ========================

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String objectKey) {
        return doExists(resolveMainBucket(), objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(String objectKey) {
        doDelete(resolveMainBucket(), objectKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteBatch(List<String> objectKeys) {
        return doDeleteBatch(resolveMainBucket(), objectKeys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OssResult copy(String sourceKey, String targetKey) {
        return doCopy(resolveMainBucket(), sourceKey, resolveMainBucket(), targetKey);
    }

    // ======================== 抽象方法（子类实现） ========================

    /**
     * 上传对象到存储。
     *
     * @param bucket      目标存储桶
     * @param objectKey   对象键
     * @param input       文件内容输入流
     * @param contentType MIME 类型，可能为 {@code null}
     * @param metadata    自定义元数据（如原始文件名）
     * @return 上传结果
     */
    protected abstract OssResult doPut(String bucket, String objectKey, InputStream input,
                                       String contentType, Map<String, String> metadata);

    /**
     * 获取对象元数据（不含对象内容）。
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象键
     * @return 对象元数据
     */
    protected abstract OssObjectMeta doGetObjectMeta(String bucket, String objectKey);

    /**
     * 获取对象内容和元数据。
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象键
     * @return 包含输入流和元数据的对象，调用方必须关闭
     */
    protected abstract OssObject doGet(String bucket, String objectKey);

    /**
     * 判断对象是否存在。
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象键
     * @return 存在返回 {@code true}
     */
    protected abstract boolean doExists(String bucket, String objectKey);

    /**
     * 删除单个对象。
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象键
     */
    protected abstract void doDelete(String bucket, String objectKey);

    /**
     * 批量删除对象。
     *
     * @param bucket     存储桶名称
     * @param objectKeys 对象键列表
     * @return 实际成功删除的数量
     */
    protected abstract int doDeleteBatch(String bucket, List<String> objectKeys);

    /**
     * 复制对象。
     * <p>
     * 支持跨桶复制（如从暂存桶到主桶）。
     * </p>
     *
     * @param sourceBucket 源存储桶名称
     * @param sourceKey    源对象键
     * @param targetBucket 目标存储桶名称
     * @param targetKey    目标对象键
     * @return 复制结果
     */
    protected abstract OssResult doCopy(String sourceBucket, String sourceKey,
                                        String targetBucket, String targetKey);

    /**
     * 生成预签名上传 URL（主桶）。
     *
     * @param objectKey 对象键
     * @param expiry    有效期
     * @param headers   客户端必须携带的请求头
     * @return 预签名 URL
     */
    protected abstract PresignedUrl doPresignPut(String objectKey, Duration expiry,
                                                  Map<String, String> headers);

    /**
     * 生成预签名上传 URL（指定桶）。
     * <p>
     * 默认委托给 {@link #doPresignPut(String, Duration, Map)}，使用主桶。
     * 子类如需支持暂存桶的预签名上传，应覆写此方法以支持指定桶名。
     * </p>
     *
     * @param objectKey 对象键
     * @param expiry    有效期
     * @param headers   客户端必须携带的请求头
     * @param bucket    目标存储桶名称
     * @return 预签名 URL
     */
    protected PresignedUrl doPresignPut(String objectKey, Duration expiry,
                                        Map<String, String> headers, String bucket) {
        // 默认实现忽略 bucket 参数，子类可覆写以支持指定桶
        return doPresignPut(objectKey, expiry, headers);
    }

    /**
     * 生成预签名下载 URL。
     *
     * @param objectKey 对象键
     * @param expiry    有效期
     * @return 预签名 URL
     */
    protected abstract PresignedUrl doPresignGet(String objectKey, Duration expiry);

    /**
     * 构建对象的访问 URL（实现特定）。
     *
     * @param objectKey 对象键
     * @return 完整的访问 URL
     */
    protected abstract String doBuildUrl(String objectKey);

    /**
     * 获取对象的自定义元数据（用户元数据）。
     * <p>
     * 用于暂存确认时读取存储在对象头中的基础路径等自定义信息。
     * </p>
     *
     * @param bucket    存储桶名称
     * @param objectKey 对象键
     * @return 用户元数据映射，键含 {@code x-amz-meta-} 前缀，无元数据时返回空 Map
     */
    protected abstract Map<String, String> doGetUserMetadata(String bucket, String objectKey);

    /**
     * 列举指定前缀下的过期对象并删除。
     * <p>
     * 用于清理过期暂存文件。由 {@link #purgeExpiredStages(Duration)} 调用。
     * </p>
     *
     * @param bucket     存储桶名称
     * @param prefix     列举前缀
     * @param cutoff     过期截止时间，早于此时间的对象视为过期
     * @return 实际清理的对象数量
     */
    protected abstract int doPurgeExpired(String bucket, String prefix, java.time.Instant cutoff);

    /**
     * {@inheritDoc}
     * <p>
     * 根据暂存策略确定扫描范围：
     * <ul>
     *   <li>DIRECTORY 模式：在主桶的暂存前缀下扫描</li>
     *   <li>BUCKET 模式：在独立暂存桶中扫描全部对象</li>
     * </ul>
     * </p>
     */
    @Override
    public int purgeExpiredStages(Duration olderThan) {
        java.time.Instant cutoff = java.time.Instant.now().minus(olderThan);
        String stagingBucket = resolveStagingBucket();
        String scanPrefix = properties.getStaging().resolveScanPrefix();
        return doPurgeExpired(stagingBucket, scanPrefix, cutoff);
    }

    // ======================== 内部工具方法 ========================

    /**
     * 从文件名中提取扩展名。
     * <p>
     * 示例：{@code "report.pdf"} → {@code "pdf"}，{@code "archive.tar.gz"} → {@code "gz"}。
     * 如果文件名无扩展名或为空，返回 {@value #DEFAULT_EXTENSION}。
     * </p>
     *
     * @param filename 原始文件名
     * @return 文件扩展名（不含点号）
     */
    private String getExtension(String filename) {
        if (StringUtil.isBlank(filename)) {
            return DEFAULT_EXTENSION;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return DEFAULT_EXTENSION;
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
