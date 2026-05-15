package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConvertUtil 单元测试
 *
 * @author zerx
 */
@DisplayName("ConvertUtil 类型转换工具测试")
class ConvertUtilTest {

    // ======================== toInt ========================

    @Nested
    @DisplayName("toInt")
    class ToIntTests {

        @Test
        @DisplayName("字符串转 int - 合法值")
        void stringToInt() {
            assertEquals(123, ConvertUtil.toInt("123", 0));
            assertEquals(0, ConvertUtil.toInt("0", -1));
            assertEquals(-456, ConvertUtil.toInt("-456", 0));
        }

        @Test
        @DisplayName("字符串带空格转 int - 自动 trim")
        void stringWithSpaces() {
            assertEquals(789, ConvertUtil.toInt("  789  ", 0));
        }

        @Test
        @DisplayName("字符串转 int - 非法值返回默认值")
        void invalidString() {
            assertEquals(0, ConvertUtil.toInt("abc", 0));
            assertEquals(-1, ConvertUtil.toInt("abc", -1));
            assertEquals(42, ConvertUtil.toInt("", 42));
        }

        @Test
        @DisplayName("null 输入返回默认值")
        void nullInput() {
            assertEquals(0, ConvertUtil.toInt(null, 0));
            assertEquals(-99, ConvertUtil.toInt(null, -99));
        }

        @Test
        @DisplayName("Number 类型转 int")
        void numberToInt() {
            assertEquals(42, ConvertUtil.toInt(Integer.valueOf(42), 0));
            assertEquals(42, ConvertUtil.toInt(Long.valueOf(42), 0));
            assertEquals(42, ConvertUtil.toInt(Double.valueOf(42.9), 0));
            assertEquals(1, ConvertUtil.toInt(Float.valueOf(1.9f), 0));
        }

        @Test
        @DisplayName("Boolean 类型转 int")
        void booleanToInt() {
            assertEquals(1, ConvertUtil.toInt(true, 0));
            assertEquals(0, ConvertUtil.toInt(false, 0));
        }

        @Test
        @DisplayName("无默认值重载")
        void noDefault() {
            assertEquals(123, ConvertUtil.toInt("123"));
            assertEquals(0, ConvertUtil.toInt("abc"));
            assertEquals(0, ConvertUtil.toInt(null));
        }

        @Test
        @DisplayName("超大数截断")
        void overflow() {
            assertEquals(Integer.MAX_VALUE, ConvertUtil.toInt("2147483647", 0));
            assertEquals(Integer.MIN_VALUE, ConvertUtil.toInt("-2147483648", 0));
        }
    }

    // ======================== toInteger ========================

    @Nested
    @DisplayName("toInteger")
    class ToIntegerTests {

        @Test
        @DisplayName("合法值转 Integer")
        void validConversion() {
            assertEquals(123, ConvertUtil.toInteger("123"));
            assertEquals(-456, ConvertUtil.toInteger("-456"));
            assertEquals(42, ConvertUtil.toInteger(Integer.valueOf(42)));
        }

        @Test
        @DisplayName("非法值和 null 返回 null")
        void invalidReturnsNull() {
            assertNull(ConvertUtil.toInteger("abc"));
            assertNull(ConvertUtil.toInteger(null));
        }

        @Test
        @DisplayName("Boolean 转 Integer")
        void booleanToInteger() {
            assertEquals(1, ConvertUtil.toInteger(true));
            assertEquals(0, ConvertUtil.toInteger(false));
        }
    }

    // ======================== toLong ========================

    @Nested
    @DisplayName("toLong")
    class ToLongTests {

        @Test
        @DisplayName("字符串转 long - 合法值")
        void stringToLong() {
            assertEquals(999L, ConvertUtil.toLong("999", 0L));
            assertEquals(0L, ConvertUtil.toLong("0", -1L));
            assertEquals(-123456L, ConvertUtil.toLong("-123456", 0L));
        }

        @Test
        @DisplayName("字符串带空格转 long - 自动 trim")
        void stringWithSpaces() {
            assertEquals(789L, ConvertUtil.toLong("  789  ", 0L));
        }

        @Test
        @DisplayName("非法值返回默认值")
        void invalidString() {
            assertEquals(0L, ConvertUtil.toLong("abc", 0L));
            assertEquals(-1L, ConvertUtil.toLong("abc", -1L));
        }

        @Test
        @DisplayName("null 输入返回默认值")
        void nullInput() {
            assertEquals(0L, ConvertUtil.toLong(null, 0L));
        }

        @Test
        @DisplayName("Number 类型转 long")
        void numberToLong() {
            assertEquals(42L, ConvertUtil.toLong(Integer.valueOf(42), 0L));
            assertEquals(999L, ConvertUtil.toLong(Long.valueOf(999), 0L));
            assertEquals(3L, ConvertUtil.toLong(Double.valueOf(3.14), 0L));
        }

