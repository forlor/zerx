package com.zerx.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;

/**
 * AES 对称加解密工具类
 * <p>
 * 基于 JDK 原生 {@link javax.crypto.Cipher} 实现，零第三方依赖。
 * 支持 AES-GCM（推荐）和 AES-CBC 两种模式。
 * </p>
 *
 * <h3>性能优化：</h3>
 * <ul>
 *   <li>使用 {@link ThreadLocal} 缓存 Cipher 实例，避免每次加解密都创建新对象</li>
 *   <li>{@link SecureRandom} 使用静态单例复用，减少随机数生成器初始化开销</li>
 *   <li>所有方法均为纯函数，无共享可变状态，天然线程安全</li>
 * </ul>
 *
 * <h3>模式对比：</h3>
 * <table>
 *   <tr><th>模式</th><th>安全性</th><th>性能</th><th>推荐场景</th></tr>
 *   <tr><td>AES/GCM/NoPadding</td><td>认证加密（防篡改+防重放）</td><td>较高</td><td>API 数据加密、Token、配置加密</td></tr>
 *   <tr><td>AES/CBC/PKCS5Padding</td><td>仅加密（需额外做 HMAC 防篡改）</td><td>高</td><td>兼容旧系统、大文件加密</td></tr>
 * </table>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // AES-GCM（推荐）
 * SecretKey key = AesUtil.generateKey(256);
 * String ciphertext = AesUtil.encryptGcm("hello world", key);
 * String plaintext = AesUtil.decryptGcm(ciphertext, key);
 *
 * // AES-CBC
 * String ciphertext = AesUtil.encryptCbc("hello world", key);
 * String plaintext = AesUtil.decryptCbc(ciphertext, key);
 *
 * // 自定义密钥（从 hex 字符串构建）
 * SecretKey key2 = AesUtil.keyFromHex("603deb1015ca71be2b73aef0857d7781");
 * }</pre>
 *
 * @author zerx
 */
public final class AesUtil {

    // ======================== 算法常量 ========================

    /** AES 算法标识 */
    public static final String ALGORITHM = "AES";

    /** AES/GCM/NoPadding 变换（推荐） */
    public static final String TRANSFORMATION_GCM = "AES/GCM/NoPadding";

    /** AES/CBC/PKCS5Padding 变换 */
    public static final String TRANSFORMATION_CBC = "AES/CBC/PKCS5Padding";

    /** GCM 认证标签长度（128 位） */
    public static final int GCM_TAG_LENGTH_BITS = 128;

    /** GCM IV 长度（12 字节 / 96 位，推荐值） */
    public static final int GCM_IV_LENGTH = 12;

    /** CBC IV 长度（16 字节 / 128 位，等于 AES 块大小） */
    public static final int CBC_IV_LENGTH = 16;

    /** 默认密钥长度（256 位） */
    public static final int DEFAULT_KEY_SIZE = 256;

    // ======================== 静态资源 ========================

    /** 静态 SecureRandom 单例，线程安全，避免重复初始化 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** ThreadLocal Cipher：GCM 加密（Cipher 不是线程安全的，必须按线程隔离） */
    private static final ThreadLocal<Cipher> CIPHER_GCM_ENCRYPT =
            ThreadLocal.withInitial(() -> createCipher(TRANSFORMATION_GCM));

    /** ThreadLocal Cipher：GCM 解密 */
    private static final ThreadLocal<Cipher> CIPHER_GCM_DECRYPT =
            ThreadLocal.withInitial(() -> createCipher(TRANSFORMATION_GCM));

    /** ThreadLocal Cipher：CBC 加密 */
    private static final ThreadLocal<Cipher> CIPHER_CBC_ENCRYPT =
            ThreadLocal.withInitial(() -> createCipher(TRANSFORMATION_CBC));

    /** ThreadLocal Cipher：CBC 解密 */
    private static final ThreadLocal<Cipher> CIPHER_CBC_DECRYPT =
            ThreadLocal.withInitial(() -> createCipher(TRANSFORMATION_CBC));

    private AesUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 密钥管理 ========================

    /**
     * 生成 AES 密钥
     *
     * @param keySize 密钥长度（128、192 或 256 位）
     * @return AES 密钥
     * @throws IllegalArgumentException 如果密钥长度不合法
     */
    public static SecretKey generateKey(int keySize) {
        validateKeySize(keySize);
        try {
            KeyGenerator kg = KeyGenerator.getInstance(ALGORITHM);
            kg.init(keySize, SECURE_RANDOM);
            return kg.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("JDK 不支持 AES 算法", e);
        }
    }

