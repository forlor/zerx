package com.zerx.component.oss;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * 预签名 URL
 * <p>
 * 包含一个带有签名参数的完整 URL，客户端可直接使用此 URL 进行文件上传或下载，
 * 无需额外的认证凭据。同时包含客户端在请求时必须发送的自定义请求头。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <ul>
 *   <li><b>上传预签名</b>：服务端生成预签名 URL 和必要请求头，客户端（如浏览器）使用
 *       {@code PUT} 方法直接上传文件到对象存储，避免文件先经过服务端中转。</li>
 *   <li><b>下载预签名</b>：为私有文件生成有限时访问 URL，过期后自动失效。</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * PresignedUrl presigned = ossStorageService.presignGet("uploads/file.pdf", Duration.ofHours(1));
 * // 返回给前端，前端直接通过 presigned.url() 下载文件
 * // 如果 presigned.headers() 不为空，前端需在请求中携带这些头
 * }</pre>
 *
 * @param url       带有签名查询参数的完整 URL（full URL with signature params），
 *                  客户端可直接使用此 URL 发起 HTTP 请求
 * @param headers   客户端发起请求时必须携带的 HTTP 请求头（headers client must send）。
 *                  例如上传时可能包含 {@code x-amz-meta-zerx-filename} 用于记录原始文件名。
 *                  下载时通常为空 Map。可能为 {@code null} 或空 Map（表示无需额外头）
 * @param expiresAt 此预签名 URL 的过期时间（when this URL expires），过期后请求将被拒绝
 * @author zerx
 * @see OssStorageService#presignPut(OssPresignPutRequest)
 * @see OssStorageService#presignGet(String, java.time.Duration)
 */
public record PresignedUrl(
        String url,
        Map<String, String> headers,
        Instant expiresAt
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
