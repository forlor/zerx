package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SystemUtil} 单元测试
 */
@DisplayName("SystemUtil - 系统属性与运行时环境工具类测试")
class SystemUtilTest {

    // ======================== 操作系统判断 ========================

    @Test
    @DisplayName("osName - 返回非空非 null 的操作系统名称")
    void osName_notNull() {
        String os = SystemUtil.osName();
        assertNotNull(os);
        assertFalse(os.isEmpty());
        assertNotEquals("unknown", os);
    }

    @Test
    @DisplayName("isWindows / isLinux / isMac - 互斥性验证")
    void osChecks_mutualExclusion() {
        boolean isWin = SystemUtil.isWindows();
        boolean isLinux = SystemUtil.isLinux();
        boolean isMac = SystemUtil.isMac();

        // 至少有一个为 true
        assertTrue(isWin || isLinux || isMac, "Should detect at least one OS type");
        // 不应同时为 true
        int count = (isWin ? 1 : 0) + (isLinux ? 1 : 0) + (isMac ? 1 : 0);
        assertEquals(1, count, "Exactly one OS type should be true");
    }

    @Test
    @DisplayName("isUnix - 在 Linux 或 Mac 上返回 true")
    void isUnix() {
        assertEquals(SystemUtil.isLinux() || SystemUtil.isMac(), SystemUtil.isUnix());
    }

    // ======================== JDK 版本 ========================

    @Test
    @DisplayName("javaVersion - 返回非空版本号")
    void javaVersion_notNull() {
        String version = SystemUtil.javaVersion();
        assertNotNull(version);
        assertFalse(version.isEmpty());
        assertNotEquals("unknown", version);
    }

    @Test
    @DisplayName("javaSpecificationVersion - 返回 JDK 规范版本")
    void javaSpecificationVersion_notNull() {
        String specVersion = SystemUtil.javaSpecificationVersion();
        assertNotNull(specVersion);
        assertFalse(specVersion.isEmpty());
        // JDK 21
        assertTrue(Integer.parseInt(specVersion) >= 21, "Should be JDK 21+");
    }

    @Test
    @DisplayName("javaVmVendor - 返回 JVM 厂商信息")
    void javaVmVendor_notNull() {
        String vendor = SystemUtil.javaVmVendor();
        assertNotNull(vendor);
        assertFalse(vendor.isEmpty());
    }

    @Test
    @DisplayName("isJavaVersionAtLeast - JDK 21 应返回 true")
    void isJavaVersionAtLeast_21() {
        assertTrue(SystemUtil.isJavaVersionAtLeast(21));
    }

    @Test
    @DisplayName("isJavaVersionAtLeast - JDK 99 应返回 false")
    void isJavaVersionAtLeast_99() {
        assertFalse(SystemUtil.isJavaVersionAtLeast(99));
    }

    @Test
    @DisplayName("isJavaVersionAtLeast - JDK 1 应返回 true")
    void isJavaVersionAtLeast_1() {
        assertTrue(SystemUtil.isJavaVersionAtLeast(1));
    }

    @Test
    @DisplayName("isJavaVersionAtLeast - 相等版本应返回 true")
    void isJavaVersionAtLeast_equal() {
        String specVersion = SystemUtil.javaSpecificationVersion();
        int version = Integer.parseInt(specVersion);
        assertTrue(SystemUtil.isJavaVersionAtLeast(version));
    }

    // ======================== 系统路径 ========================

    @Test
    @DisplayName("userHome - 返回用户主目录")
    void userHome_notNull() {
        String home = SystemUtil.userHome();
        assertNotNull(home);
        assertFalse(home.isEmpty());
        assertNotEquals(".", home);
    }

    @Test
    @DisplayName("userDir - 返回当前工作目录")
    void userDir_notNull() {
        String dir = SystemUtil.userDir();
        assertNotNull(dir);
        assertFalse(dir.isEmpty());
        assertNotEquals(".", dir);
    }

    @Test
    @DisplayName("tmpDir - 返回临时目录")
    void tmpDir_notNull() {
        String tmp = SystemUtil.tmpDir();
        assertNotNull(tmp);
        assertFalse(tmp.isEmpty());
    }

    @Test
    @DisplayName("fileEncoding - 返回文件编码")
    void fileEncoding_notNull() {
        String encoding = SystemUtil.fileEncoding();
        assertNotNull(encoding);
        assertFalse(encoding.isEmpty());
    }

