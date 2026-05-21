package com.zerx.component.oss;

import java.io.Serializable;
import java.time.Duration;

/**
 * 对象存储预签名上传请求
 * <p>
 * 封装了生成预签名 PUT URL 所需的参数。客户端收到返回的 {@link PresignedUrl} 后，
 * 可直接使用 {@code PUT} 方法将文件上传到对象存储，无需经过服务端中转。
 * </p>
 *
 * <h3>使用场景：</h3>
 * <p>
 * 与 {@link OssUploadRequest}（服务端中转上传）不同，预签名上传允许客户端
 * 直接上传到对象存储，减少服务端带宽压力，适用于大文件上传场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * OssPresignPutRequest request = new OssPresignPutRequest(
 *     "photo.jpg",
 *     "user-avatars",
 *     Duration.ofMinutes(30),
 *     "image/jpeg"
 * );
 * PresignedUrl presigned = ossStorageService.presignPut(request);
 * // 将 presigned 返回给前端，前端使用 PUT 方法上传
 * }</pre>
 *
 * @param filename    上传的原始文件名（original filename），存储在对象自定义元数据中，
 *                    不作为路径的一部分。不能为 {@code null} 或空字符串
 * @param basePath    可选的目标基础路径（optional basePath），用于覆盖默认的存储路径前缀。
 *                    设置为 {@code null} 时使用服务默认配置的基础路径
 * @param expiry      预签名 URL 的有效期（expiry duration），过期后上传将被拒绝。
 *                    不能为 {@code null}
 * @param contentType 可选的文件 MIME 类型（optional content type），用于设置上传对象的 Content-Type。
 *                    如果为 {@code null}，对象存储服务可能根据文件扩展名自动推断
 * @author zerx
 * @see OssStorageService#presignPut(OssPresignPutRequest)
 * @see PresignedUrl
 */
public record OssPresignPutRequest(
        String filename,
        String basePath,
        Duration expiry,
        String contentType
) implements Serializable {

    private static final long serialVersionUID = 1L;
}
