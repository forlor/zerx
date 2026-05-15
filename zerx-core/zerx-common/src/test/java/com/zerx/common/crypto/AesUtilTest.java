package com.zerx.common.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AesUtil 单元测试
 */
@DisplayName("AesUtil")
class AesUtilTest {

    @Nested
    @DisplayName("密钥管理")
    class KeyManagementTest {

        @Test
        @DisplayName("generateKey - 生成 256 位密钥")
        void generateKey256() {
            SecretKey key = AesUtil.generateKey(256);
            assertNotNull(key);
            assertEquals("AES", key.getAlgorithm());
            assertEquals(32, key.getEncoded().length);
        }

        @Test
        @DisplayName("generateKey - 生成 128 位密钥")
        void generateKey128() {
            SecretKey key = AesUtil.generateKey(128);
            assertEquals(16, key.getEncoded().length);
        }

        @Test
        @DisplayName("generateKey - 生成 192 位密钥")
        void generateKey192() {
            SecretKey key = AesUtil.generateKey(192);
            assertEquals(24, key.getEncoded().length);
        }

        @Test
        @DisplayName("generateKey - 默认 256 位")
        void generateKeyDefault() {
            SecretKey key = AesUtil.generateKey();
            assertEquals(32, key.getEncoded().length);
        }

        @Test
        @DisplayName("generateKey - 非法长度应抛异常")
        void invalidKeySize() {
            assertThrows(IllegalArgumentException.class, () -> AesUtil.generateKey(64));
            assertThrows(IllegalArgumentException.class, () -> AesUtil.generateKey(512));
            assertThrows(IllegalArgumentException.class, () -> AesUtil.generateKey(0));
            assertThrows(IllegalArgumentException.class, () -> AesUtil.generateKey(-1));
        }

        @Test
        @DisplayName("generateKey - 每次生成的密钥应不同")
        void keyUniqueness() {
            SecretKey key1 = AesUtil.generateKey();
            SecretKey key2 = AesUtil.generateKey();
            assertNotEquals(HexFormat.of().formatHex(key1.getEncoded()),
                    HexFormat.of().formatHex(key2.getEncoded()));
        }

        @Test
        @DisplayName("keyFromBytes - 16 字节")
        void keyFromBytes16() {
            byte[] bytes = new byte[16];
            new java.security.SecureRandom().nextBytes(bytes);
            SecretKey key = AesUtil.keyFromBytes(bytes);
            assertEquals("AES", key.getAlgorithm());
            assertArrayEquals(bytes, key.getEncoded());
        }

        @Test
        @DisplayName("keyFromBytes - 24 字节")
        void keyFromBytes24() {
            byte[] bytes = new byte[24];
            new java.security.SecureRandom().nextBytes(bytes);
            SecretKey key = AesUtil.keyFromBytes(bytes);
            assertEquals(24, key.getEncoded().length);
        }

        @Test
        @DisplayName("keyFromBytes - 32 字节")
        void keyFromBytes32() {
            byte[] bytes = new byte[32];
            new java.security.SecureRandom().nextBytes(bytes);
            SecretKey key = AesUtil.keyFromBytes(bytes);
            assertEquals(32, key.getEncoded().length);
        }

        @Test
        @DisplayName("keyFromBytes - 非法长度应抛异常")
        void keyFromBytesInvalid() {
            assertThrows(IllegalArgumentException.class, () -> AesUtil.keyFromBytes(new byte[8]));
            assertThrows(IllegalArgumentException.class, () -> AesUtil.keyFromBytes(new byte[20]));
            assertThrows(NullPointerException.class, () -> AesUtil.keyFromBytes(null));
        }

        @Test
        @DisplayName("keyFromHex / keyToHex 往返转换")
        void hexRoundTrip() {
            SecretKey key = AesUtil.generateKey(256);
            String hex = AesUtil.keyToHex(key);
            assertEquals(64, hex.length(), "256 位密钥 HEX 长度应为 64");

            SecretKey restored = AesUtil.keyFromHex(hex);
            assertArrayEquals(key.getEncoded(), restored.getEncoded());
        }

        @Test
        @DisplayName("keyFromHex - null 应抛异常")
        void keyFromHexNull() {
            assertThrows(NullPointerException.class, () -> AesUtil.keyFromHex(null));
        }
    }

    @Nested
    @DisplayName("AES-GCM 加解密")
    class GcmTest {

