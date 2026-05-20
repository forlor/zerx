package com.zerx.spring.cache.ops;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 分段锁 — 基于固定数组避免 per-key 锁的内存泄漏。
 * <p>
 * 使用 key 的 hashCode 对固定数量的锁分段取模，
 * 不同 key 可能共享同一把锁（轻微降低并发度），
 * 但避免了 {@link java.util.concurrent.ConcurrentHashMap} 无限增长的问题。
 * <p>
 * STRIPE_COUNT 为 2 的幂，使用位运算替代取模以提升性能。
 *
 * @author zerx
 */
public final class StripedLock {

    private static final int STRIPE_COUNT = 64;
    private static final int INDEX_MASK = STRIPE_COUNT - 1;

    private final ReentrantLock[] stripes;

    public StripedLock() {
        stripes = new ReentrantLock[STRIPE_COUNT];
        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    /**
     * 根据 key 获取对应的分段锁。
     */
    public ReentrantLock get(String key) {
        return stripes[key.hashCode() & Integer.MAX_VALUE & INDEX_MASK];
    }

    /**
     * 根据 key 获取对应的分段锁（Object 版本）。
     * <p>
     * 统一委托给 {@link #get(String)}，确保同一逻辑 key 无论以 String 还是 Object
     * 传入都映射到同一把锁。
     */
    public ReentrantLock get(Object key) {
        return get(String.valueOf(key));
    }
}
