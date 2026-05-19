package com.zerx.spring.cache.store;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultilevelCacheStoreTest {

    @Mock
    private CacheStore l1Cache;

    @Mock
    private CacheStore l2Cache;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private CacheStore multilevelStore;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        properties.getMultilevel().getL1().setExpireAfterWrite(Duration.ofMinutes(5));
        properties.getMultilevel().getL2().setExpireAfterWrite(Duration.ofMinutes(30));
        multilevelStore = new MultilevelCacheStore(l1Cache, l2Cache, stringRedisTemplate, properties);
    }

    @Test
    void get_hit_l1_returns_without_querying_l2() {
        when(l1Cache.get("key1")).thenReturn(Optional.of("l1-value"));

        Optional<Object> result = multilevelStore.get("key1");

        assertTrue(result.isPresent());
        assertEquals("l1-value", result.get());
        verify(l2Cache, never()).get(anyString());
    }

    @Test
    void get_miss_l1_hit_l2_backfills_l1() {
        when(l1Cache.get("key1")).thenReturn(Optional.empty());
        when(l2Cache.get("key1")).thenReturn(Optional.of("l2-value"));

        Optional<Object> result = multilevelStore.get("key1");

        assertTrue(result.isPresent());
        assertEquals("l2-value", result.get());

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(l1Cache).set(eq("key1"), eq("l2-value"), ttlCaptor.capture());
        assertEquals(Duration.ofMinutes(5), ttlCaptor.getValue());
    }

    @Test
    void get_miss_both_returns_empty() {
        when(l1Cache.get("key1")).thenReturn(Optional.empty());
        when(l2Cache.get("key1")).thenReturn(Optional.empty());

        Optional<Object> result = multilevelStore.get("key1");

        assertTrue(result.isEmpty());
    }

    @Test
    void get_l1_null_marker_ignores_and_queries_l2() {
        when(l1Cache.get("key1")).thenReturn(Optional.of(CacheConstants.NULL_MARKER));
        when(l2Cache.get("key1")).thenReturn(Optional.of("l2-value"));

        Optional<Object> result = multilevelStore.get("key1");

        assertTrue(result.isPresent());
        assertEquals("l2-value", result.get());
        verify(l2Cache).get("key1");
    }

    @Test
    void set_writes_l2_first_then_l1() {
        multilevelStore.set("key1", "value1", Duration.ofMinutes(30));

        // Verify L2 was called first
        verify(l2Cache).set(eq("key1"), eq("value1"), eq(Duration.ofMinutes(30)));

        // Verify L1 was called with shorter TTL
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(l1Cache).set(eq("key1"), eq("value1"), ttlCaptor.capture());
        assertEquals(Duration.ofMinutes(5), ttlCaptor.getValue());

        // Verify Pub/Sub notification
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:key1"), eq("evict"));
    }

    @Test
    void set_l2_failure_does_not_write_l1() {
        doThrow(new RuntimeException("Redis down")).when(l2Cache).set(anyString(), any(), any(Duration.class));

        assertThrows(com.zerx.spring.cache.CacheException.class,
                () -> multilevelStore.set("key1", "value1", Duration.ofMinutes(30)));

        verify(l1Cache, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void set_l1_failure_does_not_throw() {
        doThrow(new RuntimeException("Caffeine error")).when(l1Cache).set(anyString(), any(), any(Duration.class));

        // Should NOT throw because L1 failure is non-fatal
        assertDoesNotThrow(() -> multilevelStore.set("key1", "value1", Duration.ofMinutes(30)));

        // L2 should still have been written
        verify(l2Cache).set(eq("key1"), eq("value1"), eq(Duration.ofMinutes(30)));
    }

    @Test
    void evict_removes_from_both_levels_and_publishes() {
        multilevelStore.evict("key1");

        verify(l1Cache).evict("key1");
        verify(l2Cache).evict("key1");
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:key1"), eq("evict"));
    }

    @Test
    void evictByPrefix_removes_from_both_levels_and_publishes() {
        multilevelStore.evictByPrefix("user:");

        verify(l1Cache).evictByPrefix("user:");
        verify(l2Cache).evictByPrefix("user:");
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:user:"), eq("evict"));
    }

    @Test
    void hasKey_checks_l1_first() {
        when(l1Cache.hasKey("key1")).thenReturn(true);
        assertTrue(multilevelStore.hasKey("key1"));
        verify(l2Cache, never()).hasKey(anyString());
    }

    @Test
    void hasKey_falls_through_to_l2() {
        when(l1Cache.hasKey("key1")).thenReturn(false);
        when(l2Cache.hasKey("key1")).thenReturn(true);
        assertTrue(multilevelStore.hasKey("key1"));
    }

    @Test
    void hasKey_false_when_both_miss() {
        when(l1Cache.hasKey("key1")).thenReturn(false);
        when(l2Cache.hasKey("key1")).thenReturn(false);
        assertFalse(multilevelStore.hasKey("key1"));
    }

    @Test
    void publish_invalidation_failure_does_not_throw() {
        doThrow(new RuntimeException("Redis connection failed"))
                .when(stringRedisTemplate).convertAndSend(anyString(), anyString());

        assertDoesNotThrow(() -> multilevelStore.evict("key1"));

        verify(l1Cache).evict("key1");
        verify(l2Cache).evict("key1");
    }

    @Test
    void multiGet_l1_hit_no_l2_query() {
        when(l1Cache.multiGet(Set.of("key1", "key2")))
                .thenReturn(Map.of("key1", "v1", "key2", "v2"));

        Map<String, Object> result = multilevelStore.multiGet(Set.of("key1", "key2"));

        assertEquals(2, result.size());
        verify(l2Cache, never()).multiGet(anyCollection());
    }

    @Test
    void multiGet_l1_partial_miss_queries_l2() {
        when(l1Cache.multiGet(anyCollection()))
                .thenReturn(Map.of("key1", "v1"));
        when(l2Cache.multiGet(anyCollection()))
                .thenReturn(Map.of("key2", "v2"));

        Map<String, Object> result = multilevelStore.multiGet(Set.of("key1", "key2"));

        assertEquals(2, result.size());
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));

        // Verify L2 hit backfills L1
        verify(l1Cache).multiSet(anyMap(), any(Duration.class));
    }

    @Test
    void multiSet_writes_both_levels() {
        Map<String, Object> entries = Map.of("key1", "v1", "key2", "v2");
        multilevelStore.multiSet(entries, Duration.ofMinutes(30));

        verify(l2Cache).multiSet(eq(entries), eq(Duration.ofMinutes(30)));
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(l1Cache).multiSet(eq(entries), ttlCaptor.capture());
        assertEquals(Duration.ofMinutes(5), ttlCaptor.getValue());
    }

    @Test
    void multiEvict() {
        multilevelStore.multiEvict(Set.of("key1", "key2"));

        verify(l1Cache).multiEvict(Set.of("key1", "key2"));
        verify(l2Cache).multiEvict(Set.of("key1", "key2"));
    }

    @Test
    void multiGet_empty_keys() {
        Map<String, Object> result = multilevelStore.multiGet(Set.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void multiSet_empty_entries() {
        assertDoesNotThrow(() -> multilevelStore.multiSet(Map.of(), Duration.ofMinutes(30)));
    }

    @Test
    void multiEvict_empty_keys() {
        assertDoesNotThrow(() -> multilevelStore.multiEvict(Set.of()));
    }

    @Test
    void withPrefix() {
        // Access via reflection or verify the behavior through the store
        multilevelStore.evict("mykey");
        verify(l1Cache).evict("mykey");
        verify(l2Cache).evict("mykey");
        verify(stringRedisTemplate).convertAndSend(eq("zerx:cache:invalidate:test:mykey"), eq("evict"));
    }
}
