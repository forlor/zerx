package com.zerx.spring.cache.manager;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;

/**
 * Zerx CacheManager — 适配 Spring Cache 抽象层。
 * <p>
 * 实现 {@link CacheManager} 接口，桥接 {@link CacheStore}，
 * 使 {@code @Cacheable}、{@code @CacheEvict}、{@code @CachePut} 等
 * Spring 原生注解也能使用 Zerx 的缓存基础设施。
 * </p>
 * <p>
 * 支持 per-cache-name TTL：通过 {@code zerx.cache.custom-ttls.<cacheName>=30m}
 * 为不同 cache name 配置独立 TTL，未配置的回退到全局默认值。
 * </p>
 *
 * @author zerx
 */
public class ZerxCacheManager implements CacheManager {

    private final CacheStore cacheStore;
    private final ZerxCacheProperties properties;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    public ZerxCacheManager(CacheStore cacheStore, ZerxCacheProperties properties) {
        this.cacheStore = cacheStore;
        this.properties = properties;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createCache);
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }

    private Cache createCache(String name) {
        Duration ttl = properties.getTtlForCache(name);
        return new ZerxCacheAdapter(name, cacheStore, ttl);
    }

    /**
     * Spring Cache 接口的适配实现。
     */
    static class ZerxCacheAdapter implements Cache {

        private final String name;
        private final CacheStore store;
        private final Duration defaultTtl;

        ZerxCacheAdapter(String name, CacheStore store, Duration defaultTtl) {
            this.name = name;
            this.store = store;
            this.defaultTtl = defaultTtl;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return store;
        }

        @Override
        public ValueWrapper get(Object key) {
            Optional<Object> value = store.get(name + ":" + key);
            if (value.isEmpty()) {
                return null;
            }
            Object v = value.get();
            if (CacheConstants.NULL_MARKER.equals(v)) {
                return () -> null;
            }
            return () -> v;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Class<T> type) {
            Optional<Object> value = store.get(name + ":" + key);
            return value.filter(v -> !CacheConstants.NULL_MARKER.equals(v))
                    .map(v -> (T) v)
                    .orElse(null);
        }

        private final ConcurrentHashMap<Object, Lock> localLocks = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            // 先无锁查询一次
            Optional<Object> existing = store.get(name + ":" + key);
            if (existing.isPresent()) {
                Object existingValue = existing.get();
                if (CacheConstants.NULL_MARKER.equals(existingValue)) {
                    return null;
                }
                return (T) existingValue;
            }

            // 获取细粒度锁防击穿
            Lock lock = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
            lock.lock();
            try {
                // 加锁后再查一次（双重检查）
                existing = store.get(name + ":" + key);
                if (existing.isPresent()) {
                    Object existingValue = existing.get();
                    if (CacheConstants.NULL_MARKER.equals(existingValue)) {
                        return null;
                    }
                    return (T) existingValue;
                }
                // 执行回源
                T value = valueLoader.call();
                Object cacheValue = value == null ? CacheConstants.NULL_MARKER : value;
                store.set(name + ":" + key, cacheValue, defaultTtl);
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            } finally {
                lock.unlock();
                // 考虑到内存泄漏，在多线程高度并发下直接 remove 可能造成后来的线程获取不到正确的锁，
                // 实际生产中可使用更高级的基于引用的锁缓存，或定时清理，这里为了简洁和有效暂不主动 remove，
                // 但为了避免 key 过多，可以简单在此处移除（如果在高并发场景下可能稍微降低一点点安全性，但能保证防内存泄漏）
                localLocks.remove(key);
            }
        }

        @Override
        public void put(Object key, Object value) {
            Object cacheValue = value == null ? CacheConstants.NULL_MARKER : value;
            store.set(name + ":" + key, cacheValue, defaultTtl);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            Optional<Object> existing = store.get(name + ":" + key);
            if (existing.isPresent()) {
                Object existingValue = existing.get();
                if (CacheConstants.NULL_MARKER.equals(existingValue)) {
                    return () -> null;
                }
                return () -> existingValue;
            }
            Object cacheValue = value == null ? CacheConstants.NULL_MARKER : value;
            store.set(name + ":" + key, cacheValue, defaultTtl);
            return null;
        }

        @Override
        public void evict(Object key) {
            store.evict(name + ":" + key);
        }

        @Override
        public boolean evictIfPresent(Object key) {
            if (store.hasKey(name + ":" + key)) {
                store.evict(name + ":" + key);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            store.evictByPrefix(name + ":");
        }

        @Override
        public boolean invalidate() {
            store.evictByPrefix(name + ":");
            return true;
        }
    }
}
