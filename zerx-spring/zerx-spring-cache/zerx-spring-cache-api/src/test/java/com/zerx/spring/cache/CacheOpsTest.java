package com.zerx.spring.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CacheOps} 接口契约测试
 * <p>
 * 使用基于 {@link InMemoryCacheStore} 的 {@link InMemoryCacheOps} 实现，
 * 验证 Cache-Aside 加载、防穿透、读穿等语义。
 * </p>
 *
 * @author zerx
 */
class CacheOpsTest {

    /**
     * 简单的 CacheOps 实现，用于接口契约测试
     */
    static class InMemoryCacheOps implements CacheOps {

        private final Map<String, Object> store = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key, java.util.function.Supplier<T> loader, long ttl, TimeUnit timeUnit) {
            Object cached = store.get(key);
            if (cached != null) {
                if (cached == CacheConstants.NULL_MARKER) {
                    return null; // 防穿透命中
                }
                return (T) cached;
            }
            T value = loader.get();
            store.put(key, value != null ? value : CacheConstants.NULL_MARKER);
            return value;
        }

        @Override
        public <T> Optional<T> getOptional(String key, java.util.function.Supplier<T> loader, long ttl, TimeUnit unit) {
            return Optional.ofNullable(get(key, loader, ttl, unit));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(String key) {
            Object cached = store.get(key);
            if (cached == null || cached == CacheConstants.NULL_MARKER) {
                return null;
            }
            return (T) cached;
        }

        @Override
        public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
            store.put(key, value);
        }

        @Override
        public void set(String key, Object value, Duration ttl) {
            store.put(key, value);
        }

        @Override
        public void evict(String key) {
            store.remove(key);
        }

        @Override
        public void evictByPrefix(String keyPrefix) {
            store.keySet().removeIf(k -> k.startsWith(keyPrefix));
        }

