package com.zerx.component.oss;

import java.io.OutputStream;
import java.time.Duration;
import java.util.List;

/**
 * 对象存储服务接口
 * <p>
 * 提供统一的对象存储操作抽象，支持多种对象存储后端（如 MinIO、S3、阿里云 OSS 等）。
 * 包含文件上传、下载、预签名、暂存（预上传）及对象管理等功能。
 * </p>
 *
 * <h3>功能概览：</h3>
 * <table>
 *   <tr><th>分类</th><th>方法</th><th>说明</th></tr>
 *   <tr><td>直接上传</td><td>{@link #put(OssUploadRequest)}</td><td>服务端中转上传</td></tr>
 *   <tr><td>直接上传</td><td>{@link #presignPut(OssPresignPutRequest)}</td><td>预签名上传（客户端直传）</td></tr>
 *   <tr><td>暂存（预上传）</td><td>{@link #presignStage(OssStageRequest)}</td><td>创建暂存预签名上传</td></tr>
 *   <tr><td>暂存（预上传）</td><td>{@link #confirm(String)}</td><td>确认暂存文件</td></tr>
 *   <tr><td>暂存（预上传）</td><td>{@link #cancel(String)}</td><td>取消暂存文件</td></tr>
 *   <tr><td>暂存（预上传）</td><td>{@link #purgeExpiredStages(Duration)}</td><td>清理过期暂存记录</td></tr>
 *   <tr><td>读取</td><td>{@link #head(String)}</td><td>获取对象元数据</td></tr>
 *   <tr><td>读取</td><td>{@link #get(String)}</td><td>获取对象内容和元数据</td></tr>
 *   <tr><td>读取</td><td>{@link #url(String)}</td><td>获取公开访问 URL</td></tr>
 *   <tr><td>读取</td><td>{@link #presignGet(String, Duration)}</td><td>生成预签名下载 URL</td></tr>
 *   <tr><td>读取</td><td>{@link #download(String, OutputStream)}</td><td>下载对象到输出流</td></tr>
 *   <tr><td>管理</td><td>{@link #exists(String)}</td><td>判断对象是否存在</td></tr>
 *   <tr><td>管理</td><td>{@link #delete(String)}</td><td>删除单个对象</td></tr>
 *   <tr><td>管理</td><td>{@link #deleteBatch(List)}</td><td>批量删除对象</td></tr>
 *   <tr><td>管理</td><td>{@link #copy(String, String)}</td><td>复制对象</td></tr>
 * </table>
 *
 * <h3>上传方式对比：</h3>
 * <ul>
 *   <li><b>{@link #put(OssUploadRequest)}</b>：服务端中转上传，适用于小文件。
 *       文件流经过服务端处理后再上传到对象存储。</li>
 *   <li><b>{@link #presignPut(OssPresignPutRequest)}</b>：预签名上传，适用于大文件。
 *       返回一个预签名 URL，客户端直接上传到对象存储，不经过服务端。</li>
 *   <li><b>{@link #presignStage(OssStageRequest)}</b>：暂存预签名上传，适用于需要校验的场景。
 *       客户端先上传到暂存区，服务端可对文件进行审核后再确认。</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 注入服务
 * &#64;Autowired
 * private OssStorageService ossStorageService;
 *
 * // 直接上传
 * OssUploadRequest request = OssUploadRequest.of(inputStream, "report.pdf");
 * OssResult result = ossStorageService.put(request);
 *
 * // 生成预签名下载链接
 * PresignedUrl presigned = ossStorageService.presignGet(result.objectKey(), Duration.ofHours(1));
 *
 * // 检查文件是否存在
 * boolean exists = ossStorageService.exists("uploads/report.pdf");
 * }</pre>
 *
 * @author zerx
 * @see OssUploadRequest
 * @see OssResult
 * @see OssPresignPutRequest
 * @see PresignedUrl
 */
public interface OssStorageService {

    // ======================== 直接上传 ========================

