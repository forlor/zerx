package com.zerx.spring.cache.config;

import com.zerx.spring.cache.ops.CacheOps;
import com.zerx.spring.cache.ops.MultilevelCacheOps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import java.nio.charset.StandardCharsets;

/**
 * Redis Pub/Sub 缓存失效监听器。
 * <p>
 * 监听 {@value MultilevelCacheOps#CHANNEL_PREFIX}* 频道，
 * 收到消息后在本地 L1 缓存中清除对应的键，
 * 保证多节点部署时各节点的 L1 缓存一致性。
 * </p>
 *
 * @author zerx
 */
public class CacheInvalidationListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);

    private final CacheOps l1Cache;

    /**
     * @param l1Cache 本地 L1 缓存（CaffeineCacheOps），用于清除失效的缓存条目
     */
    public CacheInvalidationListener(CacheOps l1Cache) {
        this.l1Cache = l1Cache;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
            // 从频道名提取键： "zerx:cache:invalidate:{key}" → key 需要去掉前缀
            String key = extractKeyFromChannel(channel);
            if (key != null) {
                l1Cache.evict(key);
                log.debug("L1 cache invalidated via Pub/Sub: key={}", key);
            }
        } catch (Exception e) {
            log.warn("Failed to process cache invalidation message", e);
        }
    }

    /**
     * 从 Redis Pub/Sub 频道名中提取缓存键。
     * <p>
     * 频道格式：{@code zerx:cache:invalidate:{prefixedKey}}
     * 返回去除前缀后的原始 key。
     * </p>
     *
     * @param channel 频道名
     * @return 缓存键，解析失败时返回 null
     */
    private String extractKeyFromChannel(String channel) {
        String prefix = MultilevelCacheOps.CHANNEL_PREFIX;
        if (channel.startsWith(prefix)) {
            return channel.substring(prefix.length());
        }
        log.warn("Unexpected channel name format: {}", channel);
        return null;
    }
}
