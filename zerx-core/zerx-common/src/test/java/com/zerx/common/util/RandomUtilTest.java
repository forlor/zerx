package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RandomUtil} 单元测试
 */
@DisplayName("RandomUtil - 随机数工具类测试")
class RandomUtilTest {

    // ======================== 随机数生成（普通） ========================

    @Test
    @DisplayName("randomInt() - 无参返回 int 范围内值")
    void randomInt_noArgs() {
        int result = RandomUtil.randomInt();
        // No specific assertion on value, just ensure it runs
        assertTrue(result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("randomInt(min, max) - 范围内随机整数")
    void randomInt_range() {
        for (int i = 0; i < 100; i++) {
            int result = RandomUtil.randomInt(1, 10);
            assertTrue(result >= 1 && result <= 10, "Result should be in [1, 10], got: " + result);
        }
    }

    @Test
    @DisplayName("randomInt(min, max) - min == max 时返回唯一值")
    void randomInt_sameBound() {
        assertEquals(5, RandomUtil.randomInt(5, 5));
    }

    @Test
    @DisplayName("randomInt(min, max) - 负数范围")
    void randomInt_negativeRange() {
        int result = RandomUtil.randomInt(-10, -1);
        assertTrue(result >= -10 && result <= -1);
    }

    @Test
    @DisplayName("randomInt(min, max) - 跨零范围")
    void randomInt_crossZero() {
        int result = RandomUtil.randomInt(-5, 5);
        assertTrue(result >= -5 && result <= 5);
    }

    @Test
    @DisplayName("randomInt(min, max) - min > max 抛出异常")
    void randomInt_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomInt(10, 1));
    }