        @Test
        @DisplayName("字符串加解密 - 256 位密钥")
        void encryptDecryptString256() {
            SecretKey key = AesUtil.generateKey(256);
            String plaintext = "Hello, Zerx Framework! 你好世界";
            String ciphertext = AesUtil.encryptGcm(plaintext, key);
            assertNotNull(ciphertext);
            assertNotEquals(plaintext, ciphertext);

            String decrypted = AesUtil.decryptGcm(ciphertext, key);
            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("字符串加解密 - 128 位密钥")
        void encryptDecryptString128() {
            SecretKey key = AesUtil.generateKey(128);
            String plaintext = "AES-128-GCM test data";
            String ciphertext = AesUtil.encryptGcm(plaintext, key);
            String decrypted = AesUtil.decryptGcm(ciphertext, key);
            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("字节数组加解密")
        void encryptDecryptBytes() {
            SecretKey key = AesUtil.generateKey();
            byte[] plaintext = "binary data \0\1\2\3".getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = AesUtil.encryptGcmBytes(plaintext, key);
            assertNotNull(ciphertext);

            byte[] decrypted = AesUtil.decryptGcmBytes(ciphertext, key);
            assertArrayEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("同一明文多次加密结果不同（随机 IV）")
        void randomIv() {
            SecretKey key = AesUtil.generateKey();
            String plaintext = "same plain text";

            String ct1 = AesUtil.encryptGcm(plaintext, key);
            String ct2 = AesUtil.encryptGcm(plaintext, key);
            assertNotEquals(ct1, ct2, "相同明文 + 相同密钥，密文应因随机 IV 而不同");
        }

        @Test
        @DisplayName("HEX 加解密完整性")
        void hexOutputFormat() {
            SecretKey key = AesUtil.generateKey();
            String ciphertext = AesUtil.encryptGcm("test", key);
            // 验证 HEX 格式（不含非法字符）
            assertDoesNotThrow(() -> HexFormat.of().parseHex(ciphertext));
            // GCM 密文长度 = IV(12) + 密文(至少 16 = Tag) = 至少 28 字节 = 56 HEX 字符
            assertTrue(ciphertext.length() >= 56, "GCM 密文 HEX 长度至少 56");
        }

        @Test
        @DisplayName("密文被篡改应解密失败")
        void tamperedCiphertext() {
            SecretKey key = AesUtil.generateKey();
            String plaintext = "sensitive data";
            String ciphertext = AesUtil.encryptGcm(plaintext, key);

            // 在字节层面篡改密文（避免产生非 HEX 字符）
            byte[] ctBytes = java.util.HexFormat.of().parseHex(ciphertext);
            ctBytes[ctBytes.length - 1] ^= (byte) 0xFF;
            String tampered = java.util.HexFormat.of().formatHex(ctBytes);

            assertThrows(IllegalStateException.class,
                    () -> AesUtil.decryptGcm(tampered, key),
                    "篡改后的密文应导致解密失败");
        }

        @Test
        @DisplayName("错误密钥应解密失败")
        void wrongKey() {
            SecretKey key1 = AesUtil.generateKey();
            SecretKey key2 = AesUtil.generateKey();
            String ciphertext = AesUtil.encryptGcm("secret", key1);

            assertThrows(IllegalStateException.class,
                    () -> AesUtil.decryptGcm(ciphertext, key2));
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void nullInput() {
            SecretKey key = AesUtil.generateKey();
            assertNull(AesUtil.encryptGcm((String) null, key));
            assertNull(AesUtil.encryptGcm((byte[]) null, key));
            assertNull(AesUtil.decryptGcm((String) null, key));
            assertNull(AesUtil.decryptGcmBytes((byte[]) null, key));
        }

        @Test
        @DisplayName("空字符串加解密")
        void emptyString() {
            SecretKey key = AesUtil.generateKey();
            String ciphertext = AesUtil.encryptGcm("", key);
            assertNotNull(ciphertext);
            assertEquals("", AesUtil.decryptGcm(ciphertext, key));
        }

        @Test
        @DisplayName("密文长度不足应抛异常")
        void shortCiphertext() {
            SecretKey key = AesUtil.generateKey();
            byte[] shortCipher = new byte[10]; // 远小于 IV + Tag
            assertThrows(IllegalArgumentException.class,
                    () -> AesUtil.decryptGcmBytes(shortCipher, key));
        }

        @Test
        @DisplayName("大文本加解密")
        void largeData() {
            SecretKey key = AesUtil.generateKey();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                sb.append("abcdefgh");
            }
            String plaintext = sb.toString();
            String ciphertext = AesUtil.encryptGcm(plaintext, key);
            assertEquals(plaintext, AesUtil.decryptGcm(ciphertext, key));
        }
    }

    @Nested
    @DisplayName("AES-CBC 加解密")
    class CbcTest {

        @Test
        @DisplayName("字符串加解密 - 256 位密钥")
        void encryptDecryptString() {
            SecretKey key = AesUtil.generateKey(256);
            String plaintext = "Hello, AES-CBC! 你好世界";
            String ciphertext = AesUtil.encryptCbc(plaintext, key);
            assertNotNull(ciphertext);
            assertNotEquals(plaintext, ciphertext);

            String decrypted = AesUtil.decryptCbc(ciphertext, key);
            assertEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("字节数组加解密")
        void encryptDecryptBytes() {
            SecretKey key = AesUtil.generateKey();
            byte[] plaintext = "binary CBC data \0\1\2".getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = AesUtil.encryptCbcBytes(plaintext, key);
            byte[] decrypted = AesUtil.decryptCbcBytes(ciphertext, key);
            assertArrayEquals(plaintext, decrypted);
        }

        @Test
        @DisplayName("同一明文多次加密结果不同（随机 IV）")
        void randomIv() {
            SecretKey key = AesUtil.generateKey();
            String ct1 = AesUtil.encryptCbc("same", key);
            String ct2 = AesUtil.encryptCbc("same", key);
            assertNotEquals(ct1, ct2);
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void nullInput() {
            SecretKey key = AesUtil.generateKey();
            assertNull(AesUtil.encryptCbc((String) null, key));
            assertNull(AesUtil.encryptCbc((byte[]) null, key));
            assertNull(AesUtil.decryptCbc((String) null, key));
            assertNull(AesUtil.decryptCbcBytes((byte[]) null, key));
        }

        @Test
        @DisplayName("空字符串加解密")
        void emptyString() {
            SecretKey key = AesUtil.generateKey();
            String ciphertext = AesUtil.encryptCbc("", key);
            assertEquals("", AesUtil.decryptCbc(ciphertext, key));
        }

        @Test
        @DisplayName("密文长度不足应抛异常")
        void shortCiphertext() {
            SecretKey key = AesUtil.generateKey();
            byte[] shortCipher = new byte[8]; // 小于 CBC IV(16)
            assertThrows(IllegalArgumentException.class,
                    () -> AesUtil.decryptCbcBytes(shortCipher, key));
        }

        @Test
        @DisplayName("key 为 null 应抛异常")
        void nullKey() {
            assertThrows(NullPointerException.class,
                    () -> AesUtil.encryptGcm("test", null));
            assertThrows(NullPointerException.class,
                    () -> AesUtil.encryptCbc("test", null));
            assertThrows(NullPointerException.class,
                    () -> AesUtil.encryptGcmBytes(new byte[1], null));
            assertThrows(NullPointerException.class,
                    () -> AesUtil.encryptCbcBytes(new byte[1], null));
        }
    }

    @Nested
    @DisplayName("并发安全")
    class ConcurrencyTest {

        @Test
        @DisplayName("多线程同时 GCM 加解密不产生异常")
        void concurrentGcm() throws Exception {
            SecretKey key = AesUtil.generateKey();
            int threadCount = 10;
            int iterations = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        String plain = "thread-" + idx + "-iter-" + j;
                        String ct = AesUtil.encryptGcm(plain, key);
                        String dt = AesUtil.decryptGcm(ct, key);
                        assertEquals(plain, dt);
                    }
                });
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        }

        @Test
        @DisplayName("多线程同时 CBC 加解密不产生异常")
        void concurrentCbc() throws Exception {
            SecretKey key = AesUtil.generateKey();
            int threadCount = 10;
            int iterations = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        String plain = "cbc-thread-" + idx + "-" + j;
                        String ct = AesUtil.encryptCbc(plain, key);
                        String dt = AesUtil.decryptCbc(ct, key);
                        assertEquals(plain, dt);
                    }
                });
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        }

        @Test
        @DisplayName("多线程同时生成密钥不产生异常")
        void concurrentKeyGen() throws Exception {
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < 50; j++) {
                        SecretKey key = AesUtil.generateKey();
                        assertNotNull(key);
                        assertEquals(32, key.getEncoded().length);
                    }
                });
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
        }
    }
}
