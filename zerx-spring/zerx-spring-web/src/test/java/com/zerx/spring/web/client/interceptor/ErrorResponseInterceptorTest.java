package com.zerx.spring.web.client.interceptor;

import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ExternalServiceException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
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
 * {@link ErrorResponseInterceptor} 单元测试
 */
class ErrorResponseInterceptorTest {

    private final ErrorResponseInterceptor interceptor = new ErrorResponseInterceptor();
    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
    private final MockClientHttpRequest request = new MockClientHttpRequest();

    @Nested
    class SuccessfulResponses {

        @Test
        void shouldPassThrough200Response() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    "{\"ok\":true}".getBytes(StandardCharsets.UTF_8), HttpStatus.OK);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertNotNull(result);
            assertEquals(200, result.getStatusCode().value());
        }

        @Test
        void shouldPassThrough201Response() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    new byte[0], HttpStatus.CREATED);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

            assertNotNull(result);
            assertEquals(201, result.getStatusCode().value());
        }
    }

    @Nested
    class ServerErrors {

        @Test
        void shouldThrowExternalServiceExceptionFor500() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    "Internal Server Error".getBytes(StandardCharsets.UTF_8), HttpStatus.INTERNAL_SERVER_ERROR);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                    () -> interceptor.intercept(request, new byte[0], execution));

            assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
            assertNotNull(ex.getServiceName());
        }

        @Test
        void shouldThrowExternalServiceExceptionFor502() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    "Bad Gateway".getBytes(StandardCharsets.UTF_8), HttpStatus.BAD_GATEWAY);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                    () -> interceptor.intercept(request, new byte[0], execution));

            assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
        }

        @Test
        void shouldThrowRateLimitExceptionFor429() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    "Too Many Requests".getBytes(StandardCharsets.UTF_8), HttpStatus.TOO_MANY_REQUESTS);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                    () -> interceptor.intercept(request, new byte[0], execution));

            assertEquals(ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT, ex.getErrorCode());
        }
    }

    @Nested
    class ClientErrors {

        @Test
        void shouldThrowDataErrorExceptionFor400() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    "Bad Request".getBytes(StandardCharsets.UTF_8), HttpStatus.BAD_REQUEST);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                    () -> interceptor.intercept(request, new byte[0], execution));

            assertEquals(ErrorCode.EXTERNAL_SERVICE_DATA_ERROR, ex.getErrorCode());
        }

        @Test
        void shouldThrowExternalServiceExceptionFor404() throws Exception {
            MockClientHttpResponse response = new MockClientHttpResponse(
                    "Not Found".getBytes(StandardCharsets.UTF_8), HttpStatus.NOT_FOUND);
            when(execution.execute(any(), any(byte[].class))).thenReturn(response);

            ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                    () -> interceptor.intercept(request, new byte[0], execution));

            assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    class NetworkErrors {

        @Test
        void shouldThrowTimeoutException() {
            ResourceAccessException timeoutException = new ResourceAccessException(
                    "I/O error on GET request: Connection timed out",
                    new java.net.ConnectException("Connection timed out"));
            try {
                doThrow(timeoutException).when(execution).execute(any(HttpRequest.class), any(byte[].class));
            } catch (IOException ignored) { }

            assertThrows(ExternalServiceException.class, () -> {
                try {
                    interceptor.intercept(request, new byte[0], execution);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Test
        void shouldThrowNetworkExceptionForNonTimeout() {
            ResourceAccessException networkException = new ResourceAccessException(
                    "I/O error: Connection refused",
                    new java.net.ConnectException("Connection refused"));
            try {
                doThrow(networkException).when(execution).execute(any(HttpRequest.class), any(byte[].class));
            } catch (IOException ignored) { }

            assertThrows(ExternalServiceException.class, () -> {
                try {
                    interceptor.intercept(request, new byte[0], execution);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Test
        void shouldHandleReadTimeout() {
            ResourceAccessException readTimeout = new ResourceAccessException("Read timed out");
            try {
                doThrow(readTimeout).when(execution).execute(any(HttpRequest.class), any(byte[].class));
            } catch (IOException ignored) { }

            assertThrows(ExternalServiceException.class, () -> {
                try {
                    interceptor.intercept(request, new byte[0], execution);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
