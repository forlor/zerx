package com.zerx.spring.cache;

import com.zerx.spring.cache.ops.CacheOps;
import com.zerx.spring.cache.ops.MultilevelCacheOps;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MultilevelCacheOps 单元测试
 *
 * @author zerx
 */
@ExtendWith(MockitoExtension.class)
class MultilevelCacheOpsTest {

    @Mock
    private CacheOps l1Cache;

    @Mock
    private CacheOps l2Cache;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private CacheOps multilevelCache;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        properties.getMultilevel().getL1().setExpireAfterWrite(Duration.ofMinutes(5));
        properties.getMultilevel().getL2().setExpireAfterWrite(Duration.ofMinutes(30));
        multilevelCache = new MultilevelCacheOps(l1Cache, l2Cache, stringRedisTemplate, properties);
    }

    @Test
    void get_hit_l1_returns_without_querying_l2() {
        when(l1Cache.get("key1")).thenReturn("l1-value");

        String result = multilevelCache.get("key1");

        assertEquals("l1-value", result);
        verify(l2Cache, never()).get(anyString());
        verify(l2Cache, never()).get(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_miss_l1_hit_l2_backfills_l1() {
        when(l1Cache.get("key1")).thenReturn(null);
        when(l2Cache.get("key1")).thenReturn("l2-value");

        String result = multilevelCache.get("key1");

        assertEquals("l2-value", result);

        // 验证 L1 被回填，使用 L1 的较短 TTL
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(l1Cache).set(eq("key1"), eq("l2-value"), ttlCaptor.capture());
        assertEquals(Duration.ofMinutes(5), ttlCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_miss_both_levels_calls_loader() {
        when(l1Cache.get("key1")).thenReturn(null);
        when(l2Cache.get("key1")).thenReturn(null);

        AtomicInteger loadCount = new AtomicInteger(0);
        String result = multilevelCache.get("key1", () -> {
            loadCount.incrementAndGet();
            return "loaded-value";
        }, 30, TimeUnit.MINUTES);

        assertEquals("loaded-value", result);
        assertEquals(1, loadCount.get());

        // 验证 L2 被写入，使用原始 TTL
        verify(l2Cache).set(eq("key1"), eq("loaded-value"), eq(30L), eq(TimeUnit.MINUTES));

        // 验证 L1 被写入，使用较短 TTL
        verify(l1Cache).set(eq("key1"), eq("loaded-value"), eq(Duration.ofMinutes(5)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_with_loader_hit_l1_returns_immediately() {
        when(l1Cache.get("key1")).thenReturn("cached-value");

        AtomicInteger loadCount = new AtomicInteger(0);
        String result = multilevelCache.get("key1", () -> {
            loadCount.incrementAndGet();
            return "fresh-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("cached-value", result);
        assertEquals(0, loadCount.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_with_loader_hit_l2_returns_and_backfills() {
        when(l1Cache.get("key1")).thenReturn(null);
        when(l2Cache.get("key1")).thenReturn("l2-value");

        AtomicInteger loadCount = new AtomicInteger(0);
        String result = multilevelCache.get("key1", () -> {
            loadCount.incrementAndGet();
            return "fresh-value";
        }, 10, TimeUnit.MINUTES);

        assertEquals("l2-value", result);
        assertEquals(0, loadCount.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void set_writes_both_levels_and_publishes_invalidation() {
        multilevelCache.set("key1", "value1", 30, TimeUnit.MINUTES);

        // 验证 L1 使用较短 TTL
        verify(l1Cache).set(eq("key1"), eq("value1"), eq(Duration.ofMinutes(5)));

        // 验证 L2 使用原始 TTL
        verify(l2Cache).set(eq("key1"), eq("value1"), eq(30L), eq(TimeUnit.MINUTES));

        // 验证发布失效消息
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:key1"), eq("evict"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void set_with_duration() {
        multilevelCache.set("key1", "value1", Duration.ofMinutes(15));

        verify(l1Cache).set(eq("key1"), eq("value1"), eq(Duration.ofMinutes(5)));
        verify(l2Cache).set(eq("key1"), eq("value1"), eq(Duration.ofMinutes(15).toMillis()), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void evict_removes_from_both_levels_and_publishes() {
        multilevelCache.evict("key1");

        verify(l1Cache).evict("key1");
        verify(l2Cache).evict("key1");
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:key1"), eq("evict"));
    }

    @Test
    void evictByPrefix_removes_from_both_levels_and_publishes() {
        multilevelCache.evictByPrefix("user:");

        verify(l1Cache).evictByPrefix("user:");
        verify(l2Cache).evictByPrefix("user:");
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:user:"), eq("evict"));
    }

    @Test
    void hasKey_checks_l1_first() {
        when(l1Cache.hasKey("key1")).thenReturn(true);

        assertTrue(multilevelCache.hasKey("key1"));

        // L1 命中后不应查询 L2
        verify(l2Cache, never()).hasKey(anyString());
    }

    @Test
    void hasKey_falls_through_to_l2() {
        when(l1Cache.hasKey("key1")).thenReturn(false);
        when(l2Cache.hasKey("key1")).thenReturn(true);

        assertTrue(multilevelCache.hasKey("key1"));
    }

    @Test
    void hasKey_false_when_both_miss() {
        when(l1Cache.hasKey("key1")).thenReturn(false);
        when(l2Cache.hasKey("key1")).thenReturn(false);

        assertFalse(multilevelCache.hasKey("key1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void direct_get_miss_both_returns_null() {
        when(l1Cache.get("key1")).thenReturn(null);
        when(l2Cache.get("key1")).thenReturn(null);

        Object result = multilevelCache.get("key1");

        assertNull(result);
        // L1 未命中时不应回填
        verify(l1Cache, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void direct_get_l2_hit_backfills_l1() {
        when(l1Cache.get("key1")).thenReturn(null);
        when(l2Cache.get("key1")).thenReturn("l2-value");

        String result = multilevelCache.get("key1");

        assertEquals("l2-value", result);
        verify(l1Cache).set(eq("key1"), eq("l2-value"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void publish_invalidation_failure_does_not_throw() {
        // 模拟 Pub/Sub 发布失败
        doThrow(new RuntimeException("Redis connection failed"))
                .when(stringRedisTemplate).convertAndSend(anyString(), anyString());

        // 不应抛出异常
        assertDoesNotThrow(() -> multilevelCache.evict("key1"));

        // 即使发布失败，L1 和 L2 仍应被清除
        verify(l1Cache).evict("key1");
        verify(l2Cache).evict("key1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_with_null_loader_result_caches_in_both_levels() {
        when(l1Cache.get("key1")).thenReturn(null);
        when(l2Cache.get("key1")).thenReturn(null);

        String result = multilevelCache.get("key1", () -> null, 30, TimeUnit.MINUTES);

        assertNull(result);

        // 验证空值被缓存到 L1 和 L2
        verify(l1Cache).set(eq("key1"), isNull(), eq(Duration.ofMinutes(5)));
        verify(l2Cache).set(eq("key1"), isNull(), eq(Duration.ofMinutes(30)));
    }
}
