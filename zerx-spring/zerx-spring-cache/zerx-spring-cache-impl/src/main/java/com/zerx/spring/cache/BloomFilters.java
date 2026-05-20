package com.zerx.spring.cache;

/**
 * 布隆过滤器工厂 — 创建不同策略的布隆过滤器实例。
 * <p>
 * 缓存模块提供基于位图的默认实现 {@link com.zerx.spring.cache.bloom.DefaultBloomFilter}，
 * 业务层也可通过此工厂创建独立的布隆过滤器用于其他防穿透场景。
 * </p>
 *
 * @author zerx
 */
public final class BloomFilters {

    private BloomFilters() {
    }

    /**
     * 创建基于位图的本地布隆过滤器
     *
     * @param expectedInsertions 预计插入数量
     * @param fpp                预期误判率（false positive probability），如 0.01
     * @param <T>                元素类型
     * @return 布隆过滤器实例
     * @throws IllegalArgumentException 参数不合法时抛出
     */
    public static <T> BloomFilter<T> create(long expectedInsertions, double fpp) {
        return new com.zerx.spring.cache.bloom.DefaultBloomFilter<>(expectedInsertions, fpp);
    }

    /**
     * 创建基于位图的本地布隆过滤器（默认 1% 误判率）
     *
     * @param expectedInsertions 预计插入数量
     * @param <T>                元素类型
     * @return 布隆过滤器实例
     */
    public static <T> BloomFilter<T> create(long expectedInsertions) {
        return create(expectedInsertions, 0.01);
    }
}
