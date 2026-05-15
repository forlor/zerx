package com.zerx.common.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * IO 工具类
 * <p>
 * 提供流操作、流转字节/字符串、资源关闭等流相关的便捷方法。
 * 所有方法均经过资源管理处理，确保 InputStream / OutputStream / Reader / Writer 被正确关闭，
 * 调用方无需手动处理 try-with-resources。
 * </p>
 *
 * @author zerx
 */
public final class IoUtil {

    /** 默认缓冲区大小（8KB） */
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    /** 私有构造器，防止实例化 */
    private IoUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 流复制 ========================

    /**
     * 将 InputStream 的内容复制到 OutputStream
     * <p>
     * 使用 8KB 缓冲区进行批量复制，完成后自动关闭两个流。
     * </p>
     *
     * @param input  输入流
     * @param output 输出流
     * @return 复制的字节数
     * @throws IOException IO 异常时抛出
     */
    public static long copy(InputStream input, OutputStream output) throws IOException {
        try (input; output) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                total += read;
            }
            output.flush();
            return total;
        }
    }

    /**
     * 将 Reader 的内容复制到 Writer
     *
     * @param reader 字符输入流
     * @param writer 字符输出流
     * @return 复制的字符数
     * @throws IOException IO 异常时抛出
     */
    public static long copy(Reader reader, Writer writer) throws IOException {
        try (reader; writer) {
            char[] buffer = new char[DEFAULT_BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, read);
                total += read;
            }
            writer.flush();
            return total;
        }
    }

    // ======================== 流转字节/字符串 ========================

    /**
     * 将 InputStream 读取为字节数组
     *
     * @param input 输入流
     * @return 字节数组
     * @throws IOException IO 异常时抛出
     */
    public static byte[] toBytes(InputStream input) throws IOException {
        try (input) {
            return input.readAllBytes();
        }
    }

    /**
     * 将 InputStream 读取为 UTF-8 字符串
     *
     * @param input 输入流
     * @return UTF-8 编码的字符串
     * @throws IOException IO 异常时抛出
     */
    public static String toString(InputStream input) throws IOException {
        return toString(input, StandardCharsets.UTF_8);
    }

    /**
     * 将 InputStream 读取为指定编码的字符串
     *
     * @param input   输入流
     * @param charset 字符集
     * @return 字符串
     * @throws IOException IO 异常时抛出
     */
    public static String toString(InputStream input, Charset charset) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(input, charset)) {
            return toString(reader);
        }
    }

    /**
     * 将 Reader 读取为字符串
     *
     * @param reader 字符输入流
     * @return 字符串
     * @throws IOException IO 异常时抛出
     */
    public static String toString(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder(DEFAULT_BUFFER_SIZE);
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        return sb.toString();
    }

    /**
     * 将 Reader 按行读取为字符串列表
     *
     * @param reader 字符输入流
     * @return 行内容列表
     * @throws IOException IO 异常时抛出
     */
    public static List<String> readLines(Reader reader) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = (reader instanceof BufferedReader bufferedReader
                ? bufferedReader
                : new BufferedReader(reader))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    // ======================== 安全关闭 ========================

    /**
     * 安全关闭 Closeable 对象
     * <p>
     * 不抛出异常，适用于 finally 块中的资源释放。
     * </p>
     *
     * @param closeable 可关闭对象
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // 静默关闭
            }
        }
    }

    /**
     * 安全关闭多个 Closeable 对象
     *
     * @param closeables 可关闭对象数组
     */
    public static void closeQuietly(Closeable... closeables) {
        if (closeables != null) {
            for (Closeable closeable : closeables) {
                closeQuietly(closeable);
            }
        }
    }
}
