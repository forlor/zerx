package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Base64Util} 单元测试
 */
@DisplayName("Base64Util - Base64 编解码工具类测试")
class Base64UtilTest {

    // ======================== 标准编码测试 ========================

    @Test
    @DisplayName("encode(String) - 普通字符串编码")
    void encodeString_normal() {
        assertEquals("SGVsbG8gV29ybGQ=", Base64Util.encode("Hello World"));
    }

    @Test
    @DisplayName("encode(String) - 空字符串编码")
    void encodeString_empty() {
        assertEquals("", Base64Util.encode(""));
    }

    @Test
    @DisplayName("encode(String) - null 输入返回 null")
    void encodeString_null() {
        assertNull(Base64Util.encode((String) null));
    }

    @Test
    @DisplayName("encode(String) - 中文编码")
    void encodeString_chinese() {
        String encoded = Base64Util.encode("你好世界");
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        // Round-trip
        assertEquals("你好世界", Base64Util.decode(encoded));
    }

    @Test
    @DisplayName("encode(String) - 特殊字符编码")
    void encodeString_special() {
        String original = "!@#$%^&*()_+-=[]{}|;':\",./<>?";
        // Round-trip verification instead of hardcoded expected value
        String encoded = Base64Util.encode(original);
        assertEquals(original, Base64Util.decode(encoded));
        assertFalse(encoded.isEmpty());
    }

