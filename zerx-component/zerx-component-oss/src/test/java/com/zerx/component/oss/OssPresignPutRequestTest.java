package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssPresignPutRequest 预签名上传请求记录测试")
class OssPresignPutRequestTest {

    private static final String FILENAME = "photo.jpg";
    private static final String BASE_PATH = "user-avatars";
    private static final Duration EXPIRY = Duration.ofMinutes(30);
    private static final String CONTENT_TYPE = "image/jpeg";

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, CONTENT_TYPE);

            assertEquals(FILENAME, request.filename());
            assertEquals(BASE_PATH, request.basePath());
            assertEquals(EXPIRY, request.expiry());
            assertEquals(CONTENT_TYPE, request.contentType());
        }

        @Test
        @DisplayName("应允许 basePath 为 null")
        void should_allow_null_basePath() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, null, EXPIRY, CONTENT_TYPE);

            assertNull(request.basePath());
            assertEquals(FILENAME, request.filename());
        }

        @Test
        @DisplayName("应允许 contentType 为 null")
        void should_allow_null_contentType() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, null);

            assertNull(request.contentType());
            assertEquals(EXPIRY, request.expiry());
        }

        @Test
        @DisplayName("应允许 basePath 和 contentType 同时为 null")
        void should_allow_both_optional_fields_null() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, null, EXPIRY, null);

            assertEquals(FILENAME, request.filename());
            assertNull(request.basePath());
            assertEquals(EXPIRY, request.expiry());
            assertNull(request.contentType());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            OssPresignPutRequest a = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, CONTENT_TYPE);
            OssPresignPutRequest b = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, CONTENT_TYPE);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 filename 的两个记录应不相等")
        void should_not_be_equal_when_different_filename() {
            OssPresignPutRequest a = new OssPresignPutRequest("a.jpg", BASE_PATH, EXPIRY, CONTENT_TYPE);
            OssPresignPutRequest b = new OssPresignPutRequest("b.jpg", BASE_PATH, EXPIRY, CONTENT_TYPE);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 expiry 的两个记录应不相等")
        void should_not_be_equal_when_different_expiry() {
            OssPresignPutRequest a = new OssPresignPutRequest(FILENAME, BASE_PATH, Duration.ofMinutes(30), CONTENT_TYPE);
            OssPresignPutRequest b = new OssPresignPutRequest(FILENAME, BASE_PATH, Duration.ofMinutes(60), CONTENT_TYPE);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 basePath 的两个记录应不相等")
        void should_not_be_equal_when_different_basePath() {
            OssPresignPutRequest a = new OssPresignPutRequest(FILENAME, "path-a", EXPIRY, CONTENT_TYPE);
            OssPresignPutRequest b = new OssPresignPutRequest(FILENAME, "path-b", EXPIRY, CONTENT_TYPE);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, CONTENT_TYPE);

            assertNotEquals(null, request);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含 filename 和 basePath 信息")
        void should_contain_filename_and_basePath_in_toString() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, CONTENT_TYPE);

            String str = request.toString();
            assertTrue(str.contains(FILENAME));
            assertTrue(str.contains(BASE_PATH));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            OssPresignPutRequest request = new OssPresignPutRequest(FILENAME, BASE_PATH, EXPIRY, CONTENT_TYPE);

            assertTrue(request instanceof java.io.Serializable);
        }
    }
}
