package com.zerx.spring.web;

import com.zerx.spring.web.properties.ZerxWebProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZerxWebProperties 配置属性测试
 *
 * @author zerx
 */
class ZerxWebPropertiesTest {

    /**
     * 测试默认值
     */
    @Test
    void testDefaults() {
        var props = new ZerxWebProperties();

        assertTrue(props.isResponseWrapEnabled(), "responseWrapEnabled 默认应为 true");
        assertNotNull(props.getResponseWrapExcludePackages(), "排除包列表不应为 null");
        assertFalse(props.getResponseWrapExcludePackages().isEmpty(), "排除包列表不应为空");
        assertNotNull(props.getCors(), "CORS 配置不应为 null");
    }

    /**
     * 测试默认排除包列表包含 springdoc 和 actuator
     */
    @Test
    void testDefaultExcludePackages() {
        var props = new ZerxWebProperties();
        var excludes = props.getResponseWrapExcludePackages();

        assertTrue(excludes.contains("org.springdoc"), "应包含 org.springdoc");
        assertTrue(excludes.contains("org.springframework.boot.actuator"), "应包含 actuator");
    }

    /**
     * 测试设置自定义排除包列表
     */
    @Test
    void testSetExcludePackages() {
        var props = new ZerxWebProperties();
        List<String> customExcludes = List.of("com.example.exclude");
        props.setResponseWrapExcludePackages(customExcludes);

        assertEquals(1, props.getResponseWrapExcludePackages().size());
        assertEquals("com.example.exclude", props.getResponseWrapExcludePackages().get(0));
    }

    /**
     * 测试关闭统一响应封装
     */
    @Test
    void testDisableResponseWrap() {
        var props = new ZerxWebProperties();
        props.setResponseWrapEnabled(false);

        assertFalse(props.isResponseWrapEnabled());
    }

    /**
     * 测试 CORS 默认配置
     */
    @Test
    void testCorsDefaults() {
        var props = new ZerxWebProperties();
        var cors = props.getCors();

        assertTrue(cors.isEnabled(), "CORS 默认应启用");
        assertTrue(cors.getAllowedOrigins().contains("*"), "默认允许所有源");
        assertNotNull(cors.getAllowedMethods(), "允许方法不应为 null");
        assertFalse(cors.getAllowedMethods().isEmpty(), "允许方法不应为空");
        assertFalse(cors.isAllowCredentials(), "默认不允许凭证");
        assertEquals(3600L, cors.getMaxAge(), "默认 max-age 应为 3600");
    }

    /**
     * 测试自定义 CORS 配置
     */
    @Test
    void testCustomCorsConfig() {
        var props = new ZerxWebProperties();
        var cors = props.getCors();

        cors.setEnabled(false);
        cors.setAllowedOrigins(List.of("http://localhost:3000"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(7200L);

        assertFalse(cors.isEnabled());
        assertEquals("http://localhost:3000", cors.getAllowedOrigins().get(0));
        assertTrue(cors.isAllowCredentials());
        assertEquals(7200L, cors.getMaxAge());
    }

    /**
     * 测试设置自定义 CORS 实例
     */
    @Test
    void testSetCorsInstance() {
        var props = new ZerxWebProperties();
        var customCors = new ZerxWebProperties.Cors();
        customCors.setEnabled(false);
        props.setCors(customCors);

        assertFalse(props.getCors().isEnabled());
    }

    /**
     * 测试暴露的响应头
     */
    @Test
    void testExposedHeaders() {
        var props = new ZerxWebProperties();
        var cors = props.getCors();

        assertNotNull(cors.getExposedHeaders());
        assertTrue(cors.getExposedHeaders().isEmpty(), "默认暴露头应为空");

        cors.setExposedHeaders(List.of("X-Custom-Header"));
        assertEquals(1, cors.getExposedHeaders().size());
        assertEquals("X-Custom-Header", cors.getExposedHeaders().get(0));
    }

    /**
     * 测试允许的请求头
     */
    @Test
    void testAllowedHeaders() {
        var props = new ZerxWebProperties();
        var cors = props.getCors();

        assertTrue(cors.getAllowedHeaders().contains("*"), "默认允许所有头");

        cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        assertEquals(2, cors.getAllowedHeaders().size());
    }
}
