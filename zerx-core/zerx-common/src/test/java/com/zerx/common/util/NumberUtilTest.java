package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NumberUtil} 单元测试
 */
@DisplayName("NumberUtil 数字工具类测试")
class NumberUtilTest {

    // ======================== 常量 ========================

    @Test
    @DisplayName("DEFAULT_SCALE 应为 2")
    void defaultScale() {
        assertEquals(2, NumberUtil.DEFAULT_SCALE);
    }

    @Test
    @DisplayName("PERCENTAGE_SCALE 应为 2")
    void percentageScale() {
        assertEquals(2, NumberUtil.PERCENTAGE_SCALE);
    }

    // ======================== 安全转换 ========================

    @Nested
    @DisplayName("toInteger 测试")
    class ToIntegerTests {

        @Test
        @DisplayName("null 返回 null")
        void toInteger_null() {
            assertNull(NumberUtil.toInteger(null));
        }

        @Test
        @DisplayName("Integer 类型直接返回")
        void toInteger_fromInteger() {
            assertEquals(42, NumberUtil.toInteger(42));
        }

        @Test
        @DisplayName("Long 类型截断为 Integer")
        void toInteger_fromLong() {
            assertEquals(99, NumberUtil.toInteger(99L));
        }

        @Test
        @DisplayName("Double 类型截断为 Integer")
        void toInteger_fromDouble() {
            assertEquals(3, NumberUtil.toInteger(3.7));
        }

        @Test
        @DisplayName("String 数字转换")
        void toInteger_fromString() {
            assertEquals(123, NumberUtil.toInteger("123"));
        }

        @Test
        @DisplayName("String 带空格转换")
        void toInteger_fromStringWithSpaces() {
            assertEquals(123, NumberUtil.toInteger(" 123 "));
        }

        @Test
        @DisplayName("无效字符串返回 null")
        void toInteger_invalidString() {
            assertNull(NumberUtil.toInteger("abc"));
            assertNull(NumberUtil.toInteger(""));
        }

        @Test
        @DisplayName("toInteger(Object, Integer) - 转换失败返回默认值")
        void toInteger_withDefault() {
            assertEquals(42, NumberUtil.toInteger(null, 42));
            assertEquals(10, NumberUtil.toInteger("abc", 10));
            assertEquals(5, NumberUtil.toInteger("5", 10));
        }
    }

    @Nested
    @DisplayName("toLong 测试")
    class ToLongTests {

        @Test
        @DisplayName("null 返回 null")
        void toLong_null() {
            assertNull(NumberUtil.toLong(null));
        }

        @Test
        @DisplayName("Integer 转为 Long")
        void toLong_fromInteger() {
            assertEquals(42L, NumberUtil.toLong(42));
        }

        @Test
        @DisplayName("Long 类型直接返回")
        void toLong_fromLong() {
            assertEquals(99L, NumberUtil.toLong(99L));
        }

        @Test
        @DisplayName("String 数字转换")
        void toLong_fromString() {
            assertEquals(123L, NumberUtil.toLong("123"));
        }

        @Test
        @DisplayName("无效字符串返回 null")
        void toLong_invalidString() {
            assertNull(NumberUtil.toLong("abc"));
        }

        @Test
        @DisplayName("toLong(Object, Long) - 转换失败返回默认值")
        void toLong_withDefault() {
            assertEquals(42L, NumberUtil.toLong(null, 42L));
            assertEquals(10L, NumberUtil.toLong("abc", 10L));
        }
    }

    @Nested
    @DisplayName("toDouble 测试")
    class ToDoubleTests {

        @Test
        @DisplayName("null 返回 null")
        void toDouble_null() {
            assertNull(NumberUtil.toDouble(null));
        }

        @Test
        @DisplayName("Integer 转为 Double")
        void toDouble_fromInteger() {
            assertEquals(42.0, NumberUtil.toDouble(42));
        }

        @Test
        @DisplayName("Double 类型直接返回")
        void toDouble_fromDouble() {
            assertEquals(3.14, NumberUtil.toDouble(3.14));
        }

