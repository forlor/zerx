package com.zerx.spring.cache;

import com.zerx.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheExceptionTest {

    @Test
    void base_exception_with_error_code() {
        CacheException ex = new CacheException(CacheException.CACHE_ERROR);
        assertEquals("10010", ex.getCode());
        assertEquals(500, ex.getHttpStatus());
    }

    @Test
    void base_exception_with_cause() {
        RuntimeException cause = new RuntimeException("redis down");
        CacheException ex = new CacheException(CacheException.CACHE_ERROR, cause);
        assertEquals(cause, ex.getCause());
        assertEquals("10010", ex.getCode());
    }

    @Test
    void base_exception_with_custom_message() {
        CacheException ex = new CacheException(CacheException.CACHE_ERROR, "custom error message");
        assertEquals("custom error message", ex.getMessage());
        assertEquals("10010", ex.getCode());
    }

    @Test
    void serialization_exception() {
        RuntimeException cause = new RuntimeException("json error");
        CacheException.SerializationException ex =
                new CacheException.SerializationException("serialize failed", cause);
        assertTrue(ex instanceof CacheException);
        assertEquals("10011", ex.getCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void lock_timeout_exception() {
        CacheException.LockTimeoutException ex =
                new CacheException.LockTimeoutException("lock timeout for key: user:1");
        assertTrue(ex instanceof CacheException);
        assertEquals("10012", ex.getCode());
        assertEquals("lock timeout for key: user:1", ex.getMessage());
    }

    @Test
    void error_codes_are_unique() {
        assertNotEquals(CacheException.CACHE_ERROR.code(),
                CacheException.CACHE_SERIALIZATION_ERROR.code());
        assertNotEquals(CacheException.CACHE_ERROR.code(),
                CacheException.CACHE_LOCK_TIMEOUT.code());
        assertNotEquals(CacheException.CACHE_SERIALIZATION_ERROR.code(),
                CacheException.CACHE_LOCK_TIMEOUT.code());
    }
}
