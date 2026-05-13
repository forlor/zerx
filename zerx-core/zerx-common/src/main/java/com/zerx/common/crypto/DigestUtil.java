package com.zerx.common.crypto;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 摘要哈希工具类
 * <p>
 * 提供 MD5、SHA-1、SHA-256、SHA-512 等常用摘要算法的便捷方法。
 * 基于 JDK 原生 {@link MessageDigest}，无第三方依赖。
 * </p>
 * <p>
 * <b>注意：MD5 和 SHA-1 已不推荐用于安全场景</b>，
 * 仅适用于数据校验、非加密场景。安全场景请使用 SHA-256 及以上。
 * </p>
 *
 * @author zerx
 */
public final class DigestUtil {

    /** MD5 算法标识（16 字节，32 字符十六进制） */
    public static final String ALGORITHM_MD5 = "MD5";

    /** SHA-1 算法标识（20 字节，40 字符十六进制） */
    public static final String ALGORITHM_SHA1 = "SHA-1";

    /** SHA-256 算法标识（32 字节，64 字符十六进制） */
    public static final String ALGORITHM_SHA256 = "SHA-256";

    /** SHA-512 算法标识（64 字节，128 字符十六进制） */
    public static final String ALGORITHM_SHA512 = "SHA-512";

    /** 默认字符集 */
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /** 私有构造器，防止实例化 */
    private DigestUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== MD5 ========================

    /**
     * 计算 MD5 摘要（UTF-8 编码）
     * <p>
     * 返回 32 位小写十六进制字符串。
     * </p>
     *
     * @param data 原始数据
     * @return MD5 摘要字符串，null 返回 null
     */
    public static String md5(String data) {
        return md5(data, DEFAULT_CHARSET);
    }

    /**
     * 计算 MD5 摘要（指定字符集）
     *
     * @param data    原始数据
     * @param charset 字符集
     * @return MD5 摘要字符串，null 返回 null
     */
    public static String md5(String data, Charset charset) {
        return digestHex(data, ALGORITHM_MD5, charset);
    }

    /**
     * 计算字节数组的 MD5 摘要
     *
     * @param data 原始字节数组
     * @return MD5 摘要字符串，null 返回 null
     */
    public static String md5(byte[] data) {
        return digestHex(data, ALGORITHM_MD5);
    }

    // ======================== SHA-1 ========================

    /**
     * 计算 SHA-1 摘要（UTF-8 编码）
     * <p>
     * 返回 40 位小写十六进制字符串。
     * </p>
     *
     * @param data 原始数据
     * @return SHA-1 摘要字符串，null 返回 null
     */
    public static String sha1(String data) {
        return sha1(data, DEFAULT_CHARSET);
    }

    /**
     * 计算 SHA-1 摘要（指定字符集）
     *
     * @param data    原始数据
     * @param charset 字符集
     * @return SHA-1 摘要字符串，null 返回 null
     */
    public static String sha1(String data, Charset charset) {
        return digestHex(data, ALGORITHM_SHA1, charset);
    }

    /**
     * 计算字节数组的 SHA-1 摘要
     *
     * @param data 原始字节数组
     * @return SHA-1 摘要字符串，null 返回 null
     */
    public static String sha1(byte[] data) {
        return digestHex(data, ALGORITHM_SHA1);
    }

    // ======================== SHA-256 ========================

    /**
     * 计算 SHA-256 摘要（UTF-8 编码）
     * <p>
     * 返回 64 位小写十六进制字符串。推荐用于密码存储、数据完整性校验等安全场景。
     * </p>
     *
     * @param data 原始数据
     * @return SHA-256 摘要字符串，null 返回 null
     */
    public static String sha256(String data) {
        return sha256(data, DEFAULT_CHARSET);
    }

    /**
     * 计算 SHA-256 摘要（指定字符集）
     *
     * @param data    原始数据
     * @param charset 字符集
     * @return SHA-256 摘要字符串，null 返回 null
     */
    public static String sha256(String data, Charset charset) {
        return digestHex(data, ALGORITHM_SHA256, charset);
    }