    /**
     * 生成 256 位 AES 密钥（默认）
     *
     * @return AES-256 密钥
     */
    public static SecretKey generateKey() {
        return generateKey(DEFAULT_KEY_SIZE);
    }

    /**
     * 从字节数组构建 AES 密钥
     *
     * @param keyBytes 密钥字节（16/24/32 字节对应 128/192/256 位）
     * @return AES 密钥
     * @throws IllegalArgumentException 如果密钥长度不合法或为 null
     */
    public static SecretKey keyFromBytes(byte[] keyBytes) {
        Objects.requireNonNull(keyBytes, "密钥字节不能为 null");
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES 密钥长度必须为 16、24 或 32 字节，当前: " + keyBytes.length);
        }
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * 从 HEX 字符串构建 AES 密钥
     *
     * @param hexKey HEX 编码的密钥字符串
     * @return AES 密钥
     */
    public static SecretKey keyFromHex(String hexKey) {
        Objects.requireNonNull(hexKey, "HEX 密钥不能为 null");
        byte[] keyBytes = HexFormat.of().parseHex(hexKey);
        return keyFromBytes(keyBytes);
    }

    /**
     * 将密钥转为 HEX 字符串
     *
     * @param key AES 密钥
     * @return HEX 编码的密钥字符串
     */
    public static String keyToHex(SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        return HexFormat.of().formatHex(key.getEncoded());
    }

    // ======================== AES-GCM（推荐） ========================

