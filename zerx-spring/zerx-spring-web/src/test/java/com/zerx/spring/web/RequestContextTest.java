package com.zerx.spring.web;

import com.zerx.spring.web.context.RequestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 请求上下文测试
 *
 * @author zerx
 */
class RequestContextTest {

    @Test
    void init_shouldCreateNewContext() {
        RequestContext.clear(); // 确保干净状态
        RequestContext ctx = RequestContext.init();

        assertNotNull(ctx, "初始化后上下文不应为 null");
        assertNotNull(RequestContext.get(), "初始化后应能获取到上下文");
        assertNotNull(RequestContext.getRequestId(), "应自动生成 requestId");
    }

    @Test
    void get_shouldReturnNullBeforeInit() {
        RequestContext.clear();
        assertNull(RequestContext.get(), "未初始化时应返回 null");
    }

    @Test
    void clear_shouldRemoveContext() {
        RequestContext.init();
        assertNotNull(RequestContext.get());

        RequestContext.clear();
        assertNull(RequestContext.get(), "清理后应为 null");
    }

    @Test
    void userId_shouldGetAndSet() {
        RequestContext.init();

        assertNull(RequestContext.getUserId(), "初始 userId 应为 null");

        RequestContext.setUserId(1001L);
        assertEquals(1001L, RequestContext.getUserId());
    }

    @Test
    void username_shouldGetAndSet() {
        RequestContext.init();

        assertNull(RequestContext.getUsername());

        RequestContext.setUsername("admin");
        assertEquals("admin", RequestContext.getUsername());
    }

    @Test
    void tenantId_shouldGetAndSet() {
        RequestContext.init();

        assertNull(RequestContext.getTenantId());

        RequestContext.setTenantId("tenant-001");
        assertEquals("tenant-001", RequestContext.getTenantId());
    }

    @Test
    void traceId_shouldGetAndSet() {
        RequestContext.init();

        assertNull(RequestContext.getTraceId());

        RequestContext.setTraceId("trace-abc-123");
        assertEquals("trace-abc-123", RequestContext.getTraceId());
    }

    @Test
    void requestIp_shouldGetAndSet() {
        RequestContext.init();

        assertNull(RequestContext.getRequestIp());

        RequestContext.setRequestIp("192.168.1.100");
        assertEquals("192.168.1.100", RequestContext.getRequestIp());
    }

    @Test
    void requestId_shouldAutoGenerate() {
        RequestContext.init();

        String requestId = RequestContext.getRequestId();
        assertNotNull(requestId);
        assertFalse(requestId.isEmpty());
    }

    @Test
    void init_shouldGenerateNewRequestIdEachTime() {
        RequestContext.init();
        String firstId = RequestContext.getRequestId();

        RequestContext.init();
        String secondId = RequestContext.getRequestId();

        assertNotEquals(firstId, secondId, "每次初始化应生成不同的 requestId");
    }

    @Test
    void setters_shouldThrowWhenNotInitialized() {
        RequestContext.clear();

        assertThrows(IllegalStateException.class, () -> RequestContext.setUserId(1L),
                "未初始化时设置 userId 应抛出异常");

        assertThrows(IllegalStateException.class, () -> RequestContext.setUsername("test"),
                "未初始化时设置 username 应抛出异常");

        assertThrows(IllegalStateException.class, () -> RequestContext.setTenantId("t1"),
                "未初始化时设置 tenantId 应抛出异常");

        assertThrows(IllegalStateException.class, () -> RequestContext.setTraceId("t1"),
                "未初始化时设置 traceId 应抛出异常");

        assertThrows(IllegalStateException.class, () -> RequestContext.setRequestIp("127.0.0.1"),
                "未初始化时设置 requestIp 应抛出异常");
    }

    @Test
    void getters_shouldReturnNullWhenNotInitialized() {
        RequestContext.clear();

        assertNull(RequestContext.getUserId());
        assertNull(RequestContext.getUsername());
        assertNull(RequestContext.getTenantId());
        assertNull(RequestContext.getTraceId());
        assertNull(RequestContext.getRequestIp());
        assertNull(RequestContext.getRequestId());
    }

    @Test
    void threadSafety_shouldIsolateBetweenThreads() throws InterruptedException {
        RequestContext.init();
        RequestContext.setUserId(1L);
        RequestContext.setUsername("main-thread");

        var error = new java.util.concurrent.atomic.AtomicReference<AssertionError>();

        Thread thread = new Thread(() -> {
            try {
                // 子线程不应看到主线程的上下文
                assertNull(RequestContext.get(), "子线程不应看到主线程的上下文");

                // 子线程可以有自己的上下文
                RequestContext.init();
                RequestContext.setUserId(2L);
                assertEquals(2L, RequestContext.getUserId());

                RequestContext.clear();
            } catch (AssertionError e) {
                error.set(e);
            }
        });

        thread.start();
        thread.join();

        assertNull(error.get(), "子线程测试不应失败");

        // 主线程的上下文不受影响
        assertEquals(1L, RequestContext.getUserId());
        assertEquals("main-thread", RequestContext.getUsername());
    }
}
