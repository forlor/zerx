package com.zerx.spring.cache.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxCacheable} 注解属性契约测试
 *
 * @author zerx
 */
class ZerxCacheableTest {

    @Nested
    @DisplayName("默认值")
    class DefaultValuesTest {

        @Test
        @DisplayName("默认 key 为空字符串")
        void defaultKey() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("cachedMethod");
            ZerxCacheable anno = method.getAnnotation(ZerxCacheable.class);
            assertEquals("", anno.key());
        }

        @Test
        @DisplayName("默认 ttl 为 -1（使用全局默认）")
        void defaultTtl() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("cachedMethod");
            ZerxCacheable anno = method.getAnnotation(ZerxCacheable.class);
            assertEquals(-1, anno.ttl());
        }

        @Test
        @DisplayName("默认 timeUnit 为 SECONDS")
        void defaultTimeUnit() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("cachedMethod");
            ZerxCacheable anno = method.getAnnotation(ZerxCacheable.class);
            assertEquals(TimeUnit.SECONDS, anno.timeUnit());
        }

        @Test
        @DisplayName("默认 nullCache 为 true")
        void defaultNullCache() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("cachedMethod");
            ZerxCacheable anno = method.getAnnotation(ZerxCacheable.class);
            assertTrue(anno.nullCache());
        }
    }

    @Nested
    @DisplayName("自定义值")
    class CustomValuesTest {

        @Test
        @DisplayName("可自定义 name、key、ttl、timeUnit、nullCache")
        void customValues() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("customCached", Long.class);
            ZerxCacheable anno = method.getAnnotation(ZerxCacheable.class);
            assertEquals("user", anno.name());
            assertEquals("#id", anno.key());
            assertEquals(30, anno.ttl());
            assertEquals(TimeUnit.MINUTES, anno.timeUnit());
            assertFalse(anno.nullCache());
        }

        @Test
        @DisplayName("可禁用空值缓存")
        void disableNullCache() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("noNullCache");
            ZerxCacheable anno = method.getAnnotation(ZerxCacheable.class);
            assertFalse(anno.nullCache());
        }
    }

    static class SampleService {

        @ZerxCacheable(name = "sample")
        public String cachedMethod() {
            return "data";
        }

        @ZerxCacheable(name = "user", key = "#id", ttl = 30, timeUnit = TimeUnit.MINUTES, nullCache = false)
        public String customCached(Long id) {
            return "user:" + id;
        }

        @ZerxCacheable(name = "config", nullCache = false)
        public String noNullCache() {
            return null;
        }
    }
}