    /**
     * AES-GCM 加密（HEX 编码输出）
     * <p>
     * 自动生成随机 12 字节 IV，密文格式为：IV（12字节） + 密文 + GCM Tag（16字节），
     * 全部 HEX 编码输出。
     * </p>
     *
     * @param plaintext 明文
     * @param key       AES 密钥
     * @return HEX 编码的密文（包含 IV），null 明文返回 null
     */
    public static String encryptGcm(String plaintext, SecretKey key) {
        if (plaintext == null) {
            return null;
        }
        return encryptGcm(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8), key);
    }

    /**
     * AES-GCM 加密字节数组（HEX 编码输出）
     *
     * @param plaintext 明文字节
     * @param key       AES 密钥
     * @return HEX 编码的密文（包含 IV）
     */
    public static String encryptGcm(byte[] plaintext, SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        if (plaintext == null) {
            return null;
        }
        byte[] ciphertext = encryptGcmBytes(plaintext, key);
        return HexFormat.of().formatHex(ciphertext);
    }

    /**
     * AES-GCM 加密字节数组（原始字节输出）
     * <p>
     * 输出格式：IV（12字节） + 密文 + GCM Tag（16字节）
     * </p>
     *
     * @param plaintext 明文字节
     * @param key       AES 密钥
     * @return 密文字节（含 IV），null 明文返回 null
     */
    public static byte[] encryptGcmBytes(byte[] plaintext, SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = generateRandomBytes(GCM_IV_LENGTH);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = CIPHER_GCM_ENCRYPT.get();
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(plaintext);

            // 拼接: IV + 密文(含 Tag)
            return ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    /**
     * AES-GCM 解密（HEX 编码输入）
     *
     * @param ciphertextHex HEX 编码的密文（包含 IV）
     * @param key           AES 密钥
     * @return 明文字符串，null 密文返回 null
     */
    public static String decryptGcm(String ciphertextHex, SecretKey key) {
        if (ciphertextHex == null) {
            return null;
        }
        byte[] plaintext = decryptGcmBytes(HexFormat.of().parseHex(ciphertextHex), key);
        return plaintext != null ? new String(plaintext, java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    /**
     * AES-GCM 解密字节数组（原始字节输入/输出）
     * <p>
     * 输入格式：IV（12字节） + 密文 + GCM Tag（16字节）
     * </p>
     *
     * @param ciphertext 密文字节（含 IV）
     * @param key        AES 密钥
     * @return 明文字节，null 密文返回 null
     */
    public static byte[] decryptGcmBytes(byte[] ciphertext, SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        if (ciphertext == null) {
            return null;
        }
        if (ciphertext.length < GCM_IV_LENGTH + GCM_TAG_LENGTH_BITS / 8) {
            throw new IllegalArgumentException(
                    "密文长度不足，至少需要 " + (GCM_IV_LENGTH + GCM_TAG_LENGTH_BITS / 8) + " 字节，当前: " + ciphertext.length);
        }
        try {
            // 提取 IV
            ByteBuffer bb = ByteBuffer.wrap(ciphertext);
            byte[] iv = new byte[GCM_IV_LENGTH];
            bb.get(iv);

            // 提取密文 + Tag
            byte[] encrypted = new byte[ciphertext.length - GCM_IV_LENGTH];
            bb.get(encrypted);

            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            Cipher cipher = CIPHER_GCM_DECRYPT.get();
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 解密失败（密文可能被篡改或密钥错误）", e);
        }
    }

    // ======================== AES-CBC ========================

    /**
     * AES-CBC 加密（HEX 编码输出）
     * <p>
     * 自动生成随机 16 字节 IV，密文格式为：IV（16字节） + 密文，全部 HEX 编码输出。
     * </p>
     *
     * @param plaintext 明文
     * @param key       AES 密钥
     * @return HEX 编码的密文（包含 IV），null 明文返回 null
     */
    public static String encryptCbc(String plaintext, SecretKey key) {
        if (plaintext == null) {
            return null;
        }
        return encryptCbc(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8), key);
    }

    /**
     * AES-CBC 加密字节数组（HEX 编码输出）
     *
     * @param plaintext 明文字节
     * @param key       AES 密钥
     * @return HEX 编码的密文（包含 IV）
     */
    public static String encryptCbc(byte[] plaintext, SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        if (plaintext == null) {
            return null;
        }
        byte[] ciphertext = encryptCbcBytes(plaintext, key);
        return HexFormat.of().formatHex(ciphertext);
    }

    /**
     * AES-CBC 加密字节数组（原始字节输出）
     * <p>
     * 输出格式：IV（16字节） + 密文
     * </p>
     *
     * @param plaintext 明文字节
     * @param key       AES 密钥
     * @return 密文字节（含 IV），null 明文返回 null
     */
    public static byte[] encryptCbcBytes(byte[] plaintext, SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = generateRandomBytes(CBC_IV_LENGTH);
            IvParameterSpec spec = new IvParameterSpec(iv);

            Cipher cipher = CIPHER_CBC_ENCRYPT.get();
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
        } catch (Exception e) {
            throw new IllegalStateException("AES-CBC 加密失败", e);
        }
    }

    /**
     * AES-CBC 解密（HEX 编码输入）
     *
     * @param ciphertextHex HEX 编码的密文（包含 IV）
     * @param key           AES 密钥
     * @return 明文字符串，null 密文返回 null
     */
    public static String decryptCbc(String ciphertextHex, SecretKey key) {
        if (ciphertextHex == null) {
            return null;
        }
        byte[] plaintext = decryptCbcBytes(HexFormat.of().parseHex(ciphertextHex), key);
        return plaintext != null ? new String(plaintext, java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    /**
     * AES-CBC 解密字节数组（原始字节输入/输出）
     * <p>
     * 输入格式：IV（16字节） + 密文
     * </p>
     *
     * @param ciphertext 密文字节（含 IV）
     * @param key        AES 密钥
     * @return 明文字节，null 密文返回 null
     */
    public static byte[] decryptCbcBytes(byte[] ciphertext, SecretKey key) {
        Objects.requireNonNull(key, "密钥不能为 null");
        if (ciphertext == null) {
            return null;
        }
        if (ciphertext.length < CBC_IV_LENGTH) {
            throw new IllegalArgumentException(
                    "密文长度不足，至少需要 " + CBC_IV_LENGTH + " 字节，当前: " + ciphertext.length);
        }
        try {
            ByteBuffer bb = ByteBuffer.wrap(ciphertext);
            byte[] iv = new byte[CBC_IV_LENGTH];
            bb.get(iv);

            byte[] encrypted = new byte[ciphertext.length - CBC_IV_LENGTH];
            bb.get(encrypted);

            IvParameterSpec spec = new IvParameterSpec(iv);

            Cipher cipher = CIPHER_CBC_DECRYPT.get();
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("AES-CBC 解密失败（密钥错误或数据损坏）", e);
        }
    }

    // ======================== 内部方法 ========================

    private static void validateKeySize(int keySize) {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new IllegalArgumentException(
                    "AES 密钥长度必须为 128、192 或 256 位，当前: " + keySize);
        }
    }

    private static Cipher createCipher(String transformation) {
        try {
            return Cipher.getInstance(transformation);
        } catch (Exception e) {
            throw new UnsupportedOperationException("JDK 不支持的加密变换: " + transformation, e);
        }
    }

    private static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
}
