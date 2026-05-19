package com.zerx.spring.cache;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 底层缓存 KV 存储接口。
 * <p>
 * 提供纯粹的缓存键值操作，不包含 Cache-Aside 加载逻辑。
 * 实现类负责键前缀处理、TTL 管理、底层存储适配等。
 * </p>
 *
 * <h3>实现约定：</h3>
 * <ul>
 *     <li>键前缀：所有实现必须在内部处理 {@code keyPrefix}，调用方传入逻辑键</li>
 *     <li>空值标记：使用 {@link CacheConstants#NULL_MARKER} 标记空值（防穿透）</li>
 *     <li>TTL 抖动：建议在 {@code set} 时添加随机抖动（防雪崩）</li>
 *     <li>线程安全：所有方法必须线程安全</li>
 * </ul>
 *
 * @author zerx
 * @see CacheOps
 */
public interface CacheStore {

    /**
     * 获取缓存值。
     *
     * @param key 逻辑键（不含前缀）
     * @return {@code Optional.empty()} 表示键不存在；
     *         {@code Optional.of(NULL_MARKER)} 表示空值已缓存（防穿透命中）；
     *         {@code Optional.of(value)} 表示正常缓存值
     */
    Optional<Object> get(String key);

    /**
     * 写入缓存值。
     *
     * @param key   逻辑键（不含前缀）
     * @param value 缓存值（可以为 {@link CacheConstants#NULL_MARKER}）
     * @param ttl   过期时间
     */
    void set(String key, Object value, Duration ttl);

    /**
     * 删除缓存。
     *
     * @param key 逻辑键（不含前缀）
     */
    void evict(String key);

    /**
     * 按前缀批量删除缓存。
     *
     * @param prefix 键前缀（不含全局前缀）
     */
    void evictByPrefix(String prefix);

    /**
     * 判断缓存是否存在。
     *
     * @param key 逻辑键（不含前缀）
     * @return {@code true} 如果键存在（包括 NULL_MARKER 占位）
     */
    boolean hasKey(String key);

    /**
     * 批量获取缓存值。
     * <p>
     * Redis 实现应使用 {@code multiGet} 或 Pipeline；Caffeine 实现应逐个查询。
     * </p>
     *
     * @param keys 逻辑键集合
     * @return 键到值的映射，不存在的键不包含在结果中；
     *         NULL_MARKER 值不包含在结果中（视为未命中）
     */
    Map<String, Object> multiGet(Collection<String> keys);

    /**
     * 批量写入缓存值。
     * <p>
     * Redis 实现应使用 Pipeline；Caffeine 实现应逐个写入。
     * </p>
     *
     * @param entries 键值对映射
     * @param ttl     统一过期时间
     */
    void multiSet(Map<String, Object> entries, Duration ttl);

    /**
     * 批量删除缓存。
     *
     * @param keys 逻辑键集合
     */
    void multiEvict(Collection<String> keys);
}
