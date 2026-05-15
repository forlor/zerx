package com.zerx.common.logging;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 日志限流器
 * <p>
 * 防止同类日志在短时间内大量重复输出，保护磁盘 I/O 和日志存储空间。
 * 基于"滑动窗口计数器"算法，纯 JDK 实现，线程安全。
 * </p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>按 key 独立限流：同一限流器实例可对多个不同的 key 分别计数</li>
 *   <li>自动抑制与摘要：超出阈值的日志被抑制，窗口结束后输出一条摘要</li>
 *   <li>惰性清理：长时间未使用的 key 自动过期，避免内存泄漏</li>
 *   <li>线程安全：基于 ConcurrentHashMap + AtomicLong，支持高并发场景</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 创建限流器：每分钟最多 10 条
 * private static final LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofMinutes(1));
 *
 * // 在日志输出前检查
 * if (limiter.tryAcquire("orderTimeout")) {
 *     log.error("订单超时, orderId={}", orderId);
 * }
 * // 第 11 条日志被抑制，窗口结束后输出摘要：
 * // "[LogRateLimiter] 'orderTimeout': suppressed 999 messages in last 60s"
 *
 * // 获取摘要（用于定时输出）
 * String summary = limiter.getSummary("orderTimeout");
 * }</pre>
 *
 * @author zerx
 */
public final class LogRateLimiter {

    /**
     * 单个 key 的限流状态
     *
     * @param windowStartNs 窗口起始时间（纳秒）
     * @param count         当前窗口内已通过的消息数
     * @param suppressed    当前窗口内被抑制的消息数
     */
    private record Bucket(long windowStartNs, AtomicLong count, LongAdder suppressed) {
        Bucket(long windowStartNs) {
            this(windowStartNs, new AtomicLong(0), new LongAdder());
        }

        boolean isExpired(long nowNs, long windowSizeNs) {
            return nowNs - windowStartNs >= windowSizeNs;
        }
    }

    /** 每个窗口允许通过的最大消息数 */
    private final long maxPerWindow;

    /** 窗口大小（纳秒） */
    private final long windowSizeNs;

    /** 所有 key 的限流桶 */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** key 过期时间（纳秒），默认为窗口大小的 5 倍 */
    private final long keyExpiryNs;

    private LogRateLimiter(long maxPerWindow, Duration windowDuration) {
        if (maxPerWindow <= 0) {
            throw new IllegalArgumentException("maxPerWindow 必须大于 0，当前: " + maxPerWindow);
        }
        Objects.requireNonNull(windowDuration, "windowDuration 不能为 null");
        if (windowDuration.isNegative() || windowDuration.isZero()) {
            throw new IllegalArgumentException("windowDuration 必须大于 0，当前: " + windowDuration);
        }
        this.maxPerWindow = maxPerWindow;
        this.windowSizeNs = windowDuration.toNanos();
        this.keyExpiryNs = windowSizeNs * 5;
    }

    // ======================== 工厂方法 ========================

    /**
     * 创建日志限流器
     *
     * @param maxPerWindow  每个时间窗口允许通过的最大消息数
     * @param windowDuration 时间窗口大小
     * @return 限流器实例
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static LogRateLimiter of(long maxPerWindow, Duration windowDuration) {
        return new LogRateLimiter(maxPerWindow, windowDuration);
    }

    /**
     * 创建默认日志限流器：每秒最多 10 条
     *
     * @return 限流器实例
     */
    public static LogRateLimiter defaultLimiter() {
        return new LogRateLimiter(10, Duration.ofSeconds(1));
    }

    // ======================== 核心方法 ========================

