package com.zerx.spring.cache;

import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ZerxException;

/**
 * 缓存操作异常基类。
 *
 * @author zerx
 */
public class CacheException extends ZerxException {

    /** 缓存操作失败 */
    public static final ErrorCode CACHE_ERROR = ErrorCode.of("10010", "缓存操作失败", 500);

    /** 缓存序列化失败 */
    public static final ErrorCode CACHE_SERIALIZATION_ERROR = ErrorCode.of("10011", "缓存序列化失败", 500);

    /** 缓存锁超时 */
    public static final ErrorCode CACHE_LOCK_TIMEOUT = ErrorCode.of("10012", "缓存锁等待超时", 500);

    public CacheException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CacheException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public CacheException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public CacheException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 缓存序列化异常。
     */
    public static class SerializationException extends CacheException {

        public SerializationException(String message, Throwable cause) {
            super(CACHE_SERIALIZATION_ERROR, message, cause);
        }
    }

    /**
     * 缓存锁获取超时异常（防击穿互斥锁等待超时）。
     */
    public static class LockTimeoutException extends CacheException {

        public LockTimeoutException(String message) {
            super(CACHE_LOCK_TIMEOUT, message);
        }
    }
}
