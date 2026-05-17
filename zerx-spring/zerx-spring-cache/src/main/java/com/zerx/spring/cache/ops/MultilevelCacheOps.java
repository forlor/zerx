package com.zerx.spring.cache.ops;

import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 多级缓存 CacheOps 实现（L1 Caffeine + L2 Redis）。
 * <p>
 * 读取策略：L1 → L2 → Loader → 写入 L2 + L1。
 * 写入策略：同时写入 L1 和 L2。
 * 失效策略：删除 L1 + L2，并通过 Redis Pub/Sub 通知其他节点清除 L1。
 * </p>
 *
 * @author zerx
 */
public class MultilevelCacheOps implements CacheOps {

    private static final Logger log = LoggerFactory.getLogger(MultilevelCacheOps.class);

    /**
     * Redis Pub/Sub 失效通知频道前缀
     */
    public static final String CHANNEL_PREFIX = "zerx:cache:invalidate:";

    /**
     * L1 缓存（Caffeine，本地内存）
     */
    private final CacheOps l1;

    /**
     * L2 缓存（Redis，分布式）
     */
    private final CacheOps l2;

    /**
     * StringRedisTemplate（用于 Pub/Sub 发布失效消息）
     */
    private final StringRedisTemplate stringRedisTemplate;

    private final ZerxCacheProperties properties;

    /**
     * @param l1                 L1 缓存实现（CaffeineCacheOps）
     * @param l2                 L2 缓存实现（RedisCacheOps）
     * @param stringRedisTemplate 用于发布缓存失效消息的 StringRedisTemplate
     * @param properties         缓存配置属性
     */
    public MultilevelCacheOps(CacheOps l1, CacheOps l2,
                              StringRedisTemplate stringRedisTemplate,
                              ZerxCacheProperties properties) {
        this.l1 = l1;
        this.l2 = l2;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    @Override
    public <T> T get(String key, Supplier<T> loader, long ttl, TimeUnit timeUnit) {
        // 1. 查询 L1
        String fullKey = withPrefix(key);
        T value = l1.get(key);
        if (value != null) {
            log.debug("Multilevel cache hit L1: {}", fullKey);
            return value;
        }

        // 2. 查询 L2
        value = l2.get(key);
        if (value != null) {
            log.debug("Multilevel cache hit L2, backfill L1: {}", fullKey);
            // 回填 L1，使用较短的 L1 TTL
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            l1.set(key, value, l1Ttl);
            return value;
        }

        // 3. 缓存全部 miss，调用 loader
        log.debug("Multilevel cache miss: {}", fullKey);
        value = loader.get();

        if (value != null) {
            // 写入 L2（使用原始 TTL）和 L1（使用较短 TTL）
            l2.set(key, value, ttl, timeUnit);
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            l1.set(key, value, l1Ttl);
        } else {
            // 空值由 L2 的防穿透机制处理，L1 也需要缓存
            // 通过 l2.get(key, loader, ...) 已经在 L2 中缓存了 NULL_MARKER
            // 这里单独在 L1 也缓存空值标记（通过重新调用 l1.get with null loader 不行，
            // 所以直接让 l2 的 get 方法处理防穿透，L1 在下次查询时自然 miss 并回填）
            // 为保持一致性，在 L1 也通过 set 写入 null
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            Duration l2Ttl = properties.getMultilevel().getL2().getExpireAfterWrite();
            l2.set(key, null, l2Ttl);
            l1.set(key, null, l1Ttl);
        }

        return value;
    }

    @Override
    public <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, TimeUnit unit) {
        return Optional.ofNullable(get(key, loader, ttl, unit));
    }

    @Override
    public <T> T get(String key) {
        // 查询 L1
        T value = l1.get(key);
        if (value != null) {
            log.debug("Multilevel cache direct hit L1: {}", withPrefix(key));
            return value;
        }

        // 查询 L2
        value = l2.get(key);
        if (value != null) {
            log.debug("Multilevel cache direct hit L2, backfill L1: {}", withPrefix(key));
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            l1.set(key, value, l1Ttl);
        }

        return value;
    }

    @Override
    public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
        // 同时写入 L1（较短 TTL）和 L2（原始 TTL）
        Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
        l1.set(key, value, l1Ttl);
        l2.set(key, value, ttl, timeUnit);
        // 通知其他节点清除 L1
        publishInvalidation(key);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void evict(String key) {
        l1.evict(key);
        l2.evict(key);
        // 通知其他节点清除 L1
        publishInvalidation(key);
    }

    @Override
    public void evictByPrefix(String keyPrefix) {
        l1.evictByPrefix(keyPrefix);
        l2.evictByPrefix(keyPrefix);
        // 通知其他节点清除前缀匹配的 L1 缓存
        publishInvalidation(keyPrefix);
    }

    @Override
    public boolean hasKey(String key) {
        return l1.hasKey(key) || l2.hasKey(key);
    }

    /**
     * 通过 Redis Pub/Sub 发布缓存失效通知。
     * <p>
     * 其他节点的 {@link com.zerx.spring.cache.config.CacheInvalidationListener}
     * 会收到消息并清除本地 L1 缓存。
     * </p>
     *
     * @param key 需要失效的缓存键
     */
    private void publishInvalidation(String key) {
        try {
            String channel = CHANNEL_PREFIX + withPrefix(key);
            stringRedisTemplate.convertAndSend(channel, "evict");
            log.debug("Published cache invalidation: channel={}", channel);
        } catch (Exception e) {
            log.warn("Failed to publish cache invalidation for key: {}", withPrefix(key), e);
        }
    }

    private String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }
}
