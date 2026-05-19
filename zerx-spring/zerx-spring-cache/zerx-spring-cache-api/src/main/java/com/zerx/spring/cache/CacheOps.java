package com.zerx.spring.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存操作高级接口 — Cache-Aside 模式封装。
 * <p>
 * 在 {@link CacheStore} 底层 KV 操作之上，提供：
 * <ul>
 *     <li>Cache-Aside 加载：miss 时自动调用 loader 并回填缓存</li>
 *     <li>防穿透：loader 返回 null 时缓存空值标记</li>
 *     <li>防击穿：高并发 miss 时仅一个线程执行 loader</li>
 *     <li>防雪崩：TTL 随机抖动避免集体过期</li>
 * </ul>
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
 * }</pre>
 *
 * @author zerx
 * @see CacheStore
 */
public interface CacheOps {

    /**
     * 获取缓存值，miss 时自动加载并写入缓存。
     * <p>
     * 执行流程：查缓存 → miss → 加锁 → 双重检查 → 调用 loader → 写缓存 → 返回。
     * </p>
     *
     * @param key      逻辑键
     * @param loader   数据加载函数
     * @param ttl      缓存时间
     * @param timeUnit 时间单位
     * @param <T>      值类型
     * @return 缓存值或 loader 加载值，loader 返回 null 时也返回 null
     */
    <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit timeUnit);

    /**
     * 获取缓存值（Optional 版本，区分"不存在"和"值为 null"）。
     *
     * @param key    逻辑键
     * @param loader 数据加载函数
     * @param ttl    缓存时间
     * @param unit   时间单位
     * @param <T>    值类型
     * @return Optional 包含缓存值，若 loader 返回 null 则返回 Optional.empty()
     */
    <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, TimeUnit unit);

    /**
     * 获取缓存值，不执行加载。
     *
     * @param key 逻辑键
     * @param <T> 值类型
     * @return 缓存值，不存在或已缓存空值时返回 null
     */
    <T> T get(String key);

    /**
     * 写入缓存。
     *
     * @param key      逻辑键
     * @param value    缓存值
     * @param ttl      缓存时间
     * @param timeUnit 时间单位
     */
    void set(String key, Object value, long ttl, TimeUnit timeUnit);

    /**
     * 写入缓存（使用 Duration）。
     *
     * @param key   逻辑键
     * @param value 缓存值
     * @param ttl   过期时间
     */
    void set(String key, Object value, Duration ttl);

    /**
     * 删除缓存。
     *
     * @param key 逻辑键
     */
    void evict(String key);

    /**
     * 按前缀批量删除缓存。
     *
     * @param keyPrefix 键前缀
     */
    void evictByPrefix(String keyPrefix);

    /**
     * 判断缓存是否存在。
     *
     * @param key 逻辑键
     * @return {@code true} 如果键存在
     */
    boolean hasKey(String key);

    /**
     * 获取底层 CacheStore 实例。
     * <p>
     * 用于需要直接操作底层 KV 存储的高级场景（如批量操作、条件缓存等）。
     * AOP 切面和 CacheManager 也通过此方法访问底层存储。
     * </p>
     *
     * @return 底层缓存存储实例
     */
    CacheStore getStore();
}
