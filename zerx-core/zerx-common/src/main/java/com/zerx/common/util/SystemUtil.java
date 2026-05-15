package com.zerx.common.util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 系统属性与运行时环境工具类
 * <p>
 * 提供操作系统判断、JDK 版本、JVM 内存、系统路径、环境变量等便捷访问方法。
 * 所有方法均为静态方法，线程安全，方便在任何场景下直接调用。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * if (SystemUtil.isLinux()) {
 *     // Linux 特定逻辑
 * }
 *
 * int cores = SystemUtil.availableProcessors();
 * long pid = SystemUtil.pid();
 * String tmpDir = SystemUtil.tmpDir();
 * }</pre>
 *
 * @author zerx
 */
public final class SystemUtil {

    /** 私有构造器，防止实例化 */
    private SystemUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 操作系统判断 ========================

    /**
     * 获取操作系统名称
     *
     * @return 操作系统名称（如 "Linux", "Windows 10", "Mac OS X"）
     */
    public static String osName() {
        return System.getProperty("os.name", "unknown");
    }

    /**
     * 当前运行环境是否为 Windows
     *
     * @return 是 Windows 返回 true
     */
    public static boolean isWindows() {
        return osName().toLowerCase().contains("windows");
    }

    /**
     * 当前运行环境是否为 Linux
     *
     * @return 是 Linux 返回 true
     */
    public static boolean isLinux() {
        return osName().toLowerCase().contains("linux");
    }

