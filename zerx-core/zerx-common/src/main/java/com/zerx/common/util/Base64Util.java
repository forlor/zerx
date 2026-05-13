package com.zerx.common.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 编解码工具类
 * <p>
 * 提供 Base64 标准编码、URL 安全编码的编解码能力，支持字符串和字节数组两种形式。
 * 基于 JDK 内置 {@link Base64}，无第三方依赖。
 * </p>
 *
 * @author zerx
 */
public final class Base64Util {

    /** 私有构造器，防止实例化 */
    private Base64Util() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 标准编解码 ========================

    /**
     * 将字符串进行 Base64 编码（使用 UTF-8 字符集）
     *
     * @param src 原始字符串
     * @return Base64 编码后的字符串，null 返回 null
     */
    public static String encode(String src) {
        return encode(src, StandardCharsets.UTF_8);
    }

    /**
     * 将字符串进行 Base64 编码（指定字符集）
     *
     * @param src     原始字符串
     * @param charset 字符集
     * @return Base64 编码后的字符串，null 返回 null
     */
    public static String encode(String src, Charset charset) {
        if (src == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(src.getBytes(charset));
    }

    /**
     * 将字节数组进行 Base64 编码
     *
     * @param src 原始字节数组
     * @return Base64 编码后的字符串，null 返回 null
     */
    public static String encode(byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(src);
    }

    /**
     * 将字节数组编码为 Base64 字节数组
     *
     * @param src 原始字节数组
     * @return Base64 编码后的字节数组，null 返回 null
     */
    public static byte[] encodeToBytes(byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getEncoder().encode(src);
    }

    /**
     * 将 Base64 字符串解码为原始字符串（使用 UTF-8 字符集）
     *
     * @param src Base64 编码的字符串
     * @return 解码后的字符串，null 或空字符串返回 null
     */
    public static String decode(String src) {
        return decode(src, StandardCharsets.UTF_8);
    }

    /**
     * 将 Base64 字符串解码为原始字符串（指定字符集）
     *
     * @param src     Base64 编码的字符串
     * @param charset 字符集
     * @return 解码后的字符串，null 或空字符串返回 null
     */
    public static String decode(String src, Charset charset) {
        if (src == null) {
            return null;
        }
        return new String(decodeToBytes(src), charset);
    }

    /**
     * 将 Base64 字符串解码为字节数组
     *
     * @param src Base64 编码的字符串
     * @return 解码后的字节数组，null 返回 null
     * @throws IllegalArgumentException 当输入不是合法的 Base64 字符串时抛出
     */
    public static byte[] decodeToBytes(String src) {
        if (src == null) {
            return null;
        }
        return Base64.getDecoder().decode(src);
    }

    /**
     * 将 Base64 字节数组解码为原始字节数组
     *
     * @param src Base64 编码的字节数组
     * @return 解码后的字节数组，null 返回 null
     */
    public static byte[] decode(byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getDecoder().decode(src);
    }

    // ======================== URL 安全编解码 ========================

    /**
     * 将字符串进行 URL 安全的 Base64 编码（使用 UTF-8 字符集）
     * <p>
     * URL 安全编码会移除 {@code '+'}、{@code '/'} 和 {@code '='} 填充符，
     * 替换为 {@code '-'}、{@code '_'}，适用于 URL 参数、文件名等场景。
     * </p>
     *
     * @param src 原始字符串
     * @return URL 安全的 Base64 编码字符串，null 返回 null
     */
    public static String encodeUrlSafe(String src) {
        return encodeUrlSafe(src, StandardCharsets.UTF_8);
    }

    /**
     * 将字符串进行 URL 安全的 Base64 编码（指定字符集）
     *
     * @param src     原始字符串
     * @param charset 字符集
     * @return URL 安全的 Base64 编码字符串，null 返回 null
     */
    public static String encodeUrlSafe(String src, Charset charset) {
        if (src == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(src.getBytes(charset));
    }

    /**
     * 将字节数组进行 URL 安全的 Base64 编码
     *
     * @param src 原始字节数组
     * @return URL 安全的 Base64 编码字符串，null 返回 null
     */
    public static String encodeUrlSafe(byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(src);
    }

    /**
     * 将 URL 安全的 Base64 字符串解码为原始字符串（使用 UTF-8 字符集）
     *
     * @param src URL 安全的 Base64 编码字符串
     * @return 解码后的字符串，null 返回 null
     */
    public static String decodeUrlSafe(String src) {
        return decodeUrlSafe(src, StandardCharsets.UTF_8);
    }

    /**
     * 将 URL 安全的 Base64 字符串解码为原始字符串（指定字符集）
     *
     * @param src     URL 安全的 Base64 编码字符串
     * @param charset 字符集
     * @return 解码后的字符串，null 返回 null
     */
    public static String decodeUrlSafe(String src, Charset charset) {
        if (src == null) {
            return null;
        }
        return new String(Base64.getUrlDecoder().decode(src), charset);
    }

    /**
     * 将 URL 安全的 Base64 字符串解码为字节数组
     *
     * @param src URL 安全的 Base64 编码字符串
     * @return 解码后的字节数组，null 返回 null
     */
    public static byte[] decodeUrlSafeToBytes(String src) {
        if (src == null) {
            return null;
        }
        return Base64.getUrlDecoder().decode(src);
    }

    // ======================== MIME 编解码 ========================

    /**
     * 将字节数组进行 MIME Base64 编码
     * <p>
     * MIME 编码每行不超过 76 个字符，并以 {@code CRLF} 分行，
     * 适用于电子邮件等 MIME 协议场景。
     * </p>
     *
     * @param src 原始字节数组
     * @return MIME Base64 编码字符串，null 返回 null
     */
    public static String encodeMime(byte[] src) {
        if (src == null) {
            return null;
        }
        return Base64.getMimeEncoder().encodeToString(src);
    }

    /**
     * 将 MIME Base64 字符串解码为字节数组
     *
     * @param src MIME Base64 编码字符串
     * @return 解码后的字节数组，null 返回 null
     */
    public static byte[] decodeMime(String src) {
        if (src == null) {
            return null;
        }
        return Base64.getMimeDecoder().decode(src);
    }

    // ======================== 校验 ========================

    /**
     * 判断字符串是否为合法的 Base64 编码
     * <p>
     * 通过尝试解码来判断合法性，不会抛出异常。
     * </p>
     *
     * @param src 待判断的字符串
     * @return 是合法的 Base64 字符串返回 true
     */
    public static boolean isBase64(String src) {
        if (src == null || src.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(src);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为合法的 URL 安全 Base64 编码
     *
     * @param src 待判断的字符串
     * @return 是合法的 URL 安全 Base64 字符串返回 true
     */
    public static boolean isUrlSafeBase64(String src) {
        if (src == null || src.isEmpty()) {
            return false;
        }
        try {
            Base64.getUrlDecoder().decode(src);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
