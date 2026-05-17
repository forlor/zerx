package com.zerx.spring.cache.ops;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 缓存操作工具接口。
 * <p>
 * 提供链式缓存 API，支持 Get-Set-Evict 模式（Cache-Aside Pattern）。
 * 实现 Cache-Aside 模式：先查缓存，miss 时执行数据加载函数并写入缓存。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 读取（miss 时自动加载）
 * UserVO user = cacheOps.get("user:1", () -> userRepository.findById(1L), 30, TimeUnit.MINUTES);
 *
 * // 写入
 * cacheOps.set("user:1", userVO, 30, TimeUnit.MINUTES);
 *
 * // 删除
 * cacheOps.evict("user:1");
 *
 * // 判断存在
 * boolean exists = cacheOps.hasKey("user:1");
 * }</pre>
 *
 * @author zerx
 */
public interface CacheOps {

    /**
     * 获取缓存值，miss 时自动加载并写入缓存
     *
     * @param key        缓存键
     * @param loader     数据加载函数（仅在缓存 miss 时执行）
     * @param ttl        过期时间
     * @param timeUnit   时间单位
     * @param <T>        值类型
     * @return 缓存值，加载失败或值为 null 时返回 null
     */
    <T> T get(String key, Supplier<T> loader, long ttl, java.util.concurrent.TimeUnit timeUnit);

    /**
     * 获取缓存值（Optional 版本，区分"不存在"和"值为 null"）
     *
     * @param key    缓存键
     * @param loader 数据加载函数
     * @param ttl    过期时间
     * @param unit   时间单位
     * @param <T>    值类型
     * @return Optional 包装的缓存值
     */
    <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, java.util.concurrent.TimeUnit unit);

    /**
     * 获取缓存值，不执行加载
     *
     * @param key 缓存键
     * @param <T> 值类型
     * @return 缓存值，不存在时返回 null
     */
    <T> T get(String key);

    /**
     * 写入缓存
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param ttl      过期时间
     * @param timeUnit 时间单位
     */
    void set(String key, Object value, long ttl, java.util.concurrent.TimeUnit timeUnit);

    /**
     * 写入缓存（使用 Duration）
     *
     * @param key    缓存键
     * @param value  缓存值
     * @param ttl    过期时间
     */
    void set(String key, Object value, Duration ttl);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void evict(String key);

    /**
     * 批量删除缓存（匹配前缀）
     *
     * @param keyPrefix 键前缀
     */
    void evictByPrefix(String keyPrefix);

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 存在返回 true
     */
    boolean hasKey(String key);
}
