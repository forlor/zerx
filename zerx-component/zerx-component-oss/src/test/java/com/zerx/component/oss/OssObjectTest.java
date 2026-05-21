package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssObject 对象存储对象测试")
class OssObjectTest {

    private static final OssObjectMeta META = new OssObjectMeta(
            "uploads/test.pdf", 1024L, "application/pdf",
            Instant.parse("2024-01-15T10:30:00Z"), "etag123", "test.pdf"
    );

    @Nested
    @DisplayName("构造函数")
    class ConstructorTest {

        @Test
        @DisplayName("应使用有效的 inputStream 和 meta 正常构造")
        void should_construct_with_valid_args() {
            InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssObject obj = new OssObject(is, META);

            assertSame(is, obj.getInputStream());
            assertSame(META, obj.getMeta());
        }

        @Test
        @DisplayName("当 inputStream 为 null 时应抛出 IllegalArgumentException")
        void should_throw_when_inputStream_is_null() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OssObject(null, META));

            assertEquals("inputStream must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("当 meta 为 null 时应抛出 IllegalArgumentException")
        void should_throw_when_meta_is_null() {
            InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OssObject(is, null));

            assertEquals("meta must not be null", ex.getMessage());
        }

        @Test
        @DisplayName("当两者均为 null 时应先校验 inputStream")
        void should_validate_inputStream_first() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new OssObject(null, null));

            assertEquals("inputStream must not be null", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Getter 方法")
    class GetterTest {

        @Test
        @DisplayName("getInputStream 应返回构造时传入的流")
        void should_return_same_inputStream() {
            InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssObject obj = new OssObject(is, META);

            assertSame(is, obj.getInputStream());
        }

        @Test
        @DisplayName("getMeta 应返回构造时传入的元数据")
        void should_return_same_meta() {
            InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssObject obj = new OssObject(is, META);

            assertSame(META, obj.getMeta());
            assertEquals("uploads/test.pdf", obj.getMeta().objectKey());
            assertEquals(1024L, obj.getMeta().size());
        }
    }

    @Nested
    @DisplayName("close() 方法")
    class CloseTest {

        @Test
        @DisplayName("关闭后底层 inputStream 应被关闭")
        void should_close_underlying_stream() throws IOException {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssObject obj = new OssObject(is, META);

            obj.close();

            // ByteArrayInputStream.read() 在关闭后读不到数据（返回 -1）
            assertEquals(-1, is.read());
        }

        @Test
        @DisplayName("多次关闭不应抛出异常（幂等性）")
        void should_be_idempotent_on_close() throws IOException {
            ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssObject obj = new OssObject(is, META);

            assertDoesNotThrow(() -> {
                obj.close();
                obj.close();
                obj.close();
            });
        }

        @Test
        @DisplayName("应实现 Closeable 接口")
        void should_implement_closeable() {
            InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
            OssObject obj = new OssObject(is, META);

            assertTrue(obj instanceof java.io.Closeable);
        }
    }

    @Nested
    @DisplayName("支持 try-with-resources")
    class TryWithResourcesTest {

        @Test
        @DisplayName("应在 try-with-resources 中正确关闭")
        void should_close_in_try_with_resources() throws IOException {
            byte[] data = "hello".getBytes();
            ByteArrayInputStream is = new ByteArrayInputStream(data);

            try (OssObject obj = new OssObject(is, META)) {
                assertNotNull(obj.getInputStream());
                assertNotNull(obj.getMeta());
            }

            // 流已被关闭，读取应返回 -1
            assertEquals(-1, is.read());
        }
    }
}
