package com.zerx.common.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 基于 LinkedHashMap 的 LRU（Least Recently Used）缓存
 * <p>
 * 当缓存容量满时，自动淘汰最近最少使用的条目。
 * 线程安全版本请使用 {@link #synchronizedLruCache(int)}。
 * </p>
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 基本用法
 * LruCache<String, User> cache = new LruCache<>(100);
 * cache.put("user:1", user);
 * User user = cache.get("user:1");
 *
 * // 计算型缓存（缓存未命中时自动计算）
 * LruCache<String, Config> cache = LruCache.computing(50, key -> loadFromDb(key));
 * Config config = cache.get("app.config");
 * }</pre>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author zerx
 */
public class LruCache<K, V> {

    /** 最大容量 */
    private final int maxSize;

    /** 内部存储（accessOrder=true 实现按访问顺序排序） */
    private final LinkedHashMap<K, V> store;

    /** 缓存命中次数 */
    private long hitCount;

    /** 缓存未命中次数 */
    private long missCount;

    /**
     * 创建指定容量的 LRU 缓存
     *
     * @param maxSize 最大容量，必须大于 0
     * @throws IllegalArgumentException 容量小于等于 0 时抛出
     */
    public LruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("缓存容量必须大于 0: " + maxSize);
        }
        this.maxSize = maxSize;
        this.store = new LinkedHashMap<>(initialCapacity(maxSize), 0.75f, true);
        this.hitCount = 0;
        this.missCount = 0;
    }

    /**
     * 创建计算型 LRU 缓存
     * <p>
     * 当缓存未命中时，自动使用 {@code loader} 函数计算值并放入缓存。
     * </p>
     *
     * @param maxSize 最大容量
     * @param loader  缓存加载函数
     * @param <K>     键类型
     * @param <V>     值类型
     * @return 计算型缓存包装
     */
    public static <K, V> LruCache<K, V> computing(int maxSize, Function<K, V> loader) {
        return new ComputingLruCache<>(maxSize, loader);
    }

    /**
     * 创建线程安全的 LRU 缓存
     * <p>
     * 通过 synchronized 方法实现线程安全。
     * 高并发场景建议外部使用读写锁或并发工具进行优化。
     * </p>
     *
     * @param maxSize 最大容量
     * @return 线程安全的 LRU 缓存
     */
    public static <K, V> LruCache<K, V> synchronizedLruCache(int maxSize) {
        return new SynchronizedLruCache<>(maxSize);
    }

    // ======================== 基本操作 ========================

    /**
     * 获取缓存值
     * <p>
     * 访问会更新键的最近使用顺序。
     * </p>
     *
     * @param key 键
     * @return 缓存的值，未命中返回 null
     */
    public V get(K key) {
        V value = store.get(key);
        if (value != null) {
            hitCount++;
            return value;
        }
        if (store.containsKey(key)) {
            // 值为 null 但键存在（也视为命中）
            hitCount++;
            return null;
        }
        missCount++;
        return null;
    }

    /**
     * 获取缓存值，未命中时返回默认值
     *
     * @param key          键
     * @param defaultValue 默认值
     * @return 缓存的值或默认值
     */
    public V getOrDefault(K key, V defaultValue) {
        if (containsKey(key)) {
            hitCount++;
            return store.get(key);
        }
        return defaultValue;
    }

    /**
     * 判断缓存是否包含指定键
     * <p>
     * 此操作不会更新键的访问顺序。
     * </p>
     *
     * @param key 键
     * @return 包含返回 true
     */
    public boolean containsKey(K key) {
        return store.containsKey(key);
    }

    /**
     * 放入缓存
     * <p>
     * 如果缓存已满，会自动淘汰最近最少使用的条目。
     * </p>
     *
     * @param key   键
     * @param value 值
     */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "缓存键不能为 null");
        store.put(key, value);
        evictIfNeeded();
    }

    /**
     * 如果键不存在则放入缓存
     *
     * @param key   键
     * @param value 值
     * @return 如果之前已存在则返回旧值，否则返回 null
     */
    public V putIfAbsent(K key, V value) {
        Objects.requireNonNull(key, "缓存键不能为 null");
        if (store.containsKey(key)) {
            hitCount++;
            return store.get(key);
        }
        store.put(key, value);
        evictIfNeeded();
        return null;
    }

    /**
     * 获取缓存值，未命中时使用 loader 计算并存入
     *
     * @param key    键
     * @param loader 加载函数
     * @return 缓存或计算后的值
     */
    public V getOrCompute(K key, Function<K, V> loader) {
        V value = get(key);
        if (value != null || containsKey(key)) {
            return value;
        }
        value = loader.apply(key);
        if (value != null) {
            put(key, value);
        }
        return value;
    }

    // ======================== 删除 ========================

    /**
     * 移除指定键
     *
     * @param key 键
     * @return 被移除的值，不存在返回 null
     */
    public V remove(K key) {
        return store.remove(key);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        store.clear();
        hitCount = 0;
        missCount = 0;
    }

    // ======================== 状态 ========================

    /**
     * 获取当前缓存大小
     *
     * @return 缓存条目数
     */
    public int size() {
        return store.size();
    }

    /**
     * 获取最大容量
     *
     * @return 最大容量
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * 判断缓存是否为空
     *
     * @return 为空返回 true
     */
    public boolean isEmpty() {
        return store.isEmpty();
    }

    /**
     * 判断缓存是否已满
     *
     * @return 已满返回 true
     */
    public boolean isFull() {
        return store.size() >= maxSize;
    }

    /**
     * 获取缓存命中率
     *
     * @return 命中率（0.0 ~ 1.0），无访问时返回 0.0
     */
    public double hitRate() {
        long total = hitCount + missCount;
        return total == 0 ? 0.0 : (double) hitCount / total;
    }

    /**
     * 获取缓存命中次数
     *
     * @return 命中次数
     */
    public long getHitCount() {
        return hitCount;
    }

    /**
     * 获取缓存未命中次数
     *
     * @return 未命中次数
     */
    public long getMissCount() {
        return missCount;
    }

    /**
     * 获取所有缓存条目的只读视图
     *
     * @return 只读 Map
     */
    public Map<K, V> asMap() {
        return Collections.unmodifiableMap(store);
    }

    // ======================== 内部方法 ========================

    /**
     * 计算合理的初始容量（避免扩容）
     * <p>
     * 加载因子 0.75，初始容量 = 最大容量 / 0.75 + 1
     * </p>
     */
    private static int initialCapacity(int maxSize) {
        return (int) Math.ceil(maxSize / 0.75f) + 1;
    }

    /**
     * 淘汰多余的条目
     */
    protected void evictIfNeeded() {
        while (store.size() > maxSize) {
            // LinkedHashMap 在 accessOrder=true 时，迭代器第一个元素就是最近最少使用的
            var iterator = store.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    // ======================== 内部实现 ========================

    /**
     * 计算型 LRU 缓存
     * <p>
     * 覆盖 get 方法，未命中时自动计算并缓存。
     * </p>
     */
    private static final class ComputingLruCache<K, V> extends LruCache<K, V> {

        private final Function<K, V> loader;

        ComputingLruCache(int maxSize, Function<K, V> loader) {
            super(maxSize);
            this.loader = Objects.requireNonNull(loader, "加载函数不能为 null");
        }

        @Override
        public V get(K key) {
            V value = super.get(key);
            if (value != null || containsKey(key)) {
                return value;
            }
            value = loader.apply(key);
            if (value != null) {
                put(key, value);
            }
            return value;
        }
    }

    /**
     * 线程安全 LRU 缓存
     * <p>
     * 通过 synchronized 方法包装实现基本的线程安全。
     * </p>
     */
    private static final class SynchronizedLruCache<K, V> extends LruCache<K, V> {

        SynchronizedLruCache(int maxSize) {
            super(maxSize);
        }

        @Override
        public synchronized V get(K key) {
            return super.get(key);
        }

        @Override
        public synchronized V getOrDefault(K key, V defaultValue) {
            return super.getOrDefault(key, defaultValue);
        }

        @Override
        public synchronized boolean containsKey(K key) {
            return super.containsKey(key);
        }

        @Override
        public synchronized void put(K key, V value) {
            super.put(key, value);
        }

        @Override
        public synchronized V putIfAbsent(K key, V value) {
            return super.putIfAbsent(key, value);
        }

        @Override
        public synchronized V getOrCompute(K key, Function<K, V> loader) {
            return super.getOrCompute(key, loader);
        }

        @Override
        public synchronized V remove(K key) {
            return super.remove(key);
        }

        @Override
        public synchronized void clear() {
            super.clear();
        }

        @Override
        public synchronized int size() {
            return super.size();
        }

        @Override
        public synchronized boolean isEmpty() {
            return super.isEmpty();
        }

        @Override
        public synchronized boolean isFull() {
            return super.isFull();
        }

        @Override
        public synchronized double hitRate() {
            return super.hitRate();
        }

        @Override
        public synchronized long getHitCount() {
            return super.getHitCount();
        }

        @Override
        public synchronized long getMissCount() {
            return super.getMissCount();
        }

        @Override
        public synchronized Map<K, V> asMap() {
            return super.asMap();
        }
    }
}
