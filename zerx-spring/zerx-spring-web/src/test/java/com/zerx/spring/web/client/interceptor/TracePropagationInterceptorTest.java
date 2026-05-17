package com.zerx.spring.web.client.interceptor;

import com.zerx.spring.web.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link TracePropagationInterceptor} 单元测试
 */
class TracePropagationInterceptorTest {

    private final TracePropagationInterceptor interceptor = new TracePropagationInterceptor();
    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final MockClientHttpRequest request = new MockClientHttpRequest();
    private final MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void shouldPropagateTraceIdFromContext() throws IOException {
        RequestContext.init();
        RequestContext.setTraceId("trace-abc-123");
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("trace-abc-123", request.getHeaders().getFirst("X-Trace-Id"));
        verify(execution).execute(any(HttpRequest.class), any(byte[].class));
    }

    @Test
    void shouldNotOverwriteExistingTraceId() throws IOException {
        RequestContext.init();
        RequestContext.setTraceId("new-trace-456");
        request.getHeaders().add("X-Trace-Id", "original-trace");
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("original-trace", request.getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void shouldSkipWhenNoRequestContext() throws IOException {
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void shouldSkipWhenTraceIdIsNull() throws IOException {
        RequestContext.init();
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst("X-Trace-Id"));
    }

    @Test
    void shouldSkipWhenTraceIdIsBlank() throws IOException {
        RequestContext.init();
        RequestContext.setTraceId("   ");
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst("X-Trace-Id"));
    }
}