        @Test
        @DisplayName("String 数字转换")
        void toDouble_fromString() {
            assertEquals(3.14, NumberUtil.toDouble("3.14"));
        }

        @Test
        @DisplayName("无效字符串返回 null")
        void toDouble_invalidString() {
            assertNull(NumberUtil.toDouble("abc"));
        }

        @Test
        @DisplayName("toDouble(Object, Double) - 转换失败返回默认值")
        void toDouble_withDefault() {
            assertEquals(1.0, NumberUtil.toDouble(null, 1.0));
            assertEquals(2.0, NumberUtil.toDouble("abc", 2.0));
        }
    }

    @Nested
    @DisplayName("toBigDecimal 测试")
    class ToBigDecimalTests {

        @Test
        @DisplayName("null 返回 null")
        void toBigDecimal_null() {
            assertNull(NumberUtil.toBigDecimal(null));
        }

        @Test
        @DisplayName("BigDecimal 直接返回")
        void toBigDecimal_fromBigDecimal() {
            BigDecimal bd = new BigDecimal("3.14");
            assertSame(bd, NumberUtil.toBigDecimal(bd));
        }

        @Test
        @DisplayName("Double 使用 valueOf 避免精度问题")
        void toBigDecimal_fromDouble() {
            BigDecimal result = NumberUtil.toBigDecimal(0.1);
            assertEquals(new BigDecimal("0.1").stripTrailingZeros(),
                    result.stripTrailingZeros());
        }

        @Test
        @DisplayName("Float 使用 valueOf 避免精度问题")
        void toBigDecimal_fromFloat() {
            BigDecimal result = NumberUtil.toBigDecimal(0.1f);
            assertNotNull(result);
        }

        @Test
        @DisplayName("Integer 转为 BigDecimal")
        void toBigDecimal_fromInteger() {
            assertEquals(new BigDecimal("42"), NumberUtil.toBigDecimal(42));
        }

        @Test
        @DisplayName("String 数字转换")
        void toBigDecimal_fromString() {
            assertEquals(new BigDecimal("3.14"), NumberUtil.toBigDecimal("3.14"));
        }

        @Test
        @DisplayName("无效字符串返回 null")
        void toBigDecimal_invalidString() {
            assertNull(NumberUtil.toBigDecimal("abc"));
        }
    }

    // ======================== 精度运算 ========================

    @Nested
    @DisplayName("add 测试")
    class AddTests {

        @Test
        @DisplayName("正常加法")
        void add_normal() {
            // BigDecimal("1").add(BigDecimal("2")) returns BigDecimal(3) with scale 0
            assertEquals(0, NumberUtil.add(new BigDecimal("1"), new BigDecimal("2")).compareTo(new BigDecimal("3")));
        }

        @Test
        @DisplayName("左边 null 当作 0")
        void add_leftNull() {
            assertEquals(new BigDecimal("5"), NumberUtil.add(null, new BigDecimal("5")));
        }

        @Test
        @DisplayName("右边 null 当作 0")
        void add_rightNull() {
            assertEquals(new BigDecimal("5"), NumberUtil.add(new BigDecimal("5"), null));
        }

        @Test
        @DisplayName("两边都 null 返回 0")
        void add_bothNull() {
            assertEquals(BigDecimal.ZERO, NumberUtil.add(null, null));
        }
    }

    @Nested
    @DisplayName("subtract 测试")
    class SubtractTests {

        @Test
        @DisplayName("正常减法")
        void subtract_normal() {
            assertEquals(new BigDecimal("1"), NumberUtil.subtract(new BigDecimal("3"), new BigDecimal("2")));
        }

        @Test
        @DisplayName("左边 null 当作 0")
        void subtract_leftNull() {
            assertEquals(new BigDecimal("-5"), NumberUtil.subtract(null, new BigDecimal("5")));
        }

        @Test
        @DisplayName("右边 null 当作 0")
        void subtract_rightNull() {
            assertEquals(new BigDecimal("5"), NumberUtil.subtract(new BigDecimal("5"), null));
        }

