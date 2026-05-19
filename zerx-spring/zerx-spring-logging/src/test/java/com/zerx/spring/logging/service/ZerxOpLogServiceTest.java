package com.zerx.spring.logging.service;

import com.zerx.spring.logging.annotation.ZerxOpLog;
import com.zerx.spring.logging.event.ZerxOpLogEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxOpLogService} 接口契约测试
 *
 * @author zerx
 */
class ZerxOpLogServiceTest {

    @Test
    void concrete_implementation_works() {
        // 验证接口可以被实现
        ZerxOpLogService service = event -> { /* no-op */ };

        ZerxOpLogEvent event = ZerxOpLogEvent.success(
                "t", 1L, "u", "m", ZerxOpLog.Type.CREATE, "d",
                "C", "M", null, null, null, 0L, null, null
        );

        // Should not throw
        assertDoesNotThrow(() -> service.save(event));
    }

    @Test
    void concrete_implementation_with_verification() {
        ZerxOpLogEvent[] captured = new ZerxOpLogEvent[1];
        ZerxOpLogService service = event -> captured[0] = event;

        ZerxOpLogEvent event = ZerxOpLogEvent.success(
                "trace-123", 42L, "testuser",
                "测试模块", ZerxOpLog.Type.DELETE, "删除数据",
                "TestController", "deleteData",
                new String[]{"id"}, new Object[]{99L},
                null, 15L,
                "10.0.0.1", Map.of("extraKey", "extraValue")
        );

        service.save(event);

        assertNotNull(captured[0]);
        assertEquals("trace-123", captured[0].traceId());
        assertEquals(42L, captured[0].userId());
        assertEquals("testuser", captured[0].username());
        assertEquals("测试模块", captured[0].module());
        assertEquals(ZerxOpLog.Type.DELETE, captured[0].type());
        assertEquals("删除数据", captured[0].description());
        assertEquals("TestController", captured[0].className());
        assertEquals("deleteData", captured[0].methodName());
        assertEquals(15L, captured[0].durationMs());
        assertEquals("10.0.0.1", captured[0].clientIp());
        assertTrue(captured[0].isSuccess());
    }
}
