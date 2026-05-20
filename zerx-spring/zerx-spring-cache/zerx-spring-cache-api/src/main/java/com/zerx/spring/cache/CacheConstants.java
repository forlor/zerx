package com.zerx.spring.cache;

/**
 * 缓存模块全局常量。
 *
 * @author zerx
 */
public final class CacheConstants {

    private CacheConstants() {
    }

    /**
     * 空值标记（防穿透）。
     * <p>
     * 当 loader 返回 null 时，缓存此标记以防止反复穿透到数据源。
     * 所有 CacheStore 实现必须识别此值并在读取时将其视为"缓存命中但值为空"。
     * </p>
     */
    public static final String NULL_MARKER = "__ZERX_CACHE_NULL__";

    /**
     * Redis Pub/Sub 缓存失效通知频道前缀。
     * <p>
     * 多级缓存模式下，节点通过此前缀的频道广播本地 L1 失效事件。
     * 完整频道格式：{@code zerx:cache:invalidate:{fullKey}}
     * </p>
     */
    public static final String INVALIDATION_CHANNEL_PREFIX = "zerx:cache:invalidate:";

    /**
     * 默认 TTL 随机抖动因子范围：0.9 ~ 1.1（±10%）。
     * <p>
     * 用于防雪崩：同一时间写入的大量缓存不会在同一时刻集体过期。
     * </p>
     */
    public static final double JITTER_MIN = 0.9;
    public static final double JITTER_MAX = 1.1;
}
