package com.zerx.spring.web;

import com.zerx.common.model.Result;
import com.zerx.spring.web.advise.ZerxResponseBodyAdvice;
import com.zerx.spring.web.annotation.ZerxResponseResult;
import com.zerx.spring.web.properties.ZerxWebProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 统一响应体增强测试
 *
 * @author zerx
 */
class ZerxResponseBodyAdviceTest {

    private ZerxResponseBodyAdvice advice;
    private ZerxWebProperties properties;
    private ServerHttpRequest request;
    private ServerHttpResponse response;

    @BeforeEach
    void setUp() throws Exception {
        properties = new ZerxWebProperties();
        properties.setResponseWrapEnabled(true);
        advice = new ZerxResponseBodyAdvice(properties);

        request = mock(ServerHttpRequest.class);
        response = mock(ServerHttpResponse.class);
        when(response.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
    }

    // ======================== supports 测试 ========================

    @Test
    void supports_shouldReturnTrueForRestController() throws Exception {
        Method method = TestRestController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        assertTrue(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void supports_shouldReturnTrueForZerxResponseResult() throws Exception {
        Method method = AnnotatedController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        assertTrue(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void supports_shouldReturnFalseWhenDisabled() throws Exception {
        properties.setResponseWrapEnabled(false);
        Method method = TestRestController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        assertFalse(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));
    }

    @Test
    void supports_shouldReturnFalseForExcludedPackage() throws Exception {
        properties.setResponseWrapExcludePackages(java.util.List.of("com.zerx.spring.web"));
        Method method = TestRestController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        assertFalse(advice.supports(returnType, MappingJackson2HttpMessageConverter.class));
    }

    // ======================== beforeBodyWrite 测试 ========================

    @Test
    void beforeBodyWrite_shouldWrapObject() throws Exception {
        Method method = TestRestController.class.getMethod("getCount");
        MethodParameter returnType = new MethodParameter(method, -1);

        Object result = advice.beforeBodyWrite(42, returnType,
                MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class,
                request, response);

        assertInstanceOf(Result.class, result);
        @SuppressWarnings("unchecked")
        Result<Integer> r = (Result<Integer>) result;
        assertTrue(r.isSuccess());
        assertEquals(42, r.data());
    }

    @Test
    void beforeBodyWrite_shouldNotDoubleWrapResult() throws Exception {
        Method method = TestRestController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        Result<String> original = Result.ok("already-wrapped");
        Object result = advice.beforeBodyWrite(original, returnType,
                MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class,
                request, response);

        assertSame(original, result, "已经是 Result 类型，不应二次包装");
    }

    @Test
    void beforeBodyWrite_shouldHandleStringSpecially() throws Exception {
        Method method = TestRestController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        Object result = advice.beforeBodyWrite("hello", returnType,
                MediaType.APPLICATION_JSON, StringHttpMessageConverter.class,
                request, response);

        // String 返回类型会被序列化为 JSON 字符串
        assertInstanceOf(String.class, result);
        assertTrue(((String) result).contains("\"hello\""));
    }

    @Test
    void beforeBodyWrite_shouldHandleNullBody() throws Exception {
        Method method = TestRestController.class.getMethod("getData");
        MethodParameter returnType = new MethodParameter(method, -1);

        Object result = advice.beforeBodyWrite(null, returnType,
                MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class,
                request, response);

        assertInstanceOf(Result.class, result);
        @SuppressWarnings("unchecked")
        Result<Object> r = (Result<Object>) result;
        assertTrue(r.isSuccess());
        assertNull(r.data());
    }

    @Test
    void beforeBodyWrite_shouldHandleVoidMethod() throws Exception {
        Method method = TestRestController.class.getMethod("voidMethod");
        MethodParameter returnType = new MethodParameter(method, -1);

        Object result = advice.beforeBodyWrite(null, returnType,
                MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class,
                request, response);

        assertInstanceOf(Result.class, result);
        @SuppressWarnings("unchecked")
        Result<Object> r = (Result<Object>) result;
        assertTrue(r.isSuccess());
    }

    @Test
    void beforeBodyWrite_shouldWrapInteger() throws Exception {
        Method method = TestRestController.class.getMethod("getCount");
        MethodParameter returnType = new MethodParameter(method, -1);

        Object result = advice.beforeBodyWrite(42, returnType,
                MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class,
                request, response);

        assertInstanceOf(Result.class, result);
        @SuppressWarnings("unchecked")
        Result<Integer> r = (Result<Integer>) result;
        assertTrue(r.isSuccess());
        assertEquals(42, r.data());
    }

    // ======================== 测试用 Controller ========================

    @RestController
    static class TestRestController {
        public String getData() {
            return "test";
        }

        public String getString() {
            return "hello";
        }

        public void voidMethod() {
            // void return type
        }

        public int getCount() {
            return 42;
        }
    }

    @ZerxResponseResult
    static class AnnotatedController {
        public String getData() {
            return "test";
        }
    }
}
