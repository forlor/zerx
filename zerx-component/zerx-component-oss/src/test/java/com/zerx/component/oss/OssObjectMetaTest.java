package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssObjectMeta 对象元数据记录测试")
class OssObjectMetaTest {

    private static final String OBJECT_KEY = "uploads/2024/01/report.pdf";
    private static final long SIZE = 204800L;
    private static final String CONTENT_TYPE = "application/pdf";
    private static final Instant LAST_MODIFIED = Instant.parse("2024-01-15T10:30:00Z");
    private static final String ETAG = "a1b2c3d4e5f6";
    private static final String ORIGINAL_FILENAME = "年度报告.pdf";

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertEquals(OBJECT_KEY, meta.objectKey());
            assertEquals(SIZE, meta.size());
            assertEquals(CONTENT_TYPE, meta.contentType());
            assertEquals(LAST_MODIFIED, meta.lastModified());
            assertEquals(ETAG, meta.etag());
            assertEquals(ORIGINAL_FILENAME, meta.originalFilename());
        }

        @Test
        @DisplayName("应允许 contentType 为 null")
        void should_allow_null_contentType() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, null, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertNull(meta.contentType());
        }

        @Test
        @DisplayName("应允许 originalFilename 为 null")
        void should_allow_null_originalFilename() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, null);

            assertNull(meta.originalFilename());
        }

        @Test
        @DisplayName("应允许 etag 为 null")
        void should_allow_null_etag() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, null, ORIGINAL_FILENAME);

            assertNull(meta.etag());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            OssObjectMeta a = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);
            OssObjectMeta b = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 objectKey 的两个记录应不相等")
        void should_not_be_equal_when_different_objectKey() {
            OssObjectMeta a = new OssObjectMeta("key-a", SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);
            OssObjectMeta b = new OssObjectMeta("key-b", SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 size 的两个记录应不相等")
        void should_not_be_equal_when_different_size() {
            OssObjectMeta a = new OssObjectMeta(OBJECT_KEY, 100L, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);
            OssObjectMeta b = new OssObjectMeta(OBJECT_KEY, 200L, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertNotEquals(null, meta);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含关键字段信息")
        void should_contain_key_fields_in_toString() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            String str = meta.toString();
            assertTrue(str.contains(OBJECT_KEY));
            assertTrue(str.contains(ORIGINAL_FILENAME));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            OssObjectMeta meta = new OssObjectMeta(OBJECT_KEY, SIZE, CONTENT_TYPE, LAST_MODIFIED, ETAG, ORIGINAL_FILENAME);

            assertTrue(meta instanceof java.io.Serializable);
        }
    }
}
