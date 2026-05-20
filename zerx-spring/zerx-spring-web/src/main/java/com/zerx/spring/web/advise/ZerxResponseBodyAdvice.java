package com.zerx.spring.web.advise;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.model.Result;
import com.zerx.spring.web.annotation.ZerxResponseResult;
import com.zerx.spring.web.properties.ZerxWebProperties;

/**
 * 统一响应体增强 — 自动包装返回值为 {@code Result<T>}
 * <p>
 * 拦截所有标注了 {@code @RestController} 或 {@code @ZerxResponseResult} 的 Controller 方法，
 * 将其返回值自动包装为 {@link Result} 统一响应格式。
 * </p>
 *
 * <h3>包装规则：</h3>
 * <ul>
 *   <li>如果返回值已经是 {@code Result<?>} 类型，直接返回不做二次包装</li>
 *   <li>如果返回值类型为 {@code void}，包装为 {@code Result<Void>.ok()}</li>
 *   <li>如果返回值类型为 {@code String}，使用 ObjectMapper 序列化为 JSON 字符串</li>
 *   <li>其他类型统一包装为 {@code Result<T>.ok(data)}</li>
 * </ul>
 *
 * @author zerx
 */
public class ZerxResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger LOG = LoggerFactory.getLogger(ZerxResponseBodyAdvice.class);

    private final ZerxWebProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 构造统一响应体增强
     *
     * @param properties   Web 模块配置属性
     * @param objectMapper JSON 序列化器（使用 Spring 全局配置的实例）
     */
    public ZerxResponseBodyAdvice(ZerxWebProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!properties.isResponseWrapEnabled()) {
            return false;
        }

        Class<?> declaringClass = returnType.getDeclaringClass();
        boolean isRestController = declaringClass.isAnnotationPresent(RestController.class);
        boolean hasAnnotation = declaringClass.isAnnotationPresent(ZerxResponseResult.class);

        if (!isRestController && !hasAnnotation) {
            return false;
        }

        String packageName = declaringClass.getPackage().getName();
        return properties.getResponseWrapExcludePackages().stream()
                .noneMatch(packageName::startsWith);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result<?>) {
            return body;
        }

        Method method = returnType.getMethod();
        if (method != null && method.getReturnType() == void.class) {
            return Result.ok();
        }

        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                return objectMapper.writeValueAsString(Result.ok(body));
            } catch (Exception e) {
                LOG.error("Failed to serialize Result for String return type", e);
                return Result.ok(body);
            }
        }

        return Result.ok(body);
    }
}
