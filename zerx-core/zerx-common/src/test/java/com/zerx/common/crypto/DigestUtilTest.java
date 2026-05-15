package com.zerx.common.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DigestUtil}
 */
class DigestUtilTest {

    // ======================== MD5 ========================

    @Test
    @DisplayName("md5(String) computes correct MD5 hash")
    void md5String() {
        // Known test vector: MD5("") = d41d8cd98f00b204e9800998ecf8427e
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", DigestUtil.md5(""));

        // Known test vector: MD5("hello") = 5d41402abc4b2a76b9719d911017c592
        assertEquals("5d41402abc4b2a76b9719d911017c592", DigestUtil.md5("hello"));

        // Known test vector: MD5("The quick brown fox jumps over the lazy dog")
        assertEquals("9e107d9d372bb6826bd81d3542a419d6",
                DigestUtil.md5("The quick brown fox jumps over the lazy dog"));
    }

    @Test
    @DisplayName("md5(String, Charset) uses specified charset")
    void md5StringWithCharset() {
        String data = "hello";
        assertEquals(
                DigestUtil.md5(data, StandardCharsets.UTF_8),
                DigestUtil.md5(data, StandardCharsets.ISO_8859_1)
        );
    }

    @Test
    @DisplayName("md5(byte[]) computes correct MD5 hash")
    void md5Bytes() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e",
                DigestUtil.md5(new byte[0]));

        assertEquals("5d41402abc4b2a76b9719d911017c592",
                DigestUtil.md5("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("md5(String) returns null for null input")
    void md5NullString() {
        assertNull(DigestUtil.md5((String) null));
    }

    @Test
    @DisplayName("md5(byte[]) returns null for null input")
    void md5NullBytes() {
        assertNull(DigestUtil.md5((byte[]) null));
    }

    // ======================== SHA-1 ========================

    @Test
    @DisplayName("sha1(String) computes correct SHA-1 hash")
    void sha1String() {
        // Known test vector: SHA-1("") = da39a3ee5e6b4b0d3255bfef95601890afd80709
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", DigestUtil.sha1(""));

        // Known test vector: SHA-1("hello") = aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", DigestUtil.sha1("hello"));
    }

    @Test
    @DisplayName("sha1(String, Charset) uses specified charset")
    void sha1StringWithCharset() {
        String data = "hello";
        assertEquals(
                DigestUtil.sha1(data, StandardCharsets.UTF_8),
                DigestUtil.sha1(data, StandardCharsets.ISO_8859_1)
        );
    }

    @Test
    @DisplayName("sha1(byte[]) computes correct SHA-1 hash")
    void sha1Bytes() {
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709",
                DigestUtil.sha1(new byte[0]));
    }

    @Test
    @DisplayName("sha1(String) returns null for null input")
    void sha1NullString() {
        assertNull(DigestUtil.sha1((String) null));
    }

    @Test
    @DisplayName("sha1(byte[]) returns null for null input")
    void sha1NullBytes() {
        assertNull(DigestUtil.sha1((byte[]) null));
    }

    // ======================== SHA-256 ========================

    @Test
    @DisplayName("sha256(String) computes correct SHA-256 hash")
    void sha256String() {
        // Known test vector: SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                DigestUtil.sha256(""));

        // Known test vector: SHA-256("hello")
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                DigestUtil.sha256("hello"));

        // Known test vector: SHA-256("The quick brown fox jumps over the lazy dog")
        assertEquals("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
                DigestUtil.sha256("The quick brown fox jumps over the lazy dog"));
    }

    @Test
    @DisplayName("sha256(String, Charset) uses specified charset")
    void sha256StringWithCharset() {
        String data = "hello";
        assertEquals(
                DigestUtil.sha256(data, StandardCharsets.UTF_8),
                DigestUtil.sha256(data, StandardCharsets.ISO_8859_1)
        );
    }

    @Test
    @DisplayName("sha256(byte[]) computes correct SHA-256 hash")
    void sha256BytesHex() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                DigestUtil.sha256(new byte[0]));
    }

    @Test
    @DisplayName("sha256(String) returns null for null input")
    void sha256NullString() {
        assertNull(DigestUtil.sha256((String) null));
    }

    @Test
    @DisplayName("sha256(byte[]) returns null for null input")
    void sha256NullBytes() {
        assertNull(DigestUtil.sha256((byte[]) null));
    }

    // ======================== SHA-512 ========================

    @Test
    @DisplayName("sha512(String) computes correct SHA-512 hash")
    void sha512String() {
        // Known test vector: SHA-512("")
        assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                DigestUtil.sha512(""));

        // Known test vector: SHA-512("hello")
        assertEquals("9b71d224bd62f3785d96d46ad3ea3d73319bfbc2890caadae2dff72519673ca72323c3d99ba5c11d7c7acc6e14b8c5da0c4663475c2e5c3adef46f73bcdec043",
                DigestUtil.sha512("hello"));
    }

    @Test
    @DisplayName("sha512(String, Charset) uses specified charset")
    void sha512StringWithCharset() {
        String data = "hello";
        assertEquals(
                DigestUtil.sha512(data, StandardCharsets.UTF_8),
                DigestUtil.sha512(data, StandardCharsets.ISO_8859_1)
        );
    }

    @Test
    @DisplayName("sha512(byte[]) computes correct SHA-512 hash")
    void sha512BytesHex() {
        assertEquals("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
                DigestUtil.sha512(new byte[0]));
    }

    @Test
    @DisplayName("sha512(String) returns null for null input")
    void sha512NullString() {
        assertNull(DigestUtil.sha512((String) null));
    }

    @Test
    @DisplayName("sha512(byte[]) returns null for null input")
    void sha512NullBytes() {
        assertNull(DigestUtil.sha512((byte[]) null));
    }

    // ======================== Generic digest methods ========================

    @Test
    @DisplayName("digestHex(String, algorithm) works with any algorithm")
    void digestHexString() {
        assertEquals(DigestUtil.md5("test"), DigestUtil.digestHex("test", "MD5"));
        assertEquals(DigestUtil.sha256("test"), DigestUtil.digestHex("test", "SHA-256"));
    }

    @Test
    @DisplayName("digestHex(String, algorithm, Charset) uses specified charset")
    void digestHexStringWithCharset() {
        assertEquals(
                DigestUtil.digestHex("hello", "SHA-256", StandardCharsets.UTF_8),
                DigestUtil.digestHex("hello", "SHA-256", StandardCharsets.ISO_8859_1)
        );
    }

    @Test
    @DisplayName("digestHex(String, algorithm) returns null for null input")
    void digestHexNullString() {
        assertNull(DigestUtil.digestHex((String) null, "MD5"));
    }

    @Test
    @DisplayName("digestHex(String, algorithm, Charset) returns null for null input")
    void digestHexNullStringWithCharset() {
        assertNull(DigestUtil.digestHex(null, "MD5", StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("digestHex(byte[], algorithm) works correctly")
    void digestHexByteArray() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        assertEquals(DigestUtil.md5("test"), DigestUtil.digestHex(data, "MD5"));
    }

    @Test
    @DisplayName("digestHex(byte[], algorithm) returns null for null input")
    void digestHexNullByteArray() {
        assertNull(DigestUtil.digestHex((byte[]) null, "MD5"));
    }

    @Test
    @DisplayName("digest(byte[], algorithm) returns correct byte array")
    void digestByteArray() {
        byte[] hash = DigestUtil.digest("hello".getBytes(StandardCharsets.UTF_8), "MD5");
        assertNotNull(hash);
        assertEquals(16, hash.length, "MD5 hash should be 16 bytes");

        byte[] hash256 = DigestUtil.digest("hello".getBytes(StandardCharsets.UTF_8), "SHA-256");
        assertNotNull(hash256);
        assertEquals(32, hash256.length, "SHA-256 hash should be 32 bytes");
    }

    @Test
    @DisplayName("digest(byte[], algorithm) returns empty array for null input")
    void digestNullByteArray() {
        byte[] result = DigestUtil.digest(null, "MD5");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // ======================== getDigest ========================

    @Test
    @DisplayName("getDigest returns MessageDigest for valid algorithm")
    void getDigestValid() {
        MessageDigest md = DigestUtil.getDigest("SHA-256");
        assertNotNull(md);
        assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    @DisplayName("getDigest throws UnsupportedOperationException for invalid algorithm")
    void getDigestInvalid() {
        assertThrows(UnsupportedOperationException.class, () -> DigestUtil.getDigest("INVALID-ALGO"));
    }

    // ======================== Byte array convenience methods ========================

    @Test
    @DisplayName("md5Bytes returns 16-byte array")
    void md5BytesResult() {
        byte[] hash = DigestUtil.md5Bytes("hello".getBytes(StandardCharsets.UTF_8));
        assertNotNull(hash);
        assertEquals(16, hash.length);
    }

    @Test
    @DisplayName("md5Bytes returns empty array for null input")
    void md5BytesNull() {
        byte[] result = DigestUtil.md5Bytes(null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("sha256Bytes returns 32-byte array")
    void sha256BytesResult() {
        byte[] hash = DigestUtil.sha256Bytes("hello".getBytes(StandardCharsets.UTF_8));
        assertNotNull(hash);
        assertEquals(32, hash.length);
    }

    @Test
    @DisplayName("sha256Bytes returns empty array for null input")
    void sha256BytesNull() {
        byte[] result = DigestUtil.sha256Bytes(null);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    // ======================== Algorithm constants ========================

    @Test
    @DisplayName("algorithm constants have expected values")
    void algorithmConstants() {
        assertEquals("MD5", DigestUtil.ALGORITHM_MD5);
        assertEquals("SHA-1", DigestUtil.ALGORITHM_SHA1);
        assertEquals("SHA-256", DigestUtil.ALGORITHM_SHA256);
        assertEquals("SHA-512", DigestUtil.ALGORITHM_SHA512);
    }

    // ======================== Consistency checks ========================

    @Test
    @DisplayName("same input always produces same hash")
    void deterministic() {
        String data = "deterministic-test-12345";
        String hash1 = DigestUtil.sha256(data);
        String hash2 = DigestUtil.sha256(data);
        String hash3 = DigestUtil.sha256(data.getBytes(StandardCharsets.UTF_8));
        assertEquals(hash1, hash2);
        assertEquals(hash1, hash3);
    }

    @Test
    @DisplayName("different inputs produce different hashes")
    void collisionResistance() {
        String hash1 = DigestUtil.sha256("input1");
        String hash2 = DigestUtil.sha256("input2");
        assertNotEquals(hash1, hash2);
    }

    // ======================== Constructor ========================

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException")
    void privateConstructor() throws Exception {
        var constructor = DigestUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
    }
}