        @Test
        @DisplayName("Boolean 转 long")
        void booleanToLong() {
            assertEquals(1L, ConvertUtil.toLong(true, 0L));
            assertEquals(0L, ConvertUtil.toLong(false, 0L));
        }

        @Test
        @DisplayName("无默认值重载")
        void noDefault() {
            assertEquals(999L, ConvertUtil.toLong("999"));
            assertEquals(0L, ConvertUtil.toLong("abc"));
            assertEquals(0L, ConvertUtil.toLong(null));
        }
    }

    // ======================== toLongObj ========================

    @Nested
    @DisplayName("toLongObj")
    class ToLongObjTests {

        @Test
        @DisplayName("合法值转 Long")
        void validConversion() {
            assertEquals(999L, ConvertUtil.toLongObj("999"));
            assertEquals(-1L, ConvertUtil.toLongObj("-1"));
        }

        @Test
        @DisplayName("非法值和 null 返回 null")
        void invalidReturnsNull() {
            assertNull(ConvertUtil.toLongObj("abc"));
            assertNull(ConvertUtil.toLongObj(null));
        }
    }

    // ======================== toDouble ========================

    @Nested
    @DisplayName("toDouble")
    class ToDoubleTests {

        @Test
        @DisplayName("字符串转 double - 合法值")
        void stringToDouble() {
            assertEquals(3.14, ConvertUtil.toDouble("3.14", 0.0), 0.001);
            assertEquals(0.0, ConvertUtil.toDouble("0", -1.0), 0.001);
            assertEquals(-99.9, ConvertUtil.toDouble("-99.9", 0.0), 0.001);
        }

        @Test
        @DisplayName("字符串带空格转 double")
        void stringWithSpaces() {
            assertEquals(1.5, ConvertUtil.toDouble("  1.5  ", 0.0), 0.001);
        }

        @Test
        @DisplayName("非法值返回默认值")
        void invalidString() {
            assertEquals(0.0, ConvertUtil.toDouble("abc", 0.0), 0.001);
            assertEquals(-1.0, ConvertUtil.toDouble("abc", -1.0), 0.001);
        }

        @Test
        @DisplayName("null 输入返回默认值")
        void nullInput() {
            assertEquals(0.0, ConvertUtil.toDouble(null, 0.0), 0.001);
        }

        @Test
        @DisplayName("Number 类型转 double")
        void numberToDouble() {
            assertEquals(42.0, ConvertUtil.toDouble(Integer.valueOf(42), 0.0), 0.001);
            assertEquals(3.14, ConvertUtil.toDouble(Double.valueOf(3.14), 0.0), 0.001);
        }

        @Test
        @DisplayName("Boolean 转 double")
        void booleanToDouble() {
            assertEquals(1.0, ConvertUtil.toDouble(true, 0.0), 0.001);
            assertEquals(0.0, ConvertUtil.toDouble(false, 0.0), 0.001);
        }

        @Test
        @DisplayName("无默认值重载")
        void noDefault() {
            assertEquals(3.14, ConvertUtil.toDouble("3.14"), 0.001);
            assertEquals(0.0, ConvertUtil.toDouble("abc"), 0.001);
        }
    }

    // ======================== toFloat ========================

    @Nested
    @DisplayName("toFloat")
    class ToFloatTests {

        @Test
        @DisplayName("字符串转 float - 合法值")
        void stringToFloat() {
            assertEquals(3.14f, ConvertUtil.toFloat("3.14", 0.0f), 0.001f);
            assertEquals(0.0f, ConvertUtil.toFloat("abc", 0.0f), 0.001f);
            assertEquals(0.0f, ConvertUtil.toFloat(null, 0.0f), 0.001f);
        }

        @Test
        @DisplayName("Number 类型转 float")
        void numberToFloat() {
            assertEquals(42.0f, ConvertUtil.toFloat(Integer.valueOf(42), 0.0f), 0.001f);
        }

        @Test
        @DisplayName("无默认值重载")
        void noDefault() {
            assertEquals(3.14f, ConvertUtil.toFloat("3.14"), 0.001f);
            assertEquals(0.0f, ConvertUtil.toFloat("abc"), 0.001f);
        }
    }

    // ======================== toBoolean ========================

    @Nested
    @DisplayName("toBoolean")
    class ToBooleanTests {

