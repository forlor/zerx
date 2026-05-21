package com.zerx.component.oss;

import java.io.InputStream;
import java.io.Serializable;

/**
 * 对象存储上传请求
 * <p>
 * 封装了直接上传文件到对象存储所需的全部信息，包括文件内容流、原始文件名
 * 和可选的目标基础路径。原始文件名会存储在对象的自定义元数据中，而不会
 * 作为对象路径的一部分，从而支持中文文件名和特殊字符。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用工厂方法快速创建
 * OssUploadRequest request = OssUploadRequest.of(fileInputStream, "月度报表.pdf");
 * OssResult result = ossStorageService.put(request);
 *
 * // 指定自定义基础路径
 * OssUploadRequest request2 = new OssUploadRequest(
 *     fileInputStream, "photo.jpg", "user-avatars"
 * );
 * }</pre>
 *
 * @param inputStream  文件内容的输入流，不能为 {@code null}。
 *                     调用方负责在 {@link OssStorageService#put(OssUploadRequest)} 完成后关闭流
 * @param filename     上传的原始文件名（original filename），存储在对象的自定义元数据中，
 *                     不作为路径的一部分。不能为 {@code null} 或空字符串
 * @param basePath     可选的目标基础路径（optional basePath），用于覆盖默认的存储路径前缀。
 *                     例如设置为 {@code "user-avatars"} 则对象存储在 {@code user-avatars/<uuid>.ext} 下。
 *                     设置为 {@code null} 时使用服务默认配置的基础路径
 * @author zerx
 * @see OssStorageService#put(OssUploadRequest)
 */
public record OssUploadRequest(
        InputStream inputStream,
        String filename,
        String basePath
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建上传请求（使用默认基础路径）
     * <p>
     * 便捷工厂方法，{@code basePath} 设置为 {@code null}，
     * 上传文件将存储在服务默认配置的基础路径下。
     * </p>
     *
     * @param inputStream 文件内容的输入流
     * @param filename    原始文件名
     * @return {@link OssUploadRequest} 实例
     */
    public static OssUploadRequest of(InputStream inputStream, String filename) {
        return new OssUploadRequest(inputStream, filename, null);
    }
}