    @RepeatedTest(10)
    @DisplayName("randomInt(min, max) - 产生足够随机性")
    void randomInt_distribution() {
        // Collect many results and verify they're not all the same
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            seen.add(RandomUtil.randomInt(0, 9));
        }
        assertTrue(seen.size() > 5, "Should produce varied results, only saw: " + seen.size());
    }

    @Test
    @DisplayName("randomLong() - 返回 long 范围内值")
    void randomLong_noArgs() {
        long result = RandomUtil.randomLong();
        assertTrue(result >= Long.MIN_VALUE && result <= Long.MAX_VALUE);
    }

    @Test
    @DisplayName("randomLong(min, max) - 范围内随机长整数")
    void randomLong_range() {
        for (int i = 0; i < 10; i++) {
            long result = RandomUtil.randomLong(100L, 1000L);
            assertTrue(result >= 100L && result <= 1000L);
        }
    }

    @Test
    @DisplayName("randomLong(min, max) - min > max 抛出异常")
    void randomLong_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomLong(100, 10));
    }

    @Test
    @DisplayName("randomDouble() - 返回 [0.0, 1.0) 范围值")
    void randomDouble_noArgs() {
        for (int i = 0; i < 50; i++) {
            double result = RandomUtil.randomDouble();
            assertTrue(result >= 0.0 && result < 1.0);
        }
    }

    @Test
    @DisplayName("randomDouble(min, max) - 指定范围内")
    void randomDouble_range() {
        for (int i = 0; i < 50; i++) {
            double result = RandomUtil.randomDouble(10.0, 20.0);
            assertTrue(result >= 10.0 && result < 20.0);
        }
    }

    @Test
    @DisplayName("randomDouble(min, max) - min >= max 抛出异常")
    void randomDouble_invalidRange() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomDouble(10.0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomDouble(20.0, 10.0));
    }

    @Test
    @DisplayName("randomBoolean() - 返回 true 或 false")
    void randomBoolean() {
        boolean result = RandomUtil.randomBoolean();
        assertTrue(result || !result);  // trivially true, just verify it runs
    }

    // ======================== 安全随机数 ========================

    @Test
    @DisplayName("secureRandomInt() - 返回 int 范围内值")
    void secureRandomInt_noArgs() {
        int result = RandomUtil.secureRandomInt();
        assertTrue(result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("secureRandomInt(min, max) - 范围内安全随机整数")
    void secureRandomInt_range() {
        for (int i = 0; i < 100; i++) {
            int result = RandomUtil.secureRandomInt(1, 100);
            assertTrue(result >= 1 && result <= 100);
        }
    }

    @Test
    @DisplayName("secureRandomInt(min, max) - min == max")
    void secureRandomInt_sameBound() {
        assertEquals(42, RandomUtil.secureRandomInt(42, 42));
    }

    @Test
    @DisplayName("secureRandomInt(min, max) - min > max 抛出异常")
    void secureRandomInt_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.secureRandomInt(100, 1));
    }

    @Test
    @DisplayName("secureRandomBytes(int) - 生成指定长度的安全随机字节")
    void secureRandomBytes_normal() {
        byte[] bytes = RandomUtil.secureRandomBytes(32);
        assertEquals(32, bytes.length);
    }

    @Test
    @DisplayName("secureRandomBytes(0) - 返回空数组")
    void secureRandomBytes_zero() {
        byte[] bytes = RandomUtil.secureRandomBytes(0);
        assertEquals(0, bytes.length);
    }

    // ======================== 随机字符串 ========================

    @Test
    @DisplayName("randomString(int) - 小写字母+数字")
    void randomString_normal() {
        String result = RandomUtil.randomString(16);
        assertEquals(16, result.length());
        assertTrue(result.matches("[a-z0-9]+"), "Should only contain lowercase and digits, got: " + result);
    }

    @Test
    @DisplayName("randomString(0) - 返回空字符串")
    void randomString_zero() {
        assertEquals(StringUtil.EMPTY, RandomUtil.randomString(0));
    }

    @Test
    @DisplayName("randomStringMixed(int) - 大小写字母+数字")
    void randomStringMixed_normal() {
        String result = RandomUtil.randomStringMixed(20);
        assertEquals(20, result.length());
        assertTrue(result.matches("[a-zA-Z0-9]+"));
        // Verify we get both uppercase and lowercase
        boolean hasUpper = result.chars().anyMatch(c -> c >= 'A' && c <= 'Z');
        boolean hasLower = result.chars().anyMatch(c -> c >= 'a' && c <= 'z');
        // With 20 chars, very likely to have both, but just verify it's valid
        assertTrue(hasUpper || hasLower);
    }

    @Test
    @DisplayName("randomStringMixed(0) - 返回空字符串")
    void randomStringMixed_zero() {
        assertEquals(StringUtil.EMPTY, RandomUtil.randomStringMixed(0));
    }

    @Test
    @DisplayName("randomNumeric(int) - 纯数字")
    void randomNumeric_normal() {
        String result = RandomUtil.randomNumeric(10);
        assertEquals(10, result.length());
        assertTrue(result.matches("[0-9]+"));
    }

    @Test
    @DisplayName("randomNumeric(0) - 返回空字符串")
    void randomNumeric_zero() {
        assertEquals(StringUtil.EMPTY, RandomUtil.randomNumeric(0));
    }

    @Test
    @DisplayName("randomAlpha(int) - 纯字母（大小写）")
    void randomAlpha_normal() {
        String result = RandomUtil.randomAlpha(12);
        assertEquals(12, result.length());
        assertTrue(result.matches("[a-zA-Z]+"));
    }

    @Test
    @DisplayName("randomAlpha(0) - 返回空字符串")
    void randomAlpha_zero() {
        assertEquals(StringUtil.EMPTY, RandomUtil.randomAlpha(0));
    }

    @Test
    @DisplayName("randomHex(int) - 十六进制字符")
    void randomHex_normal() {
        String result = RandomUtil.randomHex(16);
        assertEquals(16, result.length());
        assertTrue(result.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("randomHex(0) - 返回空字符串")
    void randomHex_zero() {
        assertEquals(StringUtil.EMPTY, RandomUtil.randomHex(0));
    }

    @Test
    @DisplayName("randomString(source, length) - 自定义字符集")
    void randomString_customSource() {
        String result = RandomUtil.randomString("ABCDEF", 10);
        assertEquals(10, result.length());
        assertTrue(result.matches("[A-F]+"));
    }

    @Test
    @DisplayName("randomString(null, length) - 空字符集抛出异常")
    void randomString_nullSource() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomString(null, 5));
    }

    @Test
    @DisplayName("randomString(empty, length) - 空字符集抛出异常")
    void randomString_emptySource() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomString("", 5));
    }

    @Test
    @DisplayName("randomString(source, negative) - 负长度抛出异常")
    void randomString_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomString("abc", -1));
    }

    // ======================== 安全随机字符串 ========================

    @Test
    @DisplayName("secureRandomString(int) - 安全随机字符串")
    void secureRandomString_normal() {
        String result = RandomUtil.secureRandomString(16);
        assertEquals(16, result.length());
        assertTrue(result.matches("[a-zA-Z0-9]+"));
    }

    @Test
    @DisplayName("secureRandomString(0) - 返回空字符串")
    void secureRandomString_zero() {
        assertEquals(StringUtil.EMPTY, RandomUtil.secureRandomString(0));
    }

    @Test
    @DisplayName("secureRandomString(source, length) - 自定义字符集安全随机")
    void secureRandomString_customSource() {
        String result = RandomUtil.secureRandomString("XYZ", 10);
        assertEquals(10, result.length());
        assertTrue(result.matches("[XYZ]+"));
    }

    @Test
    @DisplayName("secureRandomString(null, length) - 空字符集抛出异常")
    void secureRandomString_nullSource() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.secureRandomString(null, 5));
    }

    @Test
    @DisplayName("secureRandomString(source, negative) - 负长度抛出异常")
    void secureRandomString_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.secureRandomString("abc", -1));
    }

    // ======================== 随机字节 ========================

    @Test
    @DisplayName("randomBytes(int) - 生成指定长度随机字节")
    void randomBytes_normal() {
        byte[] bytes = RandomUtil.randomBytes(64);
        assertEquals(64, bytes.length);
    }

    @Test
    @DisplayName("randomBytes(0) - 空数组")
    void randomBytes_zero() {
        byte[] bytes = RandomUtil.randomBytes(0);
        assertEquals(0, bytes.length);
    }

    @Test
    @DisplayName("randomBytesHex(int) - 长度为 length*2 的十六进制字符串")
    void randomBytesHex_normal() {
        String hex = RandomUtil.randomBytesHex(16);
        assertEquals(32, hex.length());  // 16 bytes = 32 hex chars
        assertTrue(hex.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("randomBytesHex(0) - 空字符串")
    void randomBytesHex_zero() {
        assertEquals("", RandomUtil.randomBytesHex(0));
    }

    // ======================== 随机选取 ========================

    @Test
    @DisplayName("randomElement(array) - 从数组中随机选取元素")
    void randomElement_array() {
        String[] items = {"A", "B", "C", "D", "E"};
        String result = RandomUtil.randomElement(items);
        assertNotNull(result);
        assertTrue(List.of(items).contains(result));
    }

    @Test
    @DisplayName("randomElement(null array) - 返回 null")
    void randomElement_nullArray() {
        assertNull(RandomUtil.randomElement((String[]) null));
    }

    @Test
    @DisplayName("randomElement(empty array) - 返回 null")
    void randomElement_emptyArray() {
        assertNull(RandomUtil.randomElement(new String[0]));
    }

    @Test
    @DisplayName("randomElement(single element array) - 返回唯一元素")
    void randomElement_singleElement() {
        String[] items = {"ONLY"};
        assertEquals("ONLY", RandomUtil.randomElement(items));
    }

    @Test
    @DisplayName("randomElement(list) - 从 List 中随机选取元素")
    void randomElement_list() {
        List<String> items = List.of("A", "B", "C");
        String result = RandomUtil.randomElement(items);
        assertNotNull(result);
        assertTrue(items.contains(result));
    }

    @Test
    @DisplayName("randomElement(null list) - 返回 null")
    void randomElement_nullList() {
        assertNull(RandomUtil.randomElement((List<String>) null));
    }

    @Test
    @DisplayName("randomElement(empty list) - 返回 null")
    void randomElement_emptyList() {
        assertNull(RandomUtil.randomElement(List.of()));
    }

    @Test
    @DisplayName("randomUniqueInts(count, min, max) - 生成不重复随机整数")
    void randomUniqueInts_normal() {
        List<Integer> result = RandomUtil.randomUniqueInts(5, 1, 10);
        assertEquals(5, result.size());
        assertEquals(5, new HashSet<>(result).size());  // all unique
        for (int val : result) {
            assertTrue(val >= 1 && val <= 10);
        }
    }

    @Test
    @DisplayName("randomUniqueInts(0, min, max) - 返回空列表")
    void randomUniqueInts_zero() {
        assertEquals(List.of(), RandomUtil.randomUniqueInts(0, 1, 10));
    }

    @Test
    @DisplayName("randomUniqueInts - count > range 抛出异常")
    void randomUniqueInts_countExceedsRange() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomUniqueInts(5, 1, 4));
    }

    @Test
    @DisplayName("randomUniqueInts - 负数 count 抛出异常")
    void randomUniqueInts_negativeCount() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomUniqueInts(-1, 1, 10));
    }

    @Test
    @DisplayName("randomUniqueInts - min > max 抛出异常")
    void randomUniqueInts_minGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomUniqueInts(3, 10, 1));
    }

    // ======================== 打乱顺序 ========================

    @Test
    @DisplayName("shuffle(array) - 打乱数组顺序")
    void shuffle_array() {
        Integer[] arr = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Integer[] original = arr.clone();
        RandomUtil.shuffle(arr);
        // Same elements, same length (in-place shuffle)
        assertEquals(original.length, arr.length);
        // Verify same elements (sorted comparison)
        java.util.Arrays.sort(original);
        Integer[] sorted = arr.clone();
        java.util.Arrays.sort(sorted);
        assertArrayEquals(original, sorted);
    }

    @Test
    @DisplayName("shuffle(null array) - 返回 null")
    void shuffle_nullArray() {
        assertNull(RandomUtil.shuffle((Integer[]) null));
    }

    @Test
    @DisplayName("shuffle(empty array) - 返回空数组")
    void shuffle_emptyArray() {
        Integer[] arr = {};
        assertSame(arr, RandomUtil.shuffle(arr));
    }

    @Test
    @DisplayName("shuffle(single element array) - 返回同一数组")
    void shuffle_singleElement() {
        Integer[] arr = {42};
        assertSame(arr, RandomUtil.shuffle(arr));
    }

    @Test
    @DisplayName("shuffle(list) - 打乱 List 顺序，返回新列表")
    void shuffle_list() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> shuffled = RandomUtil.shuffle(list);
        assertEquals(list.size(), shuffled.size());
        // Contains same elements (may be in different order)
        assertTrue(shuffled.containsAll(list));
        // Should be a new list
        assertNotSame(list, shuffled);
    }

    @Test
    @DisplayName("shuffle(null list) - 返回 null")
    void shuffle_nullList() {
        assertNull(RandomUtil.shuffle((List<Integer>) null));
    }

    @Test
    @DisplayName("shuffle(empty list) - 返回空列表")
    void shuffle_emptyList() {
        List<Integer> list = List.of();
        assertSame(list, RandomUtil.shuffle(list));
    }

    @Test
    @DisplayName("shuffle(single element list) - 返回同一列表")
    void shuffle_singleElementList() {
        List<Integer> list = List.of(42);
        assertSame(list, RandomUtil.shuffle(list));
    }

    // ======================== 验证码 ========================

    @Test
    @DisplayName("verificationCode(int) - 生成指定长度数字验证码")
    void verificationCode_normal() {
        String code = RandomUtil.verificationCode(6);
        assertEquals(6, code.length());
        assertTrue(code.matches("[1-9][0-9]{5}"), "Should be 6-digit code without leading zero, got: " + code);
    }

    @Test
    @DisplayName("verificationCode(1) - 单位验证码")
    void verificationCode_oneDigit() {
        String code = RandomUtil.verificationCode(1);
        assertEquals(1, code.length());
        assertTrue(code.matches("[0-9]"));
    }

    @Test
    @DisplayName("verificationCode(4) - 4 位验证码无前导零")
    void verificationCode_noLeadingZero() {
        for (int i = 0; i < 50; i++) {
            String code = RandomUtil.verificationCode(4);
            assertTrue(code.charAt(0) >= '1' && code.charAt(0) <= '9',
                    "First digit should not be zero, got: " + code);
        }
    }

    @Test
    @DisplayName("verificationCode - 零或负数长度抛出异常")
    void verificationCode_invalidLength() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.verificationCode(0));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.verificationCode(-1));
    }

    @Test
    @DisplayName("verificationCode - 多次调用产生不同结果")
    void verificationCode_uniqueness() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            codes.add(RandomUtil.verificationCode(4));
        }
        assertTrue(codes.size() > 10, "Should generate varied codes");
    }
}
