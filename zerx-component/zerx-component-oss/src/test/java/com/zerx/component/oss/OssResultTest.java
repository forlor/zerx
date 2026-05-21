package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssResult 上传结果记录测试")
class OssResultTest {

    private static final String OBJECT_KEY = "uploads/2024/01/abc123.pdf";
    private static final String URL = "https://cdn.example.com/uploads/2024/01/abc123.pdf";
    private static final String ORIGINAL_FILENAME = "月度报表.pdf";
    private static final long SIZE = 102400L;
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String ETAG = "d41d8cd98f00b204e9800998ecf8427e";
    private static final Instant LAST_MODIFIED = Instant.parse("2024-01-15T10:30:00Z");

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            OssResult result = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            assertEquals(OBJECT_KEY, result.objectKey());
            assertEquals(URL, result.url());
            assertEquals(ORIGINAL_FILENAME, result.originalFilename());
            assertEquals(SIZE, result.size());
            assertEquals(CONTENT_TYPE, result.contentType());
            assertEquals(ETAG, result.etag());
            assertEquals(LAST_MODIFIED, result.lastModified());
        }

        @Test
        @DisplayName("应允许 contentType 为 null")
        void should_allow_null_contentType() {
            OssResult result = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, null, ETAG, LAST_MODIFIED);

            assertNull(result.contentType());
        }

        @Test
        @DisplayName("应支持零大小文件")
        void should_support_zero_size() {
            OssResult result = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, 0, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            OssResult a = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);
            OssResult b = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同字段值的两个记录应不相等")
        void should_not_be_equal_when_different_fields() {
            OssResult a = new OssResult("key-a", URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);
            OssResult b = new OssResult("key-b", URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssResult result = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            assertNotEquals(null, result);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含关键字段信息")
        void should_contain_key_fields_in_toString() {
            OssResult result = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            String str = result.toString();
            assertTrue(str.contains(OBJECT_KEY));
            assertTrue(str.contains(URL));
            assertTrue(str.contains(ORIGINAL_FILENAME));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            OssResult result = new OssResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG, LAST_MODIFIED);

            assertTrue(result instanceof java.io.Serializable);
        }
    }
}