        @Test
        @DisplayName("字符串 true 的各种格式")
        void trueValues() {
            assertTrue(ConvertUtil.toBoolean("true", false));
            assertTrue(ConvertUtil.toBoolean("TRUE", false));
            assertTrue(ConvertUtil.toBoolean("True", false));
            assertTrue(ConvertUtil.toBoolean("1", false));
            assertTrue(ConvertUtil.toBoolean("yes", false));
            assertTrue(ConvertUtil.toBoolean("YES", false));
            assertTrue(ConvertUtil.toBoolean("on", false));
            assertTrue(ConvertUtil.toBoolean("ON", false));
            assertTrue(ConvertUtil.toBoolean("y", false));
            assertTrue(ConvertUtil.toBoolean("Y", false));
        }

        @Test
        @DisplayName("字符串 false 的各种格式")
        void falseValues() {
            assertFalse(ConvertUtil.toBoolean("false", true));
            assertFalse(ConvertUtil.toBoolean("FALSE", true));
            assertFalse(ConvertUtil.toBoolean("0", true));
            assertFalse(ConvertUtil.toBoolean("no", true));
            assertFalse(ConvertUtil.toBoolean("NO", true));
            assertFalse(ConvertUtil.toBoolean("off", true));
            assertFalse(ConvertUtil.toBoolean("n", true));
        }

        @Test
        @DisplayName("无法识别的值返回默认值")
        void unknownReturnsDefault() {
            assertTrue(ConvertUtil.toBoolean("abc", true));
            assertFalse(ConvertUtil.toBoolean("abc", false));
            assertTrue(ConvertUtil.toBoolean("", true));
            assertFalse(ConvertUtil.toBoolean("", false));
        }

        @Test
        @DisplayName("null 输入返回默认值")
        void nullInput() {
            assertTrue(ConvertUtil.toBoolean(null, true));
            assertFalse(ConvertUtil.toBoolean(null, false));
        }

        @Test
        @DisplayName("Boolean 类型直接返回")
        void booleanInput() {
            assertTrue(ConvertUtil.toBoolean(true, false));
            assertFalse(ConvertUtil.toBoolean(false, true));
        }

        @Test
        @DisplayName("无默认值重载")
        void noDefault() {
            assertTrue(ConvertUtil.toBoolean("true"));
            assertTrue(ConvertUtil.toBoolean("1"));
            assertFalse(ConvertUtil.toBoolean("false"));
            assertFalse(ConvertUtil.toBoolean("0"));
            assertFalse(ConvertUtil.toBoolean("abc"));
            assertFalse(ConvertUtil.toBoolean(null));
        }
    }

    // ======================== toStr ========================

    @Nested
    @DisplayName("toStr")
    class ToStrTests {

        @Test
        @DisplayName("各种类型转 String")
        void variousTypes() {
            assertEquals("123", ConvertUtil.toStr(123));
            assertEquals("hello", ConvertUtil.toStr("hello"));
            assertEquals("true", ConvertUtil.toStr(true));
            assertEquals("3.14", ConvertUtil.toStr(3.14));
        }

        @Test
        @DisplayName("null 输入返回 null")
        void nullInput() {
            assertNull(ConvertUtil.toStr(null));
        }

        @Test
        @DisplayName("toStrOrEmpty - null 返回空字符串")
        void toStrOrEmpty() {
            assertEquals("", ConvertUtil.toStrOrEmpty(null));
            assertEquals("hello", ConvertUtil.toStrOrEmpty("hello"));
        }
    }

    // ======================== toBigDecimal ========================

    @Nested
    @DisplayName("toBigDecimal")
    class ToBigDecimalTests {

        @Test
        @DisplayName("字符串转 BigDecimal")
        void stringToBigDecimal() {
            assertEquals(new BigDecimal("3.14"), ConvertUtil.toBigDecimal("3.14"));
            assertEquals(new BigDecimal("0"), ConvertUtil.toBigDecimal("0"));
            assertEquals(new BigDecimal("-99.9"), ConvertUtil.toBigDecimal("-99.9"));
        }

        @Test
        @DisplayName("带空格字符串自动 trim")
        void stringWithSpaces() {
            assertEquals(new BigDecimal("3.14"), ConvertUtil.toBigDecimal("  3.14  "));
        }

        @Test
        @DisplayName("非法值和 null 返回 null")
        void invalidReturnsNull() {
            assertNull(ConvertUtil.toBigDecimal("abc"));
            assertNull(ConvertUtil.toBigDecimal(null));
        }

        @Test
        @DisplayName("Number 类型直接转换")
        void numberToBigDecimal() {
            assertEquals(new BigDecimal("42"), ConvertUtil.toBigDecimal(Integer.valueOf(42)));
            assertEquals(new BigDecimal("3.14"), ConvertUtil.toBigDecimal(Double.valueOf(3.14)));
        }

        @Test
        @DisplayName("BigDecimal 直接返回")
        void bigDecimalPassthrough() {
            BigDecimal bd = new BigDecimal("123.456");
            assertSame(bd, ConvertUtil.toBigDecimal(bd));
        }

