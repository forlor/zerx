package com.zerx.common.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HmacUtil 单元测试
 */
@DisplayName("HmacUtil")
class HmacUtilTest {

    private static final String SECRET = "my-secret-key-2026";

    @Nested
    @DisplayName("HMAC-SHA256")
    class Sha256Test {

        @Test
        @DisplayName("字符串签名 - 固定输入应产生固定输出")
        void fixedInput() {
            String sig = HmacUtil.hmacSha256Hex("hello", SECRET);
            assertNotNull(sig);
            assertEquals(64, sig.length(), "SHA-256 签名 HEX 长度应为 64");
            // 验证可重复
            String sig2 = HmacUtil.hmacSha256Hex("hello", SECRET);
            assertEquals(sig, sig2, "相同输入应产生相同签名");
        }

        @Test
        @DisplayName("字符串签名 - 不同输入应产生不同签名")
        void differentInput() {
            String sig1 = HmacUtil.hmacSha256Hex("hello", SECRET);
            String sig2 = HmacUtil.hmacSha256Hex("world", SECRET);
            assertNotEquals(sig1, sig2);
        }

        @Test
        @DisplayName("字符串签名 - 不同密钥应产生不同签名")
        void differentSecret() {
            String sig1 = HmacUtil.hmacSha256Hex("hello", "secret1");
            String sig2 = HmacUtil.hmacSha256Hex("hello", "secret2");
            assertNotEquals(sig1, sig2);
        }

        @Test
        @DisplayName("字节数组签名")
        void bytesInput() {
            byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
            byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);
            String sig = HmacUtil.hmacSha256Hex(data, secret);
            assertNotNull(sig);
            assertEquals(64, sig.length());
        }

