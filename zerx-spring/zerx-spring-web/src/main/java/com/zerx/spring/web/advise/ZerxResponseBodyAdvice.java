package com.zerx.spring.web.advise;

import com.zerx.common.model.Result;
import com.zerx.spring.web.annotation.ZerxResponseResult;
import com.zerx.spring.web.properties.ZerxWebProperties;
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

import java.lang.reflect.Method;

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
 *   <li>如果返回值类型为 {@code String}，特殊处理以避免 {@link StringHttpMessageConverter} 的类型转换问题</li>
 *   <li>其他类型统一包装为 {@code Result<T>.ok(data)}</li>
 * </ul>
 *
 * <h3>排除规则：</h3>
 * <ul>
 *   <li>配置 {@code zerx.web.response-wrap-enabled=false} 可全局关闭</li>
 *   <li>Controller 所在包匹配 {@code zerx.web.response-wrap-exclude-packages} 时跳过</li>
 * </ul>
 *
 * @author zerx
 */
public class ZerxResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger log = LoggerFactory.getLogger(ZerxResponseBodyAdvice.class);

    private final ZerxWebProperties properties;

    /**
     * 构造统一响应体增强
     *
     * @param properties Web 模块配置属性
     */
    public ZerxResponseBodyAdvice(ZerxWebProperties properties) {
        this.properties = properties;
    }

    /**
     * 判断是否需要对当前 Controller 方法进行响应体包装
     * <p>
     * 仅当以下条件同时满足时才启用包装：
     * <ol>
     *   <li>{@code responseWrapEnabled} 为 {@code true}</li>
     *   <li>Controller 类标注了 {@code @RestController} 或 {@code @ZerxResponseResult}</li>
     *   <li>Controller 所在包不在排除列表中</li>
     * </ol>
     * </p>
     *
     * @param returnType    方法返回类型
     * @param converterType 消息转换器类型
     * @return 需要包装返回 {@code true}
     */
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

        // 检查是否在排除包列表中
        String packageName = declaringClass.getPackage().getName();
        return properties.getResponseWrapExcludePackages().stream()
                .noneMatch(packageName::startsWith);
    }

    /**
     * 对返回值进行统一响应包装
     *
     * @param body                  原始返回值
     * @param returnType            方法返回类型
     * @param selectedContentType   选中的内容类型
     * @param selectedConverterType 选中的消息转换器类型
     * @param request               当前 HTTP 请求
     * @param response              当前 HTTP 响应
     * @return 包装后的响应体
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 已经是 Result 类型，不做二次包装
        if (body instanceof Result<?>) {
            return body;
        }

        // void 返回类型，body 为 null
        Method method = returnType.getMethod();
        if (method != null && method.getReturnType() == void.class) {
            return Result.ok();
        }

        // String 类型特殊处理：StringHttpMessageConverter 无法处理非 String 类型
        if (body instanceof String) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            try {
                // 将 Result 序列化为 JSON 字符串返回
                return toJson(Result.ok(body));
            } catch (Exception e) {
                log.error("Failed to serialize Result for String return type", e);
                return Result.ok(body);
            }
        }

        return Result.ok(body);
    }

    /**
     * 将对象转换为 JSON 字符串（简易实现）
     *
     * @param obj 待序列化对象
     * @return JSON 字符串
     */
    private String toJson(Object obj) {
        if (obj instanceof Result<?> result) {
            return """
                    {"success":%s,"code":"%s","message":"%s","data":%s}"""
                    .formatted(
                            result.success(),
                            escapeJson(result.code()),
                            escapeJson(result.message()),
                            toJsonValue(result.data())
                    );
        }
        return "{}";
    }

    /**
     * JSON 转义
     *
     * @param value 原始字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 将值序列化为 JSON 值部分
     *
     * @param data 数据值
     * @return JSON 值字符串
     */
    private String toJsonValue(Object data) {
        if (data == null) {
            return "null";
        }
        if (data instanceof String s) {
            return "\"" + escapeJson(s) + "\"";
        }
        if (data instanceof Number || data instanceof Boolean) {
            return data.toString();
        }
        // 对复杂对象，使用 toString 作为简易处理
        return "\"" + escapeJson(data.toString()) + "\"";
    }
}
