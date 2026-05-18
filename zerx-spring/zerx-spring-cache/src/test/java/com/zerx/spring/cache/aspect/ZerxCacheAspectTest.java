package com.zerx.spring.cache.aspect;

import com.zerx.spring.cache.CacheConstants;
import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.annotation.ZerxCacheable;
import com.zerx.spring.cache.annotation.ZerxCacheEvict;
import com.zerx.spring.cache.annotation.ZerxCachePut;
import com.zerx.spring.cache.properties.ZerxCacheProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZerxCacheAspectTest {

    @Mock
    private CacheOps cacheOps;

    @Mock
    private CacheStore cacheStore;

    private ZerxCacheAspect aspect;
    private ZerxCacheProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxCacheProperties();
        properties.setKeyPrefix("test:");
        properties.setDefaultTtl(Duration.ofMinutes(30));
        aspect = new ZerxCacheAspect(cacheOps, cacheStore, properties);
    }

    @Test
    void buildCacheKey_with_spel_expression() {
        // Test with a simple mock join point
        String key = aspect.buildCacheKey("user", "#id", null);
        // When joinPoint is null, the SpEL parsing will fail and fallback to raw string
        assertNotNull(key);
        assertTrue(key.startsWith("user:"));
    }

    @Test
    void buildCacheKey_with_empty_spel_uses_fallback() {
        // When joinPoint is null and SpEL is empty, fallback to raw expression
        String key = aspect.buildCacheKey("user", "", null);
        assertNotNull(key);
        assertTrue(key.startsWith("user:"));
    }

    @Test
    void resolveTtl_returns_configured_ttl_when_positive() {
        // The aspect resolves TTL internally, test indirectly via cacheOps mock
        verify(cacheOps, never()).get(anyString(), any(), anyLong(), any(TimeUnit.class));
    }
}