        @Test
        @DisplayName("两边都 null 返回 0")
        void subtract_bothNull() {
            assertEquals(BigDecimal.ZERO, NumberUtil.subtract(null, null));
        }
    }

    @Nested
    @DisplayName("multiply 测试")
    class MultiplyTests {

        @Test
        @DisplayName("正常乘法")
        void multiply_normal() {
            assertEquals(new BigDecimal("6.00"), NumberUtil.multiply(new BigDecimal("2"), new BigDecimal("3"), 2));
        }

        @Test
        @DisplayName("乘法四舍五入")
        void multiply_rounding() {
            // 1.111 * 3 = 3.333, setScale(2, HALF_UP) rounds to 3.33
            assertEquals(new BigDecimal("3.33"), NumberUtil.multiply(new BigDecimal("1.111"), new BigDecimal("3"), 2));
        }

        @Test
        @DisplayName("任一为 null 返回 0")
        void multiply_null() {
            assertEquals(BigDecimal.ZERO, NumberUtil.multiply(null, new BigDecimal("5"), 2));
            assertEquals(BigDecimal.ZERO, NumberUtil.multiply(new BigDecimal("5"), null, 2));
        }
    }

    @Nested
    @DisplayName("divide 测试")
    class DivideTests {

        @Test
        @DisplayName("正常除法")
        void divide_normal() {
            assertEquals(new BigDecimal("0.33"), NumberUtil.divide(new BigDecimal("1"), new BigDecimal("3"), 2));
        }

        @Test
        @DisplayName("被除数 null 返回 0")
        void divide_nullDividend() {
            assertEquals(BigDecimal.ZERO, NumberUtil.divide(null, new BigDecimal("3"), 2));
        }

        @Test
        @DisplayName("除数 null 抛出 NullPointerException")
        void divide_nullDivisor() {
            assertThrows(NullPointerException.class, () -> NumberUtil.divide(new BigDecimal("1"), null, 2));
        }

        @Test
        @DisplayName("除数为零抛出 ArithmeticException")
        void divide_zeroDivisor() {
            assertThrows(ArithmeticException.class, () -> NumberUtil.divide(new BigDecimal("1"), BigDecimal.ZERO, 2));
        }
    }

    // ======================== 取整 ========================

    @Nested
    @DisplayName("round / ceil / floor 测试")
    class RoundingTests {

        @Test
        @DisplayName("round - 四舍五入")
        void round_normal() {
            assertEquals(new BigDecimal("3.14"), NumberUtil.round(new BigDecimal("3.14159"), 2));
            assertEquals(new BigDecimal("3.15"), NumberUtil.round(new BigDecimal("3.145"), 2));
        }

        @Test
        @DisplayName("round - null 返回 0")
        void round_null() {
            assertEquals(BigDecimal.ZERO, NumberUtil.round(null, 2));
        }

        @Test
        @DisplayName("ceil - 向上取整")
        void ceil_normal() {
            assertEquals(new BigDecimal("3.15"), NumberUtil.ceil(new BigDecimal("3.14159"), 2));
            assertEquals(new BigDecimal("3.15"), NumberUtil.ceil(new BigDecimal("3.14159"), 2));
        }

        @Test
        @DisplayName("ceil - null 返回 0")
        void ceil_null() {
            assertEquals(BigDecimal.ZERO, NumberUtil.ceil(null, 2));
        }

        @Test
        @DisplayName("floor - 向下取整")
        void floor_normal() {
            assertEquals(new BigDecimal("3.14"), NumberUtil.floor(new BigDecimal("3.14999"), 2));
        }

        @Test
        @DisplayName("floor - null 返回 0")
        void floor_null() {
            assertEquals(BigDecimal.ZERO, NumberUtil.floor(null, 2));
        }
    }

    // ======================== 格式化 ========================

    @Nested
    @DisplayName("format 测试")
    class FormatTests {

        @Test
        @DisplayName("format - null 返回 0.00 格式")
        void format_null() {
            assertEquals("0.00", NumberUtil.format(null, 2));
        }

        @Test
        @DisplayName("format - 正常格式化")
        void format_normal() {
            assertEquals("1,234.50", NumberUtil.format(new BigDecimal("1234.5"), 2));
        }

