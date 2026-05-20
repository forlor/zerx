package com.zerx.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解 — 基于滑动窗口的请求频率限制。
 * <p>
 * 标注在 Controller 方法上，限制单位时间内的最大请求数。
 * 超出限制时抛出 {@link com.zerx.common.exception.BusinessException}，
 * 错误码 {@link com.zerx.common.exception.ErrorCode#TOO_MANY_REQUESTS}，HTTP 429。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 默认：每秒最多 100 次请求（按 IP 限流）
 * @RateLimit
 * public Result<Void> list() { ... }
 *
 * // 自定义：每 10 秒最多 50 次请求，按用户维度限流
 * @RateLimit(key = "#userId", capacity = 50, windowSeconds = 10)
 * public Result<Void> query(Long userId) { ... }
 * }</pre>
 *
 * @author zerx
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key 的 SpEL 表达式，支持引用方法参数。
     * <p>
     * 为空时使用请求 URI + 方法名作为 key。
     * 最终的限流 key 会自动拼接请求 IP 前缀，实现按 IP 维度的限流。
     * </p>
     */
    String key() default "";

    /**
     * 时间窗口内的最大请求数
     */
    int capacity() default 100;

    /**
     * 时间窗口（秒）
     */
    int windowSeconds() default 1;
}
