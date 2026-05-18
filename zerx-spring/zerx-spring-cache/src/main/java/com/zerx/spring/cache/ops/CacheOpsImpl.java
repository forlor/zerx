package com.zerx.spring.cache.ops;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * CacheOps 默认实现 — 基于 {@link CacheStore} 封装 Cache-Aside 模式。
 * <p>
 * 在底层 KV 存储之上提供：
 * <ul>
 *     <li>Cache-Aside 加载：miss 时执行 loader 并回填</li>
 *     <li>防穿透（anti-penetration）：loader 返回 null 时缓存 NULL_MARKER</li>
 *     <li>防击穿（anti-stampede）：per-key ReentrantLock 互斥锁</li>
 *     <li>防雪崩（anti-avalanche）：由底层 CacheStore 的 TTL 抖动保障</li>
 * </ul>
 * </p>
 * <p>
 * 注意：防击穿的锁为 JVM 级别。在多节点部署时，分布式场景下的防击穿
 * 建议使用 Redis 分布式锁（如 Redisson）结合 {@code CacheStore} 扩展实现。
 * </p>
 *
 * @author zerx
 */
public class CacheOpsImpl implements CacheOps {

    private static final Logger log = LoggerFactory.getLogger(CacheOpsImpl.class);

    private final CacheStore store;
    private final Duration nullValueTtl;

    /**
     * per-key 互斥锁映射（防击穿）。
     * <p>
     * 使用 {@link ReentrantLock} 替代 synchronized，支持可中断等待。
     * 锁在双重检查后移除，避免内存泄漏。
     * </p>
     */
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * @param store        底层缓存存储
     * @param nullValueTtl 空值缓存 TTL（0 或负值表示不缓存空值）
     */
    public CacheOpsImpl(CacheStore store, Duration nullValueTtl) {
        this.store = store;
        this.nullValueTtl = nullValueTtl;
    }

    @Override
    public CacheStore getStore() {
        return store;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit timeUnit) {
        // 快速路径：无锁查询
        Optional<Object> cached = store.get(key);
        if (cached.isPresent()) {
            if (CacheConstants.NULL_MARKER.equals(cached.get())) {
                log.debug("Cache hit null marker (anti-penetration): {}", key);
                return null;
            }
            log.debug("Cache hit: {}", key);
            return (T) cached.get();
        }

        // 慢路径：获取 per-key 互斥锁（防击穿）
        ReentrantLock lock = lockMap.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            // 双重检查（获取锁后再次确认缓存）
            cached = store.get(key);
            if (cached.isPresent()) {
                if (CacheConstants.NULL_MARKER.equals(cached.get())) {
                    log.debug("Cache hit null marker after lock (anti-penetration): {}", key);
                    return null;
                }
                log.debug("Cache hit (after lock): {}", key);
                return (T) cached.get();
            }

            // 缓存 miss，执行 loader
            log.debug("Cache miss: {}", key);
            T value = loader.get();

            if (value != null) {
                store.set(key, value, Duration.ofMillis(timeUnit.toMillis(ttl)));
            } else if (nullValueTtl != null && nullValueTtl.toMillis() > 0) {
                // 防穿透：缓存空值标记
                store.set(key, CacheConstants.NULL_MARKER, Duration.ofMillis(nullValueTtl.toMillis()));
                log.debug("Cache null value (anti-penetration): {}", key);
            }

            return value;
        } finally {
            lock.unlock();
            lockMap.remove(key, lock);
        }
    }

    @Override
    public <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, TimeUnit unit) {
        return Optional.ofNullable(get(key, loader, ttl, unit));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Optional<Object> cached = store.get(key);
        if (cached.isPresent() && !CacheConstants.NULL_MARKER.equals(cached.get())) {
            return (T) cached.get();
        }
        return null;
    }

    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        store.set(key, value, Duration.ofMillis(timeUnit.toMillis(ttl)));
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        store.set(key, value, ttl);
    }

    @Override
    public void evict(String key) {
        store.evict(key);
    }

    @Override
    public void evictByPrefix(String keyPrefix) {
        store.evictByPrefix(keyPrefix);
    }

    @Override
    public boolean hasKey(String key) {
        return store.hasKey(key);
    }
}
