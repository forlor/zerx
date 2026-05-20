package com.zerx.spring.cache.store;

import java.util.concurrent.ThreadLocalRandom;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.properties.ZerxCacheProperties;

/**
 * CacheStore 共享工具方法。
 * <p>
 * 提取各 CacheStore 实现中重复的 withJitter / withPrefix 逻辑。
 * </p>
 *
 * @author zerx
 */
final class CacheStoreSupport {

    private CacheStoreSupport() {
    }

    /**
     * 给 TTL 添加随机抖动（防雪崩，±10%）。
     */
    static long withJitter(long ttlNanos) {
        double jitter = CacheConstants.JITTER_MIN
                + ThreadLocalRandom.current().nextDouble() * (CacheConstants.JITTER_MAX - CacheConstants.JITTER_MIN);
        return Math.max(1, (long) (ttlNanos * jitter));
    }

    /**
     * 为 key 添加配置前缀（幂等：已含前缀时不再重复添加）。
     */
    static String withPrefix(String key, ZerxCacheProperties properties) {
        String prefix = properties.getKeyPrefix();
        return key.startsWith(prefix) ? key : prefix + key;
    }
}
