package com.zerx.spring.cache.store;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheException;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 多级缓存 CacheStore 实现（L1 Caffeine + L2 Redis）。
 * <p>
 * 读取策略：L1 → L2 → 未命中（上层 CacheOps 负责调用 loader）。
 * 写入策略：L2 优先写入，成功后写 L1（保证 L2 为准）。
 * 失效策略：L1 + L2 同时删除 + Redis Pub/Sub 通知其他节点清除 L1。
 * </p>
 *
 * <h3>一致性保障：</h3>
 * <ul>
 *     <li>写操作先写 L2，L2 成功后再写 L1。若 L2 失败则不写 L1，避免不一致</li>
 *     <li>跨节点 L1 失效通过 Redis Pub/Sub 实现</li>
 *     <li>L2 miss 回填 L1，使用 L1 的较短 TTL</li>
 * </ul>
 *
 * @author zerx
 */
public class MultilevelCacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(MultilevelCacheStore.class);

    private final CacheStore l1;
    private final CacheStore l2;
    private final StringRedisTemplate stringRedisTemplate;
    private final ZerxCacheProperties properties;

    public MultilevelCacheStore(CacheStore l1, CacheStore l2,
                                StringRedisTemplate stringRedisTemplate,
                                ZerxCacheProperties properties) {
        this.l1 = l1;
        this.l2 = l2;
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
    }

    @Override
    public Optional<Object> get(String key) {
        // 1. 查询 L1
        Optional<Object> l1Value = l1.get(key);
        if (l1Value.isPresent() && !CacheConstants.NULL_MARKER.equals(l1Value.get())) {
            log.debug("Multilevel cache hit L1: {}", withPrefix(key));
            return l1Value;
        }

        // 2. 查询 L2
        Optional<Object> l2Value = l2.get(key);
        if (l2Value.isPresent()) {
            // 回填 L1，使用较短的 L1 TTL
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            l1.set(key, l2Value.get(), l1Ttl);
            log.debug("Multilevel cache hit L2, backfill L1: {}", withPrefix(key));
            return l2Value;
        }

        log.debug("Multilevel cache miss: {}", withPrefix(key));
        return Optional.empty();
    }

    /**
     * 写入策略：先写 L2，成功后再写 L1。
     * <p>
     * 这样即使 L1 写入失败，L2 中有正确的数据（下次 L1 miss 可从 L2 回填）。
     * 若 L2 写入失败，不写 L1，避免 L1 中存在 L2 没有的脏数据。
     * </p>
     */
    @Override
    public void set(String key, Object value, Duration ttl) {
        // 先写 L2（L2 是准）
        try {
            l2.set(key, value, ttl);
        } catch (Exception e) {
            throw new CacheException(CacheException.CACHE_ERROR,
                    "Multilevel cache set failed: L2 write error for key: " + key, e);
        }

        // L2 成功后再写 L1（使用较短的 L1 TTL）
        try {
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            l1.set(key, value, l1Ttl);
        } catch (Exception e) {
            // L1 写入失败不影响主流程，L1 miss 时会从 L2 回填
            log.warn("Multilevel cache: L1 set failed (non-fatal), key: {}", withPrefix(key), e);
        }

        // 通知其他节点清除 L1
        publishInvalidation(key, "evict");
    }

    @Override
    public void evict(String key) {
        l1.evict(key);
        l2.evict(key);
        publishInvalidation(key, "evict");
    }

    @Override
    public void evictByPrefix(String prefix) {
        l1.evictByPrefix(prefix);
        l2.evictByPrefix(prefix);
        publishInvalidation(prefix, "evict_prefix");
    }

    @Override
    public boolean hasKey(String key) {
        return l1.hasKey(key) || l2.hasKey(key);
    }

    @Override
    public Map<String, Object> multiGet(Collection<String> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }

        // 先批量查 L1
        Map<String, Object> result = new HashMap<>(keys.size());
        Map<String, Object> l1Hits = l1.multiGet(keys);
        result.putAll(l1Hits);

        // L1 未命中的查 L2
        Collection<String> l2Keys = keys.stream()
                .filter(k -> !l1Hits.containsKey(k))
                .toList();
        if (!l2Keys.isEmpty()) {
            Map<String, Object> l2Hits = l2.multiGet(l2Keys);
            result.putAll(l2Hits);

            // 回填 L1
            if (!l2Hits.isEmpty()) {
                Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
                l1.multiSet(l2Hits, l1Ttl);
            }
        }

        return result;
    }

    @Override
    public void multiSet(Map<String, Object> entries, Duration ttl) {
        if (entries.isEmpty()) {
            return;
        }
        // 先写 L2
        l2.multiSet(entries, ttl);
        // 再写 L1
        try {
            Duration l1Ttl = properties.getMultilevel().getL1().getExpireAfterWrite();
            l1.multiSet(entries, l1Ttl);
        } catch (Exception e) {
            log.warn("Multilevel cache: L1 multiSet failed (non-fatal)", e);
        }
    }

    @Override
    public void multiEvict(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        l1.multiEvict(keys);
        l2.multiEvict(keys);
        // 批量失效通知：计算公共前缀，优先发单条 prefix 消息
        String commonPrefix = findCommonPrefix(keys.stream().map(this::withPrefix).toList());
        if (commonPrefix != null) {
            publishInvalidation(commonPrefix, "evict_prefix");
        } else {
            keys.forEach(k -> publishInvalidation(k, "evict"));
        }
    }

    /**
     * 通过 Redis Pub/Sub 发布缓存失效通知。
     */
    private void publishInvalidation(String key, String type) {
        try {
            String channel = CacheConstants.INVALIDATION_CHANNEL_PREFIX + withPrefix(key);
            stringRedisTemplate.convertAndSend(channel, type);
            log.debug("Published cache invalidation: channel={}, type={}", channel, type);
        } catch (Exception e) {
            // Pub/Sub 失败不阻塞主流程
            log.warn("Failed to publish cache invalidation for key: {}, type: {}", withPrefix(key), type, e);
        }
    }

    String withPrefix(String key) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }

    /**
     * 计算一组 fullKey 的最长公共前缀（至少到 ':' 分隔符）。
     * <p>
     * 返回的公共前缀会在最后一个 ':' 处截断，确保覆盖所有 key。
     * 如果 key 数量较少（<= 3）或无公共前缀，返回 null 表示退回逐条通知。
     * </p>
     */
    String findCommonPrefix(List<String> fullKeys) {
        if (fullKeys.size() <= 3) {
            return null;
        }
        String first = fullKeys.get(0);
        int prefixEnd = first.length();
        for (int i = 1; i < fullKeys.size(); i++) {
            prefixEnd = Math.min(prefixEnd, commonPrefixLength(first, fullKeys.get(i)));
            if (prefixEnd == 0) {
                return null;
            }
        }
        // 在最后一个 ':' 处截断，确保前缀有效
        int lastColon = first.lastIndexOf(':', prefixEnd - 1);
        if (lastColon > 0) {
            return first.substring(0, lastColon + 1);
        }
        // 无 ':' 分隔符，返回 null 退回逐条
        return null;
    }

    private int commonPrefixLength(String a, String b) {
        int len = Math.min(a.length(), b.length());
        int i = 0;
        while (i < len && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }
}
