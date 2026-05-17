package com.zerx.spring.web.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.spring.web.properties.ZerxWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JacksonAutoConfiguration 单元测试
 *
 * @author zerx
 */
class JacksonAutoConfigurationTest {

    private ZerxWebProperties webProps;
    private JacksonAutoConfiguration config;

    @BeforeEach
    void setUp() {
        webProps = new ZerxWebProperties();
        config = new JacksonAutoConfiguration();
    }

    private ObjectMapper createMapper() {
        org.springframework.boot.autoconfigure.jackson.JacksonProperties jacksonProps =
                new org.springframework.boot.autoconfigure.jackson.JacksonProperties();
        return config.zerxObjectMapper(webProps, jacksonProps);
    }

    @Nested
    @DisplayName("Long → String 序列化")
    class LongToString {

        @Test
        @DisplayName("Long 包装类型序列化为字符串")
        void shouldSerializeLongWrapperToString() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            String json = mapper.writeValueAsString(new TestBean(123456789012345L, "test"));
            assertTrue(json.contains("\"id\":\"123456789012345\""));
        }

        @Test
        @DisplayName("long 基本类型序列化为字符串")
        void shouldSerializeLongPrimitiveToString() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            String json = mapper.writeValueAsString(new PrimitiveBean(9999999999L));
            assertTrue(json.contains("\"value\":\"9999999999\""));
        }

        @Test
        @DisplayName("null Long 不应序列化（NON_NULL）")
        void shouldNotSerializeNullLong() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            String json = mapper.writeValueAsString(new TestBean(null, "test"));
            assertFalse(json.contains("\"id\""));
        }

        record TestBean(Long id, String name) {}
        record PrimitiveBean(long value) {}
    }

    @Nested
    @DisplayName("LocalDateTime 格式化")
    class LocalDateTimeFormat {

        @Test
        @DisplayName("使用默认格式 yyyy-MM-dd HH:mm:ss")
        void shouldFormatWithDefaultPattern() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            LocalDateTime dt = LocalDateTime.of(2025, 1, 15, 10, 30, 45);
            String json = mapper.writeValueAsString(new DateBean(dt));
            assertTrue(json.contains("\"time\":\"2025-01-15 10:30:45\""));
        }

        @Test
        @DisplayName("使用自定义日期格式")
        void shouldFormatWithCustomPattern() throws JsonProcessingException {
            webProps.getJackson().setDateFormat("yyyy/MM/dd");
            ObjectMapper mapper = createMapper();
            LocalDateTime dt = LocalDateTime.of(2025, 6, 1, 0, 0, 0);
            String json = mapper.writeValueAsString(new DateBean(dt));
            assertTrue(json.contains("\"time\":\"2025/06/01\""));
        }

        record DateBean(LocalDateTime time) {}
    }

    @Nested
    @DisplayName("null 值序列化控制")
    class NullControl {

        @Test
        @DisplayName("默认 NON_NULL：不序列化 null 字段")
        void shouldExcludeNullByDefault() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            String json = mapper.writeValueAsString(new NullBean("hello", null));
            assertFalse(json.contains("\"optionalValue\""));
            assertTrue(json.contains("\"requiredValue\":\"hello\""));
        }

        @Test
        @DisplayName("includeNull=true：序列化 null 字段")
        void shouldIncludeNullWhenEnabled() throws JsonProcessingException {
            webProps.getJackson().setIncludeNull(true);
            ObjectMapper mapper = createMapper();
            String json = mapper.writeValueAsString(new NullBean("hello", null));
            assertTrue(json.contains("\"optionalValue\":null"));
        }

        record NullBean(String requiredValue, String optionalValue) {}
    }

    @Nested
    @DisplayName("特性配置")
    class FeatureConfig {

        @Test
        @DisplayName("未知属性不报错")
        void shouldIgnoreUnknownProperties() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            String json = """
                    {"id":1,"name":"test","unknownField":"should-be-ignored"}
                    """;
            assertDoesNotThrow(() -> mapper.readValue(json, TestBean.class));
        }

        @Test
        @DisplayName("ObjectMapper 使用 NON_NULL Include")
        void shouldHaveNonNullInclude() throws JsonProcessingException {
            ObjectMapper mapper = createMapper();
            String json = mapper.writeValueAsString(new NullBean("hello", null));
            assertFalse(json.contains("\"optionalValue\""), "null 字段不应被序列化");
        }

        record NullBean(String requiredValue, String optionalValue) {}

        @Test
        @DisplayName("禁用 FAIL_ON_UNKNOWN_PROPERTIES")
        void shouldDisableFailOnUnknownProperties() {
            ObjectMapper mapper = createMapper();
            assertFalse(mapper.getDeserializationConfig()
                    .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        }

        record TestBean(Long id, String name) {}
    }
}
