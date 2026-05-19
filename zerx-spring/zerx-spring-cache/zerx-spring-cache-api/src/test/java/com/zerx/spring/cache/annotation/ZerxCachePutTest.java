package com.zerx.spring.cache.annotation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxCachePut} 注解属性契约测试
 *
 * @author zerx
 */
class ZerxCachePutTest {

    @Nested
    @DisplayName("默认值")
    class DefaultValuesTest {

        @Test
        @DisplayName("默认 key 为空字符串")
        void defaultKey() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("putMethod");
            ZerxCachePut anno = method.getAnnotation(ZerxCachePut.class);
            assertEquals("", anno.key());
        }

        @Test
        @DisplayName("默认 ttl 为 -1（使用全局默认）")
        void defaultTtl() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("putMethod");
            ZerxCachePut anno = method.getAnnotation(ZerxCachePut.class);
            assertEquals(-1, anno.ttl());
        }

        @Test
        @DisplayName("默认 timeUnit 为 SECONDS")
        void defaultTimeUnit() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("putMethod");
            ZerxCachePut anno = method.getAnnotation(ZerxCachePut.class);
            assertEquals(TimeUnit.SECONDS, anno.timeUnit());
        }
    }

    @Nested
    @DisplayName("自定义值")
    class CustomValuesTest {

        @Test
        @DisplayName("可自定义全部属性")
        void customValues() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("createUser", String.class);
            ZerxCachePut anno = method.getAnnotation(ZerxCachePut.class);
            assertEquals("user", anno.name());
            assertEquals("#user", anno.key());
            assertEquals(30, anno.ttl());
            assertEquals(TimeUnit.MINUTES, anno.timeUnit());
        }

        @Test
        @DisplayName("可使用 HOURS 作为时间单位")
        void hoursTimeUnit() throws NoSuchMethodException {
            Method method = SampleService.class.getMethod("updateConfig", String.class, String.class);
            ZerxCachePut anno = method.getAnnotation(ZerxCachePut.class);
            assertEquals("config", anno.name());
            assertEquals("#key", anno.key());
            assertEquals(1, anno.ttl());
            assertEquals(TimeUnit.HOURS, anno.timeUnit());
        }
    }

    static class SampleService {

        @ZerxCachePut(name = "sample")
        public String putMethod() { return "data"; }

        @ZerxCachePut(name = "user", key = "#user", ttl = 30, timeUnit = TimeUnit.MINUTES)
        public String createUser(String user) { return "created:" + user; }

        @ZerxCachePut(name = "config", key = "#key", ttl = 1, timeUnit = TimeUnit.HOURS)
        public String updateConfig(String key, String value) { return value; }
    }
}
