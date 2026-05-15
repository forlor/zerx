package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AssertUtil} 单元测试
 */
@DisplayName("AssertUtil 断言工具类测试")
class AssertUtilTest {

    // ======================== 空值校验 ========================

    @Nested
    @DisplayName("notNull 测试")
    class NotNullTests {

        @Test
        @DisplayName("非 null 通过，返回原对象")
        void notNull_pass() {
            String value = "hello";
            assertSame(value, AssertUtil.notNull(value, "不应为空"));
        }

        @Test
        @DisplayName("null 抛出 IllegalArgumentException")
        void notNull_fail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notNull(null, "不能为空"));
            assertEquals("不能为空", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("notBlank 测试")
    class NotBlankTests {

        @Test
        @DisplayName("非空白字符串通过")
        void notBlank_pass() {
            assertEquals("hello", AssertUtil.notBlank("hello", "不能为空"));
        }

        @Test
        @DisplayName("null 抛出异常")
        void notBlank_null() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBlank(null, "不能为空"));
        }

        @Test
        @DisplayName("空字符串抛出异常")
        void notBlank_empty() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBlank("", "不能为空"));
        }

        @Test
        @DisplayName("纯空格抛出异常")
        void notBlank_whitespace() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.notBlank("   ", "不能为空"));
        }
    }

    @Nested
    @DisplayName("notEmpty(String) 测试")
    class NotEmptyStringTests {

        @Test
        @DisplayName("非空字符串通过")
        void notEmpty_pass() {
            assertEquals("hello", AssertUtil.notEmpty("hello", "不能为空"));
        }

        @Test
        @DisplayName("空格字符串通过（不做 trim）")
        void notEmpty_whitespace() {
            assertEquals("   ", AssertUtil.notEmpty("   ", "不能为空"));
        }

        @Test
        @DisplayName("null 抛出异常")
        void notEmpty_null() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty((String) null, "不能为空"));
        }

        @Test
        @DisplayName("空字符串抛出异常")
        void notEmpty_empty() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEmpty("", "不能为空"));
        }
    }

    @Nested
    @DisplayName("notEmpty(Collection) 测试")
    class NotEmptyCollectionTests {

        @Test
        @DisplayName("非空集合通过")
        void notEmpty_pass() {
            List<String> list = List.of("a");
            assertSame(list, AssertUtil.notEmpty(list, "集合不能为空"));
        }

        @Test
        @DisplayName("null 集合抛出异常")
        void notEmpty_null() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notEmpty((List<?>) null, "集合不能为空"));
        }

        @Test
        @DisplayName("空集合抛出异常")
        void notEmpty_empty() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notEmpty(List.of(), "集合不能为空"));
        }
    }

    @Nested
    @DisplayName("notEmpty(Map) 测试")
    class NotEmptyMapTests {

        @Test
        @DisplayName("非空 Map 通过")
        void notEmpty_pass() {
            Map<String, String> map = Map.of("k", "v");
            assertSame(map, AssertUtil.notEmpty(map, "Map不能为空"));
        }

        @Test
        @DisplayName("null Map 抛出异常")
        void notEmpty_null() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notEmpty((Map<?, ?>) null, "Map不能为空"));
        }

        @Test
        @DisplayName("空 Map 抛出异常")
        void notEmpty_empty() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notEmpty(Map.of(), "Map不能为空"));
        }
    }

    @Nested
    @DisplayName("notEmpty(Array) 测试")
    class NotEmptyArrayTests {

        @Test
        @DisplayName("非空数组通过")
        void notEmpty_pass() {
            String[] arr = {"a"};
            assertSame(arr, AssertUtil.notEmpty(arr, "数组不能为空"));
        }

        @Test
        @DisplayName("null 数组抛出异常")
        void notEmpty_null() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notEmpty((String[]) null, "数组不能为空"));
        }

        @Test
        @DisplayName("空数组抛出异常")
        void notEmpty_empty() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.notEmpty(new String[0], "数组不能为空"));
        }
    }

    // ======================== 数值校验 ========================

    @Nested
    @DisplayName("gt 测试")
    class GtTests {

        @Test
        @DisplayName("gt(int) - 值大于 min 通过")
        void gt_int_pass() {
            assertDoesNotThrow(() -> AssertUtil.gt(5, 3, "必须大于3"));
        }

        @Test
        @DisplayName("gt(int) - 值等于 min 抛出异常")
        void gt_int_equal() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.gt(3, 3, "必须大于3"));
        }

        @Test
        @DisplayName("gt(int) - 值小于 min 抛出异常")
        void gt_int_less() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.gt(2, 3, "必须大于3"));
        }

        @Test
        @DisplayName("gt(long) - 值大于 min 通过")
        void gt_long_pass() {
            assertDoesNotThrow(() -> AssertUtil.gt(5L, 3L, "必须大于3"));
        }

        @Test
        @DisplayName("gt(long) - 值等于 min 抛出异常")
        void gt_long_equal() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.gt(3L, 3L, "必须大于3"));
        }

        @Test
        @DisplayName("gt(double) - 值大于 min 通过")
        void gt_double_pass() {
            assertDoesNotThrow(() -> AssertUtil.gt(5.0, 3.0, "必须大于3"));
        }

        @Test
        @DisplayName("gt(double) - 值等于 min 抛出异常")
        void gt_double_equal() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.gt(3.0, 3.0, "必须大于3"));
        }

        @Test
        @DisplayName("gt(BigDecimal) - 值大于 min 通过")
        void gt_bd_pass() {
            assertDoesNotThrow(() -> AssertUtil.gt(new BigDecimal("5"), new BigDecimal("3"), "必须大于3"));
        }

        @Test
        @DisplayName("gt(BigDecimal) - null 值抛出异常")
        void gt_bd_null() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.gt(null, new BigDecimal("3"), "必须大于3"));
        }

        @Test
        @DisplayName("gt(BigDecimal) - 值等于 min 抛出异常")
        void gt_bd_equal() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.gt(new BigDecimal("3"), new BigDecimal("3"), "必须大于3"));
        }
    }

    @Nested
    @DisplayName("ge 测试")
    class GeTests {

        @Test
        @DisplayName("ge(int) - 值大于等于 min 通过")
        void ge_int_pass() {
            assertDoesNotThrow(() -> AssertUtil.ge(5, 3, "必须大于等于3"));
            assertDoesNotThrow(() -> AssertUtil.ge(3, 3, "必须大于等于3"));
        }

        @Test
        @DisplayName("ge(int) - 值小于 min 抛出异常")
        void ge_int_fail() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.ge(2, 3, "必须大于等于3"));
        }

        @Test
        @DisplayName("ge(long) - 值大于等于 min 通过")
        void ge_long_pass() {
            assertDoesNotThrow(() -> AssertUtil.ge(5L, 3L, "必须大于等于3"));
            assertDoesNotThrow(() -> AssertUtil.ge(3L, 3L, "必须大于等于3"));
        }

        @Test
        @DisplayName("ge(long) - 值小于 min 抛出异常")
        void ge_long_fail() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.ge(2L, 3L, "必须大于等于3"));
        }
    }

    @Nested
    @DisplayName("lt 测试")
    class LtTests {

        @Test
        @DisplayName("lt(int) - 值小于 max 通过")
        void lt_int_pass() {
            assertDoesNotThrow(() -> AssertUtil.lt(3, 5, "必须小于5"));
        }

        @Test
        @DisplayName("lt(int) - 值等于 max 抛出异常")
        void lt_int_equal() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.lt(5, 5, "必须小于5"));
        }

        @Test
        @DisplayName("lt(int) - 值大于 max 抛出异常")
        void lt_int_greater() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.lt(6, 5, "必须小于5"));
        }

        @Test
        @DisplayName("lt(long) - 值小于 max 通过")
        void lt_long_pass() {
            assertDoesNotThrow(() -> AssertUtil.lt(3L, 5L, "必须小于5"));
        }

        @Test
        @DisplayName("lt(long) - 值等于 max 抛出异常")
        void lt_long_equal() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.lt(5L, 5L, "必须小于5"));
        }
    }

    @Nested
    @DisplayName("le 测试")
    class LeTests {

        @Test
        @DisplayName("le(int) - 值小于等于 max 通过")
        void le_int_pass() {
            assertDoesNotThrow(() -> AssertUtil.le(3, 5, "必须小于等于5"));
            assertDoesNotThrow(() -> AssertUtil.le(5, 5, "必须小于等于5"));
        }

        @Test
        @DisplayName("le(int) - 值大于 max 抛出异常")
        void le_int_fail() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.le(6, 5, "必须小于等于5"));
        }
    }

    @Nested
    @DisplayName("between 测试")
    class BetweenTests {

        @Test
        @DisplayName("between(int) - 值在范围内通过")
        void between_int_pass() {
            assertDoesNotThrow(() -> AssertUtil.between(5, 1, 10, "必须在1-10之间"));
            assertDoesNotThrow(() -> AssertUtil.between(1, 1, 10, "必须在1-10之间"));
            assertDoesNotThrow(() -> AssertUtil.between(10, 1, 10, "必须在1-10之间"));
        }

        @Test
        @DisplayName("between(int) - 值小于 min 抛出异常")
        void between_int_below() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(0, 1, 10, "必须在1-10之间"));
        }

        @Test
        @DisplayName("between(int) - 值大于 max 抛出异常")
        void between_int_above() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(11, 1, 10, "必须在1-10之间"));
        }

        @Test
        @DisplayName("between(long) - 值在范围内通过")
        void between_long_pass() {
            assertDoesNotThrow(() -> AssertUtil.between(5L, 1L, 10L, "必须在1-10之间"));
        }

        @Test
        @DisplayName("between(long) - 值超出范围抛出异常")
        void between_long_fail() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(0L, 1L, 10L, "必须在1-10之间"));
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.between(11L, 1L, 10L, "必须在1-10之间"));
        }
    }

    @Nested
    @DisplayName("length 测试")
    class LengthTests {

        @Test
        @DisplayName("字符串长度在范围内通过")
        void length_pass() {
            assertDoesNotThrow(() -> AssertUtil.length("hello", 3, 10, "长度不合法"));
        }

        @Test
        @DisplayName("null 抛出异常")
        void length_null() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.length(null, 3, 10, "不能为空"));
        }

        @Test
        @DisplayName("长度小于最小值抛出异常")
        void length_tooShort() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.length("hi", 3, 10, "太短"));
        }

        @Test
        @DisplayName("长度大于最大值抛出异常")
        void length_tooLong() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.length("hello world", 3, 5, "太长"));
        }

        @Test
        @DisplayName("边界值通过")
        void length_boundaries() {
            assertDoesNotThrow(() -> AssertUtil.length("abc", 3, 10, "长度不合法"));
            assertDoesNotThrow(() -> AssertUtil.length("abcdefghij", 3, 10, "长度不合法"));
        }
    }

    // ======================== 条件校验 ========================

    @Nested
    @DisplayName("isTrue / isFalse 测试")
    class ConditionalTests {

        @Test
        @DisplayName("isTrue - true 通过")
        void isTrue_pass() {
            assertDoesNotThrow(() -> AssertUtil.isTrue(true, "必须为 true"));
        }

        @Test
        @DisplayName("isTrue - false 抛出异常")
        void isTrue_fail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.isTrue(false, "必须为 true"));
            assertEquals("必须为 true", ex.getMessage());
        }

        @Test
        @DisplayName("isFalse - false 通过")
        void isFalse_pass() {
            assertDoesNotThrow(() -> AssertUtil.isFalse(false, "必须为 false"));
        }

        @Test
        @DisplayName("isFalse - true 抛出异常")
        void isFalse_fail() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.isFalse(true, "必须为 false"));
            assertEquals("必须为 false", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("equals / notEquals / same 测试")
    class EqualityTests {

        @Test
        @DisplayName("equals - 相等通过")
        void equals_pass() {
            assertDoesNotThrow(() -> AssertUtil.equals("hello", "hello", "必须相等"));
        }

        @Test
        @DisplayName("equals - 不相等抛出异常")
        void equals_fail() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.equals("a", "b", "必须相等"));
        }

        @Test
        @DisplayName("equals - 两个 null 视为相等")
        void equals_bothNull() {
            assertDoesNotThrow(() -> AssertUtil.equals(null, null, "必须相等"));
        }

        @Test
        @DisplayName("equals - 一边 null 视为不等")
        void equals_oneNull() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.equals(null, "a", "必须相等"));
        }

        @Test
        @DisplayName("notEquals - 不相等通过")
        void notEquals_pass() {
            assertDoesNotThrow(() -> AssertUtil.notEquals("a", "b", "不能相等"));
        }

        @Test
        @DisplayName("notEquals - 相等抛出异常")
        void notEquals_fail() {
            assertThrows(IllegalArgumentException.class, () -> AssertUtil.notEquals("a", "a", "不能相等"));
        }

        @Test
        @DisplayName("same - 同一引用通过")
        void same_pass() {
            Object obj = new Object();
            assertDoesNotThrow(() -> AssertUtil.same(obj, obj, "必须是同一对象"));
        }

        @Test
        @DisplayName("same - 不同引用抛出异常")
        void same_fail() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.same(new String("a"), new String("a"), "必须是同一对象"));
        }

        @Test
        @DisplayName("same - 不同对象抛出异常")
        void same_differentObjects() {
            assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.same(new Object(), new Object(), "必须是同一对象"));
        }
    }

    @Nested
    @DisplayName("fail 测试")
    class FailTests {

        @Test
        @DisplayName("始终抛出 IllegalArgumentException")
        void fail_alwaysThrows() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> AssertUtil.fail("强制失败"));
            assertEquals("强制失败", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("state 测试")
    class StateTests {

        @Test
        @DisplayName("state - true 通过")
        void state_pass() {
            assertDoesNotThrow(() -> AssertUtil.state(true, "状态正确"));
        }

        @Test
        @DisplayName("state - false 抛出 IllegalStateException")
        void state_fail() {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> AssertUtil.state(false, "状态错误"));
            assertEquals("状态错误", ex.getMessage());
        }
    }
}