        @Override
        public boolean hasKey(String key) {
            return store.containsKey(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Map<String, T> getAll(Collection<String> keys,
                                         java.util.function.Function<Collection<String>, Map<String, T>> loader,
                                         long ttl, TimeUnit unit) {
            Map<String, T> result = new HashMap<>();
            List<String> missingKeys = new ArrayList<>();
            for (String key : keys) {
                Object cached = store.get(key);
                if (cached != null && cached != CacheConstants.NULL_MARKER) {
                    result.put(key, (T) cached);
                } else {
                    missingKeys.add(key);
                }
            }
            if (!missingKeys.isEmpty()) {
                Map<String, T> loaded = loader.apply(missingKeys);
                if (loaded != null) {
                    loaded.forEach((k, v) -> {
                        store.put(k, v != null ? v : CacheConstants.NULL_MARKER);
                        if (v != null) result.put(k, v);
                    });
                }
            }
            return result;
        }

        @Override
        public CacheStore getStore() {
            // CacheOpsTest 内部不依赖 CacheStoreTest 的实现，
            // 返回一个最小 CacheStore 适配
            return new CacheStore() {
                private final Map<String, Object> s = new HashMap<>();
                public java.util.Optional<Object> get(String k) { return java.util.Optional.ofNullable(s.get(k)); }
                public void set(String k, Object v, java.time.Duration t) { s.put(k, v); }
                public void evict(String k) { s.remove(k); }
                public void evictByPrefix(String p) { s.keySet().removeIf(x -> x.startsWith(p)); }
                public boolean hasKey(String k) { return s.containsKey(k); }
                public Map<String, Object> multiGet(Collection<String> ks) { var r = new HashMap<String, Object>(); for (var k : ks) { var v = s.get(k); if (v != null) r.put(k, v); } return r; }
                public void multiSet(Map<String, Object> e, java.time.Duration t) { s.putAll(e); }
                public void multiEvict(Collection<String> ks) { ks.forEach(s::remove); }
            };
        }
    }

    private InMemoryCacheOps cacheOps;

    @Nested
    @DisplayName("Cache-Aside 加载")
    class CacheAsideTest {

        @Test
        @DisplayName("get — 缓存 miss 时调用 loader 并回填")
        void get_miss_loads() {
            cacheOps = new InMemoryCacheOps();
            AtomicInteger loadCount = new AtomicInteger(0);

            String value = cacheOps.get("key1", () -> {
                loadCount.incrementAndGet();
                return "loaded";
            }, 30, TimeUnit.MINUTES);

            assertEquals("loaded", value);
            assertEquals(1, loadCount.get());
        }

        @Test
        @DisplayName("get — 缓存命中时不调用 loader")
        void get_hit_noLoad() {
            cacheOps = new InMemoryCacheOps();
            AtomicInteger loadCount = new AtomicInteger(0);

            cacheOps.get("key1", () -> {
                loadCount.incrementAndGet();
                return "first";
            }, 30, TimeUnit.MINUTES);

            String value = cacheOps.get("key1", () -> {
                loadCount.incrementAndGet();
                return "second";
            }, 30, TimeUnit.MINUTES);

            assertEquals("first", value);
            assertEquals(1, loadCount.get());
        }

        @Test
        @DisplayName("get — loader 返回 null 时缓存空值（防穿透）")
        void get_null_cachesNullMarker() {
            cacheOps = new InMemoryCacheOps();
            AtomicInteger loadCount = new AtomicInteger(0);

            String value1 = cacheOps.get("key1", () -> {
                loadCount.incrementAndGet();
                return null;
            }, 30, TimeUnit.MINUTES);

            assertNull(value1);
            assertEquals(1, loadCount.get());

            // 第二次 get 应该命中空值标记，不再调用 loader
            String value2 = cacheOps.get("key1", () -> {
                loadCount.incrementAndGet();
                return "should-not-load";
            }, 30, TimeUnit.MINUTES);

            assertNull(value2);
            assertEquals(1, loadCount.get()); // loader 仍然只被调用一次
        }

        @Test
        @DisplayName("getOptional — 正常值返回 Optional.of")
        void getOptional_present() {
            cacheOps = new InMemoryCacheOps();
            Optional<String> result = cacheOps.getOptional("key1", () -> "value", 30, TimeUnit.MINUTES);
            assertTrue(result.isPresent());
            assertEquals("value", result.get());
        }

        @Test
        @DisplayName("getOptional — loader 返回 null 时返回 empty")
        void getOptional_empty() {
            cacheOps = new InMemoryCacheOps();
            Optional<String> result = cacheOps.getOptional("key1", () -> null, 30, TimeUnit.MINUTES);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("直接操作")
    class DirectOperationsTest {

        @Test
        @DisplayName("get（无 loader）— 缓存命中")
        void getDirect_hit() {
            cacheOps = new InMemoryCacheOps();
            cacheOps.set("key1", "value1", Duration.ofMinutes(10));
            assertEquals("value1", cacheOps.get("key1"));
        }

        @Test
        @DisplayName("get（无 loader）— 缓存不存在返回 null")
        void getDirect_miss() {
            cacheOps = new InMemoryCacheOps();
            assertNull(cacheOps.get("nonexistent"));
        }

        @Test
        @DisplayName("set + evict — 写入后删除")
        void setThenEvict() {
            cacheOps = new InMemoryCacheOps();
            cacheOps.set("key1", "value1", Duration.ofMinutes(10));
            assertTrue(cacheOps.hasKey("key1"));

            cacheOps.evict("key1");
            assertFalse(cacheOps.hasKey("key1"));
            assertNull(cacheOps.get("key1"));
        }

        @Test
        @DisplayName("set（Duration 版本）— 写入后可读取")
        void setWithDuration() {
            cacheOps = new InMemoryCacheOps();
            cacheOps.set("key1", "value1", Duration.ofSeconds(60));
            assertEquals("value1", cacheOps.get("key1"));
        }

        @Test
        @DisplayName("hasKey — 键存在")
        void hasKey_true() {
            cacheOps = new InMemoryCacheOps();
            cacheOps.set("key1", "v1", Duration.ofMinutes(10));
            assertTrue(cacheOps.hasKey("key1"));
        }

        @Test
        @DisplayName("hasKey — 键不存在")
        void hasKey_false() {
            cacheOps = new InMemoryCacheOps();
            assertFalse(cacheOps.hasKey("nonexistent"));
        }
    }

    @Nested
    @DisplayName("前缀删除")
    class PrefixEvictTest {

        @Test
        @DisplayName("evictByPrefix — 按前缀批量删除")
        void evictByPrefix() {
            cacheOps = new InMemoryCacheOps();
            cacheOps.set("user:1", "u1", Duration.ofMinutes(10));
            cacheOps.set("user:2", "u2", Duration.ofMinutes(10));
            cacheOps.set("order:1", "o1", Duration.ofMinutes(10));

            cacheOps.evictByPrefix("user:");
            assertNull(cacheOps.get("user:1"));
            assertNull(cacheOps.get("user:2"));
            assertEquals("o1", cacheOps.get("order:1"));
        }
    }

    @Nested
    @DisplayName("getStore")
    class GetStoreTest {

        @Test
        @DisplayName("getStore 返回非 null 的 CacheStore 实例")
        void getStore_notNull() {
            cacheOps = new InMemoryCacheOps();
            assertNotNull(cacheOps.getStore());
            assertTrue(cacheOps.getStore() instanceof CacheStore);
        }
    }
}
