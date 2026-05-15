package com.zerx.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LruCache}
 */
class LruCacheTest {

    // ======================== Basic Operations ========================

    @Nested
    @DisplayName("Basic put/get/remove")
    class BasicOperations {

        private LruCache<String, String> cache;

        @BeforeEach
        void setUp() {
            cache = new LruCache<>(5);
        }

        @Test
        @DisplayName("put and get a value")
        void putAndGet() {
            cache.put("key1", "value1");
            assertEquals("value1", cache.get("key1"));
        }

        @Test
        @DisplayName("get non-existent key returns null")
        void getNonExistentKey() {
            assertNull(cache.get("nonexistent"));
        }

        @Test
        @DisplayName("put overwrites existing key")
        void putOverwrites() {
            cache.put("key1", "value1");
            cache.put("key1", "value2");
            assertEquals("value2", cache.get("key1"));
            assertEquals(1, cache.size());
        }

        @Test
        @DisplayName("remove existing key returns value")
        void removeExisting() {
            cache.put("key1", "value1");
            String removed = cache.remove("key1");
            assertEquals("value1", removed);
            assertFalse(cache.containsKey("key1"));
        }

        @Test
        @DisplayName("remove non-existent key returns null")
        void removeNonExistent() {
            assertNull(cache.remove("nonexistent"));
        }

        @Test
        @DisplayName("getOrDefault returns cached value")
        void getOrDefaultCached() {
            cache.put("key1", "value1");
            assertEquals("value1", cache.getOrDefault("key1", "default"));
        }

        @Test
        @DisplayName("getOrDefault returns default for missing key")
        void getOrDefaultMissing() {
            assertEquals("default", cache.getOrDefault("missing", "default"));
        }

        @Test
        @DisplayName("putIfAbsent returns null when key is new")
        void putIfAbsentNew() {
            String result = cache.putIfAbsent("key1", "value1");
            assertNull(result);
            assertEquals("value1", cache.get("key1"));
        }

        @Test
        @DisplayName("putIfAbsent returns existing value when key present")
        void putIfAbsentExisting() {
            cache.put("key1", "value1");
            String result = cache.putIfAbsent("key1", "value2");
            assertEquals("value1", result);
            assertEquals("value1", cache.get("key1"));
        }

        @Test
        @DisplayName("getOrCompute uses cache hit")
        void getOrComputeCacheHit() {
            cache.put("key1", "value1");
            AtomicInteger callCount = new AtomicInteger(0);
            String result = cache.getOrCompute("key1", k -> {
                callCount.incrementAndGet();
                return "computed";
            });
            assertEquals("value1", result);
            assertEquals(0, callCount.get());
        }

        @Test
        @DisplayName("getOrCompute computes on miss")
        void getOrComputeCacheMiss() {
            String result = cache.getOrCompute("key1", k -> "computed-" + k);
            assertEquals("computed-key1", result);
            assertEquals("computed-key1", cache.get("key1"));
        }

        @Test
        @DisplayName("put with null key throws NullPointerException")
        void putNullKey() {
            assertThrows(NullPointerException.class, () -> cache.put(null, "value"));
        }

        @Test
        @DisplayName("put with null value is allowed")
        void putNullValue() {
            cache.put("key1", null);
            assertTrue(cache.containsKey("key1"));
            assertNull(cache.get("key1"));
        }

        @Test
        @DisplayName("get with null key returns null (no exception)")
        void getNullKey() {
            assertNull(cache.get(null));
        }
    }

    // ======================== Constructor ========================

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("constructor with zero maxSize throws IllegalArgumentException")
        void zeroMaxSize() {
            assertThrows(IllegalArgumentException.class, () -> new LruCache<String, String>(0));
        }

        @Test
        @DisplayName("constructor with negative maxSize throws IllegalArgumentException")
        void negativeMaxSize() {
            assertThrows(IllegalArgumentException.class, () -> new LruCache<String, String>(-1));
        }

        @Test
        @DisplayName("constructor with maxSize 1 works")
        void maxSizeOne() {
            LruCache<String, String> cache = new LruCache<>(1);
            cache.put("a", "1");
            assertEquals("1", cache.get("a"));
            cache.put("b", "2");
            assertNull(cache.get("a"));
            assertEquals("2", cache.get("b"));
        }