    @Test
    @DisplayName("fileSeparator - 返回文件分隔符")
    void fileSeparator_valid() {
        String sep = SystemUtil.fileSeparator();
        assertNotNull(sep);
        assertTrue(sep.equals("/") || sep.equals("\\"));
    }

    @Test
    @DisplayName("pathSeparator - 返回路径分隔符")
    void pathSeparator_valid() {
        String sep = SystemUtil.pathSeparator();
        assertNotNull(sep);
        assertTrue(sep.equals(":") || sep.equals(";"));
    }

    @Test
    @DisplayName("lineSeparator - 返回行分隔符")
    void lineSeparator_notNull() {
        String sep = SystemUtil.lineSeparator();
        assertNotNull(sep);
        assertFalse(sep.isEmpty());
    }

    @Test
    @DisplayName("lineSeparator - Windows 为 \\r\\n, Unix 为 \\n")
    void lineSeparator_value() {
        String sep = SystemUtil.lineSeparator();
        if (SystemUtil.isWindows()) {
            assertEquals("\r\n", sep);
        } else {
            assertEquals("\n", sep);
        }
    }

    // ======================== 进程与硬件 ========================

    @Test
    @DisplayName("pid - 返回正数进程 ID")
    void pid_positive() {
        long pid = SystemUtil.pid();
        assertTrue(pid > 0);
    }

    @Test
    @DisplayName("availableProcessors - 返回正数核心数")
    void availableProcessors_positive() {
        int cores = SystemUtil.availableProcessors();
        assertTrue(cores >= 1);
    }

    @Test
    @DisplayName("hostname - 返回非空主机名")
    void hostname_notNull() {
        String hostname = SystemUtil.hostname();
        assertNotNull(hostname);
        assertFalse(hostname.isEmpty());
    }

    @Test
    @DisplayName("hostAddress - 返回非空 IP 地址")
    void hostAddress_notNull() {
        String address = SystemUtil.hostAddress();
        assertNotNull(address);
        assertFalse(address.isEmpty());
    }

    @Test
    @DisplayName("hostAddress - 格式验证")
    void hostAddress_format() {
        String address = SystemUtil.hostAddress();
        // Should match IPv4 or IPv6 format or fallback
        assertTrue(address.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")
                        || address.equals("127.0.0.1")
                        || address.contains(":"),
                "Should be valid IP address, got: " + address);
    }

    // ======================== JVM 内存 ========================

    @Test
    @DisplayName("maxMemory - 返回正数最大内存")
    void maxMemory_positive() {
        long max = SystemUtil.maxMemory();
        assertTrue(max > 0);
    }

    @Test
    @DisplayName("totalMemory - 返回正数已分配内存")
    void totalMemory_positive() {
        long total = SystemUtil.totalMemory();
        assertTrue(total > 0);
    }

    @Test
    @DisplayName("freeMemory - 返回非负空闲内存")
    void freeMemory_nonNegative() {
        long free = SystemUtil.freeMemory();
        assertTrue(free >= 0);
    }

    @Test
    @DisplayName("usedMemory - 返回正数已使用内存")
    void usedMemory_positive() {
        long used = SystemUtil.usedMemory();
        assertTrue(used >= 0);
    }

    @Test
    @DisplayName("usedMemory - 等于 totalMemory - freeMemory")
    void usedMemory_calculation() {
        long used = SystemUtil.usedMemory();
        long expected = SystemUtil.totalMemory() - SystemUtil.freeMemory();
        assertEquals(expected, used);
    }

    @Test
    @DisplayName("formatMemory - 字节数格式化")
    void formatMemory_bytes() {
        assertEquals("0 B", SystemUtil.formatMemory(0));
        assertEquals("500 B", SystemUtil.formatMemory(500));
        assertEquals("1.0 KB", SystemUtil.formatMemory(1024));
        assertEquals("1.5 KB", SystemUtil.formatMemory(1536));
        assertEquals("1.0 MB", SystemUtil.formatMemory(1024 * 1024));
        assertEquals("1.5 MB", SystemUtil.formatMemory(1024 * 1024 + 512 * 1024));
    }

