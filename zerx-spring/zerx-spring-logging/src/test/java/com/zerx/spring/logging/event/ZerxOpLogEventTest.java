package com.zerx.spring.logging.event;

import com.zerx.spring.logging.annotation.ZerxOpLog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxOpLogEvent} 单元测试
 *
 * @author zerx
 */
class ZerxOpLogEventTest {

    @Test
    void success_creates_event_without_exception() {
        ZerxOpLogEvent event = ZerxOpLogEvent.success(
                "trace-001", 1001L, "admin",
                "用户管理", ZerxOpLog.Type.CREATE, "创建用户",
                "UserController", "createUser",
                new String[]{"name", "email"}, new Object[]{"张三", "zhangsan@test.com"},
                "OK", 42L,
                "192.168.1.1", Map.of("key1", "value1")
        );

        assertEquals("trace-001", event.traceId());
        assertEquals(1001L, event.userId());
        assertEquals("admin", event.username());
        assertEquals("用户管理", event.module());
        assertEquals(ZerxOpLog.Type.CREATE, event.type());
        assertEquals("创建用户", event.description());
        assertEquals("UserController", event.className());
        assertEquals("createUser", event.methodName());
        assertNotNull(event.paramNames());
        assertNotNull(event.paramValues());
        assertEquals("OK", event.result());
        assertEquals(42L, event.durationMs());
        assertNull(event.exception());
        assertEquals("192.168.1.1", event.clientIp());
        assertNotNull(event.timestamp());
        assertNotNull(event.extra());
        assertTrue(event.isSuccess());
    }

    @Test
    void failure_creates_event_with_exception() {
        RuntimeException ex = new RuntimeException("业务异常");
        ZerxOpLogEvent event = ZerxOpLogEvent.failure(
                "trace-002", 1002L, "operator",
                "订单管理", ZerxOpLog.Type.UPDATE, "更新订单",
                "OrderController", "updateOrder",
                new String[]{"id"}, new Object[]{42L},
                ex, 128L,
                "10.0.0.1", null
        );

        assertEquals("trace-002", event.traceId());
        assertEquals(1002L, event.userId());
        assertEquals("operator", event.username());
        assertEquals("订单管理", event.module());
        assertEquals(ZerxOpLog.Type.UPDATE, event.type());
        assertEquals("更新订单", event.description());
        assertEquals("OrderController", event.className());
        assertEquals("updateOrder", event.methodName());
        assertNull(event.result());
        assertEquals(128L, event.durationMs());
        assertNotNull(event.exception());
        assertEquals("业务异常", event.exception().getMessage());
        assertEquals("10.0.0.1", event.clientIp());
        assertNotNull(event.timestamp());
        assertNull(event.extra());
        assertFalse(event.isSuccess());
    }

    @Test
    void isSuccess_returns_true_when_no_exception() {
        ZerxOpLogEvent event = ZerxOpLogEvent.success(
                "t", null, null, "", ZerxOpLog.Type.OTHER, "",
                "", "", null, null, null, 0L, null, null
        );
        assertTrue(event.isSuccess());
    }

    @Test
    void isSuccess_returns_false_when_has_exception() {
        ZerxOpLogEvent event = ZerxOpLogEvent.failure(
                "t", null, null, "", ZerxOpLog.Type.OTHER, "",
                "", "", null, null,
                new RuntimeException("err"), 0L, null, null
        );
        assertFalse(event.isSuccess());
    }

    @Test
    void timestamp_is_recent() {
        Instant before = Instant.now();
        ZerxOpLogEvent event = ZerxOpLogEvent.success(
                "t", null, null, "", ZerxOpLog.Type.OTHER, "",
                "", "", null, null, null, 0L, null, null
        );
        Instant after = Instant.now();

        assertTrue(!event.timestamp().isBefore(before) && !event.timestamp().isAfter(after));
    }

    @Test
    void record_equality() {
        ZerxOpLogEvent event1 = ZerxOpLogEvent.success(
                "t1", 1L, "u", "m", ZerxOpLog.Type.LOGIN, "d",
                "C", "M", null, null, null, 0L, "127.0.0.1", null
        );
        ZerxOpLogEvent event2 = ZerxOpLogEvent.success(
                "t1", 1L, "u", "m", ZerxOpLog.Type.LOGIN, "d",
                "C", "M", null, null, null, 0L, "127.0.0.1", null
        );
        // Records with same fields should be equal, but timestamps differ
        // Since timestamp is set to Instant.now(), they won't be equal
        assertNotEquals(event1, event2);
    }

    @Test
    void all_type_values_exist() {
        ZerxOpLog.Type[] types = ZerxOpLog.Type.values();
        assertEquals(9, types.length);
        assertNotNull(ZerxOpLog.Type.LOGIN);
        assertNotNull(ZerxOpLog.Type.LOGOUT);
        assertNotNull(ZerxOpLog.Type.CREATE);
        assertNotNull(ZerxOpLog.Type.UPDATE);
        assertNotNull(ZerxOpLog.Type.DELETE);
        assertNotNull(ZerxOpLog.Type.EXPORT);
        assertNotNull(ZerxOpLog.Type.IMPORT);
        assertNotNull(ZerxOpLog.Type.QUERY);
        assertNotNull(ZerxOpLog.Type.OTHER);
    }
}
