package com.zerx.spring.cache;

/**
 * 布隆过滤器接口 — 用于缓存防穿透的快速前置判空。
 * <p>
 * 布隆过滤器以极低的内存开销提供"可能存在/一定不存在"的概率性判断：
 * <ul>
 *     <li>{@link #mightContain(Object)} 返回 {@code false} → 一定不存在，直接拦截，无需查缓存或数据库</li>
 *     <li>{@link #mightContain(Object)} 返回 {@code true} → 可能存在，继续走正常缓存流程</li>
 * </ul>
 * </p>
 * <p>
 * 布隆过滤器存在假阳性（false positive），但不存在假阴性（false negative）。
 * 适用于读多写少、能容忍偶尔穿透的场景。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * // 业务层预热（如启动时或数据变更时）
 * bloomFilter.put(userId);
 *
 * // 在 CacheOps 中自动集成（前置判空）
 * if (!bloomFilter.mightContain(key)) {
 *     return null; // 一定不存在，拦截穿透
 * }
 * }</pre>
 *
 * @param <T> 元素类型（通常是 String）
 * @author zerx
 */
public interface BloomFilter<T> {

    /**
     * 判断元素是否可能存在
     *
     * @param value 待判断的值
     * @return {@code false} 表示一定不存在；{@code true} 表示可能存在
     */
    boolean mightContain(T value);

    /**
     * 将元素加入布隆过滤器
     *
     * @param value 待加入的值
     */
    void put(T value);

    /**
     * 批量加入元素
     *
     * @param values 待加入的值集合
     */
    default void putAll(Iterable<T> values) {
        if (values != null) {
            for (T value : values) {
                put(value);
            }
        }
    }

    /**
     * 获取当前预计已插入的元素数量
     *
     * @return 预计插入数量
     */
    long expectedInsertions();

    /**
     * 获取预期误判率（假阳性概率）
     *
     * @return 预期误判率，如 0.01 表示 1%
     */
    double fpp();
}
