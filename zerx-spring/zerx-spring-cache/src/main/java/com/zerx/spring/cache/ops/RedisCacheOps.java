package com.zerx.spring.cache.ops;

import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式缓存 CacheOps 实现。
 * <p>
 * 支持：
 * <ul>
 *     <li>防穿透（anti-penetration）：空值缓存 NULL_MARKER</li>
 *     <li>防击穿（anti-stampede）：per-key 本地互斥锁</li>
 *     <li>防雪崩（anti-avalanche）：TTL 随机抖动 ±10%</li>
 * </ul>
 * </p>
 * <p>
 * 注意：防击穿的互斥锁为 JVM 级别，在多节点部署时无法完全防止击穿，
 * 建议结合 Redis 分布式锁（如 Redisson）实现更强的防击穿保障。
 * </p>
 *
 * @author zerx
 */
public class RedisCacheOps implements CacheOps {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheOps.class);

    /**
     * 空值标记字符串（防穿透），用于在 Redis 中标记已缓存但值为空的键
     */
    private static final String NULL_MARKER = "__ZERX_CACHE_NULL__";

    /**
     * 空值缓存默认 TTL（当配置中未指定时使用）
     */
    private static final Duration DEFAULT_NULL_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ZerxCacheProperties properties;

    /**
     * per-key 互斥锁映射（防击穿，JVM 级别）
     */
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public RedisCacheOps(RedisTemplate<String, Object> redisTemplate, ZerxCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit timeUnit) {
        String fullKey = withPrefix(key);

        // 快速路径：查询 Redis
        Object cached = redisTemplate.opsForValue().get(fullKey);
        if (cached != null) {
            if (NULL_MARKER.equals(cached)) {
                log.debug("Cache hit null marker (anti-penetration): {}", fullKey);
                return null;
            }
            log.debug("Cache hit: {}", fullKey);
            return (T) cached;
        }

        // 慢路径：获取 per-key 互斥锁（防击穿）
        Object lock = lockMap.computeIfAbsent(fullKey, k -> new Object());
        synchronized (lock) {
            try {
                // 双重检查
                cached = redisTemplate.opsForValue().get(fullKey);
                if (cached != null) {
                    if (NULL_MARKER.equals(cached)) {
                        log.debug("Cache hit null marker after lock (anti-penetration): {}", fullKey);
                        return null;
                    }
                    log.debug("Cache hit (after lock): {}", fullKey);
                    return (T) cached;
                }

                log.debug("Cache miss: {}", fullKey);
                T value = loader.get();

                if (value != null) {
                    long ttlMillis = withJitter(timeUnit.toMillis(ttl));
                    redisTemplate.opsForValue().set(fullKey, value, ttlMillis, TimeUnit.MILLISECONDS);
                } else if (properties.getNullValueTtl().toMillis() > 0) {
                    // 防穿透：缓存空值标记
                    Duration nullTtl = properties.getNullValueTtl();
                    redisTemplate.opsForValue().set(fullKey, NULL_MARKER, nullTtl);
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
        Object cached = redisTemplate.opsForValue().get(fullKey);
        if (cached == null || NULL_MARKER.equals(cached)) {
            return null;
        }
        return (T) cached;
    }

    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        String fullKey = withPrefix(key);
        long ttlMillis = withJitter(timeUnit.toMillis(ttl));
        redisTemplate.opsForValue().set(fullKey, value, ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void evict(String key) {
        String fullKey = withPrefix(key);
        redisTemplate.delete(fullKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void evictByPrefix(String keyPrefix) {
        String fullPrefix = withPrefix(keyPrefix);
        Set<String> keys = redisTemplate.keys(fullPrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(withPrefix(key)));
    }

    /**
     * 给 TTL 添加随机抖动（防雪崩）。
     * <p>
     * 在原始 TTL 基础上乘以 [0.9, 1.1) 的随机因子，
     * 避免大量缓存条目在同一时刻过期导致请求洪峰。
     * </p>
     *
     * @param ttlMillis 原始 TTL（毫秒）
     * @return 添加抖动后的 TTL（毫秒）
     */
    private long withJitter(long ttlMillis) {
        double jitter = 0.9 + Math.random() * 0.2;
        return Math.max(1, (long) (ttlMillis * jitter));
    }

    private String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }
}
