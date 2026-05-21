package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssConfirmResult 暂存确认结果记录测试")
class OssConfirmResultTest {

    private static final String OBJECT_KEY = "uploads/2024/01/abc123.pdf";
    private static final String URL = "https://cdn.example.com/uploads/2024/01/abc123.pdf";
    private static final String ORIGINAL_FILENAME = "月度报表.pdf";
    private static final long SIZE = 102400L;
    private static final String CONTENT_TYPE = "application/pdf";
    private static final String ETAG = "d41d8cd98f00b204e9800998ecf8427e";

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            OssConfirmResult result = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);

            assertEquals(OBJECT_KEY, result.objectKey());
            assertEquals(URL, result.url());
            assertEquals(ORIGINAL_FILENAME, result.originalFilename());
            assertEquals(SIZE, result.size());
            assertEquals(CONTENT_TYPE, result.contentType());
            assertEquals(ETAG, result.etag());
        }

        @Test
        @DisplayName("应允许 contentType 为 null")
        void should_allow_null_contentType() {
            OssConfirmResult result = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, null, ETAG);

            assertNull(result.contentType());
        }

        @Test
        @DisplayName("应允许 etag 为 null")
        void should_allow_null_etag() {
            OssConfirmResult result = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, null);

            assertNull(result.etag());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            OssConfirmResult a = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);
            OssConfirmResult b = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 objectKey 的两个记录应不相等")
        void should_not_be_equal_when_different_objectKey() {
            OssConfirmResult a = new OssConfirmResult("key-a", URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);
            OssConfirmResult b = new OssConfirmResult("key-b", URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 size 的两个记录应不相等")
        void should_not_be_equal_when_different_size() {
            OssConfirmResult a = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, 100L, CONTENT_TYPE, ETAG);
            OssConfirmResult b = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, 200L, CONTENT_TYPE, ETAG);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssConfirmResult result = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);

            assertNotEquals(null, result);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含关键字段信息")
        void should_contain_key_fields_in_toString() {
            OssConfirmResult result = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);

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
            OssConfirmResult result = new OssConfirmResult(OBJECT_KEY, URL, ORIGINAL_FILENAME, SIZE, CONTENT_TYPE, ETAG);

            assertTrue(result instanceof java.io.Serializable);
        }
    }
}
