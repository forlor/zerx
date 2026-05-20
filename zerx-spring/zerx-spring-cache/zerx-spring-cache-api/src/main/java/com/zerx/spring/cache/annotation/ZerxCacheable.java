package com.zerx.spring.cache.annotation;

import java.util.concurrent.TimeUnit;

/**
 * 声明式缓存注解 — 方法返回值自动缓存。
 * <p>
 * 标注在方法上，自动实现 Cache-Aside 模式：先查缓存，miss 时执行方法体并回填。
 * 支持自定义 TTL，解决原生 {@code @Cacheable} 不支持 per-method TTL 的问题。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @ZerxCacheable(name = "user", key = "#id", ttl = 30, timeUnit = TimeUnit.MINUTES)
 * public User getUser(Long id) { ... }
 *
 * @ZerxCacheable(name = "userList", key = "#ids.hashCode()", ttl = 10, timeUnit = TimeUnit.MINUTES)
 * public List<User> listUsers(List<Long> ids) { ... }
 * }</pre>
 *
 * @author zerx
 * @see ZerxCacheEvict
 * @see ZerxCachePut
 */
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ZerxCacheable {

    /**
     * 缓存名称，作为 key 的前缀。
     * <p>
     * 最终缓存 key 格式：{@code {keyPrefix}{name}:{SpEL解析后的key}}
     * </p>
     */
    String name();

    /**
     * 缓存 key 的 SpEL 表达式。
     * <p>
     * 支持的上下文变量：方法参数（通过 {@code #paramName} 或 {@code #p0} 引用）、
     * 返回值（通过 {@code #result} 引用，仅在 after 模式下可用）。
     * </p>
     * <p>
     * 为空时使用方法签名 + 参数 hashCode 作为 key。
     * </p>
     */
    String key() default "";

    /**
     * 缓存 TTL，单位由 {@link #timeUnit()} 指定。
     * <p>
     * 设为 {@code -1} 时使用全局默认 TTL（配置中的 {@code zerx.cache.defaultTtl}）。
     * </p>
     */
    long ttl() default -1;

    /**
     * TTL 的时间单位。
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否缓存空值（防穿透）。
     * <p>
     * {@code true}：方法返回 null 时缓存空值标记，后续请求直接返回 null 而不执行方法；
     * {@code false}：方法返回 null 时不缓存，每次都会执行方法。
     * </p>
     */
    boolean nullCache() default true;

    /**
     * 缓存前置条件 SpEL 表达式。
     * <p>
     * 表达式求值为 {@code false} 时不走缓存，直接执行方法。
     * 可引用方法参数（{@code #id}、{@code #p0}）。
     * 为空字符串时视为无条件限制。
     * </p>
     *
     * @example {@code condition = "#id > 0"}
     */
    String condition() default "";

    /**
     * 缓存后置排除 SpEL 表达式。
     * <p>
     * 方法执行完成后求值，可引用方法参数和返回值（{@code #result}）。
     * 表达式求值为 {@code true} 时，不将结果写入缓存。
     * 为空字符串时表示不排除。
     * </p>
     *
     * @example {@code unless = "#result == null"} 或 {@code unless = "#result.size() == 0"}
     */
    String unless() default "";
}
