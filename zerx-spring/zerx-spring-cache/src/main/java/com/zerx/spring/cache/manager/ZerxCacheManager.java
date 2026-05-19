package com.zerx.spring.cache.manager;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

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
                return null;
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

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            Optional<Object> existing = store.get(name + ":" + key);
            if (existing.isPresent() && !CacheConstants.NULL_MARKER.equals(existing.get())) {
                return (T) existing.get();
            }
            try {
                T value = valueLoader.call();
                if (value != null) {
                    store.set(name + ":" + key, value, defaultTtl);
                }
                return value;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            store.set(name + ":" + key, value, defaultTtl);
        }

        @Override
        public ValueWrapper putIfAbsent(Object key, Object value) {
            Optional<Object> existing = store.get(name + ":" + key);
            if (existing.isPresent()) {
                Object existingValue = existing.get();
                if (!CacheConstants.NULL_MARKER.equals(existingValue)) {
                    return () -> existingValue;
                }
            }
            store.set(name + ":" + key, value, defaultTtl);
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