        @Test
        @DisplayName("原始字节数组输出")
        void rawBytesOutput() {
            byte[] data = "test".getBytes(StandardCharsets.UTF_8);
            byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);
            byte[] hash = HmacUtil.hmacSha256(data, secret);
            assertNotNull(hash);
            assertEquals(32, hash.length, "SHA-256 HMAC 输出应为 32 字节");
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void nullInput() {
            assertNull(HmacUtil.hmacSha256Hex((String) null, SECRET));
            assertNull(HmacUtil.hmacSha256Hex("data", (String) null));
            assertNull(HmacUtil.hmacSha256Hex((String) null, (String) null));
            assertNull(HmacUtil.hmacSha256((byte[]) null, new byte[1]));
            assertNull(HmacUtil.hmacSha256(new byte[1], (byte[]) null));
        }
    }

    @Nested
    @DisplayName("HMAC-SHA512")
    class Sha512Test {

        @Test
        @DisplayName("字符串签名 - 固定输入应产生固定输出")
        void fixedInput() {
            String sig = HmacUtil.hmacSha512Hex("hello", SECRET);
            assertNotNull(sig);
            assertEquals(128, sig.length(), "SHA-512 签名 HEX 长度应为 128");
        }

        @Test
        @DisplayName("不同输入应产生不同签名")
        void differentInput() {
            String sig1 = HmacUtil.hmacSha512Hex("hello", SECRET);
            String sig2 = HmacUtil.hmacSha512Hex("world", SECRET);
            assertNotEquals(sig1, sig2);
        }

        @Test
        @DisplayName("原始字节数组输出")
        void rawBytesOutput() {
            byte[] hash = HmacUtil.hmacSha512(
                    "test".getBytes(StandardCharsets.UTF_8),
                    SECRET.getBytes(StandardCharsets.UTF_8)
            );
            assertNotNull(hash);
            assertEquals(64, hash.length, "SHA-512 HMAC 输出应为 64 字节");
        }

        @Test
        @DisplayName("null 输入应返回 null")
        void nullInput() {
            assertNull(HmacUtil.hmacSha512Hex(null, SECRET));
            assertNull(HmacUtil.hmacSha512Hex("data", null));
        }
    }

    @Nested
    @DisplayName("签名验证")
    class VerifyTest {

        @Test
        @DisplayName("SHA256 验证 - 正确签名应返回 true")
        void verifySha256Correct() {
            String signature = HmacUtil.hmacSha256Hex("hello", SECRET);
            assertTrue(HmacUtil.verifySha256("hello", signature, SECRET));
        }

        @Test
        @DisplayName("SHA256 验证 - 篡改数据应返回 false")
        void verifySha256Tampered() {
            String signature = HmacUtil.hmacSha256Hex("hello", SECRET);
            assertFalse(HmacUtil.verifySha256("world", signature, SECRET));
        }

        @Test
        @DisplayName("SHA256 验证 - 篡改签名应返回 false")
        void verifySha256WrongSignature() {
            String signature = HmacUtil.hmacSha256Hex("hello", SECRET);
            // 在字节层面篡改签名（避免产生非 HEX 字符）
            byte[] sigBytes = java.util.HexFormat.of().parseHex(signature);
            sigBytes[0] ^= (byte) 0xFF;
            String tampered = java.util.HexFormat.of().formatHex(sigBytes);
            assertFalse(HmacUtil.verifySha256("hello", tampered, SECRET));
        }

        @Test
        @DisplayName("SHA256 验证 - null 参数应返回 false")
        void verifySha256Null() {
            String sig = HmacUtil.hmacSha256Hex("hello", SECRET);
            assertFalse(HmacUtil.verifySha256(null, sig, SECRET));
            assertFalse(HmacUtil.verifySha256("hello", null, SECRET));
            assertFalse(HmacUtil.verifySha256("hello", sig, null));
        }

        @Test
        @DisplayName("SHA256 字节数组验证")
        void verifySha256Bytes() {
            byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
            byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);
            byte[] expected = HmacUtil.hmacSha256(data, secret);
            assertTrue(HmacUtil.verifySha256(data, expected, secret));
        }

        @Test
        @DisplayName("SHA512 验证 - 正确签名应返回 true")
        void verifySha512Correct() {
            String signature = HmacUtil.hmacSha512Hex("hello", SECRET);
            assertTrue(HmacUtil.verifySha512("hello", signature, SECRET));
        }

        @Test
        @DisplayName("SHA512 验证 - 篡改数据应返回 false")
        void verifySha512Tampered() {
            String signature = HmacUtil.hmacSha512Hex("hello", SECRET);
            assertFalse(HmacUtil.verifySha512("world", signature, SECRET));
        }

        @Test
        @DisplayName("SHA512 null 参数应返回 false")
        void verifySha512Null() {
            String sig = HmacUtil.hmacSha512Hex("hello", SECRET);
            assertFalse(HmacUtil.verifySha512(null, sig, SECRET));
            assertFalse(HmacUtil.verifySha512("hello", null, SECRET));
            assertFalse(HmacUtil.verifySha512("hello", sig, null));
        }
    }

    @Nested
    @DisplayName("流式 API")
    class StreamingTest {

        @Test
        @DisplayName("SHA256 流式分块签名应与一次性签名结果一致")
        void streamingSha256() {
            String data = "This is a long message that we split into chunks";
            byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);

            // 一次性签名
            String expected = HmacUtil.hmacSha256Hex(data, SECRET);

            // 分块签名
            Mac mac = HmacUtil.createSha256Mac(SECRET);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            int chunkSize = 10;
            for (int i = 0; i < dataBytes.length; i += chunkSize) {
                int end = Math.min(i + chunkSize, dataBytes.length);
                mac.update(dataBytes, i, end - i);
            }
            String actual = HmacUtil.doFinalHex(mac);

            assertEquals(expected, actual, "分块签名应与一次性签名结果一致");
        }

        @Test
        @DisplayName("SHA512 流式分块签名")
        void streamingSha512() {
            String data = "Streaming HMAC-SHA512 test message";
            String expected = HmacUtil.hmacSha512Hex(data, SECRET);

            Mac mac = HmacUtil.createSha512Mac(SECRET);
            mac.update(data.getBytes(StandardCharsets.UTF_8));
            String actual = HmacUtil.doFinalHex(mac);

            assertEquals(expected, actual);
        }

        @Test
        @DisplayName("doFinalHex 应自动 reset Mac")
        void autoReset() {
            Mac mac = HmacUtil.createSha256Mac(SECRET);
            mac.update("first".getBytes(StandardCharsets.UTF_8));
            String sig1 = HmacUtil.doFinalHex(mac);

            // reset 后重新使用同一 Mac 实例
            mac.update("second".getBytes(StandardCharsets.UTF_8));
            String sig2 = HmacUtil.doFinalHex(mac);

            String expected1 = HmacUtil.hmacSha256Hex("first", SECRET);
            String expected2 = HmacUtil.hmacSha256Hex("second", SECRET);
            assertEquals(expected1, sig1);
            assertEquals(expected2, sig2);
        }

        @Test
        @DisplayName("doFinalBytes 返回原始字节")
        void doFinalBytes() {
            Mac mac = HmacUtil.createSha256Mac(SECRET);
            mac.update("test".getBytes(StandardCharsets.UTF_8));
            byte[] hash = HmacUtil.doFinalBytes(mac);
            assertEquals(32, hash.length);
        }

        @Test
        @DisplayName("createSha256Mac - 字节数组密钥")
        void createWithBytesSecret() {
            byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);
            Mac mac = HmacUtil.createSha256Mac(secret);
            mac.update("test".getBytes(StandardCharsets.UTF_8));
            String sig = HmacUtil.doFinalHex(mac);

            String expected = HmacUtil.hmacSha256Hex("test", SECRET);
            assertEquals(expected, sig);
        }

        @Test
        @DisplayName("createSha512Mac - 字节数组密钥")
        void createSha512WithBytes() {
            byte[] secret = SECRET.getBytes(StandardCharsets.UTF_8);
            Mac mac = HmacUtil.createSha512Mac(secret);
            mac.update("test".getBytes(StandardCharsets.UTF_8));
            String sig = HmacUtil.doFinalHex(mac);

            String expected = HmacUtil.hmacSha512Hex("test", SECRET);
            assertEquals(expected, sig);
        }

        @Test
        @DisplayName("doFinalHex - null Mac 应抛异常")
        void nullMac() {
            assertThrows(NullPointerException.class, () -> HmacUtil.doFinalHex(null));
            assertThrows(NullPointerException.class, () -> HmacUtil.doFinalBytes(null));
        }
    }

    @Nested
    @DisplayName("并发安全")
    class ConcurrencyTest {

        @Test
        @DisplayName("多线程同时 SHA256 签名不产生异常")
        void concurrentSha256() throws Exception {
            int threadCount = 10;
            int iterations = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        String data = "thread-" + idx + "-" + j;
                        String sig = HmacUtil.hmacSha256Hex(data, SECRET);
                        assertTrue(HmacUtil.verifySha256(data, sig, SECRET));
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
        @DisplayName("多线程同时 SHA512 签名不产生异常")
        void concurrentSha512() throws Exception {
            int threadCount = 10;
            int iterations = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        String data = "sha512-" + idx + "-" + j;
                        String sig = HmacUtil.hmacSha512Hex(data, SECRET);
                        assertTrue(HmacUtil.verifySha512(data, sig, SECRET));
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
        @DisplayName("多线程流式签名不产生异常")
        void concurrentStreaming() throws Exception {
            int threadCount = 10;
            int iterations = 50;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        String data = "stream-" + idx + "-" + j;
                        Mac mac = HmacUtil.createSha256Mac(SECRET);
                        mac.update(data.getBytes(StandardCharsets.UTF_8));
                        String sig = HmacUtil.doFinalHex(mac);
                        String expected = HmacUtil.hmacSha256Hex(data, SECRET);
                        assertEquals(expected, sig);
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
