package com.zerx.common.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * HMAC 消息认证码工具类
 * <p>
 * 基于 JDK 原生 {@link javax.crypto.Mac} 实现，零第三方依赖。
 * 支持 HMAC-SHA256 和 HMAC-SHA512 两种算法，用于消息签名、
 * API 请求防篡改、数据完整性验证等场景。
 * </p>
 *
 * <h3>性能优化：</h3>
 * <ul>
 *   <li>使用 {@link ThreadLocal} 缓存 Mac 实例，避免每次签名都重新创建</li>
 *   <li>所有方法均为纯函数，无共享可变状态，天然线程安全</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // HMAC-SHA256 签名
 * String signature = HmacUtil.hmacSha256Hex("message", "secretKey");
 *
 * // 验证签名
 * boolean valid = HmacUtil.verifySha256("message", signature, "secretKey");
 *
 * // 流式更新（大文件分块签名）
 * Mac mac = HmacUtil.createSha256Mac("secretKey");
 * mac.update(chunk1);
 * mac.update(chunk2);
 * String signature = HmacUtil.doFinalHex(mac);
 * }</pre>
 *
 * @author zerx
 */
public final class HmacUtil {

    // ======================== 算法常量 ========================

    /** HMAC-SHA256 算法标识（32 字节签名，64 字符 HEX） */
    public static final String ALGORITHM_HMAC_SHA256 = "HmacSHA256";

    /** HMAC-SHA512 算法标识（64 字节签名，128 字符 HEX） */
    public static final String ALGORITHM_HMAC_SHA512 = "HmacSHA512";

    // ======================== ThreadLocal Mac 缓存 ========================

    /** ThreadLocal Mac：SHA256（Mac 不是线程安全的，必须按线程隔离） */
    private static final ThreadLocal<Mac> MAC_SHA256 =
            ThreadLocal.withInitial(() -> createMac(ALGORITHM_HMAC_SHA256));

    /** ThreadLocal Mac：SHA512 */
    private static final ThreadLocal<Mac> MAC_SHA512 =
            ThreadLocal.withInitial(() -> createMac(ALGORITHM_HMAC_SHA512));

    private HmacUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== HMAC-SHA256 ========================

