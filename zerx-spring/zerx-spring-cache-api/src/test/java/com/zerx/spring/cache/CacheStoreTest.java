package com.zerx.spring.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CacheStore} 接口契约测试
 * <p>
 * 使用内存 Map 模拟 CacheStore 实现，验证接口方法的语义契约。
 * 真实实现（Caffeine/Redis/Multilevel）的测试在其各自模块中。
 * </p>
 *
 * @author zerx
 */
class CacheStoreTest {

    /**
     * 基于 HashMap 的最小 CacheStore 实现，仅用于契约测试
     */
    static class InMemoryCacheStore implements CacheStore {

        private final Map<String, Object> store = new HashMap<>();

        @Override
        public Optional<Object> get(String key) {
            return Optional.ofNullable(store.get(key));
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
        public void evictByPrefix(String prefix) {
            store.keySet().removeIf(k -> k.startsWith(prefix));
        }

        @Override
        public boolean hasKey(String key) {
            return store.containsKey(key);
        }

        @Override
        public Map<String, Object> multiGet(Collection<String> keys) {
            Map<String, Object> result = new HashMap<>();
            for (String key : keys) {
                Object val = store.get(key);
                if (val != null) {
                    result.put(key, val);
                }
            }
            return result;
        }

        @Override
        public void multiSet(Map<String, Object> entries, Duration ttl) {
            store.putAll(entries);
        }

        @Override
        public void multiEvict(Collection<String> keys) {
            keys.forEach(store::remove);
        }
    }

    private InMemoryCacheStore cacheStore;

    @Nested
    @DisplayName("基本 CRUD 操作")
    class BasicOperationsTest {

        @Test
        @DisplayName("get — 键不存在时返回 empty")
        void get_miss() {
            cacheStore = new InMemoryCacheStore();
            assertTrue(cacheStore.get("nonexistent").isEmpty());
        }

        @Test
        @DisplayName("get — 写入后能读取到值")
        void get_hit() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("key1", "value1", Duration.ofMinutes(10));
            Optional<Object> result = cacheStore.get("key1");
            assertTrue(result.isPresent());
            assertEquals("value1", result.get());
        }

        @Test
        @DisplayName("get — NULL_MARKER 可被缓存和读取（防穿透）")
        void get_nullMarker() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("key1", CacheConstants.NULL_MARKER, Duration.ofMinutes(10));
            Optional<Object> result = cacheStore.get("key1");
            assertTrue(result.isPresent());
            assertEquals(CacheConstants.NULL_MARKER, result.get());
        }

        @Test
        @DisplayName("set — 覆盖已存在的值")
        void set_overwrite() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("key1", "old", Duration.ofMinutes(10));
            cacheStore.set("key1", "new", Duration.ofMinutes(10));
            assertEquals("new", cacheStore.get("key1").orElse(null));
        }

        @Test
        @DisplayName("evict — 删除已存在的键")
        void evict_existing() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("key1", "value1", Duration.ofMinutes(10));
            cacheStore.evict("key1");
            assertTrue(cacheStore.get("key1").isEmpty());
        }

        @Test
        @DisplayName("evict — 删除不存在的键不抛异常")
        void evict_nonexistent() {
            cacheStore = new InMemoryCacheStore();
            assertDoesNotThrow(() -> cacheStore.evict("nonexistent"));
        }

        @Test
        @DisplayName("hasKey — 键存在返回 true")
        void hasKey_true() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("key1", "value1", Duration.ofMinutes(10));
            assertTrue(cacheStore.hasKey("key1"));
        }

        @Test
        @DisplayName("hasKey — 键不存在返回 false")
        void hasKey_false() {
            cacheStore = new InMemoryCacheStore();
            assertFalse(cacheStore.hasKey("nonexistent"));
        }
    }

    @Nested
    @DisplayName("批量操作")
    class BatchOperationsTest {

        @Test
        @DisplayName("multiGet — 批量获取存在的键")
        void multiGet_existing() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("k1", "v1", Duration.ofMinutes(10));
            cacheStore.set("k2", "v2", Duration.ofMinutes(10));
            cacheStore.set("k3", "v3", Duration.ofMinutes(10));

            Map<String, Object> result = cacheStore.multiGet(List.of("k1", "k2", "k3"));
            assertEquals(3, result.size());
            assertEquals("v1", result.get("k1"));
            assertEquals("v2", result.get("k2"));
            assertEquals("v3", result.get("k3"));
        }

        @Test
        @DisplayName("multiGet — 混合存在的键和不存在的键")
        void multiGet_partial() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("k1", "v1", Duration.ofMinutes(10));

            Map<String, Object> result = cacheStore.multiGet(List.of("k1", "k2", "k3"));
            assertEquals(1, result.size());
            assertEquals("v1", result.get("k1"));
            assertFalse(result.containsKey("k2"));
            assertFalse(result.containsKey("k3"));
        }

        @Test
        @DisplayName("multiGet — 空键集合返回空 Map")
        void multiGet_empty() {
            cacheStore = new InMemoryCacheStore();
            assertTrue(cacheStore.multiGet(List.of()).isEmpty());
        }

        @Test
        @DisplayName("multiSet — 批量写入")
        void multiSet() {
            cacheStore = new InMemoryCacheStore();
            Map<String, Object> entries = Map.of("k1", "v1", "k2", "v2", "k3", "v3");
            cacheStore.multiSet(entries, Duration.ofMinutes(10));

            assertEquals(3, cacheStore.multiGet(List.of("k1", "k2", "k3")).size());
        }

        @Test
        @DisplayName("multiEvict — 批量删除")
        void multiEvict() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("k1", "v1", Duration.ofMinutes(10));
            cacheStore.set("k2", "v2", Duration.ofMinutes(10));
            cacheStore.set("k3", "v3", Duration.ofMinutes(10));

            cacheStore.multiEvict(List.of("k1", "k3"));
            assertTrue(cacheStore.get("k1").isEmpty());
            assertTrue(cacheStore.get("k3").isEmpty());
            assertTrue(cacheStore.get("k2").isPresent());
        }

        @Test
        @DisplayName("multiEvict — 空集合不抛异常")
        void multiEvict_empty() {
            cacheStore = new InMemoryCacheStore();
            assertDoesNotThrow(() -> cacheStore.multiEvict(List.of()));
        }
    }

    @Nested
    @DisplayName("前缀删除")
    class PrefixEvictTest {

        @Test
        @DisplayName("evictByPrefix — 按前缀批量删除")
        void evictByPrefix() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("user:1", "data1", Duration.ofMinutes(10));
            cacheStore.set("user:2", "data2", Duration.ofMinutes(10));
            cacheStore.set("user:3", "data3", Duration.ofMinutes(10));
            cacheStore.set("order:1", "order1", Duration.ofMinutes(10));

            cacheStore.evictByPrefix("user:");
            assertTrue(cacheStore.get("user:1").isEmpty());
            assertTrue(cacheStore.get("user:2").isEmpty());
            assertTrue(cacheStore.get("user:3").isEmpty());
            assertTrue(cacheStore.get("order:1").isPresent());
        }

        @Test
        @DisplayName("evictByPrefix — 前缀不匹配时无影响")
        void evictByPrefix_noMatch() {
            cacheStore = new InMemoryCacheStore();
            cacheStore.set("user:1", "data1", Duration.ofMinutes(10));

            cacheStore.evictByPrefix("order:");
            assertTrue(cacheStore.get("user:1").isPresent());
        }
    }
}
