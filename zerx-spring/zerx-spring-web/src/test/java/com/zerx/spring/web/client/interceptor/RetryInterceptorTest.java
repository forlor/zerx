package com.zerx.spring.web.client.interceptor;

import com.zerx.spring.web.properties.ZerxWebProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link RetryInterceptor} 单元测试
 */
class RetryInterceptorTest {

    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final MockClientHttpRequest request = new MockClientHttpRequest();

    @Nested
    class DisabledRetry {

        @Test
        void shouldNotRetryWhenMaxRetriesIsZero() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(0);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse successResponse = new MockClientHttpResponse(
                    new byte[0], HttpStatus.OK);
            when(execution.execute(any(), any(byte[].class))).thenReturn(successResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertNotNull(result);
            verify(execution, times(1)).execute(any(), any(byte[].class));
        }
    }

    @Nested
    class ServerErrorRetry {

        @Test
        void shouldRetryOn500AndSucceedEventually() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(2);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse failResponse = new MockClientHttpResponse(
                    "Internal Server Error".getBytes(StandardCharsets.UTF_8), HttpStatus.INTERNAL_SERVER_ERROR);
            MockClientHttpResponse successResponse = new MockClientHttpResponse(
                    new byte[0], HttpStatus.OK);

            when(execution.execute(any(), any(byte[].class)))
                    .thenReturn(failResponse)
                    .thenReturn(successResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertNotNull(result);
            assertEquals(200, result.getStatusCode().value());
            verify(execution, times(2)).execute(any(), any(byte[].class));
        }

        @Test
        void shouldRetryOn503AndSucceedOnThirdAttempt() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(3);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse failResponse = new MockClientHttpResponse(
                    new byte[0], HttpStatus.SERVICE_UNAVAILABLE);
            MockClientHttpResponse successResponse = new MockClientHttpResponse(
                    new byte[0], HttpStatus.OK);

            when(execution.execute(any(), any(byte[].class)))
                    .thenReturn(failResponse)
                    .thenReturn(failResponse)
                    .thenReturn(successResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertEquals(200, result.getStatusCode().value());
            verify(execution, times(3)).execute(any(), any(byte[].class));
        }

        @Test
        void shouldRetryOn429RateLimit() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(2);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse rateLimitResponse = new MockClientHttpResponse(
                    "Too Many Requests".getBytes(StandardCharsets.UTF_8), HttpStatus.TOO_MANY_REQUESTS);
            MockClientHttpResponse successResponse = new MockClientHttpResponse(
                    new byte[0], HttpStatus.OK);

            when(execution.execute(any(), any(byte[].class)))
                    .thenReturn(rateLimitResponse)
                    .thenReturn(successResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertEquals(200, result.getStatusCode().value());
            verify(execution, times(2)).execute(any(), any(byte[].class));
        }

        @Test
        void shouldReturnAfterMaxRetriesExhausted() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(2);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse failResponse = new MockClientHttpResponse(
                    "error".getBytes(StandardCharsets.UTF_8), HttpStatus.INTERNAL_SERVER_ERROR);

            when(execution.execute(any(), any(byte[].class)))
                    .thenReturn(failResponse)
                    .thenReturn(failResponse)
                    .thenReturn(failResponse);

            // 重试耗尽后返回最后一次的 500 响应，由 ErrorResponseInterceptor 处理
            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);
            assertEquals(500, result.getStatusCode().value());
            verify(execution, times(3)).execute(any(), any(byte[].class));
        }
    }

    @Nested
    class NetworkErrorRetry {

        @Test
        void shouldRetryOnConnectionTimeout() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(2);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            ResourceAccessException timeoutException = new ResourceAccessException(
                    "Connection timed out", new java.net.ConnectException("timeout"));
            MockClientHttpResponse successResponse = new MockClientHttpResponse(
                    new byte[0], HttpStatus.OK);

            when(execution.execute(any(), any(byte[].class)))
                    .thenThrow(timeoutException)
                    .thenReturn(successResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertEquals(200, result.getStatusCode().value());
            verify(execution, times(2)).execute(any(), any(byte[].class));
        }
    }

    @Nested
    class NonRetryableErrors {

        @Test
        void shouldNotRetryOnClientErrors() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(2);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse badRequestResponse = new MockClientHttpResponse(
                    "Bad Request".getBytes(StandardCharsets.UTF_8), HttpStatus.BAD_REQUEST);

            when(execution.execute(any(), any(byte[].class)))
                    .thenReturn(badRequestResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertEquals(400, result.getStatusCode().value());
            verify(execution, times(1)).execute(any(), any(byte[].class));
        }

        @Test
        void shouldNotRetryOn404() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(2);
            config.setRetryInitialDelayMs(10);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse notFoundResponse = new MockClientHttpResponse(
                    "Not Found".getBytes(StandardCharsets.UTF_8), HttpStatus.NOT_FOUND);

            when(execution.execute(any(), any(byte[].class)))
                    .thenReturn(notFoundResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertEquals(404, result.getStatusCode().value());
            verify(execution, times(1)).execute(any(), any(byte[].class));
        }
    }

    @Nested
    class SuccessfulNoRetry {

        @Test
        void shouldNotRetryOnFirstSuccess() throws IOException {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setMaxRetries(3);
            RetryInterceptor interceptor = new RetryInterceptor(config);

            MockClientHttpResponse successResponse = new MockClientHttpResponse(
                    "OK".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
            when(execution.execute(any(), any(byte[].class))).thenReturn(successResponse);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertEquals(200, result.getStatusCode().value());
            verify(execution, times(1)).execute(any(), any(byte[].class));
        }
    }
}
