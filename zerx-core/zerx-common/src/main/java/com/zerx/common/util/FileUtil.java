package com.zerx.common.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;


/**
 * 文件工具类
 * <p>
 * 提供文件读写、文件系统操作、文件名处理等文件相关的便捷方法。
 * 所有方法均为静态方法，调用方无需实例化。
 * </p>
 *
 * @author zerx
 */
public final class FileUtil {

    /** 私有构造器，防止实例化 */
    private FileUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 文件读取 ========================

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
     * 读取文本文件全部内容（UTF-8 编码）
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
     * 按行读取文本文件（UTF-8 编码）
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

    // ======================== 文件写入 ========================

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
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
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
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 按行写入文本文件（UTF-8 编码）
     *
     * @param path  文件路径
     * @param lines 行内容列表
     * @throws IOException IO 异常时抛出
     */
    public static void writeFileLines(Path path, Iterable<String> lines) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // ======================== 文件系统操作 ========================

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

    // ======================== 文件名操作 ========================

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
