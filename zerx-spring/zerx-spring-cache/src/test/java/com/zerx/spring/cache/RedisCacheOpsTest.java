package com.zerx.spring.cache;

import com.zerx.spring.cache.ops.CacheOps;
import com.zerx.spring.cache.ops.RedisCacheOps;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisCacheOps 单元测试（使用 Mockito 模拟 Redis）
 *
 * @author zerx
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheOpsTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CacheOps cacheOps;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        cacheOps = new RedisCacheOps(redisTemplate, properties);
    }

    @Test
    void set_stores_value_with_jittered_ttl() {
        cacheOps.set("key1", "value1", 10, TimeUnit.MINUTES);

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("test:key1"), eq("value1"), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));

        // 验证 TTL 带有抖动：10min = 600000ms，抖动范围 [540000, 660000)
        long ttl = ttlCaptor.getValue();
        assertTrue(ttl >= 540000 && ttl < 660000,
                "TTL should have jitter: expected ~600000ms ±10%, got " + ttl);
    }

    @Test
    void set_with_duration() {
        cacheOps.set("key1", "value1", Duration.ofSeconds(30));

        verify(valueOperations).set(eq("test:key1"), eq("value1"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void get_returns_cached_value() {
        when(valueOperations.get("test:key1")).thenReturn("cached-value");

        String result = cacheOps.get("key1");

        assertEquals("cached-value", result);
    }

    @Test
    void get_returns_null_for_missing_key() {
        when(valueOperations.get("test:key1")).thenReturn(null);

        String result = cacheOps.get("key1");

        assertNull(result);
    }

    @Test
    void get_with_loader_hit_does_not_call_loader() {
        when(valueOperations.get("test:key1")).thenReturn("cached-value");

        AtomicInteger loadCount = new AtomicInteger(0);
        String result = cacheOps.get("key1", () -> {
            loadCount.incrementAndGet();
            return "fresh-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("cached-value", result);
        assertEquals(0, loadCount.get());
    }

    @Test
    void get_with_loader_miss_calls_loader_and_caches() {
        when(valueOperations.get("test:key1")).thenReturn(null);

        AtomicInteger loadCount = new AtomicInteger(0);
        String result = cacheOps.get("key1", () -> {
            loadCount.incrementAndGet();
            return "loaded-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("loaded-value", result);
        assertEquals(1, loadCount.get());

        // 验证值被写入 Redis
        verify(valueOperations).set(eq("test:key1"), eq("loaded-value"), anyLong(), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void anti_penetration_null_value_cached_as_marker() {
        // 第一次调用：miss + loader 返回 null
        when(valueOperations.get("test:nullKey")).thenReturn(null);

        AtomicInteger loadCount = new AtomicInteger(0);
        String result1 = cacheOps.get("nullKey", () -> {
            loadCount.incrementAndGet();
            return null;
        }, 10, TimeUnit.MINUTES);

        assertNull(result1);
        assertEquals(1, loadCount.get());

        // 验证 NULL_MARKER 被缓存
        verify(valueOperations).set(eq("test:nullKey"), eq("__ZERX_CACHE_NULL__"),
                any(Duration.class));

        // 重置 mock
        reset(valueOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 第二次调用：命中 NULL_MARKER
        when(valueOperations.get("test:nullKey")).thenReturn("__ZERX_CACHE_NULL__");

        String result2 = cacheOps.get("nullKey", () -> {
            loadCount.incrementAndGet();
            return "should-not-be-called";
        }, 10, TimeUnit.MINUTES);

        assertNull(result2);
        assertEquals(1, loadCount.get(), "Loader should not be called again for cached null");
    }

    @Test
    void get_with_loader_double_check_after_lock() {
        // 第一次返回 null（miss），第二次返回值（另一个线程已写入）
        when(valueOperations.get("test:key1"))
                .thenReturn(null)
                .thenReturn("concurrent-value");

        AtomicInteger loadCount = new AtomicInteger(0);
        String result = cacheOps.get("key1", () -> {
            loadCount.incrementAndGet();
            return "loader-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("concurrent-value", result);
        assertEquals(0, loadCount.get(), "Loader should not be called when double-check finds value");
    }

    @Test
    void evict_deletes_key() {
        cacheOps.evict("key1");

        verify(redisTemplate).delete("test:key1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void evictByPrefix_deletes_matching_keys() {
        Set<String> keys = Set.of("test:user:1", "test:user:2", "test:user:3");
        when(redisTemplate.keys("test:user:*")).thenReturn(keys);

        cacheOps.evictByPrefix("user:");

        verify(redisTemplate).delete(keys);
    }

    @Test
    void evictByPrefix_no_match_does_not_delete() {
        when(redisTemplate.keys("test:user:*")).thenReturn(null);

        cacheOps.evictByPrefix("user:");

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void hasKey_true() {
        when(redisTemplate.hasKey("test:key1")).thenReturn(true);

        assertTrue(cacheOps.hasKey("key1"));
    }

    @Test
    void hasKey_false() {
        when(redisTemplate.hasKey("test:key1")).thenReturn(false);

        assertFalse(cacheOps.hasKey("key1"));
    }

    @Test
    void hasKey_null_returns_false() {
        when(redisTemplate.hasKey("test:key1")).thenReturn(null);

        assertFalse(cacheOps.hasKey("key1"));
    }

    @Test
    void key_prefix_applied() {
        cacheOps.set("mykey", "value", 10, TimeUnit.MINUTES);
        verify(valueOperations).set(eq("test:mykey"), eq("value"), anyLong(), any(TimeUnit.class));
    }

    @Test
    void get_with_loader_null_marker_detected_on_direct_get() {
        // 直接 get 方法遇到 NULL_MARKER 应返回 null
        when(valueOperations.get("test:nullKey")).thenReturn("__ZERX_CACHE_NULL__");

        Object result = cacheOps.get("nullKey");

        assertNull(result, "Direct get should return null for NULL_MARKER");
    }

    @Test
    void anti_avalanche_different_keys_get_different_ttls() {
        // 设置多个条目，验证 TTL 各不相同（概率性测试）
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);

        for (int i = 0; i < 10; i++) {
            cacheOps.set("key:" + i, "value:" + i, 60000, TimeUnit.MILLISECONDS);
        }

        verify(valueOperations, times(10)).set(anyString(), any(), ttlCaptor.capture(), eq(TimeUnit.MILLISECONDS));

        var ttls = ttlCaptor.getAllValues();
        // 验证至少存在不同的 TTL（由于随机抖动）
        long firstTtl = ttls.get(0);
        boolean hasVariation = ttls.stream().anyMatch(ttl -> ttl != firstTtl);
        assertTrue(hasVariation, "TTLs should vary due to jitter, but all were " + ttls);
    }
}
