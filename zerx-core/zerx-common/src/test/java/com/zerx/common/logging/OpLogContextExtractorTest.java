package com.zerx.common.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link OpLogContextExtractor} 单元测试
 *
 * @author zerx
 */
class OpLogContextExtractorTest {

    @Test
    void opLogContext_record_fields() {
        OpLogContextExtractor.OpLogContext ctx = new OpLogContextExtractor.OpLogContext(1L, "admin", "127.0.0.1");
        assertEquals(1L, ctx.userId());
        assertEquals("admin", ctx.username());
        assertEquals("127.0.0.1", ctx.clientIp());
    }

    @Test
    void opLogContext_null_fields() {
        OpLogContextExtractor.OpLogContext ctx = new OpLogContextExtractor.OpLogContext(null, null, null);
        assertNull(ctx.userId());
        assertNull(ctx.username());
        assertNull(ctx.clientIp());
    }

    @Test
    void opLogContext_equality() {
        OpLogContextExtractor.OpLogContext a = new OpLogContextExtractor.OpLogContext(1L, "admin", "10.0.0.1");
        OpLogContextExtractor.OpLogContext b = new OpLogContextExtractor.OpLogContext(1L, "admin", "10.0.0.1");
        assertEquals(a, b);
    }

    @Test
    void functional_interface_extract() {
        OpLogContextExtractor extractor = () -> new OpLogContextExtractor.OpLogContext(42L, "test", "192.168.1.1");
        OpLogContextExtractor.OpLogContext ctx = extractor.extract();
        assertNotNull(ctx);
        assertEquals(42L, ctx.userId());
        assertEquals("test", ctx.username());
        assertEquals("192.168.1.1", ctx.clientIp());
    }

    @Test
    void functional_interface_extract_null() {
        OpLogContextExtractor extractor = () -> null;
        assertNull(extractor.extract());
    }
}