    /**
     * 上传文件到对象存储（服务端中转）
     * <p>
     * 将文件流上传到对象存储，文件名存储在对象的自定义元数据中，不作为路径的一部分。
     * 实际的存储路径由服务端根据配置生成（通常包含时间前缀和 UUID）。
     * </p>
     *
     * @param request 上传请求，包含文件流、原始文件名和可选的基础路径
     * @return 上传结果，包含存储路径、访问 URL 和文件元数据
     * @throws OssException 如果上传过程中发生 I/O 错误、网络错误或权限不足等
     * @see OssUploadRequest
     * @see OssResult
     */
    OssResult put(OssUploadRequest request);

    /**
     * 生成预签名上传 URL（客户端直传）
     * <p>
     * 生成一个带有签名的上传 URL，客户端可直接使用 {@code PUT} 方法将文件上传到对象存储，
     * 无需经过服务端中转。适用于大文件上传场景，可减轻服务端带宽压力。
     * </p>
     *
     * @param request 预签名上传请求，包含文件名、有效期和可选的 Content-Type
     * @return 预签名 URL，包含签名参数和客户端必须发送的请求头
     * @throws OssException 如果生成预签名 URL 时发生错误
     * @see OssPresignPutRequest
     * @see PresignedUrl
     */
    PresignedUrl presignPut(OssPresignPutRequest request);

    // ======================== 暂存（预上传） ========================

    /**
     * 创建暂存预签名上传
     * <p>
     * 为客户端生成一个暂存区的预签名上传 URL。客户端上传文件到暂存区后，
     * 需调用 {@link #confirm(String)} 确认提交或 {@link #cancel(String)} 取消暂存。
     * 暂存文件在指定 TTL 过期后可被 {@link #purgeExpiredStages(Duration)} 清理。
     * </p>
     *
     * @param request 暂存请求，包含文件名、Content-Type、基础路径和有效期
     * @return 暂存结果，包含暂存令牌（用于确认/取消）、预签名 URL 和暂存路径
     * @throws OssException 如果创建暂存记录或生成预签名 URL 时发生错误
     * @see OssStageRequest
     * @see OssStageResult
     * @see #confirm(String)
     * @see #cancel(String)
     */
    OssStageResult presignStage(OssStageRequest request);

    /**
     * 确认暂存文件
     * <p>
     * 将暂存区的文件移动到正式存储路径，完成整个上传流程。确认操作通常是幂等的，
     * 重复使用相同的 {@code stageToken} 确认不会产生副作用。
     * </p>
     *
     * @param stageToken 暂存令牌，由 {@link #presignStage(OssStageRequest)} 返回
     * @return 确认结果，包含最终存储路径、访问 URL 和文件元数据
     * @throws OssException 如果暂存令牌无效、已过期或暂存文件不存在
     * @see OssStageResult#stageToken()
     * @see OssConfirmResult
     */
    OssConfirmResult confirm(String stageToken);

    /**
     * 取消暂存文件
     * <p>
     * 取消指定的暂存操作，删除暂存区的文件并释放相关资源。
     * 取消操作通常是幂等的，重复使用相同的 {@code stageToken} 取消不会产生副作用。
     * </p>
     *
     * @param stageToken 暂存令牌，由 {@link #presignStage(OssStageRequest)} 返回
     * @throws OssException 如果暂存令牌无效或暂存文件删除失败
     * @see OssStageResult#stageToken()
     */
    void cancel(String stageToken);

    /**
     * 清理过期的暂存记录
     * <p>
     * 扫描并清理超过指定时长的暂存记录，删除对应的暂存文件并释放资源。
     * 适用于定时任务定期清理未确认的过期暂存。
     * </p>
     *
     * @param olderThan 清理阈值，清理创建时间早于此时间点的暂存记录
     * @return 实际清理的暂存记录数量
     * @throws OssException 如果清理过程中发生批量删除错误
     */
    int purgeExpiredStages(Duration olderThan);

    // ======================== 读取 ========================

    /**
     * 获取对象元数据
     * <p>
     * 查询指定对象的元数据信息，不下载对象内容。可用于检查文件是否存在、
     * 获取文件大小、MIME 类型等信息。
     * </p>
     *
     * @param objectKey 对象在存储中的唯一路径
     * @return 对象元数据，包含大小、Content-Type、ETag 等信息
     * @throws OssException 如果对象不存在或获取元数据时发生错误
     * @see OssObjectMeta
     */
    OssObjectMeta head(String objectKey);

