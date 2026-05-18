package com.zerx.spring.web.filter;

import com.zerx.spring.web.properties.ZerxWebProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.web.filter.OncePerRequestFilter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AccessLogFilter 单元测试
 *
 * @author zerx
 */
class AccessLogFilterTest {

    @Nested
    @DisplayName("路径排除逻辑")
    class ShouldNotFilter {

        @Test
        @DisplayName("排除 actuator 路径")
        void shouldExcludeActuator() throws Exception {
            ZerxWebProperties props = new ZerxWebProperties();
            AccessLogFilter filter = new AccessLogFilter(props);
            jakarta.servlet.http.HttpServletRequest request = mockRequest("/actuator/health", "GET");
            assertTrue(invokeShouldNotFilter(filter, request));
        }

        @Test
        @DisplayName("排除精确匹配路径 doc.html")
        void shouldExcludeExactMatch() throws Exception {
            ZerxWebProperties props = new ZerxWebProperties();
            AccessLogFilter filter = new AccessLogFilter(props);
            jakarta.servlet.http.HttpServletRequest request = mockRequest("/doc.html", "GET");
            assertTrue(invokeShouldNotFilter(filter, request));
        }

        @Test
        @DisplayName("排除 swagger-ui 路径")
        void shouldExcludeSwagger() throws Exception {
            ZerxWebProperties props = new ZerxWebProperties();
            AccessLogFilter filter = new AccessLogFilter(props);
            jakarta.servlet.http.HttpServletRequest request = mockRequest("/swagger-ui/index.html", "GET");
            assertTrue(invokeShouldNotFilter(filter, request));
        }

        @Test
        @DisplayName("不排除 API 路径")
        void shouldNotExcludeApi() throws Exception {
            ZerxWebProperties props = new ZerxWebProperties();
            AccessLogFilter filter = new AccessLogFilter(props);
            jakarta.servlet.http.HttpServletRequest request = mockRequest("/api/users", "GET");
            assertFalse(invokeShouldNotFilter(filter, request));
        }

        @Test
        @DisplayName("空排除列表不过滤任何路径")
        void shouldNotFilterWhenEmptyList() throws Exception {
            ZerxWebProperties props = new ZerxWebProperties();
            props.getAccessLog().setExcludeUrls(List.of());
            AccessLogFilter filter = new AccessLogFilter(props);
            jakarta.servlet.http.HttpServletRequest request = mockRequest("/actuator/health", "GET");
            assertFalse(invokeShouldNotFilter(filter, request));
        }
    }

    @Nested
    @DisplayName("脱敏配置")
    class SensitiveConfig {

        @Test
        @DisplayName("默认包含常见敏感参数")
        void shouldHaveDefaultSensitiveParams() {
            ZerxWebProperties.AccessLog accessLog = new ZerxWebProperties.AccessLog();
            assertTrue(accessLog.getSensitiveParams().contains("password"));
            assertTrue(accessLog.getSensitiveParams().contains("token"));
            assertTrue(accessLog.getSensitiveParams().contains("secret"));
        }

        @Test
        @DisplayName("默认排除常见路径")
        void shouldHaveDefaultExcludeUrls() {
            ZerxWebProperties.AccessLog accessLog = new ZerxWebProperties.AccessLog();
            assertTrue(accessLog.getExcludeUrls().stream().anyMatch(u -> u.contains("actuator")));
            assertTrue(accessLog.getExcludeUrls().stream().anyMatch(u -> u.contains("swagger")));
        }

        @Test
        @DisplayName("默认慢请求阈值 3000ms")
        void shouldHaveDefaultSlowThreshold() {
            ZerxWebProperties.AccessLog accessLog = new ZerxWebProperties.AccessLog();
            assertEquals(3000L, accessLog.getSlowThresholdMs());
        }

        @Test
        @DisplayName("默认启用")
        void shouldBeEnabledByDefault() {
            ZerxWebProperties.AccessLog accessLog = new ZerxWebProperties.AccessLog();
            assertTrue(accessLog.isEnabled());
        }
    }

    @Nested
    @DisplayName("Properties 配置")
    class PropertiesConfig {

        @Test
        @DisplayName("Jackson 默认配置")
        void shouldHaveJacksonDefaults() {
            ZerxWebProperties.Jackson jackson = new ZerxWebProperties.Jackson();
            assertEquals("yyyy-MM-dd HH:mm:ss", jackson.getDateFormat());
            assertFalse(jackson.isIncludeNull());
        }

        @Test
        @DisplayName("Jackson 自定义配置生效")
        void shouldApplyCustomJacksonConfig() {
            ZerxWebProperties props = new ZerxWebProperties();
            props.getJackson().setDateFormat("yyyy/MM/dd");
            props.getJackson().setIncludeNull(true);
            assertEquals("yyyy/MM/dd", props.getJackson().getDateFormat());
            assertTrue(props.getJackson().isIncludeNull());
        }

        @Test
        @DisplayName("AccessLog 自定义慢请求阈值")
        void shouldApplyCustomSlowThreshold() {
            ZerxWebProperties props = new ZerxWebProperties();
            props.getAccessLog().setSlowThresholdMs(5000);
            assertEquals(5000L, props.getAccessLog().getSlowThresholdMs());
        }
    }

    // ==================== 辅助方法 ====================

    private jakarta.servlet.http.HttpServletRequest mockRequest(String uri, String method) {
        return new org.springframework.mock.web.MockHttpServletRequest(null, uri);
    }

    private boolean invokeShouldNotFilter(AccessLogFilter filter,
                                           jakarta.servlet.http.HttpServletRequest request) throws Exception {
        Method m = OncePerRequestFilter.class.getDeclaredMethod(
                "shouldNotFilter", jakarta.servlet.http.HttpServletRequest.class);
        m.setAccessible(true);
        return (boolean) m.invoke(filter, request);
    }
}
