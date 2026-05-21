package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssStageRequest 暂存请求记录测试")
class OssStageRequestTest {

    private static final String FILENAME = "report.pdf";
    private static final String CONTENT_TYPE = "application/pdf";
    private static final Duration TTL = Duration.ofHours(2);

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            OssStageRequest request = new OssStageRequest(FILENAME, CONTENT_TYPE, "uploads/reports", TTL);

            assertEquals(FILENAME, request.filename());
            assertEquals(CONTENT_TYPE, request.contentType());
            assertEquals("uploads/reports", request.basePath());
            assertEquals(TTL, request.ttl());
        }

        @Test
        @DisplayName("应允许 contentType 为 null")
        void should_allow_null_contentType() {
            OssStageRequest request = new OssStageRequest(FILENAME, null, null, TTL);

            assertNull(request.contentType());
        }

        @Test
        @DisplayName("应允许 basePath 为 null")
        void should_allow_null_basePath() {
            OssStageRequest request = new OssStageRequest(FILENAME, CONTENT_TYPE, null, TTL);

            assertNull(request.basePath());
        }

        @Test
        @DisplayName("应允许 ttl 为 null")
        void should_allow_null_ttl() {
            OssStageRequest request = new OssStageRequest(FILENAME, CONTENT_TYPE, null, null);

            assertNull(request.ttl());
        }

        @Test
        @DisplayName("应允许所有可选字段为 null")
        void should_allow_all_optional_fields_null() {
            OssStageRequest request = new OssStageRequest(FILENAME, null, null, null);

            assertEquals(FILENAME, request.filename());
            assertNull(request.contentType());
            assertNull(request.basePath());
            assertNull(request.ttl());
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            OssStageRequest a = new OssStageRequest(FILENAME, CONTENT_TYPE, "path", TTL);
            OssStageRequest b = new OssStageRequest(FILENAME, CONTENT_TYPE, "path", TTL);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 filename 的两个记录应不相等")
        void should_not_be_equal_when_different_filename() {
            OssStageRequest a = new OssStageRequest("a.pdf", CONTENT_TYPE, null, TTL);
            OssStageRequest b = new OssStageRequest("b.pdf", CONTENT_TYPE, null, TTL);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 ttl 的两个记录应不相等")
        void should_not_be_equal_when_different_ttl() {
            OssStageRequest a = new OssStageRequest(FILENAME, CONTENT_TYPE, null, Duration.ofHours(1));
            OssStageRequest b = new OssStageRequest(FILENAME, CONTENT_TYPE, null, Duration.ofHours(2));

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssStageRequest request = new OssStageRequest(FILENAME, CONTENT_TYPE, null, TTL);

            assertNotEquals(null, request);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含 filename 信息")
        void should_contain_filename_in_toString() {
            OssStageRequest request = new OssStageRequest(FILENAME, CONTENT_TYPE, "reports", TTL);

            String str = request.toString();
            assertTrue(str.contains(FILENAME));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            OssStageRequest request = new OssStageRequest(FILENAME, CONTENT_TYPE, null, TTL);

            assertTrue(request instanceof java.io.Serializable);
        }
    }
}