    /**
     * 获取对象内容和元数据
     * <p>
     * 获取指定对象的内容输入流和元数据。调用方必须在使用完毕后关闭返回的
     * {@link OssObject} 以释放底层连接资源，推荐使用 {@code try-with-resources}。
     * </p>
     *
     * <h3>使用示例：</h3>
     * <pre>{@code
     * try (OssObject ossObject = ossStorageService.get("uploads/report.pdf")) {
     *     OssObjectMeta meta = ossObject.getMeta();
     *     InputStream content = ossObject.getInputStream();
     *     // 处理文件内容...
     * }
     * }</pre>
     *
     * @param objectKey 对象在存储中的唯一路径
     * @return 包含输入流和元数据的 {@link OssObject}，调用方必须关闭
     * @throws OssException 如果对象不存在或获取对象时发生 I/O 错误
     * @see OssObject
     */
    OssObject get(String objectKey);

    /**
     * 获取对象的公开访问 URL
     * <p>
     * 返回指定对象的完整访问 URL。此 URL 仅适用于公开访问（public-read）策略的对象。
     * 对于私有对象，应使用 {@link #presignGet(String, Duration)} 生成带签名的临时 URL。
     * </p>
     *
     * @param objectKey 对象在存储中的唯一路径
     * @return 对象的完整访问 URL
     * @throws OssException 如果构建 URL 时发生错误
     */
    String url(String objectKey);

    /**
     * 生成预签名下载 URL
     * <p>
     * 为指定对象生成一个带签名的临时访问 URL，适用于私有对象的有限时访问场景。
     * URL 过期后将无法使用，客户端会收到权限拒绝的错误。
     * </p>
     *
     * @param objectKey 对象在存储中的唯一路径
     * @param expiry    预签名 URL 的有效期，过期后访问将被拒绝
     * @return 预签名 URL，包含签名查询参数和过期时间
     * @throws OssException 如果对象不存在或生成预签名 URL 时发生错误
     * @see PresignedUrl
     */
    PresignedUrl presignGet(String objectKey, Duration expiry);

    /**
     * 下载对象到输出流
     * <p>
     * 将指定对象的内容直接写入输出流。适用于需要将文件内容传输到其他地方的场景（如
     * 写入 HttpServletResponse 的输出流、写入文件等）。
     * </p>
     *
     * @param objectKey   对象在存储中的唯一路径
     * @param out         目标输出流，方法不会关闭此流（调用方负责关闭）
     * @throws OssException 如果对象不存在或下载过程中发生 I/O 错误
     */
    void download(String objectKey, OutputStream out);

    // ======================== 管理 ========================

    /**
     * 判断对象是否存在
     * <p>
     * 检查指定路径的对象是否存在于对象存储中。
     * </p>
     *
     * @param objectKey 对象在存储中的唯一路径
     * @return 如果对象存在返回 {@code true}，否则返回 {@code false}
     * @throws OssException 如果检查过程中发生网络或权限错误
     */
    boolean exists(String objectKey);

    /**
     * 删除单个对象
     * <p>
     * 从对象存储中永久删除指定对象。删除操作是不可逆的。
     * 删除不存在的对象通常不会抛出异常（幂等操作）。
     * </p>
     *
     * @param objectKey 要删除的对象路径
     * @throws OssException 如果删除过程中发生 I/O 或权限错误
     */
    void delete(String objectKey);

    /**
     * 批量删除对象
     * <p>
     * 一次性删除多个对象。删除操作是不可逆的。
     * 删除不存在的对象通常不会导致整个批量操作失败。
     * </p>
     *
     * @param objectKeys 要删除的对象路径列表
     * @return 实际成功删除的对象数量
     * @throws OssException 如果批量删除过程中发生严重错误
     */
    int deleteBatch(List<String> objectKeys);

    /**
     * 复制对象
     * <p>
     * 将一个对象从源路径复制到目标路径。源对象和目标路径可以在同一存储桶内，
     * 也可以在不同存储桶之间（取决于具体实现）。
     * </p>
     *
     * @param sourceKey 源对象路径
     * @param targetKey 目标对象路径
     * @return 复制结果，包含目标对象的路径、URL 和元数据
     * @throws OssException 如果源对象不存在或复制过程中发生错误
     * @see OssResult
     */
    OssResult copy(String sourceKey, String targetKey);
}
