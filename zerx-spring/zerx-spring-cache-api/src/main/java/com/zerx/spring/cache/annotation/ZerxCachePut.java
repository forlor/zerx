package com.zerx.spring.cache.annotation;

import java.util.concurrent.TimeUnit;

/**
 * 声明式缓存更新注解 — 将方法返回值直接写入缓存。
 * <p>
 * 与 {@link ZerxCacheable} 不同，{@code @ZerxCachePut} 不检查缓存是否已存在，
 * 每次方法执行后都将返回值写入缓存。适用于"更新后立即刷新缓存"的场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @ZerxCachePut(name = "user", key = "#user.id", ttl = 30, timeUnit = TimeUnit.MINUTES)
 * public User createUser(User user) { ... }
 *
 * @ZerxCachePut(name = "config", key = "#key", ttl = 1, timeUnit = TimeUnit.HOURS)
 * public Config updateConfig(String key, String value) { ... }
 * }</pre>
 *
 * @author zerx
 * @see ZerxCacheable
 * @see ZerxCacheEvict
 */
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ZerxCachePut {

    /**
     * 缓存名称，作为 key 的前缀。
     */
    String name();

    /**
     * 缓存 key 的 SpEL 表达式。
     */
    String key() default "";

    /**
     * 缓存 TTL，单位由 {@link #timeUnit()} 指定。
     * <p>
     * 设为 {@code -1} 时使用全局默认 TTL。
     * </p>
     */
    long ttl() default -1;

    /**
     * TTL 的时间单位。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
