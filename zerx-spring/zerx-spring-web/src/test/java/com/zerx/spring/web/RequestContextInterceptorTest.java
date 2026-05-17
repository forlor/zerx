package com.zerx.spring.web;

import com.zerx.spring.web.context.RequestContext;
import com.zerx.spring.web.interceptor.RequestContextInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RequestContextInterceptor} 单元测试
 *
 * @author zerx
 */
class RequestContextInterceptorTest {

    private RequestContextInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RequestContextInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        RequestContext.clear();
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void preHandle_extractsIpFromXForwardedFor_singleIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.50");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("203.0.113.50");
    }

    @Test
    void preHandle_extractsFirstIpFromXForwardedFor_multipleIps() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 70.41.3.18");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("203.0.113.1");
    }

    @Test
    void preHandle_fallsBackToXRealIp_whenXForwardedForMissing() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void preHandle_fallsBackToRemoteAddr_whenBothHeadersMissing() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("192.168.1.100");
    }

    @Test
    void preHandle_fallsBackToXRealIp_whenXForwardedForIsBlank() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getHeader("X-Real-IP")).thenReturn("172.16.0.1");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("172.16.0.1");
    }

    @Test
    void preHandle_initializesRequestContext_whenNull() {
        // RequestContext is null due to @BeforeEach clear()
        assertThat(RequestContext.get()).isNull();

        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.get()).isNotNull();
        assertThat(RequestContext.getRequestIp()).isEqualTo("1.2.3.4");
    }

    @Test
    void preHandle_reusesExistingRequestContext_whenAlreadyInitialized() {
        // Simulate TraceFilter having already initialized RequestContext
        RequestContext.init();
        RequestContext.setTraceId("existing-trace-id");

        when(request.getHeader("X-Forwarded-For")).thenReturn("5.6.7.8");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        // The same context instance should be reused (traceId preserved)
        assertThat(RequestContext.getTraceId()).isEqualTo("existing-trace-id");
        assertThat(RequestContext.getRequestIp()).isEqualTo("5.6.7.8");
    }

    @Test
    void afterCompletion_doesNotThrow() {
        assertThatCode(() -> interceptor.afterCompletion(request, response, new Object(), null))
                .doesNotThrowAnyException();
    }

    @Test
    void afterCompletion_doesNotThrowWithException() {
        var ex = new RuntimeException("test error");

        assertThatCode(() -> interceptor.afterCompletion(request, response, new Object(), ex))
                .doesNotThrowAnyException();
    }

    @Test
    void preHandle_trimsWhitespaceFromIpValues() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.100  ");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("203.0.113.100");
    }

    @Test
    void preHandle_trimsFirstIpFromCommaSeparatedList() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.1 , 70.41.3.18");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(RequestContext.getRequestIp()).isEqualTo("203.0.113.1");
    }
}
