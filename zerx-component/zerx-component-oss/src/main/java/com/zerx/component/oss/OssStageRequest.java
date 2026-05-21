package com.zerx.component.oss;

import java.io.Serializable;
import java.time.Duration;

/**
 * 对象存储暂存（预上传）请求
 * <p>
 * 封装了创建预签名暂存上传所需的参数。暂存机制允许客户端先获取上传凭证，
 * 再由客户端直接上传文件到对象存储的暂存区，上传完成后通过
 * {@link OssStorageService#confirm(String)} 提交或
 * {@link OssStorageService#cancel(String)} 取消。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>服务端创建 {@code OssStageRequest} 并调用
 *       {@link OssStorageService#presignStage(OssStageRequest)}</li>
 *   <li>返回 {@link OssStageResult}，其中包含 {@link PresignedUrl} 和 {@code stageToken}</li>
 *   <li>客户端使用预签名 URL 上传文件</li>
 *   <li>客户端上传完成后，使用 {@code stageToken} 调用 confirm/cancel</li>
 * </ol>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * OssStageRequest request = new OssStageRequest(
 *     "report.pdf",
 *     "application/pdf",
 *     null,               // 使用默认 basePath
 *     Duration.ofHours(2)  // 暂存有效期 2 小时
 * );
 * OssStageResult result = ossStorageService.presignStage(request);
 * }</pre>
 *
 * @param filename     上传的原始文件名（original filename），存储在对象自定义元数据中，
 *                     不作为路径的一部分。不能为 {@code null} 或空字符串
 * @param contentType  可选的文件 MIME 类型（optional content type），用于设置上传对象的 Content-Type。
 *                     如果为 {@code null}，对象存储服务可能根据文件扩展名自动推断
 * @param basePath     可选的目标基础路径（optional basePath），用于指定确认后文件的最终存储路径前缀。
 *                     设置为 {@code null} 时使用服务默认配置的基础路径
 * @param ttl          暂存有效期（staging TTL），超过此时间后暂存记录将被视为过期并可被清理。
 *                     设置为 {@code null} 时使用服务默认配置的有效期
 * @author zerx
 * @see OssStorageService#presignStage(OssStageRequest)
 * @see OssStageResult
 */
public record OssStageRequest(
        String filename,
        String contentType,
        String basePath,
        Duration ttl
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
