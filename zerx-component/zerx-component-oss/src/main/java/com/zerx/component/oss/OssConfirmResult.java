package com.zerx.component.oss;

import java.io.Serializable;

/**
 * 对象存储暂存确认结果
 * <p>
 * 表示暂存文件确认（commit）操作的返回结果。确认操作将暂存区的文件移动到
 * 正式存储路径，并返回最终存储的信息。此结果不包含 {@code lastModified} 字段，
 * 如需完整的元数据可通过 {@link OssStorageService#head(String)} 获取。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * OssConfirmResult result = ossStorageService.confirm(stageToken);
 * System.out.println("文件已存储至: " + result.url());
 * System.out.println("原始文件名: " + result.originalFilename());
 * }</pre>
 *
 * @param objectKey         对象在存储系统中的最终路径（storage path）
 * @param url               对象的完整访问 URL（full access URL），可直接用于 HTTP 访问
 * @param originalFilename  上传时的原始文件名，从对象自定义元数据中提取
 * @param size              文件大小，单位为字节
 * @param contentType       文件的 MIME 类型（如 {@code image/png}、{@code application/pdf}），
 *                          可能为 {@code null}
 * @param etag              对象的 ETag 值，用于缓存控制和一致性校验
 * @author zerx
 * @see OssStorageService#confirm(String)
 * @see OssStageResult
 */
public record OssConfirmResult(
        String objectKey,
        String url,
        String originalFilename,
        long size,
        String contentType,
        String etag
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