        @Test
        @DisplayName("format - scale 为 0")
        void format_zeroScale() {
            assertEquals("1,235", NumberUtil.format(new BigDecimal("1234.5"), 0));
        }
    }

    @Nested
    @DisplayName("formatPercent 测试")
    class FormatPercentTests {

        @Test
        @DisplayName("formatPercent(double) - 正常百分比")
        void formatPercent_double() {
            String result = NumberUtil.formatPercent(0.1234);
            assertTrue(result.contains("%"));
            assertEquals("12.34%", result);
        }

        @Test
        @DisplayName("formatPercent(BigDecimal, int) - null 当作 0")
        void formatPercent_null() {
            assertEquals("0.00%", NumberUtil.formatPercent(null, 2));
        }

        @Test
        @DisplayName("formatPercent(BigDecimal, int) - 正常格式化")
        void formatPercent_bigDecimal() {
            assertEquals("12.34%", NumberUtil.formatPercent(new BigDecimal("0.1234"), 2));
        }
    }

    @Nested
    @DisplayName("formatThousands 测试")
    class FormatThousandsTests {

        @Test
        @DisplayName("正常千分位格式化")
        void formatThousands_normal() {
            assertEquals("1,234,567", NumberUtil.formatThousands(1234567));
        }

        @Test
        @DisplayName("小数字不加千分位")
        void formatThousands_small() {
            assertEquals("999", NumberUtil.formatThousands(999));
        }

        @Test
        @DisplayName("负数")
        void formatThousands_negative() {
            assertEquals("-1,234", NumberUtil.formatThousands(-1234));
        }

        @Test
        @DisplayName("零")
        void formatThousands_zero() {
            assertEquals("0", NumberUtil.formatThousands(0));
        }
    }

    @Nested
    @DisplayName("formatFileSize 测试")
    class FormatFileSizeTests {

        @Test
        @DisplayName("负数返回 0 B")
        void formatFileSize_negative() {
            assertEquals("0 B", NumberUtil.formatFileSize(-1));
        }

        @Test
        @DisplayName("小于 1024 返回 B")
        void formatFileSize_bytes() {
            assertEquals("500 B", NumberUtil.formatFileSize(500));
            assertEquals("0 B", NumberUtil.formatFileSize(0));
            assertEquals("1023 B", NumberUtil.formatFileSize(1023));
        }

        @Test
        @DisplayName("KB 级别")
        void formatFileSize_kb() {
            assertEquals("1.00 KB", NumberUtil.formatFileSize(1024));
            assertEquals("1.50 KB", NumberUtil.formatFileSize(1536));
        }

        @Test
        @DisplayName("MB 级别")
        void formatFileSize_mb() {
            assertEquals("1.00 MB", NumberUtil.formatFileSize(1024 * 1024));
        }

        @Test
        @DisplayName("GB 级别")
        void formatFileSize_gb() {
            assertEquals("1.00 GB", NumberUtil.formatFileSize(1024L * 1024 * 1024));
        }

        @Test
        @DisplayName("TB 级别")
        void formatFileSize_tb() {
            assertEquals("1.00 TB", NumberUtil.formatFileSize(1024L * 1024 * 1024 * 1024));
        }

        @Test
        @DisplayName("PB 级别")
        void formatFileSize_pb() {
            assertEquals("1.00 PB", NumberUtil.formatFileSize(1024L * 1024 * 1024 * 1024 * 1024));
        }
    }

    // ======================== 比较 ========================

    @Nested
    @DisplayName("eq / gt / ge / lt / le 测试")
    class CompareTests {

        @Test
        @DisplayName("eq - 两个 null 相等")
        void eq_bothNull() {
            assertTrue(NumberUtil.eq(null, null));
        }

        @Test
        @DisplayName("eq - 一边 null 不等")
        void eq_oneNull() {
            assertFalse(NumberUtil.eq(null, BigDecimal.ZERO));
            assertFalse(NumberUtil.eq(BigDecimal.ZERO, null));
        }