    /**
     * 计算字节数组的 SHA-256 摘要
     *
     * @param data 原始字节数组
     * @return SHA-256 摘要字符串，null 返回 null
     */
    public static String sha256(byte[] data) {
        return digestHex(data, ALGORITHM_SHA256);
    }

    // ======================== SHA-512 ========================

    /**
     * 计算 SHA-512 摘要（UTF-8 编码）
     * <p>
     * 返回 128 位小写十六进制字符串。安全性高于 SHA-256，
     * 适用于对安全性要求更高的场景。
     * </p>
     *
     * @param data 原始数据
     * @return SHA-512 摘要字符串，null 返回 null
     */
    public static String sha512(String data) {
        return sha512(data, DEFAULT_CHARSET);
    }

    /**
     * 计算 SHA-512 摘要（指定字符集）
     *
     * @param data    原始数据
     * @param charset 字符集
     * @return SHA-512 摘要字符串，null 返回 null
     */
    public static String sha512(String data, Charset charset) {
        return digestHex(data, ALGORITHM_SHA512, charset);
    }

    /**
     * 计算字节数组的 SHA-512 摘要
     *
     * @param data 原始字节数组
     * @return SHA-512 摘要字符串，null 返回 null
     */
    public static String sha512(byte[] data) {
        return digestHex(data, ALGORITHM_SHA512);
    }

    // ======================== 通用摘要 ========================

    /**
     * 使用指定算法计算字符串摘要
     *
     * @param data      原始数据
     * @param algorithm 算法名称（如 "SHA-256"）
     * @return 十六进制摘要字符串，null 返回 null
     */
    public static String digestHex(String data, String algorithm) {
        return digestHex(data, algorithm, DEFAULT_CHARSET);
    }

    /**
     * 使用指定算法计算字符串摘要（指定字符集）
     *
     * @param data      原始数据
     * @param algorithm 算法名称
     * @param charset   字符集
     * @return 十六进制摘要字符串，null 返回 null
     */
    public static String digestHex(String data, String algorithm, Charset charset) {
        if (data == null) {
            return null;
        }
        return digestHex(data.getBytes(charset), algorithm);
    }

    /**
     * 使用指定算法计算字节数组摘要
     *
     * @param data      原始字节数组
     * @param algorithm 算法名称
     * @return 十六进制摘要字符串，null 返回 null
     */
    public static String digestHex(byte[] data, String algorithm) {
        if (data == null) {
            return null;
        }
        byte[] hash = digest(data, algorithm);
        return HexFormat.of().formatHex(hash);
    }

    /**
     * 使用指定算法计算字节数组摘要，返回原始字节数组
     *
     * @param data      原始字节数组
     * @param algorithm 算法名称
     * @return 摘要字节数组，null 返回空数组
     */
    public static byte[] digest(byte[] data, String algorithm) {
        if (data == null) {
            return new byte[0];
        }
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("不支持的摘要算法: " + algorithm, e);
        }
    }

    /**
     * 获取指定算法的 MessageDigest 实例
     * <p>
     * 适用于需要分多次 update 的场景。
     * </p>
     *
     * @param algorithm 算法名称
     * @return MessageDigest 实例
     * @throws UnsupportedOperationException 不支持的算法时抛出
     */
    public static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("不支持的摘要算法: " + algorithm, e);
        }
    }

    // ======================== 字节数组形式 ========================

    /**
     * 计算字节数组的 MD5 摘要，返回字节数组形式（16 字节）
     *
     * @param data 原始字节数组
     * @return MD5 摘要字节数组，null 返回空数组
     */
    public static byte[] md5Bytes(byte[] data) {
        return digest(data, ALGORITHM_MD5);
    }

    /**
     * 计算字节数组的 SHA-256 摘要，返回字节数组形式（32 字节）
     *
     * @param data 原始字节数组
     * @return SHA-256 摘要字节数组，null 返回空数组
     */
    public static byte[] sha256Bytes(byte[] data) {
        return digest(data, ALGORITHM_SHA256);
    }
}
