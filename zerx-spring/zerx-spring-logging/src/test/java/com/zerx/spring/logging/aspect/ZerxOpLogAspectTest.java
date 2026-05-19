package com.zerx.spring.logging.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.logging.LogRateLimiter;
import com.zerx.spring.logging.annotation.ZerxOpLog;
import com.zerx.spring.logging.event.ZerxOpLogEvent;
import com.zerx.spring.logging.properties.ZerxLoggingProperties;
import com.zerx.spring.logging.service.ZerxOpLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link ZerxOpLogAspect} 单元测试
 *
 * @author zerx
 */
@ExtendWith(MockitoExtension.class)
class ZerxOpLogAspectTest {

    @Mock
    private ZerxOpLogService opLogService;

    private ZerxLoggingProperties properties;
    private ZerxOpLogAspect aspect;

    @BeforeEach
    void setUp() {
        properties = new ZerxLoggingProperties();
        aspect = new ZerxOpLogAspect(properties, opLogService,
                new ObjectMapper(), null);
    }

    @Test
    void resolveDescription_with_static_text() throws NoSuchMethodException {
        // 静态文本描述（纯字符串字面量）
        String desc = aspect.resolveDescription("创建订单", null, null, null);
        assertEquals("创建订单", desc);
    }

    @Test
    void resolveDescription_with_empty_value() {
        String desc = aspect.resolveDescription("", null, null, null);
        assertEquals("", desc);
    }

    @Test
    void resolveDescription_with_null_value() {
        String desc = aspect.resolveDescription(null, null, null, null);
        assertEquals("", desc);
    }

    @Test
    void truncateParams_with_no_params() {
        Object result = aspect.truncateParams(new String[0], new Object[0], new String[0]);
        assertNull(result);
    }

    @Test
    void truncateParams_with_null_paramNames() {
        Object result = aspect.truncateParams(null, new Object[0], new String[0]);
        assertNull(result);
    }

    @Test
    void truncateParams_with_sensitive_params() {
        String[] names = {"username", "password", "email"};
        Object[] values = {"admin", "secret123", "admin@test.com"};
        String[] sensitive = {"password"};

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) aspect.truncateParams(names, values, sensitive);

        assertEquals("\"admin\"", result.get("username"));
        assertEquals("******", result.get("password"));
        assertEquals("\"admin@test.com\"", result.get("email"));
    }

    @Test
    void truncateParams_with_empty_sensitive_list() {
        String[] names = {"username"};
        Object[] values = {"admin"};

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) aspect.truncateParams(names, values, new String[0]);

        assertEquals("\"admin\"", result.get("username"));
    }

    @Test
    void truncate_with_null_object() {
        Object result = aspect.truncate(null, 1024);
        assertNull(result);
    }

    @Test
    void truncate_with_short_string() {
        Object result = aspect.truncate("hello", 1024);
        assertEquals("\"hello\"", result.toString());
    }

    @Test
    void truncate_with_long_string() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append("a");
        }
        Object result = aspect.truncate(sb.toString(), 100);
        String str = result.toString();
        assertTrue(str.startsWith("\""));
        assertTrue(str.contains("...(truncated)"));
    }

    @Test
    void truncate_with_non_serializable() {
        Object result = aspect.truncate(new Object(), 1024);
        assertNotNull(result);
    }

    @Test
    void logRateLimiter_blocks_excessive_logs() {
        LogRateLimiter limiter = LogRateLimiter.of(2, java.time.Duration.ofSeconds(1));
        ZerxOpLogAspect rateLimitedAspect = new ZerxOpLogAspect(properties, opLogService,
                new ObjectMapper(), limiter);

        // First 2 calls should pass
        assertTrue(limiter.tryAcquire("test"));
        assertTrue(limiter.tryAcquire("test"));
        // Third call should be blocked
        assertFalse(limiter.tryAcquire("test"));
    }
}
