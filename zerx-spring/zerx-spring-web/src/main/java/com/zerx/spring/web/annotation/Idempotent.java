package com.zerx.spring.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 请求幂等性注解 — 防止重复提交。
 * <p>
 * 标注在 Controller 方法上，在指定时间窗口内，相同请求只会被执行一次。
 * 基于请求指纹（userId + URI + 参数 hash）实现去重。
 * 重复请求时抛出 {@link com.zerx.common.exception.BusinessException}，
 * 错误码 {@link com.zerx.common.exception.ErrorCode#STATE_CONFLICT}，HTTP 409。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 默认：5 秒内防止重复提交
 * @Idempotent
 * public Result<Void> submit(OrderCreateRequest request) { ... }
 *
 * // 自定义：10 秒窗口，使用订单号作为幂等 key
 * @Idempotent(key = "#request.orderNo", ttl = 10, timeUnit = TimeUnit.SECONDS)
 * public Result<Void> createOrder(OrderCreateRequest request) { ... }
 * }</pre>
 *
 * @author zerx
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 幂等性 key 的 SpEL 表达式，支持引用方法参数。
     * <p>
     * 为空时自动生成（基于方法签名 + 参数 hashCode）。
     * </p>
     */
    String key() default "";

    /**
     * 幂等性有效时间（窗口期）
     */
    long ttl() default 5;

    /**
     * TTL 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