    /**
     * 当前运行环境是否为 macOS
     *
     * @return 是 macOS 返回 true
     */
    public static boolean isMac() {
        String os = osName().toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    /**
     * 当前运行环境是否为 Unix-like 系统（Linux / macOS / AIX 等）
     *
     * @return 是 Unix-like 返回 true
     */
    public static boolean isUnix() {
        return isLinux() || isMac();
    }

    // ======================== JDK 版本 ========================

    /**
     * 获取 JDK 版本号（如 "21.0.11"）
     *
     * @return JDK 版本字符串
     */
    public static String javaVersion() {
        return System.getProperty("java.version", "unknown");
    }

    /**
     * 获取 JDK 规范版本（如 "21"）
     *
     * @return JDK 规范版本字符串
     */
    public static String javaSpecificationVersion() {
        return System.getProperty("java.specification.version", "unknown");
    }

    /**
     * 获取 JVM 厂商信息（如 "Oracle Corporation"）
     *
     * @return JVM 厂商名称
     */
    public static String javaVmVendor() {
        return System.getProperty("java.vm.vendor", "unknown");
    }

    /**
     * 判断当前 JDK 版本是否大于等于指定版本
     *
     * @param targetVersion 目标版本号（如 21）
     * @return 满足返回 true
     */
    public static boolean isJavaVersionAtLeast(int targetVersion) {
        try {
            return Integer.parseInt(javaSpecificationVersion()) >= targetVersion;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ======================== 系统路径 ========================

    /**
     * 获取用户主目录
     *
     * @return 用户主目录路径
     */
    public static String userHome() {
        return System.getProperty("user.home", ".");
    }

    /**
     * 获取当前工作目录
     *
     * @return 当前工作目录路径
     */
    public static String userDir() {
        return System.getProperty("user.dir", ".");
    }

    /**
     * 获取系统临时目录
     *
     * @return 临时目录路径
     */
    public static String tmpDir() {
        return System.getProperty("java.io.tmpdir", "/tmp");
    }

    /**
     * 获取文件编码
     *
     * @return 文件编码名称（如 "UTF-8"）
     */
    public static String fileEncoding() {
        return System.getProperty("file.encoding", StandardCharsets.UTF_8.name());
    }

    /**
     * 获取文件分隔符（Windows: "\"，Linux/macOS: "/"）
     *
     * @return 文件分隔符
     */
    public static String fileSeparator() {
        return System.getProperty("file.separator", "/");
    }

    /**
     * 获取路径分隔符（Windows: ";"，Linux/macOS: ":"）
     *
     * @return 路径分隔符
     */
    public static String pathSeparator() {
        return System.getProperty("path.separator", ":");
    }

    /**
     * 获取行分隔符
     *
     * @return 行分隔符字符串
     */
    public static String lineSeparator() {
        return System.lineSeparator();
    }

    // ======================== 进程与硬件 ========================

    /**
     * 获取当前 Java 进程 ID
     *
     * @return 进程 ID
     */
    public static long pid() {
        return ProcessHandle.current().pid();
    }

    /**
     * 获取可用处理器核心数
     *
     * @return CPU 核心数
     */
    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取本机主机名
     *
     * @return 主机名，获取失败返回 "localhost"
     */
    public static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * 获取本机 IP 地址（IPv4）
     *
     * @return IP 地址字符串，获取失败返回 "127.0.0.1"
     */
    public static String hostAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    // ======================== JVM 内存 ========================

    /**
     * 获取 JVM 最大可用内存（字节）
     *
     * @return 最大可用内存字节数
     */
    public static long maxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 获取 JVM 当前已分配的堆内存总量（字节）
     *
     * @return 已分配的堆内存字节数
     */
    public static long totalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * 获取 JVM 当前空闲的堆内存（字节）
     *
     * @return 空闲内存字节数
     */
    public static long freeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * 获取 JVM 当前已使用的堆内存（字节）
     *
     * @return 已使用内存字节数
     */
    public static long usedMemory() {
        return totalMemory() - freeMemory();
    }

    /**
     * 格式化内存大小为可读字符串（如 "512 MB"、"1.5 GB"）
     *
     * @param bytes 字节数
     * @return 格式化后的内存大小字符串
     */
    public static String formatMemory(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 获取 JVM 运行时间（毫秒）
     *
     * @return JVM 启动后的运行时间毫秒数
     */
    public static long uptimeMillis() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    /**
     * 获取 JVM 运行时间的可读描述（如 "2h 30m 15s"）
     *
     * @return 运行时间描述
     */
    public static String uptime() {
        long millis = uptimeMillis();
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        hours %= 24;
        if (hours > 0 || sb.length() > 0) {
            sb.append(hours).append("h ");
        }
        minutes %= 60;
        if (minutes > 0 || sb.length() > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds % 60).append("s");
        return sb.toString();
    }

    // ======================== 环境变量 ========================

    /**
     * 安全获取环境变量
     *
     * @param name 环境变量名
     * @return 环境变量值，不存在返回 null
     */
    public static String env(String name) {
        return System.getenv(name);
    }

    /**
     * 获取环境变量，不存在时返回默认值
     *
     * @param name         环境变量名
     * @param defaultValue 默认值
     * @return 环境变量值，不存在返回默认值
     */
    public static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    /**
     * 安全获取系统属性
     *
     * @param key 系统属性名
     * @return 系统属性值，不存在返回 null
     */
    public static String getProperty(String key) {
        return System.getProperty(key);
    }

    /**
     * 获取系统属性，不存在时返回默认值
     *
     * @param key          系统属性名
     * @param defaultValue 默认值
     * @return 系统属性值，不存在返回默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }

    // ======================== 架构信息 ========================

    /**
     * 获取 CPU 架构（如 "amd64"、"aarch64"）
     *
     * @return CPU 架构字符串
     */
    public static String osArch() {
        return System.getProperty("os.arch", "unknown");
    }

    /**
     * 判断当前是否为 ARM 架构
     *
     * @return 是 ARM 架构返回 true
     */
    public static boolean isArm() {
        String arch = osArch().toLowerCase();
        return arch.contains("aarch64") || arch.contains("arm");
    }

    /**
     * 判断当前是否为 x86_64 架构
     *
     * @return 是 x86_64 架构返回 true
     */
    public static boolean isX64() {
        String arch = osArch().toLowerCase();
        return arch.contains("amd64") || arch.contains("x86_64");
    }

    /**
     * 获取系统所有环境变量的快照（只读）
     *
     * @return 环境变量 Map（只读）
     */
    public static Map<String, String> envMap() {
        return Map.copyOf(System.getenv());
    }
}
