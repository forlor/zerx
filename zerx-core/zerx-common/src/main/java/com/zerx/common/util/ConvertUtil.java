package com.zerx.common.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * 类型转换工具类
 * <p>
 * 提供对象到目标类型的通用转换能力，覆盖日常开发中使用频率最高的转换场景。
 * 基于 JDK 原生 API，零第三方依赖，线程安全（无共享可变状态）。
 * </p>
 *
 * <h3>核心转换能力：</h3>
 * <ul>
 *   <li>基础类型：String ↔ int/long/double/float/boolean</li>
 *   <li>包装类型：String ↔ Integer/Long/Double/Float/Boolean</li>
 *   <li>大数类型：String ↔ BigDecimal/BigInteger</li>
 *   <li>泛型转换：Object → T（自动推断目标类型）</li>
 * </ul>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>转换失败返回默认值，不抛异常（安全转换）</li>
 *   <li>null 输入返回 null（不抛 NPE）</li>
 *   <li>字符串前后空白自动 trim</li>
 *   <li>所有方法均为纯静态函数，天然线程安全</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 安全转换（失败返回默认值）
 * int val = ConvertUtil.toInt("123", 0);       // 123
 * int val2 = ConvertUtil.toInt("abc", 0);      // 0
 * long val3 = ConvertUtil.toLong("999", -1L);  // 999
 *
 * // 转换为包装类型（失败返回 null）
 * Integer val4 = ConvertUtil.toInteger("456");  // 456
 * Integer val5 = ConvertUtil.toInteger("abc");  // null
 *
 * // 转换为 BigDecimal
 * BigDecimal val6 = ConvertUtil.toBigDecimal("3.14");  // 3.14
 *
 * // 布尔值转换（支持多种格式）
 * boolean b1 = ConvertUtil.toBoolean("true");     // true
 * boolean b2 = ConvertUtil.toBoolean("1");        // true
 * boolean b3 = ConvertUtil.toBoolean("yes");      // true
 * boolean b4 = ConvertUtil.toBoolean("false", true); // false
 * }</pre>
 *
 * @author zerx
 */
public final class ConvertUtil {

    /** 布尔 true 的识别值（小写，已 trim） */
    private static final String[] TRUE_VALUES = {"true", "1", "yes", "on", "y"};

    private ConvertUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== toInt ========================

    /**
     * 将对象转换为 int
     * <p>
     * 支持的输入类型：String、Number、Boolean。
     * 转换失败或 null 输入返回默认值。
     * </p>
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        try {
            return Integer.parseInt(trim(value.toString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 int，转换失败返回 0
     *
     * @param value 输入值
     * @return 转换结果
     */
    public static int toInt(Object value) {
        return toInt(value, 0);
    }

    // ======================== toInteger ========================

