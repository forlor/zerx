package com.zerx.spring.cache.store;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheException;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.types.Expiration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的分布式缓存 CacheStore 实现。
 * <p>
 * 特性：
 * <ul>
 *     <li>TTL 随机抖动 ±10%（防雪崩）</li>
 *     <li>SCAN 替代 KEYS 做前缀扫描（避免大库阻塞）</li>
 *     <li>Pipeline 批量写入（multiSet）</li>
 *     <li>multiGet 使用 RedisTemplate 批量查询</li>
 * </ul>
 * </p>
 *
 * @author zerx
 */
public class RedisCacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheStore.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ZerxCacheProperties properties;

    public RedisCacheStore(RedisTemplate<String, Object> redisTemplate,
                           ZerxCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<Object> get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(withPrefix(key));
            return Optional.ofNullable(value);
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis get failed for key: " + key, e);
        }
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        try {
            String fullKey = withPrefix(key);
            long ttlMillis = withJitter(ttl.toMillis());
            redisTemplate.opsForValue().set(fullKey, value, ttlMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis set failed for key: " + key, e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            redisTemplate.delete(withPrefix(key));
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis evict failed for key: " + key, e);
        }
    }

    /**
     * 使用 SCAN 替代 KEYS 做前缀扫描，避免大库阻塞。
     */
    @Override
    public void evictByPrefix(String keyPrefix) {
        try {
            String fullPrefix = withPrefix(keyPrefix);
            List<String> matchedKeys = new ArrayList<>();

            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(fullPrefix + "*")
                    .count(1000)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
                cursor.forEachRemaining(matchedKeys::add);
            }

            if (!matchedKeys.isEmpty()) {
                redisTemplate.delete(matchedKeys);
                log.debug("Evicted {} keys with prefix: {}", matchedKeys.size(), fullPrefix);
            }
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR,
                    "Redis evictByPrefix failed for prefix: " + keyPrefix, e);
        }
    }

    @Override
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(withPrefix(key)));
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis hasKey failed for key: " + key, e);
        }
    }

    /**
     * 使用 Redis multiGet 批量查询，一次网络往返获取多个值。
     */
    @Override
    public Map<String, Object> multiGet(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        try {
            List<String> fullKeys = keys.stream().map(this::withPrefix).toList();
            List<Object> values = redisTemplate.opsForValue().multiGet(fullKeys);

            Map<String, Object> result = new HashMap<>(keys.size());
            if (values != null) {
                int index = 0;
                for (String logicalKey : keys) {
                    Object value = values.get(index);
                    if (value != null && !CacheConstants.NULL_MARKER.equals(value)) {
                        result.put(logicalKey, value);
                    }
                    index++;
                }
            }
            return result;
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis multiGet failed", e);
        }
    }

    /**
     * 使用 Pipeline 批量写入，减少网络往返次数。
     */
    @Override
    @SuppressWarnings("unchecked")
    public void multiSet(Map<String, Object> entries, Duration ttl) {
        if (entries.isEmpty()) {
            return;
        }
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                entries.forEach((key, value) -> {
                    String fullKey = withPrefix(key);
                    byte[] keyBytes = fullKey.getBytes(StandardCharsets.UTF_8);
                    byte[] valueBytes = serialize(value);
                    long ttlMillis = withJitter(ttl.toMillis());
                    connection.stringCommands().set(keyBytes, valueBytes,
                            Expiration.milliseconds(ttlMillis),
                            RedisStringCommands.SetOption.UPSERT);
                });
                return null;
            });
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis multiSet failed", e);
        }
    }

    @Override
    public void multiEvict(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        try {
            List<String> fullKeys = keys.stream().map(this::withPrefix).toList();
            redisTemplate.delete(fullKeys);
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR, "Redis multiEvict failed", e);
        }
    }

    // ======================== 内部工具方法 ========================

    long withJitter(long ttlMillis) {
        double jitter = CacheConstants.JITTER_MIN + Math.random() * (CacheConstants.JITTER_MAX - CacheConstants.JITTER_MIN);
        return Math.max(1, (long) (ttlMillis * jitter));
    }

    String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }

    @SuppressWarnings("unchecked")
    private byte[] serialize(Object value) {
        if (value == null) {
            return new byte[0];
        }
        org.springframework.data.redis.serializer.RedisSerializer<Object> serializer =
                (org.springframework.data.redis.serializer.RedisSerializer<Object>) redisTemplate.getValueSerializer();
        if (serializer != null) {
            return serializer.serialize(value);
        }
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }
}