    /**
     * HMAC-SHA256 签名（HEX 编码）
     *
     * @param data   原始数据
     * @param secret 密钥
     * @return HEX 编码的签名，null 输入返回 null
     */
    public static String hmacSha256Hex(String data, String secret) {
        if (data == null || secret == null) {
            return null;
        }
        return hmacSha256Hex(data.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * HMAC-SHA256 签名（HEX 编码，字节数组输入）
     *
     * @param data   原始数据字节
     * @param secret 密钥字节
     * @return HEX 编码的签名，null 输入返回 null
     */
    public static String hmacSha256Hex(byte[] data, byte[] secret) {
        byte[] hash = hmacSha256(data, secret);
        return hash != null ? HexFormat.of().formatHex(hash) : null;
    }

    /**
     * HMAC-SHA256 签名（原始字节数组）
     *
     * @param data   原始数据字节
     * @param secret 密钥字节
     * @return 签名字节数组，null 输入返回 null
     */
    public static byte[] hmacSha256(byte[] data, byte[] secret) {
        if (data == null || secret == null) {
            return null;
        }
        return doHmac(data, secret, ALGORITHM_HMAC_SHA256, MAC_SHA256);
    }

    /**
     * 验证 HMAC-SHA256 签名（常量时间比较，防时序攻击）
     *
     * @param data      原始数据
     * @param signature HEX 编码的签名
     * @param secret    密钥
     * @return 签名是否匹配
     */
    public static boolean verifySha256(String data, String signature, String secret) {
        if (data == null || signature == null || secret == null) {
            return false;
        }
        return verifySha256(
                data.getBytes(StandardCharsets.UTF_8),
                HexFormat.of().parseHex(signature),
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 验证 HMAC-SHA256 签名（原始字节输入，常量时间比较）
     *
     * @param data        原始数据字节
     * @param expected    期望的签名字节
     * @param secret      密钥字节
     * @return 签名是否匹配
     */
    public static boolean verifySha256(byte[] data, byte[] expected, byte[] secret) {
        if (data == null || expected == null || secret == null) {
            return false;
        }
        byte[] actual = hmacSha256(data, secret);
        return MessageDigestIsEqual.isEqual(expected, actual);
    }

    // ======================== HMAC-SHA512 ========================

    /**
     * HMAC-SHA512 签名（HEX 编码）
     *
     * @param data   原始数据
     * @param secret 密钥
     * @return HEX 编码的签名，null 输入返回 null
     */
    public static String hmacSha512Hex(String data, String secret) {
        if (data == null || secret == null) {
            return null;
        }
        return hmacSha512Hex(data.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * HMAC-SHA512 签名（HEX 编码，字节数组输入）
     *
     * @param data   原始数据字节
     * @param secret 密钥字节
     * @return HEX 编码的签名，null 输入返回 null
     */
    public static String hmacSha512Hex(byte[] data, byte[] secret) {
        byte[] hash = hmacSha512(data, secret);
        return hash != null ? HexFormat.of().formatHex(hash) : null;
    }

    /**
     * HMAC-SHA512 签名（原始字节数组）
     *
     * @param data   原始数据字节
     * @param secret 密钥字节
     * @return 签名字节，null 输入返回 null
     */
    public static byte[] hmacSha512(byte[] data, byte[] secret) {
        if (data == null || secret == null) {
            return null;
        }
        return doHmac(data, secret, ALGORITHM_HMAC_SHA512, MAC_SHA512);
    }

    /**
     * 验证 HMAC-SHA512 签名（常量时间比较）
     *
     * @param data      原始数据
     * @param signature HEX 编码的签名
     * @param secret    密钥
     * @return 签名是否匹配
     */
    public static boolean verifySha512(String data, String signature, String secret) {
        if (data == null || signature == null || secret == null) {
            return false;
        }
        return verifySha512(
                data.getBytes(StandardCharsets.UTF_8),
                HexFormat.of().parseHex(signature),
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 验证 HMAC-SHA512 签名（原始字节输入，常量时间比较）
     *
     * @param data     原始数据字节
     * @param expected 期望的签名字节
     * @param secret   密钥字节
     * @return 签名是否匹配
     */
    public static boolean verifySha512(byte[] data, byte[] expected, byte[] secret) {
        if (data == null || expected == null || secret == null) {
            return false;
        }
        byte[] actual = hmacSha512(data, secret);
        return MessageDigestIsEqual.isEqual(expected, actual);
    }

    // ======================== 流式 API ========================

    /**
     * 创建 HMAC-SHA256 的 Mac 实例（用于流式分块签名）
     * <p>
     * 调用方通过 {@link Mac#update(byte[])} 分批写入数据，
     * 最后调用 {@link Mac#doFinal()} 获取签名。
     * 使用完毕后无需手动 reset，下次操作前 Mac 会自动重新初始化。
     * </p>
     *
     * @param secret 密钥（UTF-8 编码）
     * @return 初始化后的 Mac 实例
     */
    public static Mac createSha256Mac(String secret) {
        return initMac(ALGORITHM_HMAC_SHA256, MAC_SHA256, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 HMAC-SHA256 的 Mac 实例（字节数组密钥）
     *
     * @param secret 密钥字节
     * @return 初始化后的 Mac 实例
     */
    public static Mac createSha256Mac(byte[] secret) {
        return initMac(ALGORITHM_HMAC_SHA256, MAC_SHA256, secret);
    }

    /**
     * 创建 HMAC-SHA512 的 Mac 实例（用于流式分块签名）
     *
     * @param secret 密钥（UTF-8 编码）
     * @return 初始化后的 Mac 实例
     */
    public static Mac createSha512Mac(String secret) {
        return initMac(ALGORITHM_HMAC_SHA512, MAC_SHA512, secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 HMAC-SHA512 的 Mac 实例（字节数组密钥）
     *
     * @param secret 密钥字节
     * @return 初始化后的 Mac 实例
     */
    public static Mac createSha512Mac(byte[] secret) {
        return initMac(ALGORITHM_HMAC_SHA512, MAC_SHA512, secret);
    }

    /**
     * 完成 Mac 计算并返回 HEX 编码的签名
     * <p>
     * 会自动 reset Mac 实例，使其可用于下一次签名操作。
     * </p>
     *
     * @param mac Mac 实例（通过 createXxxMac 创建）
     * @return HEX 编码的签名
     */
    public static String doFinalHex(Mac mac) {
        Objects.requireNonNull(mac, "Mac 实例不能为 null");
        byte[] hash = mac.doFinal();
        mac.reset();
        return HexFormat.of().formatHex(hash);
    }

    /**
     * 完成 Mac 计算并返回原始签名字节
     *
     * @param mac Mac 实例
     * @return 签名字节数组
     */
    public static byte[] doFinalBytes(Mac mac) {
        Objects.requireNonNull(mac, "Mac 实例不能为 null");
        byte[] hash = mac.doFinal();
        mac.reset();
        return hash;
    }

    // ======================== 内部方法 ========================

    private static byte[] doHmac(byte[] data, byte[] secret, String algorithm, ThreadLocal<Mac> macCache) {
        try {
            Mac mac = macCache.get();
            mac.reset();
            mac.init(new SecretKeySpec(secret, algorithm));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败: " + algorithm, e);
        }
    }

    private static Mac initMac(String algorithm, ThreadLocal<Mac> macCache, byte[] secret) {
        try {
            Mac mac = macCache.get();
            mac.reset();
            mac.init(new SecretKeySpec(secret, algorithm));
            return mac;
        } catch (Exception e) {
            throw new IllegalStateException("Mac 初始化失败: " + algorithm, e);
        }
    }

    private static Mac createMac(String algorithm) {
        try {
            return Mac.getInstance(algorithm);
        } catch (Exception e) {
            throw new UnsupportedOperationException("JDK 不支持的 MAC 算法: " + algorithm, e);
        }
    }

    /**
     * 常量时间比较，防止时序攻击（timing attack）
     * <p>
     * 两个数组长度不同时也始终比较所有字节，不会提前返回。
     * </p>
     */
    private static final class MessageDigestIsEqual {

        static boolean isEqual(byte[] a, byte[] b) {
            if (a == b) {
                return true;
            }
            if (a == null || b == null) {
                return false;
            }
            int result = a.length ^ b.length;
            for (int i = 0; i < a.length && i < b.length; i++) {
                result |= a[i] ^ b[i];
            }
            return result == 0;
        }
    }
}
