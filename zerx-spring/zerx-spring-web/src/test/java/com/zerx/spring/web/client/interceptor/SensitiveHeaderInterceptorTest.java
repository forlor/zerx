package com.zerx.spring.web.client.interceptor;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SensitiveHeaderInterceptor} 单元测试
 */
class SensitiveHeaderInterceptorTest {

    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);

    @Nested
    class DefaultSensitiveHeaders {

        private final SensitiveHeaderInterceptor interceptor = new SensitiveHeaderInterceptor();
        private final MockClientHttpRequest request = new MockClientHttpRequest();

        @Test
        void shouldMaskAuthorizationBearerToken() throws IOException {
            request.getHeaders().add("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.abc123");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("Bearer ******", request.getHeaders().getFirst("Authorization"));
        }

        @Test
        void shouldMaskAuthorizationBasicAuth() throws IOException {
            request.getHeaders().add("Authorization", "Basic dXNlcjpwYXNz");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("******", request.getHeaders().getFirst("Authorization"));
        }

        @Test
        void shouldMaskXTokenHeader() throws IOException {
            request.getHeaders().add("X-Token", "some-secret-token-value");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("******", request.getHeaders().getFirst("X-Token"));
        }

        @Test
        void shouldMaskXApiKeyHeader() throws IOException {
            request.getHeaders().add("X-Api-Key", "sk-1234567890abcdef");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("******", request.getHeaders().getFirst("X-Api-Key"));
        }

        @Test
        void shouldNotMaskNonSensitiveHeaders() throws IOException {
            request.getHeaders().add("Content-Type", "application/json");
            request.getHeaders().add("Accept", "application/json");
            request.getHeaders().add("X-Custom", "some-value");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("application/json", request.getHeaders().getFirst("Content-Type"));
            assertEquals("application/json", request.getHeaders().getFirst("Accept"));
            assertEquals("some-value", request.getHeaders().getFirst("X-Custom"));
        }

        @Test
        void shouldHandleNullHeaderValue() throws IOException {
            request.getHeaders().add("Authorization", (String) null);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("******", request.getHeaders().getFirst("Authorization"));
        }

        @Test
        void shouldHandleEmptyHeaderValue() throws IOException {
            request.getHeaders().add("Authorization", "");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("******", request.getHeaders().getFirst("Authorization"));
        }

        @Test
        void shouldMaskCaseInsensitive() throws IOException {
            request.getHeaders().add("AUTHORIZATION", "Bearer token123");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("Bearer ******", request.getHeaders().getFirst("AUTHORIZATION"));
        }

        @Test
        void shouldHandleEmptyHeaders() throws IOException {
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            verify(execution).execute(any(), any(byte[].class));
        }
    }

    @Nested
    class CustomSensitiveHeaders {

        @Test
        void shouldOnlyMaskConfiguredHeaders() throws IOException {
            SensitiveHeaderInterceptor interceptor = new SensitiveHeaderInterceptor(
                    Set.of("x-my-secret"));
            MockClientHttpRequest request = new MockClientHttpRequest();
            request.getHeaders().add("X-My-Secret", "secret-value");
            request.getHeaders().add("Authorization", "Bearer should-not-be-masked");
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            interceptor.intercept(request, new byte[0], execution);

            assertEquals("******", request.getHeaders().getFirst("X-My-Secret"));
            assertEquals("Bearer should-not-be-masked", request.getHeaders().getFirst("Authorization"));
        }
    }
}
