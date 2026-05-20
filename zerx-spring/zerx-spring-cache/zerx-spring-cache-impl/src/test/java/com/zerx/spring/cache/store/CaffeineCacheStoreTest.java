package com.zerx.spring.cache.store;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineCacheStoreTest {

    private CaffeineCacheStore store;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        // 禁用 access expiry，避免 get() 延长 TTL 影响定时过期测试
        properties.getCaffeine().setExpireAfterAccess(Duration.ZERO);
        store = new CaffeineCacheStore(properties);
    }

    @Test
    void set_and_get() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        var result = store.get("key1");
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void get_miss_returns_empty() {
        var result = store.get("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void null_marker_is_stored_and_retrieved() {
        store.set("nullKey", CacheConstants.NULL_MARKER, Duration.ofMinutes(5));
        var result = store.get("nullKey");
        assertTrue(result.isPresent());
        assertEquals(CacheConstants.NULL_MARKER, result.get());
    }

    @Test
    void evict() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        assertTrue(store.hasKey("key1"));

        store.evict("key1");
        assertFalse(store.hasKey("key1"));
    }

    @Test
    void evictByPrefix() {
        store.set("user:1", "data1", Duration.ofMinutes(10));
        store.set("user:2", "data2", Duration.ofMinutes(10));
        store.set("order:1", "data3", Duration.ofMinutes(10));

        store.evictByPrefix("user:");

        assertFalse(store.hasKey("user:1"));
        assertFalse(store.hasKey("user:2"));
        assertTrue(store.hasKey("order:1"));
    }

    @Test
    void evictByPrefix_no_match() {
        store.set("user:1", "data1", Duration.ofMinutes(10));
        int beforeSize = store.multiGet(Set.of("user:1")).size();

        store.evictByPrefix("order:");

        assertTrue(store.hasKey("user:1"));
        assertEquals(beforeSize, store.multiGet(Set.of("user:1")).size());
    }

    @Test
    void hasKey_true() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        assertTrue(store.hasKey("key1"));
    }

    @Test
    void hasKey_false() {
        assertFalse(store.hasKey("nonexistent"));
    }

    @Test
    void key_prefix_applied() {
        store.set("mykey", "value", Duration.ofMinutes(10));
        assertTrue(store.hasKey("mykey"));
    }

    @Test
    void overwrite_value() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        assertEquals("value1", store.get("key1").orElse(null));

        store.set("key1", "value2", Duration.ofMinutes(10));
        assertEquals("value2", store.get("key1").orElse(null));
    }

    @Test
    void multiGet() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        store.set("key2", "value2", Duration.ofMinutes(10));
        store.set("key3", "value3", Duration.ofMinutes(10));

        Map<String, Object> result = store.multiGet(Set.of("key1", "key2", "key3", "missing"));
        assertEquals(3, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertEquals("value3", result.get("key3"));
        assertFalse(result.containsKey("missing"));
    }

    @Test
    void multiGet_includes_null_marker() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        store.set("nullKey", CacheConstants.NULL_MARKER, Duration.ofMinutes(5));

        Map<String, Object> result = store.multiGet(Set.of("key1", "nullKey"));
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals(CacheConstants.NULL_MARKER, result.get("nullKey"));
    }

    @Test
    void multiSet() {
        Map<String, Object> entries = Map.of(
                "key1", "value1",
                "key2", "value2",
                "key3", "value3"
        );
        store.multiSet(entries, Duration.ofMinutes(10));

        assertEquals("value1", store.get("key1").orElse(null));
        assertEquals("value2", store.get("key2").orElse(null));
        assertEquals("value3", store.get("key3").orElse(null));
    }

    @Test
    void multiEvict() {
        store.set("key1", "value1", Duration.ofMinutes(10));
        store.set("key2", "value2", Duration.ofMinutes(10));
        store.set("key3", "value3", Duration.ofMinutes(10));

        store.multiEvict(Set.of("key1", "key3"));

        assertFalse(store.hasKey("key1"));
        assertFalse(store.hasKey("key3"));
        assertTrue(store.hasKey("key2"));
    }

    @Test
    void multiGet_empty_keys() {
        Map<String, Object> result = store.multiGet(Set.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void multiSet_empty_entries() {
        assertDoesNotThrow(() -> store.multiSet(Map.of(), Duration.ofMinutes(10)));
    }

    @Test
    void multiEvict_empty_keys() {
        assertDoesNotThrow(() -> store.multiEvict(Set.of()));
    }

    @Test
    void large_number_of_entries() {
        for (int i = 0; i < 1000; i++) {
            store.set("key:" + i, "value:" + i, Duration.ofMinutes(10));
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals("value:" + i, store.get("key:" + i).orElse(null));
        }
    }

    @Test
    void jitter_produces_variable_ttl() throws InterruptedException {
        long baseTtlMs = 200;
        int entryCount = 20;

        for (int i = 0; i < entryCount; i++) {
            store.set("jitter:" + i, "value:" + i, Duration.ofMillis(baseTtlMs));
        }

        Thread.sleep(185);

        int aliveCount = 0;
        for (int i = 0; i < entryCount; i++) {
            if (store.get("jitter:" + i).isPresent()) {
                aliveCount++;
            }
        }

        assertTrue(aliveCount > 0, "Some entries should still be alive near base TTL due to jitter");

        Thread.sleep(100);

        int remainingCount = 0;
        for (int i = 0; i < entryCount; i++) {
            if (store.get("jitter:" + i).isPresent()) {
                remainingCount++;
            }
        }

        assertEquals(0, remainingCount, "All entries should have expired after max TTL + buffer");
    }

    @Test
    void per_entry_ttl_shorter_expires_before_longer() throws InterruptedException {
        store.set("short", "short-value", Duration.ofMillis(150));
        store.set("long", "long-value", Duration.ofMinutes(5));

        Thread.sleep(200);

        assertTrue(store.get("short").isEmpty(), "Short TTL entry should have expired");
        assertTrue(store.get("long").isPresent(), "Long TTL entry should still be alive");
    }

    @Test
    void withPrefix_already_has_prefix() {
        String result = store.withPrefix("test:mykey");
        assertEquals("test:mykey", result);
    }

    @Test
    void withPrefix_no_prefix() {
        String result = store.withPrefix("mykey");
        assertEquals("test:mykey", result);
    }

    @Test
    void complex_object_value() {
        var data = new TestData("hello", 42);
        store.set("complex", data, Duration.ofMinutes(10));
        var retrieved = store.get("complex");
        assertTrue(retrieved.isPresent());
        assertInstanceOf(TestData.class, retrieved.get());
        assertEquals("hello", ((TestData) retrieved.get()).name());
        assertEquals(42, ((TestData) retrieved.get()).count());
    }

    record TestData(String name, int count) {}
}
