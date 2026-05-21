package com.zerx.component.oss;

import java.io.Serializable;
import java.time.Instant;

/**
 * 对象存储上传结果
 * <p>
 * 表示一次对象上传操作的完整结果，包含存储路径、访问地址、文件元数据等信息。
 * 作为 {@link OssStorageService#put(OssUploadRequest)} 和
 * {@link OssStorageService#copy(String, String)} 的返回值使用。
 * </p>
 *
 * <h3>字段说明：</h3>
 * <table>
 *   <tr><th>字段</th><th>说明</th></tr>
 *   <tr><td>{@code objectKey}</td><td>对象在存储中的唯一路径（如 {@code uploads/2024/01/abc123.pdf}）</td></tr>
 *   <tr><td>{@code url}</td><td>对象的完整访问 URL（可直接用于 HTTP 下载/预览）</td></tr>
 *   <tr><td>{@code originalFilename}</td><td>上传时的原始文件名（存储在对象自定义元数据中）</td></tr>
 *   <tr><td>{@code size}</td><td>文件大小（字节）</td></tr>
 *   <tr><td>{@code contentType}</td><td>MIME 类型（如 {@code application/pdf}）</td></tr>
 *   <tr><td>{@code etag}</td><td>ETag 标识（用于缓存一致性校验和断点续传）</td></tr>
 *   <tr><td>{@code lastModified}</td><td>最后修改时间（UTC 时间戳）</td></tr>
 * </table>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * OssResult result = ossStorageService.put(request);
 * String downloadUrl = result.url();
 * long fileSize = result.size();
 * }</pre>
 *
 * @param objectKey         对象在存储系统中的唯一路径（storage path），不包含域名前缀
 * @param url               对象的完整访问 URL（full access URL），可直接用于 HTTP 访问
 * @param originalFilename  上传时的原始文件名，从对象自定义元数据中提取
 * @param size              文件大小，单位为字节
 * @param contentType       文件的 MIME 类型（如 {@code image/png}、{@code application/pdf}），
 *                          可能为 {@code null}（上传时未指定）
 * @param etag              对象的 ETag 值，用于缓存控制和一致性校验；
 *                          格式通常为十六进制字符串的 MD5 值
 * @param lastModified      对象的最后修改时间（UTC），用于条件请求和缓存判断
 * @author zerx
 * @see OssStorageService#put(OssUploadRequest)
 * @see OssStorageService#copy(String, String)
 */
public record OssResult(
        String objectKey,
        String url,
        String originalFilename,
        long size,
        String contentType,
        String etag,
        Instant lastModified
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
