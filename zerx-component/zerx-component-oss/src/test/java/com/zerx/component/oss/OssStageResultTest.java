package com.zerx.component.oss;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OssStageResult 暂存结果记录测试")
class OssStageResultTest {

    private static final String STAGE_TOKEN = "550e8400-e29b-41d4-a716-446655440000";
    private static final String STAGING_OBJECT_KEY = "_staging/550e8400-e29b-41d4-a716-446655440000.pdf";
    private static final PresignedUrl PRESIGNED_URL = new PresignedUrl(
            "https://oss.example.com/_staging/file.pdf?signature=abc",
            Map.of("Content-Type", "application/pdf"),
            Instant.parse("2024-12-31T23:59:59Z")
    );

    @Nested
    @DisplayName("记录创建与访问器")
    class RecordCreationAndAccessors {

        @Test
        @DisplayName("应正确创建记录并获取所有字段值")
        void should_create_record_with_all_fields() {
            OssStageResult result = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);

            assertEquals(STAGE_TOKEN, result.stageToken());
            assertSame(PRESIGNED_URL, result.presignedUrl());
            assertEquals(STAGING_OBJECT_KEY, result.stagingObjectKey());
        }

        @Test
        @DisplayName("应正确获取嵌套的 PresignedUrl 字段")
        void should_access_nested_presignedUrl_fields() {
            OssStageResult result = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);

            PresignedUrl presigned = result.presignedUrl();
            assertTrue(presigned.url().startsWith("https://oss.example.com"));
            assertEquals("application/pdf", presigned.headers().get("Content-Type"));
        }
    }

    @Nested
    @DisplayName("equals 与 hashCode")
    class EqualsAndHashCode {

        @Test
        @DisplayName("相同字段值的两个记录应相等")
        void should_be_equal_when_same_fields() {
            OssStageResult a = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);
            OssStageResult b = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);

            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("不同 stageToken 的两个记录应不相等")
        void should_not_be_equal_when_different_stageToken() {
            OssStageResult a = new OssStageResult("token-a", PRESIGNED_URL, STAGING_OBJECT_KEY);
            OssStageResult b = new OssStageResult("token-b", PRESIGNED_URL, STAGING_OBJECT_KEY);

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("不同 stagingObjectKey 的两个记录应不相等")
        void should_not_be_equal_when_different_stagingObjectKey() {
            OssStageResult a = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, "path-a");
            OssStageResult b = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, "path-b");

            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("与 null 比较应返回 false")
        void should_not_equal_null() {
            OssStageResult result = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);

            assertNotEquals(null, result);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 应包含关键字段信息")
        void should_contain_key_fields_in_toString() {
            OssStageResult result = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);

            String str = result.toString();
            assertTrue(str.contains(STAGE_TOKEN));
            assertTrue(str.contains(STAGING_OBJECT_KEY));
        }
    }

    @Nested
    @DisplayName("Serializable 接口")
    class SerializableTest {

        @Test
        @DisplayName("应实现 Serializable 接口")
        void should_implement_serializable() {
            OssStageResult result = new OssStageResult(STAGE_TOKEN, PRESIGNED_URL, STAGING_OBJECT_KEY);

            assertTrue(result instanceof java.io.Serializable);
        }
    }
}
