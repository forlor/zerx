package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PresignedUrl 预签名 URL 记录测试")
class PresignedUrlTest {

    private static final String URL = "https://oss.example.com/bucket/file.pdf?X-Amz-Signature=abc123";
    private static final Instant EXPIRES_AT = Instant.parse("2024-12-31T23:59:59Z");

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/pdf");
            headers.put("x-amz-meta-zerx-filename", "report.pdf");

            PresignedUrl presigned = new PresignedUrl(URL, headers, EXPIRES_AT);

            assertEquals(URL, presigned.url());
            assertEquals(2, presigned.headers().size());
            assertEquals("application/pdf", presigned.headers().get("Content-Type"));
            assertEquals(EXPIRES_AT, presigned.expiresAt());
        }

        @Test
        @DisplayName("应允许 headers 为 null")
        void should_allow_null_headers() {
            PresignedUrl presigned = new PresignedUrl(URL, null, EXPIRES_AT);

            assertNull(presigned.headers());
        }

        @Test
        @DisplayName("应允许 headers 为空 Map")
        void should_allow_empty_headers() {
            PresignedUrl presigned = new PresignedUrl(URL, Collections.emptyMap(), EXPIRES_AT);

            assertNotNull(presigned.headers());
            assertTrue(presigned.headers().isEmpty());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            Map<String, String> h1 = Map.of("Content-Type", "application/pdf");
            Map<String, String> h2 = new HashMap<>(h1);

            PresignedUrl a = new PresignedUrl(URL, h1, EXPIRES_AT);
            PresignedUrl b = new PresignedUrl(URL, h2, EXPIRES_AT);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 URL 的两个记录应不相等")
        void should_not_be_equal_when_different_url() {
            Map<String, String> headers = Map.of("Content-Type", "application/pdf");

            PresignedUrl a = new PresignedUrl("https://a.example.com/file", headers, EXPIRES_AT);
            PresignedUrl b = new PresignedUrl("https://b.example.com/file", headers, EXPIRES_AT);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同过期时间的两个记录应不相等")
        void should_not_be_equal_when_different_expiresAt() {
            Instant otherExpiry = Instant.parse("2025-01-01T00:00:00Z");

            PresignedUrl a = new PresignedUrl(URL, null, EXPIRES_AT);
            PresignedUrl b = new PresignedUrl(URL, null, otherExpiry);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            PresignedUrl presigned = new PresignedUrl(URL, null, EXPIRES_AT);

            assertNotEquals(null, presigned);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含 URL 信息")
        void should_contain_url_in_toString() {
            PresignedUrl presigned = new PresignedUrl(URL, null, EXPIRES_AT);

            String str = presigned.toString();
            assertTrue(str.contains(URL));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            PresignedUrl presigned = new PresignedUrl(URL, null, EXPIRES_AT);

            assertTrue(presigned instanceof java.io.Serializable);
        }
    }
}
