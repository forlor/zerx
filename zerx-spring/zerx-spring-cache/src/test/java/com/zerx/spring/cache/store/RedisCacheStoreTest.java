package com.zerx.spring.cache.store;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheStoreTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisCacheStore store;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        store = new RedisCacheStore(redisTemplate, properties);
    }

    @Test
    void set_stores_value_with_jittered_ttl() {
        store.set("key1", "value1", Duration.ofMinutes(10));

        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(valueOperations).set(eq("test:key1"), eq("value1"), ttlCaptor.capture(), eq(java.util.concurrent.TimeUnit.MILLISECONDS));

        long ttl = ttlCaptor.getValue();
        assertTrue(ttl >= 540000 && ttl < 660000,
                "TTL should have jitter: expected ~600000ms ±10%, got " + ttl);
    }

    @Test
    void get_returns_cached_value() {
        when(valueOperations.get("test:key1")).thenReturn("cached-value");
        var result = store.get("key1");
        assertTrue(result.isPresent());
        assertEquals("cached-value", result.get());
    }

    @Test
    void get_returns_empty_for_missing_key() {
        when(valueOperations.get("test:key1")).thenReturn(null);
        var result = store.get("key1");
        assertTrue(result.isEmpty());
    }

    @Test
    void evict_deletes_key() {
        store.evict("key1");
        verify(redisTemplate).delete("test:key1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void evictByPrefix_uses_scan_not_keys() {
        // Mock SCAN cursor that returns keys via forEachRemaining
        Cursor<String> cursor = mock(Cursor.class);
        doAnswer(inv -> {
            java.util.function.Consumer<? super String> action = inv.getArgument(0);
            action.accept("test:user:1");
            action.accept("test:user:2");
            return null;
        }).when(cursor).forEachRemaining(any(java.util.function.Consumer.class));
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.evictByPrefix("user:");

        // Verify SCAN was used (not keys())
        verify(redisTemplate).scan(any(ScanOptions.class));
        // Verify keys() was NOT called
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void evictByPrefix_no_match_does_not_delete() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        store.evictByPrefix("order:");

        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void hasKey_true() {
        when(redisTemplate.hasKey("test:key1")).thenReturn(true);
        assertTrue(store.hasKey("key1"));
    }

    @Test
    void hasKey_false() {
        when(redisTemplate.hasKey("test:key1")).thenReturn(false);
        assertFalse(store.hasKey("key1"));
    }

    @Test
    void multiGet() {
        // Use ordered keys to ensure predictable mapping
        when(valueOperations.multiGet(anyList()))
                .thenReturn(Arrays.asList("value1", "value2", null));

        Map<String, Object> result = store.multiGet(List.of("key1", "key2", "missing"));

        assertTrue(result.size() >= 2);
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));
        assertFalse(result.containsKey("missing"));
    }

    @Test
    void multiGet_excludes_null_marker() {
        when(valueOperations.multiGet(anyList()))
                .thenReturn(Arrays.asList("value1", CacheConstants.NULL_MARKER));

        Map<String, Object> result = store.multiGet(List.of("key1", "nullKey"));

        assertEquals(1, result.size());
        assertTrue(result.containsKey("key1"));
        assertFalse(result.containsKey("nullKey"));
    }

    @Test
    void multiGet_empty_keys() {
        Map<String, Object> result = store.multiGet(Set.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void multiSet_uses_pipeline() {
        Map<String, Object> entries = Map.of("key1", "value1", "key2", "value2");

        store.multiSet(entries, Duration.ofMinutes(10));

        verify(redisTemplate).executePipelined(any(RedisCallback.class));
    }

    @Test
    void multiEvict() {
        when(redisTemplate.delete(anyList())).thenReturn(3L);
        store.multiEvict(Set.of("key1", "key2", "key3"));
        verify(redisTemplate).delete(anyList());
    }

    @Test
    void multiEvict_empty_keys() {
        store.multiEvict(Set.of());
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    void jitter_produces_different_ttls() {
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);

        for (int i = 0; i < 10; i++) {
            store.set("key:" + i, "value:" + i, Duration.ofSeconds(60));
        }

        verify(valueOperations, times(10)).set(anyString(), any(), ttlCaptor.capture(),
                eq(java.util.concurrent.TimeUnit.MILLISECONDS));

        var ttls = ttlCaptor.getAllValues();
        long firstTtl = ttls.get(0);
        boolean hasVariation = ttls.stream().anyMatch(ttl -> ttl != firstTtl);
        assertTrue(hasVariation, "TTLs should vary due to jitter, but all were " + ttls);
    }

    @Test
    void withPrefix() {
        assertEquals("test:mykey", store.withPrefix("mykey"));
        assertEquals("test:mykey", store.withPrefix("test:mykey"));
    }
}