        @Test
        @DisplayName("Boolean 转 BigDecimal")
        void booleanToBigDecimal() {
            assertEquals(BigDecimal.ONE, ConvertUtil.toBigDecimal(true));
            assertEquals(BigDecimal.ZERO, ConvertUtil.toBigDecimal(false));
        }

        @Test
        @DisplayName("带默认值的重载")
        void withDefault() {
            assertEquals(new BigDecimal("3.14"), ConvertUtil.toBigDecimal("3.14", BigDecimal.ZERO));
            assertEquals(BigDecimal.ZERO, ConvertUtil.toBigDecimal("abc", BigDecimal.ZERO));
            assertEquals(new BigDecimal("-1"), ConvertUtil.toBigDecimal(null, new BigDecimal("-1")));
        }
    }

    // ======================== toBigInteger ========================

    @Nested
    @DisplayName("toBigInteger")
    class ToBigIntegerTests {

        @Test
        @DisplayName("字符串转 BigInteger")
        void stringToBigInteger() {
            assertEquals(new BigInteger("123"), ConvertUtil.toBigInteger("123"));
            assertEquals(new BigInteger("0"), ConvertUtil.toBigInteger("0"));
            assertEquals(new BigInteger("-999"), ConvertUtil.toBigInteger("-999"));
        }

        @Test
        @DisplayName("非法值和 null 返回 null")
        void invalidReturnsNull() {
            assertNull(ConvertUtil.toBigInteger("abc"));
            assertNull(ConvertUtil.toBigInteger(null));
        }

        @Test
        @DisplayName("BigInteger 直接返回")
        void bigIntegerPassthrough() {
            BigInteger bi = new BigInteger("12345");
            assertSame(bi, ConvertUtil.toBigInteger(bi));
        }

        @Test
        @DisplayName("Boolean 转 BigInteger")
        void booleanToBigInteger() {
            assertEquals(BigInteger.ONE, ConvertUtil.toBigInteger(true));
            assertEquals(BigInteger.ZERO, ConvertUtil.toBigInteger(false));
        }
    }

    // ======================== toShort ========================

    @Nested
    @DisplayName("toShort")
    class ToShortTests {

        @Test
        @DisplayName("字符串转 short")
        void stringToShort() {
            assertEquals((short) 123, ConvertUtil.toShort("123", (short) 0));
            assertEquals((short) -1, ConvertUtil.toShort("-1", (short) 0));
        }

        @Test
        @DisplayName("非法值和 null 返回默认值")
        void invalidReturnsDefault() {
            assertEquals((short) 0, ConvertUtil.toShort("abc", (short) 0));
            assertEquals((short) -1, ConvertUtil.toShort(null, (short) -1));
        }

        @Test
        @DisplayName("Number 类型转 short")
        void numberToShort() {
            assertEquals((short) 42, ConvertUtil.toShort(Integer.valueOf(42), (short) 0));
        }

        @Test
        @DisplayName("无默认值重载")
        void noDefault() {
            assertEquals((short) 123, ConvertUtil.toShort("123"));
            assertEquals((short) 0, ConvertUtil.toShort("abc"));
        }
    }

    // ======================== toByte ========================

    @Nested
    @DisplayName("toByte")
    class ToByteTests {

        @Test
        @DisplayName("字符串转 byte")
        void stringToByte() {
            assertEquals((byte) 127, ConvertUtil.toByte("127", (byte) 0));
            assertEquals((byte) -1, ConvertUtil.toByte("-1", (byte) 0));
        }

        @Test
        @DisplayName("非法值和 null 返回默认值")
        void invalidReturnsDefault() {
            assertEquals((byte) 0, ConvertUtil.toByte("abc", (byte) 0));
            assertEquals((byte) -1, ConvertUtil.toByte(null, (byte) -1));
        }

        @Test
        @DisplayName("Number 类型转 byte")
        void numberToByte() {
            assertEquals((byte) 42, ConvertUtil.toByte(Integer.valueOf(42), (byte) 0));
        }
    }

    // ======================== toCharArray ========================

    @Nested
    @DisplayName("toCharArray")
    class ToCharArrayTests {

        @Test
        @DisplayName("字符串转 char 数组")
        void stringToCharArray() {
            assertArrayEquals(new char[]{'a', 'b', 'c'}, ConvertUtil.toCharArray("abc"));
        }

        @Test
        @DisplayName("null 输入返回 null")
        void nullInput() {
            assertNull(ConvertUtil.toCharArray(null));
        }
    }

    // ======================== 工具类约束 ========================

    @Test
    @DisplayName("工具类不可实例化")
    void utilityClass() throws Exception {
        var c = ConvertUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        var e = assertThrows(java.lang.reflect.InvocationTargetException.class, c::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, e.getCause());
    }
}
