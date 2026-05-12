package com.zerx.common.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 断言工具类
 * <p>
 * 提供参数校验的便捷断言方法，校验失败时抛出带有明确错误信息的异常。
 * 主要用于方法入口处的参数校验，替代散落各处的 {@code if (x == null) throw new IllegalArgumentException(...)}
 * 样板代码，使校验逻辑更加简洁、统一。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public void createUser(String name, int age) {
 *     AssertUtil.notBlank(name, "用户名不能为空");
 *     AssertUtil.gt(age, 0, "年龄必须大于 0");
 * }
 * }</pre>
 *
 * @author zerx
 */
public final class AssertUtil {

    /** 私有构造器，防止实例化 */
    private AssertUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 空值校验 ========================

    /**
     * 断言对象不为 null
     *
     * @param obj     待校验对象
     * @param message 异常提示信息
     * @param <T>     对象类型
     * @return 校验通过返回原对象（支持链式调用）
     * @throws IllegalArgumentException 对象为 null 时抛出
     */
    public static <T> T notNull(T obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
        return obj;
    }

    /**
     * 断言字符串不为空白（null、空字符串、纯空格）
     *
     * @param str     待校验字符串
     * @param message 异常提示信息
     * @return 校验通过返回原字符串
     * @throws IllegalArgumentException 字符串为空白时抛出
     */
    public static String notBlank(String str, String message) {
        if (StringUtil.isBlank(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    /**
     * 断言字符串不为空（null 或长度为 0，不做 trim）
     *
     * @param str     待校验字符串
     * @param message 异常提示信息
     * @return 校验通过返回原字符串
     * @throws IllegalArgumentException 字符串为空时抛出
     */
    public static String notEmpty(String str, String message) {
        if (StringUtil.isEmpty(str)) {
            throw new IllegalArgumentException(message);
        }
        return str;
    }

    /**
     * 断言集合不为空（null 或无元素）
     *
     * @param collection 待校验集合
     * @param message    异常提示信息
     * @param <T>        集合元素类型
     * @return 校验通过返回原集合
     * @throws IllegalArgumentException 集合为空时抛出
     */
    public static <T> Collection<T> notEmpty(Collection<T> collection, String message) {
        if (CollectionUtil.isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }

    /**
     * 断言 Map 不为空
     *
     * @param map     待校验 Map
     * @param message 异常提示信息
     * @param <K>     键类型
     * @param <V>     值类型
     * @return 校验通过返回原 Map
     * @throws IllegalArgumentException Map 为空时抛出
     */
    public static <K, V> Map<K, V> notEmpty(Map<K, V> map, String message) {
        if (CollectionUtil.isEmpty(map)) {
            throw new IllegalArgumentException(message);
        }
        return map;
    }

    /**
     * 断言数组不为空（null 或长度为 0）
     *
     * @param array   待校验数组
     * @param message 异常提示信息
     * @param <T>     数组元素类型
     * @return 校验通过返回原数组
     * @throws IllegalArgumentException 数组为空时抛出
     */
    public static <T> T[] notEmpty(T[] array, String message) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(message);
        }
        return array;
    }

    // ======================== 数值校验 ========================

    /**
     * 断言数值大于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（不包含）
     * @param message 异常提示信息
     * @throws IllegalArgumentException 数值不大于 min 时抛出
     */
    public static void gt(int value, int min, String message) {
        if (!(value > min)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值大于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（不包含）
     * @param message 异常提示信息
     */
    public static void gt(long value, long min, String message) {
        if (!(value > min)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值大于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（不包含）
     * @param message 异常提示信息
     */
    public static void gt(double value, double min, String message) {
        if (!(value > min)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言 BigDecimal 大于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（不包含）
     * @param message 异常提示信息
     */
    public static void gt(BigDecimal value, BigDecimal min, String message) {
        if (value == null || value.compareTo(min) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值大于等于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（包含）
     * @param message 异常提示信息
     */
    public static void ge(int value, int min, String message) {
        if (!(value >= min)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值大于等于指定值
     *
     * @param value   待校验数值
     * @param min     最小值（包含）
     * @param message 异常提示信息
     */
    public static void ge(long value, long min, String message) {
        if (!(value >= min)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值小于指定值
     *
     * @param value   待校验数值
     * @param max     最大值（不包含）
     * @param message 异常提示信息
     */
    public static void lt(int value, int max, String message) {
        if (!(value < max)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值小于指定值
     *
     * @param value   待校验数值
     * @param max     最大值（不包含）
     * @param message 异常提示信息
     */
    public static void lt(long value, long max, String message) {
        if (!(value < max)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值小于等于指定值
     *
     * @param value   待校验数值
     * @param max     最大值（包含）
     * @param message 异常提示信息
     */
    public static void le(int value, int max, String message) {
        if (!(value <= max)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值在指定范围内（包含边界）
     *
     * @param value   待校验数值
     * @param min     最小值（包含）
     * @param max     最大值（包含）
     * @param message 异常提示信息
     */
    public static void between(int value, int min, int max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言数值在指定范围内（包含边界）
     *
     * @param value   待校验数值
     * @param min     最小值（包含）
     * @param max     最大值（包含）
     * @param message 异常提示信息
     */
    public static void between(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言字符串长度在指定范围内
     *
     * @param str     待校验字符串
     * @param minLen  最小长度（包含）
     * @param maxLen  最大长度（包含）
     * @param message 异常提示信息
     */
    public static void length(String str, int minLen, int maxLen, String message) {
        if (str == null || str.length() < minLen || str.length() > maxLen) {
            throw new IllegalArgumentException(message);
        }
    }

    // ======================== 条件校验 ========================

    /**
     * 断言条件为 true
     *
     * @param condition 布尔条件
     * @param message   异常提示信息
     * @throws IllegalArgumentException 条件为 false 时抛出
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言条件为 false
     *
     * @param condition 布尔条件
     * @param message   异常提示信息
     * @throws IllegalArgumentException 条件为 true 时抛出
     */
    public static void isFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个对象相等
     *
     * @param expected 期望值
     * @param actual   实际值
     * @param message  异常提示信息
     */
    public static void equals(Object expected, Object actual, String message) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个对象不相等
     *
     * @param unexpected 不期望的值
     * @param actual     实际值
     * @param message    异常提示信息
     */
    public static void notEquals(Object unexpected, Object actual, String message) {
        if (java.util.Objects.equals(unexpected, actual)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 断言两个对象为同一个引用（==）
     *
     * @param expected 期望对象
     * @param actual   实际对象
     * @param message  异常提示信息
     */
    public static void same(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 直接抛出异常（用于复杂的条件判断后）
     *
     * @param message 异常提示信息
     * @throws IllegalArgumentException 直接抛出
     */
    public static void fail(String message) {
        throw new IllegalArgumentException(message);
    }

    /**
     * 断言状态，失败时抛出 IllegalStateException
     * <p>
     * 适用于对象状态校验（如调用顺序、生命周期等），而非参数校验。
     * </p>
     *
     * @param condition 布尔条件
     * @param message   异常提示信息
     * @throws IllegalStateException 条件为 false 时抛出
     */
    public static void state(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