        @Test
        @DisplayName("eq - 忽略精度的数值相等")
        void eq_differentScale() {
            assertTrue(NumberUtil.eq(new BigDecimal("1.0"), new BigDecimal("1.00")));
        }

        @Test
        @DisplayName("eq - 不相等")
        void eq_notEqual() {
            assertFalse(NumberUtil.eq(new BigDecimal("1"), new BigDecimal("2")));
        }

        @Test
        @DisplayName("gt - null 返回 false")
        void gt_null() {
            assertFalse(NumberUtil.gt(null, BigDecimal.ZERO));
            assertFalse(NumberUtil.gt(BigDecimal.ONE, null));
        }

        @Test
        @DisplayName("gt - 大于返回 true")
        void gt_true() {
            assertTrue(NumberUtil.gt(new BigDecimal("5"), new BigDecimal("3")));
        }

        @Test
        @DisplayName("gt - 等于返回 false")
        void gt_equal() {
            assertFalse(NumberUtil.gt(new BigDecimal("3"), new BigDecimal("3")));
        }

        @Test
        @DisplayName("ge - null 返回 false")
        void ge_null() {
            assertFalse(NumberUtil.ge(null, BigDecimal.ZERO));
            assertFalse(NumberUtil.ge(BigDecimal.ONE, null));
        }

        @Test
        @DisplayName("ge - 大于等于返回 true")
        void ge_true() {
            assertTrue(NumberUtil.ge(new BigDecimal("5"), new BigDecimal("3")));
            assertTrue(NumberUtil.ge(new BigDecimal("3"), new BigDecimal("3")));
        }

        @Test
        @DisplayName("lt - null 返回 false")
        void lt_null() {
            assertFalse(NumberUtil.lt(null, BigDecimal.ONE));
            assertFalse(NumberUtil.lt(BigDecimal.ZERO, null));
        }

        @Test
        @DisplayName("lt - 小于返回 true")
        void lt_true() {
            assertTrue(NumberUtil.lt(new BigDecimal("3"), new BigDecimal("5")));
        }

        @Test
        @DisplayName("le - null 返回 false")
        void le_null() {
            assertFalse(NumberUtil.le(null, BigDecimal.ONE));
            assertFalse(NumberUtil.le(BigDecimal.ZERO, null));
        }

        @Test
        @DisplayName("le - 小于等于返回 true")
        void le_true() {
            assertTrue(NumberUtil.le(new BigDecimal("3"), new BigDecimal("5")));
            assertTrue(NumberUtil.le(new BigDecimal("3"), new BigDecimal("3")));
        }
    }

    @Nested
    @DisplayName("isBetween 测试")
    class IsBetweenTests {

        @Test
        @DisplayName("null 值返回 false")
        void isBetween_null() {
            assertFalse(NumberUtil.isBetween(null, BigDecimal.ZERO, BigDecimal.TEN));
        }

        @Test
        @DisplayName("值在范围内（边界）")
        void isBetween_inRange() {
            assertTrue(NumberUtil.isBetween(new BigDecimal("5"), new BigDecimal("1"), new BigDecimal("10")));
            assertTrue(NumberUtil.isBetween(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("10")));
            assertTrue(NumberUtil.isBetween(new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("10")));
        }

        @Test
        @DisplayName("值不在范围内")
        void isBetween_outOfRange() {
            assertFalse(NumberUtil.isBetween(new BigDecimal("0"), new BigDecimal("1"), new BigDecimal("10")));
            assertFalse(NumberUtil.isBetween(new BigDecimal("11"), new BigDecimal("1"), new BigDecimal("10")));
        }
    }

    @Nested
    @DisplayName("isZero / isPositive / isNegative 测试")
    class SignTests {

        @Test
        @DisplayName("isZero - null 返回 false")
        void isZero_null() {
            assertFalse(NumberUtil.isZero(null));
        }

        @Test
        @DisplayName("isZero - 零返回 true")
        void isZero_true() {
            assertTrue(NumberUtil.isZero(BigDecimal.ZERO));
            assertTrue(NumberUtil.isZero(new BigDecimal("0.00")));
        }

