package com.zerx.spring.web;

import com.zerx.spring.web.context.RequestContext;
import com.zerx.spring.web.filter.TraceFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 链路追踪过滤器测试
 * <p>
 * 由于 TraceFilter 在 finally 块中清理 RequestContext 和 MDC，
 * 需要在 FilterChain 执行期间通过 Answer 捕获状态进行断言。
 * </p>
 *
 * @author zerx
 */
class TraceFilterTest {

    /** 用于暴露 protected doFilterInternal 的测试子类 */
    static class TestableTraceFilter extends TraceFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {
            super.doFilterInternal(request, response, filterChain);
        }
    }

    private TestableTraceFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private AtomicReference<String> capturedTraceId;
    private AtomicReference<String> capturedRequestId;
    private AtomicReference<String> capturedMdcTraceId;

    @BeforeEach
    void setUp() throws ServletException, IOException {
        filter = new TestableTraceFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);

        capturedTraceId = new AtomicReference<>();
        capturedRequestId = new AtomicReference<>();
        capturedMdcTraceId = new AtomicReference<>();

        // FilterChain 在执行时捕获当前 RequestContext 和 MDC 状态
        filterChain = (req, resp) -> {
            capturedTraceId.set(RequestContext.getTraceId());
            capturedRequestId.set(RequestContext.getRequestId());
            capturedMdcTraceId.set(MDC.get("traceId"));
        };

        RequestContext.clear();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
        MDC.clear();
    }

    @Test
    void doFilter_shouldGenerateTraceId() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        String traceId = capturedTraceId.get();
        assertNotNull(traceId, "应生成 TraceID");
        assertFalse(traceId.isEmpty());
    }

    @Test
    void doFilter_shouldUseExistingTraceIdFromHeader() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn("existing-trace-123");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("existing-trace-123", capturedTraceId.get(),
                "应使用请求头中的 TraceID");
    }

    @Test
    void doFilter_shouldTrimTraceIdFromHeader() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn("  padded-trace  ");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("padded-trace", capturedTraceId.get(),
                "应去除 TraceID 前后空白");
    }

    @Test
    void doFilter_shouldSetMdc() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn("mdc-trace-001");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("mdc-trace-001", capturedMdcTraceId.get(),
                "TraceID 应设置到 MDC");
    }

    @Test
    void doFilter_shouldSetResponseHeader() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn("response-trace-001");

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader("X-Trace-Id", "response-trace-001");
    }

    @Test
    void doFilter_shouldProceedFilterChain() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        // filterChain was invoked (captured values are not null)
        assertNotNull(capturedTraceId.get());
    }

    @Test
    void doFilter_shouldInitializeRequestId() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(capturedRequestId.get(), "应生成 requestId");
    }

    @Test
    void doFilter_shouldCleanupOnCompletion() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn("cleanup-test-001");

        filter.doFilterInternal(request, response, filterChain);

        // 过滤器完成后应清理 RequestContext 和 MDC
        assertNull(RequestContext.get(), "请求完成后应清理 RequestContext");
        assertNull(MDC.get("traceId"), "请求完成后应清理 MDC");
    }

    @Test
    void doFilter_shouldCleanupEvenOnException() throws ServletException, IOException {
        FilterChain throwingChain = (req, resp) -> {
            throw new RuntimeException("test error");
        };

        when(request.getHeader("X-Trace-Id")).thenReturn("exception-test-001");

        assertThrows(RuntimeException.class, () ->
                filter.doFilterInternal(request, response, throwingChain));

        // 即使抛异常也应清理
        assertNull(RequestContext.get(), "异常后应清理 RequestContext");
        assertNull(MDC.get("traceId"), "异常后应清理 MDC");
    }

    @Test
    void doFilter_shouldIgnoreBlankTraceIdHeader() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn("   ");

        filter.doFilterInternal(request, response, filterChain);

        String traceId = capturedTraceId.get();
        assertNotNull(traceId);
        // 空白头应被忽略，使用生成的
        assertFalse(traceId.isBlank());
    }

    @Test
    void doFilter_shouldGenerate32CharTraceId() throws ServletException, IOException {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        String traceId = capturedTraceId.get();
        assertEquals(32, traceId.length(), "UUID 去除连字符后应为 32 个字符");
        assertTrue(traceId.matches("[a-f0-9]+"), "应为十六进制字符串");
    }
}