    @Test
    @DisplayName("encode(String, Charset) - 指定 UTF-8 编码")
    void encodeStringWithCharset_utf8() {
        assertEquals("SGVsbG8=", Base64Util.encode("Hello", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("encode(String, Charset) - 指定 ISO-8859-1 编码")
    void encodeStringWithCharset_iso() {
        String encoded = Base64Util.encode("Hello", StandardCharsets.ISO_8859_1);
        assertEquals("SGVsbG8=", encoded);
    }

    @Test
    @DisplayName("encode(String, Charset) - null 字符串返回 null")
    void encodeStringWithCharset_nullString() {
        assertNull(Base64Util.encode(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("encode(byte[]) - 字节数组编码")
    void encodeBytes_normal() {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        assertEquals("SGVsbG8gV29ybGQ=", Base64Util.encode(data));
    }

    @Test
    @DisplayName("encode(byte[]) - 空字节数组编码")
    void encodeBytes_empty() {
        assertEquals("", Base64Util.encode(new byte[0]));
    }

    @Test
    @DisplayName("encode(byte[]) - null 字节数组返回 null")
    void encodeBytes_null() {
        assertNull(Base64Util.encode((byte[]) null));
    }

    @Test
    @DisplayName("encode(byte[]) - 二进制数据编码")
    void encodeBytes_binary() {
        byte[] binary = {(byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0xFF, (byte) 0xFE};
        String encoded = Base64Util.encode(binary);
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Test
    @DisplayName("encodeToBytes(byte[]) - 编码为 Base64 字节数组")
    void encodeToBytes_normal() {
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        byte[] result = Base64Util.encodeToBytes(data);
        assertNotNull(result);
        assertEquals("SGVsbG8=", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("encodeToBytes(byte[]) - null 输入返回 null")
    void encodeToBytes_null() {
        assertNull(Base64Util.encodeToBytes(null));
    }

    // ======================== 标准解码测试 ========================

    @Test
    @DisplayName("decode(String) - 标准解码 (UTF-8)")
    void decodeString_normal() {
        assertEquals("Hello World", Base64Util.decode("SGVsbG8gV29ybGQ="));
    }

    @Test
    @DisplayName("decode(String) - null 输入返回 null")
    void decodeString_null() {
        assertNull(Base64Util.decode((String) null));
    }

    @Test
    @DisplayName("decode(String) - 空字符串返回空")
    void decodeString_empty() {
        assertEquals("", Base64Util.decode(""));
    }

    @Test
    @DisplayName("decode(String, Charset) - 指定字符集解码")
    void decodeStringWithCharset_utf8() {
        assertEquals("Hello", Base64Util.decode("SGVsbG8=", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decode(String, Charset) - null 输入返回 null")
    void decodeStringWithCharset_null() {
        assertNull(Base64Util.decode(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decodeToBytes(String) - 解码为字节数组")
    void decodeToBytes_normal() {
        byte[] result = Base64Util.decodeToBytes("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decodeToBytes(String) - null 输入返回 null")
    void decodeToBytes_null() {
        assertNull(Base64Util.decodeToBytes(null));
    }

    @Test
    @DisplayName("decodeToBytes(String) - 无效 Base64 抛出异常")
    void decodeToBytes_invalid() {
        assertThrows(IllegalArgumentException.class, () -> Base64Util.decodeToBytes("!!!invalid!!!"));
    }

    @Test
    @DisplayName("decode(byte[]) - Base64 字节数组解码")
    void decodeBytesArray_normal() {
        byte[] base64Bytes = "SGVsbG8=".getBytes(StandardCharsets.UTF_8);
        byte[] result = Base64Util.decode(base64Bytes);
        assertEquals("Hello", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decode(byte[]) - null 输入返回 null")
    void decodeBytesArray_null() {
        assertNull(Base64Util.decode((byte[]) null));
    }

    // ======================== 往返测试 ========================

    @Test
    @DisplayName("Round-trip: encode → decode 字符串保持一致")
    void roundtrip_string() {
        String original = "The quick brown fox jumps over the lazy dog. 1234567890!@#";
        assertEquals(original, Base64Util.decode(Base64Util.encode(original)));
    }

    @Test
    @DisplayName("Round-trip: encode(bytes) → decodeToBytes → 字符串一致")
    void roundtrip_bytes() {
        String original = "Binary data test 你好 世界";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Util.encode(data);
        byte[] decoded = Base64Util.decodeToBytes(encoded);
        assertArrayEquals(data, decoded);
    }

    @Test
    @DisplayName("Round-trip: encodeToBytes → decode 一致")
    void roundtrip_encodeToBytes() {
        String original = "Test round trip bytes";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        byte[] encoded = Base64Util.encodeToBytes(data);
        byte[] decoded = Base64Util.decode(encoded);
        assertArrayEquals(data, decoded);
    }

    @Test
    @DisplayName("Round-trip: 中文编解码往返")
    void roundtrip_chinese() {
        String original = "Java Base64 编解码测试！";
        assertEquals(original, Base64Util.decode(Base64Util.encode(original)));
    }

    // ======================== URL 安全编解码测试 ========================

    @Test
    @DisplayName("encodeUrlSafe(String) - URL 安全编码无 +/ 和 = 填充")
    void encodeUrlSafeString_normal() {
        String encoded = Base64Util.encodeUrlSafe("subjects?_d");
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));
    }

    @Test
    @DisplayName("encodeUrlSafe(String) - null 返回 null")
    void encodeUrlSafeString_null() {
        assertNull(Base64Util.encodeUrlSafe((String) null));
    }

    @Test
    @DisplayName("encodeUrlSafe(String) - 空字符串编码")
    void encodeUrlSafeString_empty() {
        assertEquals("", Base64Util.encodeUrlSafe(""));
    }

    @Test
    @DisplayName("encodeUrlSafe(String, Charset) - 指定字符集 URL 安全编码")
    void encodeUrlSafeStringWithCharset() {
        String encoded = Base64Util.encodeUrlSafe("Hello", StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
    }

    @Test
    @DisplayName("encodeUrlSafe(byte[]) - 字节数组 URL 安全编码")
    void encodeUrlSafeBytes_normal() {
        byte[] data = "url?safe=data+here".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Util.encodeUrlSafe(data);
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));
    }

    @Test
    @DisplayName("encodeUrlSafe(byte[]) - null 返回 null")
    void encodeUrlSafeBytes_null() {
        assertNull(Base64Util.encodeUrlSafe((byte[]) null));
    }

    @Test
    @DisplayName("decodeUrlSafe(String) - URL 安全解码")
    void decodeUrlSafeString_normal() {
        String original = "subjects?_d";
        String encoded = Base64Util.encodeUrlSafe(original);
        assertEquals(original, Base64Util.decodeUrlSafe(encoded));
    }

    @Test
    @DisplayName("decodeUrlSafe(String) - null 返回 null")
    void decodeUrlSafeString_null() {
        assertNull(Base64Util.decodeUrlSafe((String) null));
    }

    @Test
    @DisplayName("decodeUrlSafe(String, Charset) - 指定字符集 URL 安全解码")
    void decodeUrlSafeStringWithCharset() {
        String original = "Hello World";
        String encoded = Base64Util.encodeUrlSafe(original, StandardCharsets.UTF_8);
        assertEquals(original, Base64Util.decodeUrlSafe(encoded, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decodeUrlSafeToBytes(String) - URL 安全解码为字节数组")
    void decodeUrlSafeToBytes_normal() {
        String original = "Test data";
        String encoded = Base64Util.encodeUrlSafe(original);
        byte[] decoded = Base64Util.decodeUrlSafeToBytes(encoded);
        assertEquals(original, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decodeUrlSafeToBytes(String) - null 返回 null")
    void decodeUrlSafeToBytes_null() {
        assertNull(Base64Util.decodeUrlSafeToBytes(null));
    }

    @Test
    @DisplayName("Round-trip: URL 安全编解码")
    void roundtrip_urlSafe() {
        String original = "URL-safe base64 test 你好=+/";
        String encoded = Base64Util.encodeUrlSafe(original);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("="));
        assertEquals(original, Base64Util.decodeUrlSafe(encoded));
    }

    // ======================== MIME 编解码测试 ========================

    @Test
    @DisplayName("encodeMime(byte[]) - MIME 编码带换行")
    void encodeMime_normal() {
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 256);
        }
        String encoded = Base64Util.encodeMime(data);
        assertNotNull(encoded);
        // MIME 编码每行不超过 76 个字符
        String[] lines = encoded.split("\r?\n");
        for (String line : lines) {
            assertTrue(line.length() <= 76, "MIME line should not exceed 76 chars");
        }
    }

    @Test
    @DisplayName("encodeMime(byte[]) - null 返回 null")
    void encodeMime_null() {
        assertNull(Base64Util.encodeMime(null));
    }

    @Test
    @DisplayName("encodeMime(byte[]) - 空字节数组")
    void encodeMime_empty() {
        assertEquals("", Base64Util.encodeMime(new byte[0]));
    }

    @Test
    @DisplayName("decodeMime(String) - MIME 解码")
    void decodeMime_normal() {
        String original = "This is a MIME encoded message.";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Util.encodeMime(data);
        byte[] decoded = Base64Util.decodeMime(encoded);
        assertEquals(original, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("decodeMime(String) - null 返回 null")
    void decodeMime_null() {
        assertNull(Base64Util.decodeMime(null));
    }

    @Test
    @DisplayName("Round-trip: MIME 编解码")
    void roundtrip_mime() {
        String original = "Long MIME content test with various characters: 1234567890 abcdefghijklmnopqrstuvwxyz";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        String encoded = Base64Util.encodeMime(data);
        byte[] decoded = Base64Util.decodeMime(encoded);
        assertArrayEquals(data, decoded);
    }

    // ======================== 校验测试 ========================

    @Test
    @DisplayName("isBase64(String) - 合法 Base64 返回 true")
    void isBase64_valid() {
        assertTrue(Base64Util.isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(Base64Util.isBase64("SGVsbG8="));
        assertTrue(Base64Util.isBase64("AAAA"));  // 无填充
    }

    @Test
    @DisplayName("isBase64(String) - 非法 Base64 返回 false")
    void isBase64_invalid() {
        assertFalse(Base64Util.isBase64("!!!not base64!!!"));
        assertFalse(Base64Util.isBase64("Hello World"));  // 包含空格
    }

    @Test
    @DisplayName("isBase64(String) - null 返回 false")
    void isBase64_null() {
        assertFalse(Base64Util.isBase64(null));
    }

    @Test
    @DisplayName("isBase64(String) - 空字符串返回 false")
    void isBase64_empty() {
        assertFalse(Base64Util.isBase64(""));
    }

    @Test
    @DisplayName("isUrlSafeBase64(String) - 合法 URL 安全 Base64 返回 true")
    void isUrlSafeBase64_valid() {
        String encoded = Base64Util.encodeUrlSafe("Hello World");
        assertTrue(Base64Util.isUrlSafeBase64(encoded));
    }

    @Test
    @DisplayName("isUrlSafeBase64(String) - 非法返回 false")
    void isUrlSafeBase64_invalid() {
        assertFalse(Base64Util.isUrlSafeBase64("not valid!!!"));
    }

    @Test
    @DisplayName("isUrlSafeBase64(String) - null 返回 false")
    void isUrlSafeBase64_null() {
        assertFalse(Base64Util.isUrlSafeBase64(null));
    }

    @Test
    @DisplayName("isUrlSafeBase64(String) - 空字符串返回 false")
    void isUrlSafeBase64_empty() {
        assertFalse(Base64Util.isUrlSafeBase64(""));
    }

    // ======================== 边界情况 ========================

    @Test
    @DisplayName("编码后解码边界 - 单个字符")
    void edge_singleChar() {
        assertEquals("A", Base64Util.decode(Base64Util.encode("A")));
    }

    @Test
    @DisplayName("编码后解码边界 - 2个字符（补1个=）")
    void edge_twoChars() {
        assertEquals("AB", Base64Util.decode(Base64Util.encode("AB")));
    }

    @Test
    @DisplayName("编码后解码边界 - 3个字符（无需补=）")
    void edge_threeChars() {
        assertEquals("ABC", Base64Util.decode(Base64Util.encode("ABC")));
    }

    @Test
    @DisplayName("已知标准编码值验证")
    void knownEncodings() {
        assertEquals("", Base64Util.encode(""));
        assertEquals("TQ==", Base64Util.encode("M"));
        assertEquals("TWE=", Base64Util.encode("Ma"));
        assertEquals("TWFu", Base64Util.encode("Man"));
    }
}
