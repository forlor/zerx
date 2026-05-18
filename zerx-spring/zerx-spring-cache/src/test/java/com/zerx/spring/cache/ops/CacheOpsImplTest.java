package com.zerx.spring.cache.ops;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import com.zerx.spring.cache.store.CaffeineCacheStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CacheOpsImplTest {

    private CacheOps cacheOps;
    private CacheStore store;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        store = new CaffeineCacheStore(properties);
        cacheOps = new CacheOpsImpl(store, properties.getNullValueTtl());
    }

    @Test
    void getStore_returns_underlying_store() {
        assertEquals(store, cacheOps.getStore());
    }

    @Test
    void set_and_get() {
        cacheOps.set("key1", "value1", Duration.ofMinutes(10));
        String result = cacheOps.get("key1");
        assertEquals("value1", result);
    }

    @Test
    void get_miss_returns_null() {
        String result = cacheOps.get("nonexistent");
        assertNull(result);
    }

    @Test
    void get_with_loader_hit() {
        cacheOps.set("key1", "loaded-value", Duration.ofMinutes(10));
        AtomicInteger loadCount = new AtomicInteger(0);

        String result = cacheOps.get("key1", () -> {
            loadCount.incrementAndGet();
            return "fresh-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("loaded-value", result);
        assertEquals(0, loadCount.get());
    }

    @Test
    void get_with_loader_miss() {
        AtomicInteger loadCount = new AtomicInteger(0);

        String result = cacheOps.get("newKey", () -> {
            loadCount.incrementAndGet();
            return "loaded-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("loaded-value", result);
        assertEquals(1, loadCount.get());
    }

    @Test
    void anti_penetration_null_cached_and_loader_not_called_again() {
        AtomicInteger loadCount = new AtomicInteger(0);

        String result1 = cacheOps.get("nullKey", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);

        assertNull(result1);
        assertEquals(1, loadCount.get());

        // Second call should NOT call loader again (anti-penetration)
        String result2 = cacheOps.get("nullKey", () -> {
            loadCount.incrementAndGet();
            return "should-not-be-called";
        }, 10, TimeUnit.MINUTES);

        assertNull(result2);
        assertEquals(1, loadCount.get());
    }

    @Test
    void anti_penetration_evict_clears_null_marker() {
        AtomicInteger loadCount = new AtomicInteger(0);

        cacheOps.get("pen-key", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);
        assertEquals(1, loadCount.get());

        cacheOps.evict("pen-key");
        assertFalse(cacheOps.hasKey("pen-key"));

        // After evict, loader should be called again
        cacheOps.get("pen-key", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);
        assertEquals(2, loadCount.get());
    }

    @Test
    void anti_stampede_loader_called_only_once() throws InterruptedException {
        AtomicInteger loadCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    String result = cacheOps.get("stampede-key", () -> {
                        loadCount.incrementAndGet();
                        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        return "loaded-value";
                    }, 10, TimeUnit.MINUTES);
                    assertEquals("loaded-value", result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(1, loadCount.get(), "Loader should be called exactly once due to anti-stampede");
    }

    @Test
    void anti_stampede_with_null_value() throws InterruptedException {
        AtomicInteger loadCount = new AtomicInteger(0);
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    String result = cacheOps.get("stampede-null", () -> {
                        loadCount.incrementAndGet();
                        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        return null;
                    }, 10, TimeUnit.MINUTES);
                    assertNull(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        assertEquals(1, loadCount.get());
    }

    @Test
    void getOptional_present() {
        cacheOps.set("key1", "value1", Duration.ofMinutes(10));
        Optional<String> result = cacheOps.getOptional("key1", () -> "fallback", 10, TimeUnit.MINUTES);
        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void getOptional_empty_for_null() {
        Optional<String> result = cacheOps.getOptional("nonexistent", () -> null, 10, TimeUnit.MINUTES);
        assertFalse(result.isPresent());
    }

    @Test
    void evict() {
        cacheOps.set("key1", "value1", Duration.ofMinutes(10));
        assertTrue(cacheOps.hasKey("key1"));
        cacheOps.evict("key1");
        assertFalse(cacheOps.hasKey("key1"));
    }

    @Test
    void evictByPrefix() {
        cacheOps.set("user:1", "data1", Duration.ofMinutes(10));
        cacheOps.set("user:2", "data2", Duration.ofMinutes(10));
        cacheOps.set("order:1", "data3", Duration.ofMinutes(10));

        cacheOps.evictByPrefix("user:");

        assertFalse(cacheOps.hasKey("user:1"));
        assertFalse(cacheOps.hasKey("user:2"));
        assertTrue(cacheOps.hasKey("order:1"));
    }

    @Test
    void set_with_timeunit() {
        cacheOps.set("key1", "value1", 30, TimeUnit.SECONDS);
        assertEquals("value1", cacheOps.get("key1"));
    }
}
