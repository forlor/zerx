package com.zerx.component.oss;

import java.io.IOException;
import java.io.InputStream;

/**
 * 对象存储对象
 * <p>
 * 封装了一个对象存储中的文件对象，包含对象内容的 {@link InputStream} 和
 * 对象的元数据 {@link OssObjectMeta}。实现了 {@link java.io.Closeable} 接口，
 * 调用方使用完毕后必须关闭以释放底层连接资源。
 * </p>
 *
 * <h3>资源管理：</h3>
 * <p>
 * {@code OssObject} 持有来自对象存储服务的底层网络连接，未关闭可能导致连接泄漏。
 * 推荐使用 {@code try-with-resources} 语句确保资源正确释放：
 * </p>
 *
 * <pre>{@code
 * try (OssObject ossObject = ossStorageService.get("uploads/report.pdf")) {
 *     OssObjectMeta meta = ossObject.getMeta();
 *     InputStream content = ossObject.getInputStream();
 *     // 读取内容...
 * }
 * }</pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>关闭 {@code OssObject} 后，其内部的 {@link InputStream} 也会被关闭</li>
 *   <li>关闭后的 {@code OssObject} 不应再被使用</li>
 *   <li>此类的实例不是线程安全的</li>
 * </ul>
 *
 * @author zerx
 * @see OssStorageService#get(String)
 * @see OssObjectMeta
 */
public final class OssObject implements java.io.Closeable {

    /** 对象内容输入流 */
    private final InputStream inputStream;

    /** 对象元数据 */
    private final OssObjectMeta meta;

    /**
     * 构造对象存储对象
     *
     * @param inputStream 对象内容的输入流，不能为 {@code null}
     * @param meta        对象元数据，不能为 {@code null}
     * @throws IllegalArgumentException 如果 {@code inputStream} 或 {@code meta} 为 {@code null}
     */
    public OssObject(InputStream inputStream, OssObjectMeta meta) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        if (meta == null) {
            throw new IllegalArgumentException("meta must not be null");
        }
        this.inputStream = inputStream;
        this.meta = meta;
    }

    /**
     * 获取对象内容的输入流
     *
     * @return 对象内容的 {@link InputStream}
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * 获取对象元数据
     *
     * @return 对象的 {@link OssObjectMeta}
     */
    public OssObjectMeta getMeta() {
        return meta;
    }

    /**
     * 关闭此对象，释放底层输入流和相关资源
     * <p>
     * 调用后，此对象持有的 {@link InputStream} 将被关闭，后续使用将抛出异常。
     * 此方法是幂等的，多次调用不会产生副作用。
     * </p>
     *
     * @throws IOException 如果关闭输入流时发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