    /**
     * 尝试获取日志输出许可
     * <p>
     * 如果当前窗口内该 key 的通过次数未达上限，计数并返回 true；
     * 否则增加抑制计数并返回 false。
     * </p>
     *
     * @param key 限流 key（通常为日志标识，如异常类名、业务事件名）
     * @return true 表示允许输出日志，false 表示应抑制
     */
    public boolean tryAcquire(String key) {
        Objects.requireNonNull(key, "限流 key 不能为 null");
        long nowNs = System.nanoTime();

        Bucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(nowNs, windowSizeNs)) {
                return new Bucket(nowNs);
            }
            return existing;
        });

        long current = bucket.count().getAndIncrement();
        if (current < maxPerWindow) {
            return true;
        }
        // 超出阈值，计入抑制数
        bucket.suppressed().increment();
        return false;
    }

    /**
     * 获取指定 key 的抑制摘要
     * <p>
     * 返回该 key 在当前窗口内被抑制的消息数。如果窗口已过期，
     * 会先重置窗口并返回上一窗口的抑制摘要。
     * </p>
     *
     * @param key 限流 key
     * @return 摘要字符串，如果该 key 没有被抑制的消息则返回 null
     */
    public String getSummary(String key) {
        Objects.requireNonNull(key, "限流 key 不能为 null");
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return null;
        }

        long suppressed = bucket.suppressed().sum();
        if (suppressed == 0) {
            return null;
        }

        long windowMs = windowSizeNs / 1_000_000;
        String summary = "[LogRateLimiter] '" + key + "': suppressed " + suppressed
                + " messages in last " + windowMs + "ms";

        // 如果窗口已过期，重置计数器以便下一窗口重新统计
        long nowNs = System.nanoTime();
        if (bucket.isExpired(nowNs, windowSizeNs)) {
            buckets.computeIfPresent(key, (k, existing) -> {
                if (existing.isExpired(nowNs, windowSizeNs)) {
                    return new Bucket(nowNs);
                }
                return existing;
            });
        }

        return summary;
    }

    /**
     * 获取所有 key 的抑制摘要（惰性清理过期的 key）
     *
     * @return 摘要字符串列表（仅包含有被抑制消息的 key）
     */
    public Map<String, String> getAllSummaries() {
        long nowNs = System.nanoTime();
        ConcurrentHashMap<String, String> summaries = new ConcurrentHashMap<>();

        buckets.forEach((key, bucket) -> {
            // 惰性清理：超过过期时间的 key 直接移除
            if (nowNs - bucket.windowStartNs() >= keyExpiryNs) {
                buckets.remove(key, bucket);
                return;
            }
            long suppressed = bucket.suppressed().sum();
            if (suppressed > 0) {
                long windowMs = windowSizeNs / 1_000_000;
                summaries.put(key, "[LogRateLimiter] '" + key + "': suppressed " + suppressed
                        + " messages in last " + windowMs + "ms");
            }
        });

        return summaries;
    }

    // ======================== 管理方法 ========================

    /**
     * 重置指定 key 的计数器
     *
     * @param key 限流 key
     */
    public void reset(String key) {
        buckets.remove(key);
    }

    /**
     * 重置所有 key 的计数器
     */
    public void resetAll() {
        buckets.clear();
    }

    /**
     * 获取当前管理的 key 数量
     *
     * @return key 数量
     */
    public int size() {
        return buckets.size();
    }

    /**
     * 获取每个窗口允许通过的最大消息数
     *
     * @return 最大消息数
     */
    public long getMaxPerWindow() {
        return maxPerWindow;
    }

    /**
     * 获取窗口大小
     *
     * @return 窗口大小
     */
    public Duration getWindowDuration() {
        return Duration.ofNanos(windowSizeNs);
    }

    /**
     * 获取指定 key 在当前窗口内已通过的消息数
     *
     * @param key 限流 key
     * @return 已通过的消息数，key 不存在时返回 0
     */
    public long getPassedCount(String key) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return 0;
        }
        // 窗口过期时不返回旧值
        if (bucket.isExpired(System.nanoTime(), windowSizeNs)) {
            return 0;
        }
        return Math.min(bucket.count().get(), maxPerWindow);
    }

    /**
     * 获取指定 key 在当前窗口内被抑制的消息数
     *
     * @param key 限流 key
     * @return 被抑制的消息数，key 不存在时返回 0
     */
    public long getSuppressedCount(String key) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return 0;
        }
        return bucket.suppressed().sum();
    }
}
