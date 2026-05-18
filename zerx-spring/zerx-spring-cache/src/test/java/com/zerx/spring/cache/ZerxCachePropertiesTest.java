package com.zerx.spring.cache.properties;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ZerxCachePropertiesTest {

    @Test
    void defaultValues() {
        var props = new ZerxCacheProperties();

        assertEquals(ZerxCacheProperties.CacheType.CAFFEINE, props.getType());
        assertEquals("zerx:", props.getKeyPrefix());
        assertTrue(props.isEnabled());
        assertEquals(Duration.ofMinutes(30), props.getDefaultTtl());
        assertEquals(Duration.ofMinutes(5), props.getNullValueTtl());
        assertEquals(Duration.ofSeconds(5), props.getLockTimeout());
        assertEquals(ZerxCacheProperties.SerializerType.JACKSON, props.getSerializer());
    }

    @Test
    void caffeineDefaults() {
        var props = new ZerxCacheProperties();
        var spec = props.getCaffeine();

        assertEquals(10000, spec.getMaxSize());
        assertEquals(Duration.ofMinutes(10), spec.getExpireAfterWrite());
        assertEquals(Duration.ofMinutes(30), spec.getExpireAfterAccess());
        assertFalse(spec.isRecordStats());
    }

    @Test
    void multilevelDefaults() {
        var props = new ZerxCacheProperties();
        var ml = props.getMultilevel();

        assertEquals(1000, ml.getL1().getMaxSize());
        assertEquals(Duration.ofMinutes(5), ml.getL1().getExpireAfterWrite());
        assertEquals(Duration.ofMinutes(30), ml.getL2().getExpireAfterWrite());
    }

    @Test
    void setType() {
        var props = new ZerxCacheProperties();
        props.setType(ZerxCacheProperties.CacheType.REDIS);
        assertEquals(ZerxCacheProperties.CacheType.REDIS, props.getType());

        props.setType(ZerxCacheProperties.CacheType.MULTILEVEL);
        assertEquals(ZerxCacheProperties.CacheType.MULTILEVEL, props.getType());
    }

    @Test
    void disabledCache() {
        var props = new ZerxCacheProperties();
        props.setEnabled(false);
        assertFalse(props.isEnabled());
    }

    @Test
    void serializerType() {
        var props = new ZerxCacheProperties();
        props.setSerializer(ZerxCacheProperties.SerializerType.JSON);
        assertEquals(ZerxCacheProperties.SerializerType.JSON, props.getSerializer());
    }

    @Test
    void defaultTtl() {
        var props = new ZerxCacheProperties();
        props.setDefaultTtl(Duration.ofHours(1));
        assertEquals(Duration.ofHours(1), props.getDefaultTtl());
    }
}
