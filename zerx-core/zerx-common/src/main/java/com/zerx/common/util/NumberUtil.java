package com.zerx.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

/**
 * 数字工具类
 * <p>
 * 提供数字格式化、安全转换、精度运算、比较等常用方法。
 * 所有精度运算基于 {@link BigDecimal}，避免浮点数精度丢失问题。
 * </p>
 *
 * @author zerx
 */
public final class NumberUtil {

    /** 默认保留小数位数 */
    public static final int DEFAULT_SCALE = 2;

    /** 百分比的小数位数 */
    public static final int PERCENTAGE_SCALE = 2;

    /** 私有构造器，防止实例化 */
    private NumberUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 安全转换 ========================

    /**
     * 安全将 Object 转为 Integer
     * <p>
     * 支持 String、Number 及其子类的转换，转换失败返回 null。
     * </p>
     *
     * @param value 原始值
     * @return Integer 值，转换失败返回 null
     */
    public static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全将 Object 转为 Long
     *
     * @param value 原始值
     * @return Long 值，转换失败返回 null
     */
    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全将 Object 转为 Double
     *
     * @param value 原始值
     * @return Double 值，转换失败返回 null
     */
    public static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全将 Object 转为 BigDecimal
     *
     * @param value 原始值
     * @return BigDecimal 值，转换失败返回 null
     */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Double || value instanceof Float) {
            // 浮点数用 valueOf 避免精度问题
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 安全将 Object 转为 Integer，转换失败返回默认值
     *
     * @param value       原始值
     * @param defaultValue 默认值
     * @return Integer 值
     */
    public static Integer toInteger(Object value, Integer defaultValue) {
        Integer result = toInteger(value);
        return result != null ? result : defaultValue;
    }

    /**
     * 安全将 Object 转为 Long，转换失败返回默认值
     *
     * @param value       原始值
     * @param defaultValue 默认值
     * @return Long 值
     */
    public static Long toLong(Object value, Long defaultValue) {
        Long result = toLong(value);
        return result != null ? result : defaultValue;
    }

    /**
     * 安全将 Object 转为 Double，转换失败返回默认值
     *
     * @param value       原始值
     * @param defaultValue 默认值
     * @return Double 值
     */
    public static Double toDouble(Object value, Double defaultValue) {
        Double result = toDouble(value);
        return result != null ? result : defaultValue;
    }

    // ======================== 精度运算 ========================

    /**
     * 精确加法（基于 BigDecimal）
     *
     * @param a 加数
     * @param b 加数
     * @return a + b
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        BigDecimal left = (a != null) ? a : BigDecimal.ZERO;
        BigDecimal right = (b != null) ? b : BigDecimal.ZERO;
        return left.add(right);
    }

    /**
     * 精确减法
     *
     * @param a 被减数
     * @param b 减数
     * @return a - b
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal left = (a != null) ? a : BigDecimal.ZERO;
        BigDecimal right = (b != null) ? b : BigDecimal.ZERO;
        return left.subtract(right);
    }

    /**
     * 精确乘法
     *
     * @param a 被乘数
     * @param b 乘数
     * @param scale 保留小数位数
     * @return a × b（四舍五入）
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b, int scale) {
        if (a == null || b == null) {
            return BigDecimal.ZERO;
        }
        return a.multiply(b).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 精确除法
     *
     * @param a 被除数
     * @param b 除数
     * @param scale 保留小数位数
     * @return a ÷ b（四舍五入）
     * @throws ArithmeticException 除数为零时抛出
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b, int scale) {
        Objects.requireNonNull(b, "除数不能为 null");
        if (a == null) {
            return BigDecimal.ZERO;
        }
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("除数不能为零");
        }
        return a.divide(b, scale, RoundingMode.HALF_UP);
    }

    /**
     * 四舍五入
     *
     * @param value 原始值
     * @param scale 保留小数位数
     * @return 四舍五入后的值
     */
    public static BigDecimal round(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 向上取整
     *
     * @param value 原始值
     * @param scale 保留小数位数
     * @return 向上取整后的值
     */
    public static BigDecimal ceil(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(scale, RoundingMode.CEILING);
    }

    /**
     * 向下取整
     *
     * @param value 原始值
     * @param scale 保留小数位数
     * @return 向下取整后的值
     */
    public static BigDecimal floor(BigDecimal value, int scale) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(scale, RoundingMode.FLOOR);
    }

    // ======================== 格式化 ========================

    /**
     * 格式化数字为指定小数位的字符串
     * <p>
     * 示例：format(1234.5, 2) → "1234.50"
     * </p>
     *
     * @param value 数字
     * @param scale 保留小数位数
     * @return 格式化后的字符串
     */
    public static String format(BigDecimal value, int scale) {
        if (value == null) {
            return "0." + "0".repeat(scale);
        }
        String pattern = "#,##0" + (scale > 0 ? "." + "0".repeat(scale) : "");
        DecimalFormat df = new DecimalFormat(pattern);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(value);
    }

    /**
     * 格式化数字为百分比字符串
     * <p>
     * 示例：formatPercent(0.1234) → "12.34%"
     * </p>
     *
     * @param decimal 小数值（0 ~ 1）
     * @return 百分比字符串
     */
    public static String formatPercent(double decimal) {
        return formatPercent(BigDecimal.valueOf(decimal), PERCENTAGE_SCALE);
    }

    /**
     * 格式化 BigDecimal 为百分比字符串
     * <p>
     * 示例：formatPercent(new BigDecimal("0.1234"), 2) → "12.34%"
     * </p>
     *
     * @param decimal 小数值（0 ~ 1）
     * @param scale   保留小数位数
     * @return 百分比字符串
     */
    public static String formatPercent(BigDecimal decimal, int scale) {
        if (decimal == null) {
            decimal = BigDecimal.ZERO;
        }
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMinimumFractionDigits(scale);
        percentFormat.setMaximumFractionDigits(scale);
        percentFormat.setRoundingMode(RoundingMode.HALF_UP);
        return percentFormat.format(decimal);
    }

    /**
     * 格式化数字为千分位字符串
     * <p>
     * 示例：formatThousands(1234567) → "1,234,567"
     * </p>
     *
     * @param value 数字值
     * @return 千分位格式化字符串
     */
    public static String formatThousands(long value) {
        DecimalFormat df = new DecimalFormat("#,##0");
        return df.format(value);
    }

    /**
     * 格式化文件大小为可读字符串
     * <p>
     * 示例：1234567 → "1.18 MB"
     * </p>
     *
     * @param bytes 字节数
     * @return 可读的文件大小字符串
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes / 1024.0;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    // ======================== 比较 ========================

    /**
     * 比较两个 BigDecimal 是否相等（忽略精度差异）
     * <p>
     * 使用 compareTo 代替 equals，因为 equals 要求精度也相同。
     * 示例：new BigDecimal("1.0") vs new BigDecimal("1.00") → equals=false, eq=true
     * </p>
     *
     * @param a 第一个值
     * @param b 第二个值
     * @return 数值相等返回 true
     */
    public static boolean eq(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    /**
     * 判断 a 是否大于 b
     *
     * @param a 第一个值
     * @param b 第二个值
     * @return a &gt; b 返回 true
     */
    public static boolean gt(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) > 0;
    }

    /**
     * 判断 a 是否大于等于 b
     *
     * @param a 第一个值
     * @param b 第二个值
     * @return a &gt;= b 返回 true
     */
    public static boolean ge(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) >= 0;
    }

    /**
     * 判断 a 是否小于 b
     *
     * @param a 第一个值
     * @param b 第二个值
     * @return a &lt; b 返回 true
     */
    public static boolean lt(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) < 0;
    }

    /**
     * 判断 a 是否小于等于 b
     *
     * @param a 第一个值
     * @param b 第二个值
     * @return a &lt;= b 返回 true
     */
    public static boolean le(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) <= 0;
    }

    /**
     * 判断值是否在指定范围内（包含边界）
     *
     * @param value 待判断的值
     * @param min   最小值
     * @param max   最大值
     * @return 在范围内返回 true
     */
    public static boolean isBetween(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return false;
        }
        return ge(value, min) && le(value, max);
    }

    /**
     * 判断值是否为零
     *
     * @param value 待判断的值
     * @return 为零返回 true
     */
    public static boolean isZero(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 判断值是否为正数
     *
     * @param value 待判断的值
     * @return 正数返回 true
     */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断值是否为负数
     *
     * @param value 待判断的值
     * @return 负数返回 true
     */
    public static boolean isNegative(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) < 0;
    }

    // ======================== 范围校验 ========================

    /**
     * 将数值限制在指定范围内
     * <p>
     * 示例：clamp(15, 0, 10) → 10
     * </p>
     *
     * @param value 数值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    public static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将数值限制在指定范围内
     *
     * @param value 数值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 将 BigDecimal 限制在指定范围内
     *
     * @param value 数值
     * @param min   最小值
     * @param max   最大值
     * @return 限制后的值
     */
    public static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null) {
            return min;
        }
        if (lt(value, min)) {
            return min;
        }
        if (gt(value, max)) {
            return max;
        }
        return value;
    }
}
