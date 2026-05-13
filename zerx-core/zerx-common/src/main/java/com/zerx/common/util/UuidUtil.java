package com.zerx.common.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * UUIDv7 工具类
 * <p>
 * 基于 RFC 9562 规范实现 UUIDv7（时间排序 UUID）。
 * UUIDv7 由 48 位毫秒级 Unix 时间戳 + 4 位版本标识 + 12 位随机位 + 2 位变体标识 + 62 位随机位组成，
 * 天然支持按时间排序，适合用作数据库主键、分布式 ID 等场景。
 * </p>
 *
 * <h3>UUIDv7 结构（128 位）：</h3>
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         unix_ts_ms                            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |          unix_ts_ms           |  ver  |       rand_a          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |var|                       rand_b                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           rand_b                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * <h3>特点：</h3>
 * <ul>
 *   <li>基于时间戳，天然有序，适合索引场景</li>
 *   <li>128 位长度，碰撞概率极低</li>
 *   <li>相比 UUIDv4 在 B+ 树索引中插入性能更优（无随机写入开销）</li>
 *   <li>纯 JDK 实现，无第三方依赖</li>
 * </ul>
 *
 * @author zerx
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9562">RFC 9562 - Universally Unique IDentifiers (UUIDs)</a>
 */
public final class UuidUtil {

    /** UUIDv7 版本号 */
    private static final int UUID_VERSION = 7;

    /** 变体标识（RFC 9562 变体 2: 10xxxxxx） */
    private static final byte VARIANT = (byte) 0x80;

    /** 毫秒级时间戳在 UUID 中的 bit 偏移量 */
    private static final long TIMESTAMP_SHIFT = 16;

    /** 时间戳占用的位数（48 位） */
    private static final long TIMESTAMP_MASK = 0xFFFFFFFFFFFFL;

    /** 安全随机数生成器，用于生成随机位 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 普通随机数生成器，性能更高但安全性较低 */
    private static final java.util.Random RANDOM = new java.util.Random();

    /** 上一次生成的时间戳，用于同一毫秒内递增处理（仅在 synchronized 方法内访问） */
    private static long lastTimestamp = -1L;

    /** 同一毫秒内的递增计数器（12 位，仅在 synchronized 方法内访问） */
    private static int counter = 0;

    /** 计数器最大值（12 位 = 4095） */
    private static final int MAX_COUNTER = 0x0FFF;

    /** Hex 格式化器 */
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /** 私有构造器，防止实例化 */
    private UuidUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== UUIDv7 生成 ========================

    /**
     * 生成一个 UUIDv7（使用安全随机数）
     * <p>
     * 基于 RFC 9562 规范，前 48 位为毫秒级 Unix 时间戳，后 74 位为随机数据。
     * 生成的 UUID 天然按时间排序，适合用作分布式唯一 ID。
     * </p>
     *
     * @return UUIDv7 实例
     */
    public static UUID uuidv7() {
        return uuidv7(false);
    }

    /**
     * 生成一个 UUIDv7（使用快速随机数）
     * <p>
     * 使用 {@link java.util.Random} 替代 {@link SecureRandom}，性能更高，
     * 适用于对加密安全性无要求但需要高性能的场景（如日志追踪 ID、临时标识等）。
     * </p>
     *
     * @return UUIDv7 实例
     */
    public static UUID fastUuidv7() {
        return uuidv7(true);
    }

    /**
     * 生成一个 UUIDv7（单调递增保证）
     * <p>
     * 在同一毫秒内多次调用时，会自动递增低位随机数，确保严格单调递增。
     * 适用于需要强排序保证的场景（如数据库主键、消息排序等）。
     * </p>
     *
     * @return 单调递增的 UUIDv7 实例
     */
    public static synchronized UUID monotonicUuidv7() {
        long timestamp = currentTimestampMillis();

        long msb;
        long lsb;

        if (timestamp == lastTimestamp) {
            // 同一毫秒内：递增计数器
            if (counter >= MAX_COUNTER) {
                // 计数器溢出，等待到下一毫秒
                waitNextMillis(timestamp);
                timestamp = currentTimestampMillis();
                counter = 0;
            }
            msb = (timestamp << TIMESTAMP_SHIFT) | ((long) UUID_VERSION << 12) | counter;
            lsb = (long) VARIANT << 56 | RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;
            counter++;
        } else {
            // 新的毫秒：重置计数器
            counter = 0;
            msb = (timestamp << TIMESTAMP_SHIFT) | ((long) UUID_VERSION << 12) | (RANDOM.nextInt(0x1000));
            lsb = (long) VARIANT << 56 | (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL);
        }

        lastTimestamp = timestamp;
        return new UUID(msb, lsb);
    }

