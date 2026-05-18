package com.zerx.spring.web.client.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link OutboundAccessLogInterceptor} 单元测试
 */
class OutboundAccessLogInterceptorTest {

    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

    @Test
    void shouldLogSuccessfulRequest() throws IOException {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor(512);
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setMethod(HttpMethod.POST);
        MockClientHttpResponse response = new MockClientHttpResponse(
                new byte[]{}, HttpStatus.OK);
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request,
                "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8), execution);

        assertNotNull(result);
        verify(execution).execute(any(), any(byte[].class));
    }

    @Test
    void shouldCreateWithDefaultMaxLength() {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor();
        assertNotNull(interceptor);
    }

    @Test
    void shouldCreateWithCustomMaxLength() {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor(2048);
        assertNotNull(interceptor);
    }

    @Test
    void shouldHandleZeroMaxLength() throws IOException {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor(0);
        MockClientHttpRequest request = new MockClientHttpRequest();
        MockClientHttpResponse response = new MockClientHttpResponse(
                "large response body".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertNotNull(result);
    }

    @Test
    void shouldHandleErrorResponse() throws IOException {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor(512);
        MockClientHttpRequest request = new MockClientHttpRequest();
        MockClientHttpResponse response = new MockClientHttpResponse(
                "error message".getBytes(StandardCharsets.UTF_8), HttpStatus.INTERNAL_SERVER_ERROR);
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertNotNull(result);
    }

    @Test
    void shouldHandleEmptyRequestBody() throws IOException {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor(512);
        MockClientHttpRequest request = new MockClientHttpRequest();
        MockClientHttpResponse response = new MockClientHttpResponse(
                new byte[0], HttpStatus.OK);
        when(execution.execute(any(), any(byte[].class))).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        assertNotNull(result);
    }

    @Test
    void shouldHandleNullRequestBody() throws IOException {
        OutboundAccessLogInterceptor interceptor = new OutboundAccessLogInterceptor(512);
        MockClientHttpRequest request = new MockClientHttpRequest();
        MockClientHttpResponse response = new MockClientHttpResponse(
                new byte[0], HttpStatus.OK);
        when(execution.execute(any(HttpRequest.class), any())).thenReturn(response);

        ClientHttpResponse result = interceptor.intercept(request, null, execution);

        assertNotNull(result);
    }
}
