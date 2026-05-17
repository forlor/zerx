package com.zerx.spring.cache.ops;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Caffeine 的本地缓存 CacheOps 实现。
 * <p>
 * 提供线程安全的高性能本地缓存操作，支持：
 * <ul>
 *     <li>每条记录独立 TTL（per-entry expiry）</li>
 *     <li>防穿透（anti-penetration）：空值缓存 NULL_MARKER</li>
 *     <li>防击穿（anti-stampede）：per-key 互斥锁</li>
 *     <li>防雪崩（anti-avalanche）：TTL 随机抖动 ±10%</li>
 * </ul>
 * </p>
 *
 * @author zerx
 */
public class CaffeineCacheOps implements CacheOps {

    private static final Logger log = LoggerFactory.getLogger(CaffeineCacheOps.class);

    /**
     * 空值标记对象（防穿透）
     */
    private static final Object NULL_MARKER = new Object();

    /**
     * 每条记录的可过期包装
     *
     * @param value         实际缓存值（可能为 NULL_MARKER）
     * @param expireAtNanos 基于 {@link System#nanoTime()} 的过期时间戳
     */
    private record ExpirableEntry(Object value, long expireAtNanos) {}

    @SuppressWarnings("unchecked")
    private final Cache<String, Object> cache;
    private final ZerxCacheProperties properties;

    /**
     * per-key 互斥锁映射（防击穿）
     */
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public CaffeineCacheOps(ZerxCacheProperties properties) {
        this.properties = properties;
        ZerxCacheProperties.CaffeineSpec spec = properties.getCaffeine();
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(spec.getMaxSize())
                .expireAfter(new Expiry<Object, Object>() {
                    @Override
                    public long expireAfterCreate(Object key, Object value, long currentTime) {
                        if (value instanceof ExpirableEntry entry) {
                            return Math.max(0, entry.expireAtNanos() - currentTime);
                        }
                        return TimeUnit.MINUTES.toNanos(spec.getExpireAfterWrite().toMinutes());
                    }

                    @Override
                    public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
                        return expireAfterCreate(key, value, currentTime);
                    }

                    @Override
                    public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                });
        if (spec.isRecordStats()) {
            builder.recordStats();
        }
        this.cache = (Cache<String, Object>) (Cache<?, ?>) builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit timeUnit) {
        String fullKey = withPrefix(key);

        // 快速路径：无锁检查
        Object cached = cache.getIfPresent(fullKey);
        if (cached instanceof ExpirableEntry entry) {
            if (entry.value() == NULL_MARKER) {
                return null;
            }
            log.debug("Cache hit: {}", fullKey);
            return (T) entry.value();
        }

        // 慢路径：获取 per-key 互斥锁（防击穿）
        Object lock = lockMap.computeIfAbsent(fullKey, k -> new Object());
        synchronized (lock) {
            try {
                // 双重检查（获取锁后再次确认缓存）
                cached = cache.getIfPresent(fullKey);
                if (cached instanceof ExpirableEntry entry) {
                    if (entry.value() == NULL_MARKER) {
                        return null;
                    }
                    log.debug("Cache hit (after lock): {}", fullKey);
                    return (T) entry.value();
                }

                log.debug("Cache miss: {}", fullKey);
                T value = loader.get();
                long ttlNanos = withJitter(timeUnit.toNanos(ttl));

                if (value != null) {
                    cache.put(fullKey, new ExpirableEntry(value, System.nanoTime() + ttlNanos));
                } else if (properties.getNullValueTtl().toMillis() > 0) {
                    // 防穿透：缓存空值标记
                    long nullTtlNanos = properties.getNullValueTtl().toNanos();
                    cache.put(fullKey, new ExpirableEntry(NULL_MARKER, System.nanoTime() + nullTtlNanos));
                    log.debug("Cache null value (anti-penetration): {}", fullKey);
                }

                return value;
            } finally {
                lockMap.remove(fullKey);
            }
        }
    }

    @Override
    public <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, TimeUnit unit) {
        return Optional.ofNullable(get(key, loader, ttl, unit));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        String fullKey = withPrefix(key);
        Object cached = cache.getIfPresent(fullKey);
        if (cached instanceof ExpirableEntry entry) {
            if (entry.value() == NULL_MARKER) {
                return null;
            }
            return (T) entry.value();
        }
        return null;
    }

    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        String fullKey = withPrefix(key);
        long ttlNanos = withJitter(timeUnit.toNanos(ttl));
        cache.put(fullKey, new ExpirableEntry(value, System.nanoTime() + ttlNanos));
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void evict(String key) {
        String fullKey = withPrefix(key);
        cache.invalidate(fullKey);
    }

    @Override
    public void evictByPrefix(String keyPrefix) {
        String fullPrefix = withPrefix(keyPrefix);
        cache.asMap().keySet().removeIf(k -> k.startsWith(fullPrefix));
    }

    @Override
    public boolean hasKey(String key) {
        return cache.getIfPresent(withPrefix(key)) != null;
    }

    /**
     * 给 TTL 添加随机抖动（防雪崩）。
     * <p>
     * 在原始 TTL 基础上乘以 [0.9, 1.1) 的随机因子，
     * 避免大量缓存条目在同一时刻过期导致请求洪峰。
     * </p>
     *
     * @param ttlNanos 原始 TTL（纳秒）
     * @return 添加抖动后的 TTL（纳秒）
     */
    private long withJitter(long ttlNanos) {
        double jitter = 0.9 + Math.random() * 0.2;
        return (long) (ttlNanos * jitter);
    }

    private String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }
}
