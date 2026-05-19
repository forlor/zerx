package com.zerx.spring.cache.annotation;

/**
 * 声明式缓存失效注解 — 方法执行后删除缓存。
 * <p>
 * 标注在方法上，方法执行成功后自动删除对应缓存。
 * 支持单键删除和前缀批量删除两种模式。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 删除单个键
 * @ZerxCacheEvict(name = "user", key = "#id")
 * public void updateUser(Long id, User user) { ... }
 *
 * // 按前缀批量删除（如清空用户的所有关联缓存）
 * @ZerxCacheEvict(name = "user", key = "#userId", prefixEvict = true)
 * public void clearUserCache(Long userId) { ... }
 * }</pre>
 *
 * @author zerx
 * @see ZerxCacheable
 * @see ZerxCachePut
 */
@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ZerxCacheEvict {

    /**
     * 缓存名称，作为 key 的前缀。
     */
    String name();

    /**
     * 缓存 key 的 SpEL 表达式。
     */
    String key() default "";

    /**
     * 是否按前缀批量删除。
     * <p>
     * {@code true}：删除所有以 {@code {keyPrefix}{name}:{SpEL解析后的key}} 为前缀的缓存；
     * {@code false}：精确删除单个键。
     * </p>
     */
    boolean prefixEvict() default false;
}
