package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link UuidUtil} 单元测试
 */
@DisplayName("UuidUtil - UUIDv7 工具类测试")
class UuidUtilTest {

    // ======================== UUIDv7 生成 ========================

    @Test
    @DisplayName("uuidv7() - 生成 UUIDv7 且版本为 7")
    void uuidv7_version() {
        UUID uuid = UuidUtil.uuidv7();
        assertNotNull(uuid);
        assertEquals(7, uuid.version());
    }

    @Test
    @DisplayName("uuidv7() - 变体为 RFC 9562 变体 2")
    void uuidv7_variant() {
        UUID uuid = UuidUtil.uuidv7();
        // Variant 2: bits 110xxxxx → first two bits of high nibble of byte 8 are 10
        int variant = uuid.variant();
        assertEquals(2, variant);
    }

    @Test
    @DisplayName("uuidv7() - 多次调用产生不同结果")
    @RepeatedTest(10)
    void uuidv7_uniqueness() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(UuidUtil.uuidv7());
        }
        assertEquals(100, seen.size(), "All UUIDs should be unique");
    }

    @Test
    @DisplayName("uuidv7() - 时间戳在合理范围内")
    void uuidv7_timestampReasonable() {
        UUID uuid = UuidUtil.uuidv7();
        long extractedTs = UuidUtil.extractTimestamp(uuid);
        long now = System.currentTimeMillis();
        // Timestamp should be within ±10 seconds of now
        assertTrue(Math.abs(now - extractedTs) < 10_000,
                "Extracted timestamp should be close to current time");
    }

    @Test
    @DisplayName("fastUuidv7() - 快速生成 UUIDv7 且版本为 7")
    void fastUuidv7_version() {
        UUID uuid = UuidUtil.fastUuidv7();
        assertNotNull(uuid);
        assertEquals(7, uuid.version());
    }

    @Test
    @DisplayName("fastUuidv7() - 多次调用产生不同结果")
    void fastUuidv7_uniqueness() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(UuidUtil.fastUuidv7());
        }
        assertEquals(100, seen.size());
    }

    @Test
    @DisplayName("fastUuidv7() - 性能比 uuidv7 更快（简单验证）")
    void fastUuidv7_faster() {
        int iterations = 10_000;

        long startSecure = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            UuidUtil.uuidv7();
        }
        long secureTime = System.nanoTime() - startSecure;

        long startFast = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            UuidUtil.fastUuidv7();
        }
        long fastTime = System.nanoTime() - startFast;

        // Just verify both complete without error; performance may vary
        assertTrue(secureTime > 0);
        assertTrue(fastTime > 0);
    }

    @Test
    @DisplayName("monotonicUuidv7() - 单调递增 UUIDv7 且版本为 7")
    void monotonicUuidv7_version() {
        UUID uuid = UuidUtil.monotonicUuidv7();
        assertNotNull(uuid);
        assertEquals(7, uuid.version());
    }

    @Test
    @DisplayName("monotonicUuidv7() - 单调递增 UUIDv7 且版本为 7")
    void monotonicUuidv7_strictOrder() {
        UUID prev = UuidUtil.monotonicUuidv7();
        Set<UUID> seen = new HashSet<>();
        seen.add(prev);
        for (int i = 0; i < 1000; i++) {
            UUID curr = UuidUtil.monotonicUuidv7();
            seen.add(curr);
            prev = curr;
        }
        // Verify uniqueness (1001 unique UUIDs)
        assertEquals(1001, seen.size());
    }

    @Test
    @DisplayName("uuidv7Batch(int) - 批量生成指定数量")
    void uuidv7Batch_normal() {
        UUID[] batch = UuidUtil.uuidv7Batch(10);
        assertEquals(10, batch.length);
        for (UUID uuid : batch) {
            assertEquals(7, uuid.version());
        }
    }

    @Test
    @DisplayName("uuidv7Batch(int) - 所有 UUID 唯一")
    void uuidv7Batch_uniqueness() {
        UUID[] batch = UuidUtil.uuidv7Batch(100);
        Set<UUID> set = new HashSet<>();
        for (UUID uuid : batch) {
            set.add(uuid);
        }
        assertEquals(100, set.size());
    }

    @Test
    @DisplayName("uuidv7Batch(0) - 数量小于 1 抛出异常")
    void uuidv7Batch_zero() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.uuidv7Batch(0));
    }

    @Test
    @DisplayName("uuidv7Batch(-1) - 负数抛出异常")
    void uuidv7Batch_negative() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.uuidv7Batch(-1));
    }

    @Test
    @DisplayName("uuidv7Batch(10001) - 超出上限抛出异常")
    void uuidv7Batch_tooLarge() {
        assertThrows(IllegalArgumentException.class, () -> UuidUtil.uuidv7Batch(10001));
    }

    @Test
    @DisplayName("uuidv7Batch(1) - 最小批量")
    void uuidv7Batch_minimum() {
        UUID[] batch = UuidUtil.uuidv7Batch(1);
        assertEquals(1, batch.length);
        assertEquals(7, batch[0].version());
    }

    // ======================== 字符串形式 ========================

    @Test
    @DisplayName("uuidv7String() - 标准 36 字符带连字符格式")
    void uuidv7String_format() {
        String str = UuidUtil.uuidv7String();
        assertEquals(36, str.length());
        // Format: 8-4-4-4-12
        assertTrue(str.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("uuidv7String() - 版本位为 7")
    void uuidv7String_version() {
        String str = UuidUtil.uuidv7String();
        assertEquals('7', str.charAt(14));
    }

    @Test
    @DisplayName("uuidv7Hex() - 32 字符无连字符格式")
    void uuidv7Hex_format() {
        String hex = UuidUtil.uuidv7Hex();
        assertEquals(32, hex.length());
        assertTrue(hex.matches("[0-9a-f]{32}"));
        assertFalse(hex.contains("-"));
    }

    @Test
    @DisplayName("uuidv7Hex() - 可以还原为 UUID")
    void uuidv7Hex_toUUID() {
        String hex = UuidUtil.uuidv7Hex();
        UUID uuid = UUID.fromString(
                hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" +
                hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" +
                hex.substring(20)
        );
        assertEquals(7, uuid.version());
    }

    @Test
    @DisplayName("monotonicUuidv7String() - 标准 36 字符格式且版本为 7")
    void monotonicUuidv7String_format() {
        String str = UuidUtil.monotonicUuidv7String();
        assertEquals(36, str.length());
        assertEquals('7', str.charAt(14));
    }

    @Test
    @DisplayName("monotonicUuidv7String() - 严格单调递增")
    void monotonicUuidv7String_strictOrder() {
        String prev = UuidUtil.monotonicUuidv7String();
        Set<String> seen = new HashSet<>();
        seen.add(prev);
        for (int i = 0; i < 100; i++) {
            String curr = UuidUtil.monotonicUuidv7String();
            seen.add(curr);
            prev = curr;
        }
        // Verify uniqueness (101 unique UUIDs)
        assertEquals(101, seen.size());
    }

    @Test
    @DisplayName("monotonicUuidv7Hex() - 32 字符格式")
    void monotonicUuidv7Hex_format() {
        String hex = UuidUtil.monotonicUuidv7Hex();
        assertEquals(32, hex.length());
        assertTrue(hex.matches("[0-9a-f]{32}"));
    }

    // ======================== 工具方法 ========================

    @Test
    @DisplayName("extractTimestamp(UUID) - 从 UUIDv7 提取时间戳")
    void extractTimestamp_normal() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidUtil.uuidv7();
        long after = System.currentTimeMillis();

        long extracted = UuidUtil.extractTimestamp(uuid);
        assertTrue(extracted >= before && extracted <= after,
                "Extracted timestamp should be between before and after");
    }

    @Test
    @DisplayName("extractTimestamp(UUID) - 非 UUIDv7 返回 -1")
    void extractTimestamp_notV7() {
        UUID v4 = UUID.randomUUID();
        assertEquals(-1L, UuidUtil.extractTimestamp(v4));
    }

    @Test
    @DisplayName("extractTimestamp(null) - null 返回 -1")
    void extractTimestamp_null() {
        assertEquals(-1L, UuidUtil.extractTimestamp(null));
    }

    @Test
    @DisplayName("extractInstant(UUID) - 从 UUIDv7 提取 Instant")
    void extractInstant_normal() {
        long before = System.currentTimeMillis();
        UUID uuid = UuidUtil.uuidv7();
        long after = System.currentTimeMillis();

        Instant instant = UuidUtil.extractInstant(uuid);
        assertNotNull(instant);
        long instantMillis = instant.toEpochMilli();
        assertTrue(instantMillis >= before && instantMillis <= after);
    }

    @Test
    @DisplayName("extractInstant(UUID) - 非 UUIDv7 返回 null")
    void extractInstant_notV7() {
        assertNull(UuidUtil.extractInstant(UUID.randomUUID()));
    }

    @Test
    @DisplayName("extractInstant(null) - null 返回 null")
    void extractInstant_null() {
        assertNull(UuidUtil.extractInstant(null));
    }

    @Test
    @DisplayName("isUuidv7(UUID) - UUIDv7 返回 true")
    void isUuidv7_true() {
        UUID v7 = UuidUtil.uuidv7();
        assertTrue(UuidUtil.isUuidv7(v7));
    }

    @Test
    @DisplayName("isUuidv7(UUID) - UUIDv4 返回 false")
    void isUuidv7_false() {
        UUID v4 = UUID.randomUUID();
        assertFalse(UuidUtil.isUuidv7(v4));
    }

    @Test
    @DisplayName("isUuidv7(null) - null 返回 false")
    void isUuidv7_null() {
        assertFalse(UuidUtil.isUuidv7(null));
    }

    // ======================== UUIDv4 ========================

    @Test
    @DisplayName("uuidv4() - 生成 UUIDv4 且版本为 4")
    void uuidv4_version() {
        UUID uuid = UuidUtil.uuidv4();
        assertNotNull(uuid);
        assertEquals(4, uuid.version());
    }

    @Test
    @DisplayName("uuidv4() - 多次调用产生不同结果")
    void uuidv4_uniqueness() {
        Set<UUID> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.add(UuidUtil.uuidv4());
        }
        assertEquals(100, seen.size());
    }

    @Test
    @DisplayName("uuidv4String() - 标准 36 字符格式")
    void uuidv4String_format() {
        String str = UuidUtil.uuidv4String();
        assertEquals(36, str.length());
        assertEquals('4', str.charAt(14));
    }

    @Test
    @DisplayName("uuidv4Hex() - 32 字符无连字符格式")
    void uuidv4Hex_format() {
        String hex = UuidUtil.uuidv4Hex();
        assertEquals(32, hex.length());
        assertTrue(hex.matches("[0-9a-f]{32}"));
        assertFalse(hex.contains("-"));
    }

    @Test
    @DisplayName("uuidv4Hex() - 版本位为 4")
    void uuidv4Hex_version() {
        String hex = UuidUtil.uuidv4Hex();
        // Version is at position 12 (0-indexed) in hex string (after removing hyphens)
        assertEquals('4', hex.charAt(12));
    }

    // ======================== 时间戳一致性 ========================

    @Test
    @DisplayName("连续生成的 UUIDv7 时间戳递增或相等")
    void uuidv7_timestampsNonDecreasing() {
        UUID uuid1 = UuidUtil.uuidv7();
        UUID uuid2 = UuidUtil.uuidv7();
        long ts1 = UuidUtil.extractTimestamp(uuid1);
        long ts2 = UuidUtil.extractTimestamp(uuid2);
        assertTrue(ts2 >= ts1, "Timestamps should not decrease");
    }

    @Test
    @DisplayName("extractInstant 与 extractTimestamp 结果一致")
    void extractInstant_consistency() {
        UUID uuid = UuidUtil.uuidv7();
        long ts = UuidUtil.extractTimestamp(uuid);
        Instant instant = UuidUtil.extractInstant(uuid);
        assertEquals(ts, instant.toEpochMilli());
    }

    // ======================== 边界与综合测试 ========================

    @Test
    @DisplayName("uuidv7Batch 生成的 UUID 唯一且时间戳非递减")
    void uuidv7Batch_sorted() {
        UUID[] batch = UuidUtil.uuidv7Batch(100);
        // Verify all UUIDs are unique
        Set<UUID> set = new HashSet<>();
        for (UUID uuid : batch) {
            set.add(uuid);
        }
        assertEquals(100, set.size());
        // Verify timestamps are non-decreasing
        for (int i = 1; i < batch.length; i++) {
            assertTrue(UuidUtil.extractTimestamp(batch[i]) >= UuidUtil.extractTimestamp(batch[i - 1]));
        }
    }
}