    /**
     * 将对象转换为 Integer（包装类型）
     * <p>
     * 转换失败或 null 输入返回 null。
     * </p>
     *
     * @param value 输入值
     * @return 转换结果，失败返回 null
     */
    public static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n instanceof Integer i ? i : n.intValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        try {
            return Integer.parseInt(trim(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ======================== toLong ========================

    /**
     * 将对象转换为 long
     * <p>
     * 支持的输入类型：String、Number、Boolean。
     * 转换失败或 null 输入返回默认值。
     * </p>
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1L : 0L;
        }
        try {
            return Long.parseLong(trim(value.toString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 long，转换失败返回 0L
     *
     * @param value 输入值
     * @return 转换结果
     */
    public static long toLong(Object value) {
        return toLong(value, 0L);
    }

    // ======================== toLong (包装类型) ========================

    /**
     * 将对象转换为 Long（包装类型）
     * <p>
     * 转换失败或 null 输入返回 null。
     * </p>
     *
     * @param value 输入值
     * @return 转换结果，失败返回 null
     */
    public static Long toLongObj(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n instanceof Long l ? l : n.longValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1L : 0L;
        }
        try {
            return Long.parseLong(trim(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ======================== toDouble ========================

    /**
     * 将对象转换为 double
     * <p>
     * 支持的输入类型：String、Number、Boolean。
     * 转换失败或 null 输入返回默认值。
     * </p>
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static double toDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        }
        try {
            return Double.parseDouble(trim(value.toString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 double，转换失败返回 0.0
     *
     * @param value 输入值
     * @return 转换结果
     */
    public static double toDouble(Object value) {
        return toDouble(value, 0.0);
    }

    // ======================== toFloat ========================

    /**
     * 将对象转换为 float
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static float toFloat(Object value, float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.floatValue();
        }
        if (value instanceof Boolean b) {
            return b ? 1.0f : 0.0f;
        }
        try {
            return Float.parseFloat(trim(value.toString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 float，转换失败返回 0.0f
     *
     * @param value 输入值
     * @return 转换结果
     */
    public static float toFloat(Object value) {
        return toFloat(value, 0.0f);
    }

    // ======================== toBoolean ========================

    /**
     * 将对象转换为 boolean
     * <p>
     * 支持多种布尔格式：true/false、yes/no、on/off、1/0、y/n（不区分大小写）。
     * 转换失败或 null 输入返回默认值。
     * </p>
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String str = trim(value.toString()).toLowerCase();
        for (String trueVal : TRUE_VALUES) {
            if (trueVal.equals(str)) {
                return true;
            }
        }
        // 检查是否为明确的 false 值
        if ("false".equals(str) || "0".equals(str) || "no".equals(str)
                || "off".equals(str) || "n".equals(str)) {
            return false;
        }
        return defaultValue;
    }

    /**
     * 将对象转换为 boolean，转换失败返回 false
     *
     * @param value 输入值
     * @return 转换结果
     */
    public static boolean toBoolean(Object value) {
        return toBoolean(value, false);
    }

    // ======================== toString ========================

    /**
     * 将对象转换为 String
     * <p>
     * null 输入返回 null，数组调用 {@link Objects#toString(Object)} 处理。
     * </p>
     *
     * @param value 输入值
     * @return 字符串形式，null 输入返回 null
     */
    public static String toStr(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 将对象转换为 String，null 输入返回空字符串
     *
     * @param value 输入值
     * @return 字符串形式，null 输入返回 ""
     */
    public static String toStrOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    // ======================== toBigDecimal ========================

    /**
     * 将对象转换为 BigDecimal
     * <p>
     * 支持的输入类型：String、Number、Boolean。
     * 转换失败或 null 输入返回 null。
     * </p>
     *
     * @param value 输入值
     * @return BigDecimal 值，失败返回 null
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Boolean b) {
            return b ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(trim(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 将对象转换为 BigDecimal，失败返回默认值
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return BigDecimal 值
     */
    public static BigDecimal toBigDecimal(Object value, BigDecimal defaultValue) {
        BigDecimal result = toBigDecimal(value);
        return result != null ? result : defaultValue;
    }

    // ======================== toBigInteger ========================

    /**
     * 将对象转换为 BigInteger
     * <p>
     * 支持的输入类型：String、Number、Boolean。
     * 转换失败或 null 输入返回 null。
     * </p>
     *
     * @param value 输入值
     * @return BigInteger 值，失败返回 null
     */
    public static BigInteger toBigInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigInteger bi) {
            return bi;
        }
        if (value instanceof Boolean b) {
            return b ? BigInteger.ONE : BigInteger.ZERO;
        }
        try {
            return new BigInteger(trim(value.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ======================== toShort ========================

    /**
     * 将对象转换为 short
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static short toShort(Object value, short defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.shortValue();
        }
        try {
            return Short.parseShort(trim(value.toString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 将对象转换为 short，转换失败返回 0
     *
     * @param value 输入值
     * @return 转换结果
     */
    public static short toShort(Object value) {
        return toShort(value, (short) 0);
    }

    // ======================== toByte ========================

    /**
     * 将对象转换为 byte
     *
     * @param value        输入值
     * @param defaultValue 转换失败时的默认值
     * @return 转换结果
     */
    public static byte toByte(Object value, byte defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.byteValue();
        }
        try {
            return Byte.parseByte(trim(value.toString()));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ======================== toCharArray ========================

    /**
     * 将对象转换为 char 数组
     *
     * @param value 输入值
     * @return char 数组，null 输入返回 null
     */
    public static char[] toCharArray(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().toCharArray();
    }

    // ======================== 内部方法 ========================

    /**
     * trim 字符串，空字符串返回空字符串
     */
    private static String trim(String str) {
        return str == null ? "" : str.trim();
    }
}