    @Test
    @DisplayName("formatMemory - GB 级别")
    void formatMemory_gb() {
        assertEquals("1.00 GB", SystemUtil.formatMemory(1024L * 1024 * 1024));
        assertEquals("2.00 GB", SystemUtil.formatMemory(2L * 1024 * 1024 * 1024));
    }

    @Test
    @DisplayName("uptimeMillis - 返回非负运行时间")
    void uptimeMillis_nonNegative() {
        long uptime = SystemUtil.uptimeMillis();
        assertTrue(uptime >= 0);
    }

    @Test
    @DisplayName("uptime - 返回非空运行时间描述")
    void uptime_notNull() {
        String uptime = SystemUtil.uptime();
        assertNotNull(uptime);
        assertFalse(uptime.isEmpty());
        assertTrue(uptime.endsWith("s"), "Should end with 's', got: " + uptime);
    }

    @Test
    @DisplayName("uptime - 包含时间单位")
    void uptime_containsUnits() {
        String uptime = SystemUtil.uptime();
        // Should contain at least seconds
        assertTrue(uptime.contains("s"));
    }

    // ======================== 环境变量 ========================

    @Test
    @DisplayName("env(String) - 不存在的环境变量返回 null")
    void env_notFound() {
        assertNull(SystemUtil.env("NON_EXISTENT_ENV_VAR_ZERX_TEST_12345"));
    }

    @Test
    @DisplayName("env(String, String) - 不存在时返回默认值")
    void env_withDefault() {
        assertEquals("default_value", SystemUtil.env("NON_EXISTENT_ENV_VAR_ZERX", "default_value"));
    }

    @Test
    @DisplayName("env(String, String) - 存在时返回实际值")
    void env_withDefault_exists() {
        // PATH should always exist
        String path = SystemUtil.env("PATH", "default");
        assertNotNull(path);
        assertNotEquals("default", path);
    }

    @Test
    @DisplayName("getProperty(String) - 不存在的系统属性返回 null")
    void getProperty_notFound() {
        assertNull(SystemUtil.getProperty("non.existent.property.zerx.test"));
    }

    @Test
    @DisplayName("getProperty(String, String) - 不存在时返回默认值")
    void getProperty_withDefault() {
        assertEquals("default", SystemUtil.getProperty("non.existent.property.zerx", "default"));
    }

    @Test
    @DisplayName("getProperty(String, String) - 存在时返回实际值")
    void getProperty_withDefault_exists() {
        String javaHome = SystemUtil.getProperty("java.home", "default");
        assertNotNull(javaHome);
        assertNotEquals("default", javaHome);
    }

    @Test
    @DisplayName("envMap - 返回非空只读 Map")
    void envMap_notNull() {
        Map<String, String> env = SystemUtil.envMap();
        assertNotNull(env);
        assertFalse(env.isEmpty());
        // Should contain PATH or similar
        assertTrue(env.containsKey("PATH") || env.containsKey("path"));
    }

    @Test
    @DisplayName("envMap - 返回不可变 Map")
    void envMap_immutable() {
        Map<String, String> env = SystemUtil.envMap();
        assertThrows(UnsupportedOperationException.class, () -> env.put("test", "value"));
    }

    // ======================== 架构信息 ========================

    @Test
    @DisplayName("osArch - 返回非空架构字符串")
    void osArch_notNull() {
        String arch = SystemUtil.osArch();
        assertNotNull(arch);
        assertFalse(arch.isEmpty());
        assertNotEquals("unknown", arch);
    }

    @Test
    @DisplayName("isArm / isX64 - 互斥性验证")
    void archChecks_mutualExclusion() {
        boolean isArm = SystemUtil.isArm();
        boolean isX64 = SystemUtil.isX64();

        // Note: they could both be false on rare architectures (e.g., 32-bit x86)
        // But they shouldn't both be true
        if (isArm && isX64) {
            fail("Cannot be both ARM and x64");
        }
    }

    @Test
    @DisplayName("isArm / isX64 - 至少一个或均不满足，逻辑合理")
    void archChecks_reasonable() {
        String arch = SystemUtil.osArch().toLowerCase();
        boolean isArm = SystemUtil.isArm();
        boolean isX64 = SystemUtil.isX64();

        if (arch.contains("aarch64") || arch.contains("arm")) {
            assertTrue(isArm);
        }
        if (arch.contains("amd64") || arch.contains("x86_64")) {
            assertTrue(isX64);
        }
    }
}
