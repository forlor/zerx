package com.zerx.spring.cache.store;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 基于 Caffeine 的本地缓存 CacheStore 实现。
 * <p>
 * 特性：
 * <ul>
 *     <li>每条记录独立 TTL（per-entry expiry）</li>
 *     <li>TTL 随机抖动 ±10%（防雪崩）</li>
 *     <li>高效前缀清除：收集匹配 key 后批量 invalidateAll</li>
 * </ul>
 * </p>
 *
 * @author zerx
 */
public class CaffeineCacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(CaffeineCacheStore.class);

    /**
     * 每条记录的可过期包装
     *
     * @param value         实际缓存值
     * @param expireAtNanos 基于 {@link System#nanoTime()} 的过期时间戳
     */
    record ExpirableEntry(Object value, long expireAtNanos) {}

    @SuppressWarnings("unchecked")
    private final Cache<String, Object> cache;
    private final ZerxCacheProperties properties;

    public CaffeineCacheStore(ZerxCacheProperties properties) {
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
    public Optional<Object> get(String key) {
        String fullKey = withPrefix(key);
        Object cached = cache.getIfPresent(fullKey);
        if (cached instanceof ExpirableEntry entry) {
            return Optional.of(entry.value());
        }
        return Optional.empty();
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        String fullKey = withPrefix(key);
        long ttlNanos = withJitter(ttl.toNanos());
        cache.put(fullKey, new ExpirableEntry(value, System.nanoTime() + ttlNanos));
    }

    @Override
    public void evict(String key) {
        cache.invalidate(withPrefix(key));
    }

    @Override
    public void evictByPrefix(String keyPrefix) {
        String fullPrefix = withPrefix(keyPrefix);
        Set<String> matchedKeys = cache.asMap().keySet().stream()
                .filter(k -> k.startsWith(fullPrefix))
                .collect(Collectors.toSet());
        if (!matchedKeys.isEmpty()) {
            cache.invalidateAll(matchedKeys);
            log.debug("Evicted {} keys with prefix: {}", matchedKeys.size(), fullPrefix);
        }
    }

    @Override
    public boolean hasKey(String key) {
        return cache.getIfPresent(withPrefix(key)) != null;
    }

    @Override
    public Map<String, Object> multiGet(Collection<String> keys) {
        Map<String, Object> result = new HashMap<>(keys.size());
        for (String key : keys) {
            get(key).filter(v -> !CacheConstants.NULL_MARKER.equals(v))
                    .ifPresent(v -> result.put(key, v));
        }
        return result;
    }

    @Override
    public void multiSet(Map<String, Object> entries, Duration ttl) {
        entries.forEach((key, value) -> set(key, value, ttl));
    }

    @Override
    public void multiEvict(Collection<String> keys) {
        keys.forEach(this::evict);
    }

    /**
     * 给 TTL 添加随机抖动（防雪崩）。
     */
    long withJitter(long ttlNanos) {
        double jitter = CacheConstants.JITTER_MIN + Math.random() * (CacheConstants.JITTER_MAX - CacheConstants.JITTER_MIN);
        return (long) (ttlNanos * jitter);
    }

    String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }
}
