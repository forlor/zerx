package com.zerx.spring.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheConstantsTest {

    @Test
    void null_marker_is_non_empty_string() {
        assertFalse(CacheConstants.NULL_MARKER.isEmpty());
        assertTrue(CacheConstants.NULL_MARKER.startsWith("__"));
        assertTrue(CacheConstants.NULL_MARKER.endsWith("__"));
    }

    @Test
    void invalidation_channel_prefix_starts_with_zerx() {
        assertTrue(CacheConstants.INVALIDATION_CHANNEL_PREFIX.startsWith("zerx:"));
        assertTrue(CacheConstants.INVALIDATION_CHANNEL_PREFIX.contains("invalidate:"));
    }

    @Test
    void jitter_range_is_valid() {
        assertTrue(CacheConstants.JITTER_MIN < 1.0);
        assertTrue(CacheConstants.JITTER_MAX > 1.0);
        assertTrue(CacheConstants.JITTER_MIN < CacheConstants.JITTER_MAX);
    }
}
