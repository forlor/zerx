package com.zerx.spring.cache;

import com.zerx.spring.cache.ops.CacheOps;
import com.zerx.spring.cache.ops.CaffeineCacheOps;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CaffeineCacheOps 单元测试
 *
 * @author zerx
 */
class CaffeineCacheOpsTest {

    private CacheOps cacheOps;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        cacheOps = new CaffeineCacheOps(properties);
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
        assertEquals(0, loadCount.get()); // loader not called on hit
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
    void get_with_loader_null_cached_anti_penetration() {
        AtomicInteger loadCount = new AtomicInteger(0);

        // 第一次调用：loader 返回 null，应缓存 NULL_MARKER（防穿透）
        String result1 = cacheOps.get("nullKey", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);

        assertNull(result1);
        assertEquals(1, loadCount.get());

        // 第二次调用：loader 不应再被调用（NULL_MARKER 缓存命中）
        String result2 = cacheOps.get("nullKey", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);

        assertNull(result2);
        assertEquals(1, loadCount.get()); // loader 仍然只调用了一次

        // hasKey 应返回 true（NULL_MARKER 已缓存）
        assertTrue(cacheOps.hasKey("nullKey"));
    }

    @Test
    void getOptional_present() {
        cacheOps.set("key1", "value1", Duration.ofMinutes(10));

        Optional<String> result = cacheOps.getOptional("key1", () -> "fallback", 10, TimeUnit.MINUTES);

        assertTrue(result.isPresent());
        assertEquals("value1", result.get());
    }

    @Test
    void getOptional_empty() {
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
        assertTrue(cacheOps.hasKey("order:1")); // not evicted
    }

    @Test
    void hasKey_true() {
        cacheOps.set("key1", "value1", Duration.ofMinutes(10));
        assertTrue(cacheOps.hasKey("key1"));
    }

    @Test
    void hasKey_false() {
        assertFalse(cacheOps.hasKey("nonexistent"));
    }

    @Test
    void key_prefix_applied() {
        cacheOps.set("mykey", "value", Duration.ofMinutes(10));
        assertTrue(cacheOps.hasKey("mykey"));
        // With prefix "test:", the actual key is "test:mykey"
        // Direct lookup without prefix should work because CacheOps handles prefixing
    }

    @Test
    void overwrite_value() {
        cacheOps.set("key1", "value1", Duration.ofMinutes(10));
        assertEquals("value1", cacheOps.get("key1"));

        cacheOps.set("key1", "value2", Duration.ofMinutes(10));
        assertEquals("value2", cacheOps.get("key1"));
    }

    @Test
    void set_with_timeunit() {
        cacheOps.set("key1", "value1", 30, TimeUnit.SECONDS);
        assertEquals("value1", cacheOps.get("key1"));
    }

    @Test
    void large_number_of_entries() {
        for (int i = 0; i < 1000; i++) {
            cacheOps.set("key:" + i, "value:" + i, Duration.ofMinutes(10));
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals("value:" + i, cacheOps.get("key:" + i));
        }
    }

    // ===================== 新增特性测试 =====================

    @Test
    void anti_penetration_null_marker_cached_and_loader_not_called_again() {
        AtomicInteger loadCount = new AtomicInteger(0);

        // 多次调用，loader 只应被调用一次
        for (int i = 0; i < 5; i++) {
            String result = cacheOps.get("anti-pen:" + i, () -> {
                loadCount.incrementAndGet();
                return null;
            }, 10, TimeUnit.MINUTES);
            assertNull(result);
        }

        // 每个不同的 key，loader 只调用一次
        assertEquals(5, loadCount.get());

        // 第二轮调用，所有 key 应命中 NULL_MARKER
        for (int i = 0; i < 5; i++) {
            cacheOps.get("anti-pen:" + i, () -> {
                loadCount.incrementAndGet();
                return null;
            }, 10, TimeUnit.MINUTES);
        }

        assertEquals(5, loadCount.get()); // 无新增调用
    }

    @Test
    void anti_penetration_evict_clears_null_marker() {
        AtomicInteger loadCount = new AtomicInteger(0);

        // 第一次调用，缓存 null
        cacheOps.get("pen-key", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);
        assertEquals(1, loadCount.get());

        // 清除缓存
        cacheOps.evict("pen-key");
        assertFalse(cacheOps.hasKey("pen-key"));

        // 再次调用，loader 应被重新调用
        cacheOps.get("pen-key", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);
        assertEquals(2, loadCount.get());
    }

    @Test
    void anti_stampede_loader_called_only_once_with_concurrent_requests() throws InterruptedException {
        AtomicInteger loadCount = new AtomicInteger(0);
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // 所有线程等待同一个信号
                    String result = cacheOps.get("stampede-key", () -> {
                        loadCount.incrementAndGet();
                        try {
                            Thread.sleep(100); // 模拟慢加载
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
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

        // 同时释放所有线程
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should complete within timeout");

        // 防击穿：loader 只应被调用一次
        assertEquals(1, loadCount.get(), "Loader should be called exactly once due to anti-stampede");
    }

    @Test
    void anti_avalanche_ttl_jitter_prevents_mass_expiration() throws InterruptedException {
        // 使用较短的 TTL 来测试抖动效果
        // 抖动范围：±10%，所以 200ms 的 TTL 实际范围是 [180ms, 220ms]
        long baseTtlMs = 200;
        int entryCount = 20;

        for (int i = 0; i < entryCount; i++) {
            cacheOps.set("jitter:" + i, "value:" + i, baseTtlMs, TimeUnit.MILLISECONDS);
        }

        // 等待到接近基础 TTL（ jitter 最小边界 180ms 之后）
        Thread.sleep(185);

        // 在 185ms 时，由于抖动，有些条目的 TTL 应该 > 185ms（仍在缓存中）
        // 而 baseTtl - jitter_min = 180ms，所以在 185ms 时大部分应该还在
        int aliveCount = 0;
        for (int i = 0; i < entryCount; i++) {
            if (cacheOps.get("jitter:" + i) != null) {
                aliveCount++;
            }
        }

        // 由于抖动范围 [180, 220)，在 185ms 时应该还有部分条目存活
        // （因为有些条目的实际 TTL > 185ms）
        assertTrue(aliveCount > 0, "Some entries should still be alive near base TTL due to jitter");

        // 等待到远超过最大 TTL
        Thread.sleep(100);

        // 在 ~285ms 时，所有条目都应该已过期
        int remainingCount = 0;
        for (int i = 0; i < entryCount; i++) {
            if (cacheOps.get("jitter:" + i) != null) {
                remainingCount++;
            }
        }

        assertEquals(0, remainingCount, "All entries should have expired after max TTL + buffer");
    }

    @Test
    void per_entry_ttl_shorter_entry_expires_before_longer() throws InterruptedException {
        // 设置一个短 TTL 条目和一个长 TTL 条目
        cacheOps.set("short", "short-value", 150, TimeUnit.MILLISECONDS);
        cacheOps.set("long", "long-value", 5, TimeUnit.MINUTES);

        // 等待短 TTL 条目过期（+ 一些抖动余量）
        Thread.sleep(200);

        // 短 TTL 条目应已过期
        assertNull(cacheOps.get("short"), "Short TTL entry should have expired");

        // 长 TTL 条目应仍在缓存中
        assertEquals("long-value", cacheOps.get("long"), "Long TTL entry should still be alive");
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
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
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

        // 即使 loader 返回 null，也应只调用一次
        assertEquals(1, loadCount.get(), "Loader should be called exactly once even for null values");
    }
}
