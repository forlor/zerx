package com.zerx.spring.web.sensitive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensitiveDataMasker 单元测试
 *
 * @author zerx
 */
class SensitiveDataMaskerTest {

    private SensitiveDataMasker masker;

    @BeforeEach
    void setUp() {
        masker = new SensitiveDataMasker(
                List.of("password", "token", "secret", "phone")
        );
    }

    @Nested
    @DisplayName("URL Query String 脱敏")
    class QueryStringMasking {

        @Test
        @DisplayName("password 参数值脱敏")
        void shouldMaskPassword() {
            assertEquals("name=admin&password=******&role=user",
                    masker.mask("name=admin&password=abc123&role=user"));
        }

        @Test
        @DisplayName("token 参数值脱敏")
        void shouldMaskToken() {
            assertEquals("?token=******",
                    masker.mask("?token=eyJhbGciOiJIUzI1NiJ9"));
        }

        @Test
        @DisplayName("多个敏感参数同时脱敏")
        void shouldMaskMultiple() {
            String input = "user=admin&password=123&token=abc&secret=xyz";
            String result = masker.mask(input);
            assertTrue(result.contains("password=******"));
            assertTrue(result.contains("token=******"));
            assertTrue(result.contains("secret=******"));
            assertTrue(result.contains("user=admin"));
        }

        @Test
        @DisplayName("大小写不敏感")
        void shouldBeCaseInsensitive() {
            assertEquals("Password=******",
                    masker.mask("Password=mysecret"));
        }

        @Test
        @DisplayName("? 开头的查询字符串")
        void shouldHandleLeadingQuestionMark() {
            assertEquals("?password=******&name=test",
                    masker.mask("?password=mypass&name=test"));
        }
    }

    @Nested
    @DisplayName("JSON Key-Value 脱敏")
    class JsonMasking {

        @Test
        @DisplayName("JSON password 字段脱敏")
        void shouldMaskJsonPassword() {
            String json = """
                    {"username":"admin","password":"abc123","role":"user"}
                    """;
            String result = masker.mask(json);
            assertTrue(result.contains("\"password\":\"******\""));
            assertTrue(result.contains("\"username\":\"admin\""));
        }

        @Test
        @DisplayName("JSON token 字段脱敏")
        void shouldMaskJsonToken() {
            String json = """
                    {"access_token":"eyJhbGciOiJIUzI1NiJ9","expires_in":3600}
                    """;
            String result = masker.mask(json);
            assertTrue(result.contains("\"access_token\":\"******\""));
        }

        @Test
        @DisplayName("JSON phone 字段脱敏")
        void shouldMaskJsonPhone() {
            String json = """
                    {"name":"张三","phone":"13800138000"}
                    """;
            String result = masker.mask(json);
            assertTrue(result.contains("\"phone\":\"******\""));
        }
    }

    @Nested
    @DisplayName("边界条件")
    class EdgeCases {

        @Test
        @DisplayName("null 输入返回 null")
        void shouldReturnNullForNull() {
            assertNull(masker.mask(null));
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void shouldReturnEmptyForEmpty() {
            assertEquals("", masker.mask(""));
        }

        @Test
        @DisplayName("无敏感参数返回原字符串")
        void shouldReturnOriginalWhenNoSensitive() {
            String input = "name=admin&role=user&age=25";
            assertEquals(input, masker.mask(input));
        }

        @Test
        @DisplayName("空敏感参数列表 — 不脱敏")
        void shouldNotMaskWithEmptyParams() {
            SensitiveDataMasker emptyMasker = new SensitiveDataMasker(List.of());
            String input = "password=secret";
            assertEquals(input, emptyMasker.mask(input));
        }
    }
}