    /**
     * 生成多个 UUIDv7（批量生成）
     * <p>
     * 使用安全随机数生成器批量生成，适用于需要预生成 ID 的场景。
     * </p>
     *
     * @param count 生成数量（最大 10000）
     * @return UUIDv7 数组
     * @throws IllegalArgumentException 数量超出范围时抛出
     */
    public static UUID[] uuidv7Batch(int count) {
        if (count < 1 || count > 10000) {
            throw new IllegalArgumentException("生成数量需在 1 ~ 10000 之间，当前: " + count);
        }
        UUID[] result = new UUID[count];
        for (int i = 0; i < count; i++) {
            result[i] = uuidv7();
        }
        return result;
    }

    // ======================== 字符串形式 ========================

    /**
     * 生成 UUIDv7 的标准字符串形式（带连字符）
     * <p>
     * 示例输出：0194a2b3-c4d5-7e6f-8a9b-0c1d2e3f4a5b
     * </p>
     *
     * @return 36 字符的 UUID 字符串
     */
    public static String uuidv7String() {
        return uuidv7().toString();
    }

    /**
     * 生成 UUIDv7 的无连字符形式
     * <p>
     * 示例输出：0194a2b3c4d57e6f8a9b0c1d2e3f4a5b
     * </p>
     *
     * @return 32 字符的十六进制字符串
     */
    public static String uuidv7Hex() {
        return removeHyphens(uuidv7().toString());
    }

    /**
     * 生成单调递增 UUIDv7 的字符串形式（带连字符）
     *
     * @return 36 字符的 UUID 字符串
     */
    public static String monotonicUuidv7String() {
        return monotonicUuidv7().toString();
    }

    /**
     * 生成单调递增 UUIDv7 的无连字符形式
     *
     * @return 32 字符的十六进制字符串
     */
    public static String monotonicUuidv7Hex() {
        return removeHyphens(monotonicUuidv7().toString());
    }

    // ======================== 工具方法 ========================

    /**
     * 从 UUIDv7 中提取毫秒级时间戳
     *
     * @param uuid UUIDv7 实例
     * @return 毫秒级 Unix 时间戳，非 UUIDv7 返回 -1
     */
    public static long extractTimestamp(UUID uuid) {
        if (uuid == null || uuid.version() != UUID_VERSION) {
            return -1L;
        }
        return (uuid.getMostSignificantBits() >>> TIMESTAMP_SHIFT) & TIMESTAMP_MASK;
    }

    /**
     * 将 UUID 时间戳转换为 {@link Instant}
     *
     * @param uuid UUIDv7 实例
     * @return Instant 时间，非 UUIDv7 返回 null
     */
    public static Instant extractInstant(UUID uuid) {
        long timestamp = extractTimestamp(uuid);
        if (timestamp < 0) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp);
    }

    /**
     * 判断 UUID 是否为 UUIDv7
     *
     * @param uuid UUID 实例
     * @return 是 UUIDv7 返回 true
     */
    public static boolean isUuidv7(UUID uuid) {
        return uuid != null && uuid.version() == UUID_VERSION;
    }

    /**
     * 生成一个随机的 UUIDv4（兼容工具方法）
     *
     * @return UUIDv4 实例
     */
    public static UUID uuidv4() {
        return UUID.randomUUID();
    }

    /**
     * 生成 UUIDv4 的标准字符串形式（带连字符）
     *
     * @return 36 字符的 UUID 字符串
     */
    public static String uuidv4String() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成 UUIDv4 的无连字符形式
     *
     * @return 32 字符的十六进制字符串
     */
    public static String uuidv4Hex() {
        return removeHyphens(UUID.randomUUID().toString());
    }

    // ======================== 内部方法 ========================

    /**
     * 内部生成 UUIDv7
     *
     * @param fast 是否使用快速随机数
     * @return UUIDv7 实例
     */
    private static UUID uuidv7(boolean fast) {
        long timestamp = currentTimestampMillis();
        java.util.Random random = fast ? RANDOM : SECURE_RANDOM;

        // 构建 MSB：48 位时间戳 | 4 位版本 | 12 位随机
        long msb = (timestamp << TIMESTAMP_SHIFT)
                | ((long) UUID_VERSION << 12)
                | (random.nextInt(0x1000) & 0x0FFFL);

        // 构建 LSB：2 位变体 | 62 位随机
        long lsb = (long) VARIANT << 56
                | (random.nextLong() & 0x3FFFFFFFFFFFFFFFL);

        return new UUID(msb, lsb);
    }

    /**
     * 获取当前毫秒级时间戳
     *
     * @return 毫秒时间戳
     */
    private static long currentTimestampMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 等待到下一个毫秒
     *
     * @param lastTimestamp 上一次的时间戳
     */
    private static void waitNextMillis(long lastTimestamp) {
        long timestamp = currentTimestampMillis();
        while (timestamp <= lastTimestamp) {
            Thread.onSpinWait();
            timestamp = currentTimestampMillis();
        }
    }

    /**
     * 移除 UUID 字符串中的连字符
     *
     * @param uuidStr 带连字符的 UUID 字符串
     * @return 无连字符的字符串
     */
    private static String removeHyphens(String uuidStr) {
        if (uuidStr == null) {
            return null;
        }
        return uuidStr.replace("-", "");
    }
}