        @Test
        @DisplayName("isZero - 非零返回 false")
        void isZero_false() {
            assertFalse(NumberUtil.isZero(BigDecimal.ONE));
            assertFalse(NumberUtil.isZero(new BigDecimal("-1")));
        }

        @Test
        @DisplayName("isPositive - null 返回 false")
        void isPositive_null() {
            assertFalse(NumberUtil.isPositive(null));
        }

        @Test
        @DisplayName("isPositive - 正数返回 true")
        void isPositive_true() {
            assertTrue(NumberUtil.isPositive(BigDecimal.ONE));
            assertTrue(NumberUtil.isPositive(new BigDecimal("0.01")));
        }

        @Test
        @DisplayName("isPositive - 零和负数返回 false")
        void isPositive_false() {
            assertFalse(NumberUtil.isPositive(BigDecimal.ZERO));
            assertFalse(NumberUtil.isPositive(new BigDecimal("-1")));
        }

        @Test
        @DisplayName("isNegative - null 返回 false")
        void isNegative_null() {
            assertFalse(NumberUtil.isNegative(null));
        }

        @Test
        @DisplayName("isNegative - 负数返回 true")
        void isNegative_true() {
            assertTrue(NumberUtil.isNegative(new BigDecimal("-1")));
            assertTrue(NumberUtil.isNegative(new BigDecimal("-0.01")));
        }

        @Test
        @DisplayName("isNegative - 零和正数返回 false")
        void isNegative_false() {
            assertFalse(NumberUtil.isNegative(BigDecimal.ZERO));
            assertFalse(NumberUtil.isNegative(BigDecimal.ONE));
        }
    }

    // ======================== 范围校验 ========================

    @Nested
    @DisplayName("clamp 测试")
    class ClampTests {

        @Test
        @DisplayName("clamp(long) - 值在范围内返回原值")
        void clamp_long_inRange() {
            assertEquals(5L, NumberUtil.clamp(5L, 0L, 10L));
        }

        @Test
        @DisplayName("clamp(long) - 值小于最小值返回最小值")
        void clamp_long_belowMin() {
            assertEquals(0L, NumberUtil.clamp(-5L, 0L, 10L));
        }

        @Test
        @DisplayName("clamp(long) - 值大于最大值返回最大值")
        void clamp_long_aboveMax() {
            assertEquals(10L, NumberUtil.clamp(15L, 0L, 10L));
        }

        @Test
        @DisplayName("clamp(double) - 值在范围内返回原值")
        void clamp_double_inRange() {
            assertEquals(5.0, NumberUtil.clamp(5.0, 0.0, 10.0));
        }

        @Test
        @DisplayName("clamp(double) - 边界值")
        void clamp_double_boundaries() {
            assertEquals(0.0, NumberUtil.clamp(0.0, 0.0, 10.0));
            assertEquals(10.0, NumberUtil.clamp(10.0, 0.0, 10.0));
        }

        @Test
        @DisplayName("clamp(BigDecimal) - null 值返回 min")
        void clamp_bigDecimal_null() {
            assertEquals(new BigDecimal("0"), NumberUtil.clamp(null, BigDecimal.ZERO, BigDecimal.TEN));
        }

        @Test
        @DisplayName("clamp(BigDecimal) - 值在范围内返回原值")
        void clamp_bigDecimal_inRange() {
            assertEquals(new BigDecimal("5"), NumberUtil.clamp(new BigDecimal("5"), BigDecimal.ZERO, BigDecimal.TEN));
        }

        @Test
        @DisplayName("clamp(BigDecimal) - 小于最小值返回最小值")
        void clamp_bigDecimal_belowMin() {
            assertEquals(BigDecimal.ZERO, NumberUtil.clamp(new BigDecimal("-5"), BigDecimal.ZERO, BigDecimal.TEN));
        }

        @Test
        @DisplayName("clamp(BigDecimal) - 大于最大值返回最大值")
        void clamp_bigDecimal_aboveMax() {
            assertEquals(BigDecimal.TEN, NumberUtil.clamp(new BigDecimal("15"), BigDecimal.ZERO, BigDecimal.TEN));
        }
    }
}
