package com.zerx.common.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;


/**
 * IO 工具类
 * <p>
 * 提供流操作、文件读写、资源关闭等 IO 相关的便捷方法。
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

    // ======================== 文件读写 ========================

    /**
     * 读取文本文件全部内容（UTF-8 编码）
     *
     * @param path 文件路径
     * @return 文件内容字符串
     * @throws IOException IO 异常时抛出
     */
    public static String readFileString(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    /**
     * 读取文本文件全部内容
     *
     * @param path    文件路径
     * @param charset 字符集
     * @return 文件内容字符串
     * @throws IOException IO 异常时抛出
     */
    public static String readFileString(String path, Charset charset) throws IOException {
        return Files.readString(Path.of(path), charset);
    }

    /**
     * 读取文本文件全部内容
     *
     * @param path 文件路径
     * @return 文件内容字符串
     * @throws IOException IO 异常时抛出
     */
    public static String readFileString(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 读取文件为字节数组
     *
     * @param path 文件路径
     * @return 字节数组
     * @throws IOException IO 异常时抛出
     */
    public static byte[] readFileBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    /**
     * 按行读取文本文件
     *
     * @param path 文件路径
     * @return 行内容列表
     * @throws IOException IO 异常时抛出
     */
    public static List<String> readFileLines(Path path) throws IOException {
        return Files.readAllLines(path, StandardCharsets.UTF_8);
    }

    /**
     * 按行读取文本文件
     *
     * @param path    文件路径
     * @param charset 字符集
     * @return 行内容列表
     * @throws IOException IO 异常时抛出
     */
    public static List<String> readFileLines(Path path, Charset charset) throws IOException {
        return Files.readAllLines(path, charset);
    }

    /**
     * 将字符串写入文件（UTF-8 编码），文件不存在则创建，存在则覆盖
     *
     * @param path    文件路径
     * @param content 写入内容
     * @throws IOException IO 异常时抛出
     */
    public static void writeFile(String path, String content) throws IOException {
        writeFile(Path.of(path), content, StandardCharsets.UTF_8, false);
    }

    /**
     * 将字符串写入文件
     *
     * @param path    文件路径
     * @param content 写入内容
     * @param charset 字符集
     * @param append  是否追加模式
     * @throws IOException IO 异常时抛出
     */
    public static void writeFile(Path path, String content, Charset charset, boolean append) throws IOException {
        // 确保父目录存在
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        if (append) {
            Files.writeString(path, content, charset, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.writeString(path, content, charset, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    /**
     * 将字节数组写入文件
     *
     * @param path  文件路径
     * @param bytes 字节数组
     * @throws IOException IO 异常时抛出
     */
    public static void writeFile(Path path, byte[] bytes) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 按行写入文本文件
     *
     * @param path  文件路径
     * @param lines 行内容列表
     * @throws IOException IO 异常时抛出
     */
    public static void writeFileLines(Path path, Iterable<String> lines) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
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

    // ======================== 文件操作 ========================

    /**
     * 判断文件是否存在
     *
     * @param path 文件路径
     * @return 存在返回 true
     */
    public static boolean exists(Path path) {
        return path != null && Files.exists(path);
    }

    /**
     * 判断路径是否为普通文件（非目录）
     *
     * @param path 文件路径
     * @return 是普通文件返回 true
     */
    public static boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    /**
     * 判断路径是否为目录
     *
     * @param path 路径
     * @return 是目录返回 true
     */
    public static boolean isDirectory(Path path) {
        return path != null && Files.isDirectory(path);
    }

    /**
     * 删除文件或空目录
     *
     * @param path 文件路径
     * @return 删除成功返回 true
     */
    public static boolean delete(Path path) {
        if (path == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 创建目录（含父目录）
     *
     * @param path 目录路径
     * @throws IOException IO 异常时抛出
     */
    public static void mkdirs(Path path) throws IOException {
        if (path != null) {
            Files.createDirectories(path);
        }
    }

    /**
     * 获取文件大小（字节）
     *
     * @param path 文件路径
     * @return 文件大小，文件不存在返回 -1
     */
    public static long fileSize(Path path) {
        if (!isFile(path)) {
            return -1L;
        }
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    /**
     * 获取文件扩展名（不含点号）
     *
     * @param filename 文件名
     * @return 扩展名，无扩展名返回空字符串
     */
    public static String getFileExtension(String filename) {
        if (StringUtil.isBlank(filename)) {
            return StringUtil.EMPTY;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == filename.length() - 1) {
            return StringUtil.EMPTY;
        }
        return filename.substring(lastDot + 1);
    }

    /**
     * 获取不含扩展名的文件名
     *
     * @param filename 文件名
     * @return 不含扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String filename) {
        if (StringUtil.isBlank(filename)) {
            return StringUtil.EMPTY;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot <= 0) {
            return filename;
        }
        return filename.substring(0, lastDot);
    }
}
