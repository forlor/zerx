package com.zerx.spring.cache.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxCacheEvict} 注解属性契约测试
 *
 * @author zerx
 */
class ZerxCacheEvictTest {

    @Nested
    @DisplayName("默认值")
    class DefaultValuesTest {

        @Test
        @DisplayName("默认 key 为空字符串")
        void defaultKey() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("evictMethod");
            ZerxCacheEvict anno = method.getAnnotation(ZerxCacheEvict.class);
            assertEquals("", anno.key());
        }

        @Test
        @DisplayName("默认 prefixEvict 为 false")
        void defaultPrefixEvict() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("evictMethod");
            ZerxCacheEvict anno = method.getAnnotation(ZerxCacheEvict.class);
            assertFalse(anno.prefixEvict());
        }
    }

    @Nested
    @DisplayName("自定义值")
    class CustomValuesTest {

        @Test
        @DisplayName("精确删除模式")
        void exactEvict() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("updateUser", Long.class);
            ZerxCacheEvict anno = method.getAnnotation(ZerxCacheEvict.class);
            assertEquals("user", anno.name());
            assertEquals("#id", anno.key());
            assertFalse(anno.prefixEvict());
        }

        @Test
        @DisplayName("前缀批量删除模式")
        void prefixEvict() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("clearUserCache", Long.class);
            ZerxCacheEvict anno = method.getAnnotation(ZerxCacheEvict.class);
            assertEquals("user", anno.name());
            assertEquals("#userId", anno.key());
            assertTrue(anno.prefixEvict());
        }
    }

    static class SampleService {

        @ZerxCacheEvict(name = "sample")
        public void evictMethod() {}

        @ZerxCacheEvict(name = "user", key = "#id")
        public void updateUser(Long id) {}

        @ZerxCacheEvict(name = "user", key = "#userId", prefixEvict = true)
        public void clearUserCache(Long userId) {}
    }
}