        @Test
        @DisplayName("getMaxSize returns correct value")
        void getMaxSize() {
            LruCache<String, String> cache = new LruCache<>(100);
            assertEquals(100, cache.getMaxSize());
        }
    }

    // ======================== LRU Eviction ========================

    @Nested
    @DisplayName("LRU eviction")
    class LruEviction {

        @Test
        @DisplayName("evicts least recently used entry when capacity exceeded")
        void evictsOnOverflow() {
            LruCache<Integer, String> cache = new LruCache<>(3);
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");
            // Adding a 4th entry evicts the LRU (key=1)
            cache.put(4, "four");

            assertNull(cache.get(1));
            assertEquals("two", cache.get(2));
            assertEquals("three", cache.get(3));
            assertEquals("four", cache.get(4));
            assertEquals(3, cache.size());
        }

        @Test
        @DisplayName("accessing an entry moves it to most-recently-used")
        void accessUpdatesRecency() {
            LruCache<Integer, String> cache = new LruCache<>(3);
            cache.put(1, "one");
            cache.put(2, "two");
            cache.put(3, "three");

            // Access key=1 to make it most recently used
            cache.get(1);

            // Now key=2 is LRU
            cache.put(4, "four");

            assertNull(cache.get(2)); // evicted
            assertEquals("one", cache.get(1));
            assertEquals("three", cache.get(3));
            assertEquals("four", cache.get(4));
        }

        @Test
        @DisplayName("evicts multiple entries when many added at once")
        void evictsMultiple() {
            LruCache<Integer, Integer> cache = new LruCache<>(3);
            cache.put(1, 1);
            cache.put(2, 2);
            cache.put(3, 3);
            cache.put(4, 4);
            cache.put(5, 5);

            assertEquals(3, cache.size());
            assertNull(cache.get(1));
            assertNull(cache.get(2));
            assertEquals(3, cache.get(3));
            assertEquals(4, cache.get(4));
            assertEquals(5, cache.get(5));
        }
    }

    // ======================== State queries ========================

    @Nested
    @DisplayName("State queries")
    class StateQueries {

        private LruCache<String, String> cache;

        @BeforeEach
        void setUp() {
            cache = new LruCache<>(5);
        }

        @Test
        @DisplayName("containsKey returns true for existing key")
        void containsKeyTrue() {
            cache.put("key1", "value1");
            assertTrue(cache.containsKey("key1"));
        }

        @Test
        @DisplayName("containsKey returns false for missing key")
        void containsKeyFalse() {
            assertFalse(cache.containsKey("missing"));
        }

        @Test
        @DisplayName("size returns correct count")
        void size() {
            assertEquals(0, cache.size());
            cache.put("a", "1");
            assertEquals(1, cache.size());
            cache.put("b", "2");
            cache.put("c", "3");
            assertEquals(3, cache.size());
            cache.remove("b");
            assertEquals(2, cache.size());
        }

        @Test
        @DisplayName("isEmpty returns true for empty cache")
        void isEmptyTrue() {
            assertTrue(cache.isEmpty());
        }

        @Test
        @DisplayName("isEmpty returns false for non-empty cache")
        void isEmptyFalse() {
            cache.put("a", "1");
            assertFalse(cache.isEmpty());
        }

        @Test
        @DisplayName("isFull returns true when at capacity")
        void isFullTrue() {
            LruCache<String, String> c = new LruCache<>(2);
            c.put("a", "1");
            c.put("b", "2");
            assertTrue(c.isFull());
        }

        @Test
        @DisplayName("isFull returns false when below capacity")
        void isFullFalse() {
            assertFalse(cache.isFull());
            cache.put("a", "1");
            assertFalse(cache.isFull());
        }

        @Test
        @DisplayName("clear removes all entries and resets stats")
        void clear() {
            cache.put("a", "1");
            cache.put("b", "2");
            cache.get("a"); // hit
            cache.get("missing"); // miss
            cache.clear();
            assertTrue(cache.isEmpty());
            assertEquals(0, cache.size());
            assertEquals(0, cache.getHitCount());
            assertEquals(0, cache.getMissCount());
        }
    }

    // ======================== Stats ========================

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        private LruCache<String, String> cache;

        @BeforeEach
        void setUp() {
            cache = new LruCache<>(10);
        }

        @Test
        @DisplayName("hitCount increments on cache hit")
        void hitCountIncrement() {
            cache.put("key1", "value1");
            cache.get("key1");
            cache.get("key1");
            assertEquals(2, cache.getHitCount());
        }

        @Test
        @DisplayName("missCount increments on cache miss")
        void missCountIncrement() {
            cache.get("missing1");
            cache.get("missing2");
            assertEquals(2, cache.getMissCount());
        }

        @Test
        @DisplayName("hitRate calculates correctly")
        void hitRate() {
            cache.put("key1", "value1");
            cache.get("key1");   // hit
            cache.get("key1");   // hit
            cache.get("missing"); // miss
            double rate = cache.hitRate();
            assertEquals(2.0 / 3.0, rate, 0.0001);
        }

        @Test
        @DisplayName("hitRate returns 0.0 when no accesses")
        void hitRateNoAccess() {
            assertEquals(0.0, cache.hitRate(), 0.0001);
        }

        @Test
        @DisplayName("hitRate returns 1.0 when all hits")
        void hitRateAllHits() {
            cache.put("a", "1");
            cache.get("a");
            cache.get("a");
            assertEquals(1.0, cache.hitRate(), 0.0001);
        }

        @Test
        @DisplayName("null value key is counted as hit")
        void nullValueHit() {
            cache.put("key1", null);
            cache.get("key1"); // should count as hit
            assertEquals(1, cache.getHitCount());
            assertEquals(0, cache.getMissCount());
        }
    }

    // ======================== asMap ========================

    @Nested
    @DisplayName("asMap view")
    class AsMapView {

        @Test
        @DisplayName("asMap returns unmodifiable map")
        void asMapUnmodifiable() {
            LruCache<String, String> cache = new LruCache<>(5);
            cache.put("a", "1");
            cache.put("b", "2");
            Map<String, String> map = cache.asMap();
            assertThrows(UnsupportedOperationException.class, () -> map.put("c", "3"));
        }

        @Test
        @DisplayName("asMap reflects cache content")
        void asMapContent() {
            LruCache<String, String> cache = new LruCache<>(5);
            cache.put("a", "1");
            cache.put("b", "2");
            Map<String, String> map = cache.asMap();
            assertEquals(2, map.size());
            assertEquals("1", map.get("a"));
            assertEquals("2", map.get("b"));
        }
    }

    // ======================== Computing LRU Cache ========================

    @Nested
    @DisplayName("Computing LRU Cache")
    class ComputingLruCache {

        @Test
        @DisplayName("computing() factory creates cache that auto-computes on miss")
        void computingAutoCompute() {
            AtomicInteger computeCount = new AtomicInteger(0);
            LruCache<String, String> cache = LruCache.computing(5, key -> {
                computeCount.incrementAndGet();
                return "computed:" + key;
            });

            String result1 = cache.get("key1");
            assertEquals("computed:key1", result1);
            assertEquals(1, computeCount.get());

            // Second access should hit cache
            String result2 = cache.get("key1");
            assertEquals("computed:key1", result2);
            assertEquals(1, computeCount.get());
        }

        @Test
        @DisplayName("computing() loader returns null does not cache")
        void computingNullLoader() {
            LruCache<String, String> cache = LruCache.computing(5, key -> null);
            assertNull(cache.get("key1"));
            assertFalse(cache.containsKey("key1"));
        }

        @Test
        @DisplayName("computing() eviction works")
        void computingEviction() {
            LruCache<Integer, String> cache = LruCache.computing(3, k -> "value" + k);
            cache.get(1);
            cache.get(2);
            cache.get(3);
            cache.get(4); // evicts 1
            // Computing cache auto-computes on miss, so get(1) triggers loader again
            // Verify the cache size is at most 3
            assertEquals(3, cache.size());
            assertEquals("value1", cache.get(1)); // re-computed and cached
        }

        @Test
        @DisplayName("computing() loader is required (not null)")
        void computingNullLoaderThrows() {
            assertThrows(NullPointerException.class, () -> LruCache.computing(5, null));
        }
    }

    // ======================== Synchronized LRU Cache ========================

    @Nested
    @DisplayName("Synchronized LRU Cache")
    class SynchronizedLruCache {

        @Test
        @DisplayName("synchronizedLruCache() creates functional cache")
        void basicOperation() {
            LruCache<String, String> cache = LruCache.synchronizedLruCache(5);
            cache.put("a", "1");
            assertEquals("1", cache.get("a"));
            assertEquals(1, cache.size());
            assertTrue(cache.containsKey("a"));
            cache.clear();
            assertTrue(cache.isEmpty());
        }

        @Test
        @DisplayName("synchronizedLruCache() is thread-safe under concurrent access")
        void threadSafety() throws InterruptedException {
            LruCache<Integer, Integer> cache = LruCache.synchronizedLruCache(100);
            int threadCount = 10;
            int opsPerThread = 1000;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger errors = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < opsPerThread; i++) {
                            int key = threadId * opsPerThread + i;
                            cache.put(key, key * 2);
                            cache.get(key);
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await();
            assertEquals(0, errors.get());
            // All entries should be present (1000 entries fit in capacity 10000)
            // Capacity is 100, so size should be at most 100 (entries get evicted)
            assertTrue(cache.size() <= 100, "Size should be <= 100, was " + cache.size());
        }

        @Test
        @DisplayName("synchronizedLruCache() maintains correct hit/miss counts under concurrency")
        void concurrentStats() throws InterruptedException {
            LruCache<Integer, Integer> cache = LruCache.synchronizedLruCache(100);
            cache.put(1, 100);
            cache.put(2, 200);

            int threadCount = 10;
            int opsPerThread = 100;
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                new Thread(() -> {
                    try {
                        for (int i = 0; i < opsPerThread; i++) {
                            cache.get(1); // hit
                            cache.get(999); // miss
                        }
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await();
            assertEquals(threadCount * opsPerThread, cache.getHitCount());
            assertEquals(threadCount * opsPerThread, cache.getMissCount());
        }
    }
}
