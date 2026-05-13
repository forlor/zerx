package com.zerx.common.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 字符串工具类
 * <p>
 * 提供字符串常用操作，包括判空、裁剪、驼峰转换、正则匹配、编码转换等。
 * 所有方法均为静态方法，线程安全，方便在任何场景下直接调用。
 * </p>
 *
 * @author zerx
 */
public final class StringUtil {

    /** 空字符串常量 */
    public static final String EMPTY = "";

    /** Unicode 不可见零宽字符正则（用于清理隐藏字符） */
    private static final Pattern INVISIBLE_CHAR_PATTERN =
            Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]");

    /** 私有构造器，防止实例化 */
    private StringUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 判空相关 ========================

    /**
     * 判断字符串是否为 null 或空字符串
     *
     * @param str 待判断的字符串
     * @return 为 null 或空字符串返回 true
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为 null 且不是空字符串
     *
     * @param str 待判断的字符串
     * @return 有内容返回 true
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 判断字符串是否为 null 或长度为 0
     * <p>
     * 与 {@link #isBlank(String)} 不同，此方法不进行 trim 操作
     * </p>
     *
     * @param str 待判断的字符串
     * @return 为 null 或长度为 0 返回 true
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断是否所有字符串都为空
     *
     * @param strings 待判断的字符串数组
     * @return 所有字符串都为空返回 true，数组本身为 null 也返回 true
     */
    public static boolean isAllBlank(String... strings) {
        if (strings == null) {
            return true;
        }
        for (String str : strings) {
            if (isNotBlank(str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断是否任一字符串为空白
     *
     * @param strings 待判断的字符串数组
     * @return 任一字符串为空白返回 true，数组本身为 null 也返回 true
     */
    public static boolean isAnyBlank(String... strings) {
        if (strings == null) {
            return true;
        }
        for (String str : strings) {
            if (isBlank(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否所有字符串都不为空白
     *
     * @param strings 待判断的字符串数组
     * @return 所有字符串都不为空白返回 true
     */
    public static boolean isNoneBlank(String... strings) {
        return !isAnyBlank(strings);
    }

    // ======================== 裁剪与清理 ========================

    /**
     * 给定默认值：当字符串为空时返回默认值，否则返回原字符串
     *
     * @param str        原字符串
     * @param defaultVal 默认值
     * @return 非空字符串或默认值
     */
    public static String defaultIfBlank(String str, String defaultVal) {
        return isBlank(str) ? defaultVal : str;
    }

    /**
     * 截取字符串，超出部分用省略号代替
     *
     * @param str    原字符串
     * @param maxLength 最大保留长度
     * @return 截取后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (maxLength < 0) {
            maxLength = 0;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        if (maxLength <= 3) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 清理字符串中的不可见 Unicode 字符（零宽字符等）
     *
     * @param str 原字符串
     * @return 清理后的字符串
     */
    public static String cleanInvisibleChars(String str) {
        if (str == null) {
            return null;
        }
        return INVISIBLE_CHAR_PATTERN.matcher(str).replaceAll(EMPTY);
    }

    /**
     * 移除字符串前缀
     *
     * @param str    原字符串
     * @param prefix 要移除的前缀（不区分大小写）
     * @return 移除前缀后的字符串
     */
    public static String removePrefix(String str, String prefix) {
        if (str == null || prefix == null) {
            return str;
        }
        if (str.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return str.substring(prefix.length());
        }
        return str;
    }

    /**
     * 移除字符串后缀
     *
     * @param str    原字符串
     * @param suffix 要移除的后缀（不区分大小写）
     * @return 移除后缀后的字符串
     */
    public static String removeSuffix(String str, String suffix) {
        if (str == null || suffix == null) {
            return str;
        }
        if (str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length())) {
            return str.substring(0, str.length() - suffix.length());
        }
        return str;
    }

    // ======================== 驼峰转换 ========================

    /**
     * 驼峰转下划线命名
     * <p>
     * 示例：userName → user_name，HttpServletRequest → http_servlet_request
     * </p>
     *
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToUnderscore(String camelCase) {
        if (isBlank(camelCase)) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder(camelCase.length() + 4);
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 下划线转驼峰命名
     * <p>
     * 示例：user_name → userName，_private_field → PrivateField
     * </p>
     *
     * @param underscore 下划线命名字符串
     * @return 驼峰命名字符串
     */
    public static String underscoreToCamel(String underscore) {
        if (isBlank(underscore)) {
            return underscore;
        }
        StringBuilder sb = new StringBuilder(underscore.length());
        boolean upperNext = false;
        for (int i = 0; i < underscore.length(); i++) {
            char c = underscore.charAt(i);
            if (c == '_') {
                upperNext = true;
            } else {
                if (upperNext) {
                    sb.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 首字母大写
     *
     * @param str 原字符串
     * @return 首字母大写的字符串
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * 首字母小写
     *
     * @param str 原字符串
     * @return 首字母小写的字符串
     */
    public static String uncapitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    // ======================== 正则匹配 ========================

    /**
     * 简单正则匹配
     *
     * @param str     待匹配的字符串
     * @param regex   正则表达式
     * @return 匹配成功返回 true，null 始终返回 false
     */
    public static boolean matches(String str, String regex) {
        if (str == null || regex == null) {
            return false;
        }
        return Pattern.matches(regex, str);
    }

    /**
     * 判断是否为纯中文字符串
     *
     * @param str 待判断的字符串
     * @return 全部为中文字符返回 true
     */
    public static boolean isChinese(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return matches(str.trim(), "^[\u4e00-\u9fa5]+$");
    }

    /**
     * 判断是否为有效的手机号（中国大陆）
     *
     * @param str 待判断的字符串
     * @return 符合手机号格式返回 true
     */
    public static boolean isMobile(String str) {
        return isNotBlank(str) && matches(str.trim(), "^1[3-9]\\d{9}$");
    }

    /**
     * 判断是否为有效的邮箱地址
     *
     * @param str 待判断的字符串
     * @return 符合邮箱格式返回 true
     */
    public static boolean isEmail(String str) {
        return isNotBlank(str) && matches(str.trim(),
                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    // ======================== 编码转换 ========================

    /**
     * 将字符串转换为 UTF-8 字节数组
     *
     * @param str 原字符串
     * @return UTF-8 编码的字节数组，null 返回空数组
     */
    public static byte[] toUtf8Bytes(String str) {
        if (str == null) {
            return new byte[0];
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将 UTF-8 字节数组转换为字符串
     *
     * @param bytes UTF-8 编码的字节数组
     * @return 解码后的字符串，null 返回 null
     */
    public static String fromUtf8Bytes(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 按指定编码将字符串转换为字节数组
     *
     * @param str     原字符串
     * @param charset 字符集
     * @return 编码后的字节数组
     */
    public static byte[] toBytes(String str, Charset charset) {
        if (str == null) {
            return new byte[0];
        }
        return str.getBytes(charset);
    }

    /**
     * 将字符串转换为十六进制表示
     * <p>
     * 示例："Hello" → "48656c6c6f"
     * </p>
     *
     * @param str 原字符串
     * @return 十六进制字符串，null 返回 null
     */
    public static String toHexString(String str) {
        if (str == null) {
            return null;
        }
        return HexFormat.of().formatHex(str.getBytes(StandardCharsets.UTF_8));
    }

    // ======================== 重复与填充 ========================

    /**
     * 重复字符串
     *
     * @param str     要重复的字符串
     * @param count   重复次数
     * @return 重复后的字符串
     */
    public static String repeat(String str, int count) {
        if (str == null) {
            return null;
        }
        if (count <= 0) {
            return EMPTY;
        }
        return str.repeat(count);
    }

    /**
     * 反转字符串
     *
     * @param str 原字符串
     * @return 反转后的字符串
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }

    // ======================== 分割与连接 ========================

    /**
     * 分割字符串，自动去除空白项
     *
     * @param str       原字符串
     * @param separator 分隔符
     * @return 分割后的字符串数组
     */
    public static String[] splitTrim(String str, String separator) {
        if (isEmpty(str)) {
            return new String[0];
        }
        String[] parts = str.split(separator);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    /**
     * 使用分隔符连接字符串数组
     *
     * @param separator 分隔符
     * @param parts     字符串数组
     * @return 连接后的字符串
     */
    public static String join(String separator, String... parts) {
        if (parts == null || parts.length == 0) {
            return EMPTY;
        }
        return String.join(separator, parts);
    }
}
