package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssUploadRequest 上传请求记录测试")
class OssUploadRequestTest {

    private static final ByteArrayInputStream INPUT_STREAM = new ByteArrayInputStream(new byte[]{1, 2, 3});
    private static final String FILENAME = "月度报表.pdf";

    @Nested
    @DisplayName("of() 工厂方法")
    class OfFactoryMethod {

        @Test
        @DisplayName("应使用 inputStream 和 filename 创建请求，basePath 为 null")
        void should_create_request_with_null_basePath() {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest request = OssUploadRequest.of(is, FILENAME);

            assertSame(is, request.inputStream());
            assertEquals(FILENAME, request.filename());
            assertNull(request.basePath());
        }

        @Test
        @DisplayName("应允许 filename 为空字符串")
        void should_allow_empty_filename() {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest request = OssUploadRequest.of(is, "");

            assertEquals("", request.filename());
        }

        @Test
        @DisplayName("应允许 null basePath")
        void should_allow_null_basePath_in_of() {
            OssUploadRequest request = OssUploadRequest.of(INPUT_STREAM, FILENAME);

            assertNull(request.basePath());
        }
    }

    @Nested
    @DisplayName("记录构造与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建包含 basePath 的记录")
        void should_create_record_with_basePath() {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest request = new OssUploadRequest(is, FILENAME, "user-avatars");

            assertSame(is, request.inputStream());
            assertEquals(FILENAME, request.filename());
            assertEquals("user-avatars", request.basePath());
        }

        @Test
        @DisplayName("应允许 basePath 为 null")
        void should_allow_null_basePath() {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest request = new OssUploadRequest(is, FILENAME, null);

            assertNull(request.basePath());
        }

        @Test
        @DisplayName("应允许 basePath 为空字符串")
        void should_allow_empty_basePath() {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest request = new OssUploadRequest(is, FILENAME, "");

            assertEquals("", request.basePath());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("of() 创建的记录与手动构造相同字段的记录应相等")
        void should_equal_manually_constructed() {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest fromFactory = OssUploadRequest.of(is, FILENAME);
            OssUploadRequest fromConstructor = new OssUploadRequest(is, FILENAME, null);

            assertEquals(fromFactory, fromConstructor);
            assertEquals(fromFactory.hashCode(), fromConstructor.hashCode());
        }

        @Test
        @DisplayName("不同 basePath 的两个记录应不相等")
        void should_not_be_equal_when_different_basePath() {
            ByteArrayInputStream is1 = new ByteArrayInputStream(new byte[]{1, 2, 3});
            ByteArrayInputStream is2 = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssUploadRequest a = new OssUploadRequest(is1, FILENAME, "path-a");
            OssUploadRequest b = new OssUploadRequest(is2, FILENAME, "path-b");

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssUploadRequest request = OssUploadRequest.of(INPUT_STREAM, FILENAME);

            assertNotEquals(null, request);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含 filename 和 basePath")
        void should_contain_filename_and_basePath_in_toString() {
            OssUploadRequest request = new OssUploadRequest(INPUT_STREAM, FILENAME, "reports");

            String str = request.toString();
            assertTrue(str.contains(FILENAME));
            assertTrue(str.contains("reports"));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            OssUploadRequest request = OssUploadRequest.of(INPUT_STREAM, FILENAME);

            assertTrue(request instanceof java.io.Serializable);
        }
    }
}
