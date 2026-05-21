package com.zerx.component.oss;

import java.io.Serializable;
import java.time.Instant;

/**
 * 对象存储对象元数据
 * <p>
 * 表示存储系统中一个对象（文件）的元数据信息，不包含对象内容本身。
 * 可通过 {@link OssStorageService#head(String)} 获取，也可从
 * {@link OssObject#getMeta()} 中获取。
 * </p>
 *
 * <h3>字段说明：</h3>
 * <table>
 *   <tr><th>字段</th><th>说明</th></tr>
 *   <tr><td>{@code objectKey}</td><td>对象在存储中的唯一路径</td></tr>
 *   <tr><td>{@code size}</td><td>文件大小（字节）</td></tr>
 *   <tr><td>{@code contentType}</td><td>MIME 类型</td></tr>
 *   <tr><td>{@code lastModified}</td><td>最后修改时间（UTC）</td></tr>
 *   <tr><td>{@code etag}</td><td>ETag 标识</td></tr>
 *   <tr><td>{@code originalFilename}</td><td>原始文件名（从自定义元数据中提取）</td></tr>
 * </table>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * OssObjectMeta meta = ossStorageService.head("uploads/report.pdf");
 * if (meta != null && meta.size() > 0) {
 *     System.out.println("文件大小: " + meta.size() + " 字节");
 * }
 * }</pre>
 *
 * @param objectKey         对象在存储系统中的唯一路径（storage path）
 * @param size              文件大小，单位为字节
 * @param contentType       文件的 MIME 类型（如 {@code image/png}、{@code application/pdf}），
 *                          可能为 {@code null}
 * @param lastModified      对象的最后修改时间（UTC）
 * @param etag              对象的 ETag 值，用于缓存控制和一致性校验
 * @param originalFilename  上传时的原始文件名，从对象的自定义元数据（custom metadata）中提取；
 *                          如果上传时未设置则可能为 {@code null}
 * @author zerx
 * @see OssStorageService#head(String)
 * @see OssObject
 */
public record OssObjectMeta(
        String objectKey,
        long size,
        String contentType,
        Instant lastModified,
        String etag,
        String originalFilename
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
