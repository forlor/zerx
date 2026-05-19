package com.zerx.spring.web.autoconfigure;

import com.zerx.common.logging.OpLogContextExtractor;
import com.zerx.spring.web.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link RequestContextOpLogContextExtractor} 单元测试
 *
 * @author zerx
 */
class RequestContextOpLogContextExtractorTest {

    private final RequestContextOpLogContextExtractor extractor = new RequestContextOpLogContextExtractor();

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void extract_returns_context_when_initialized() {
        RequestContext ctx = RequestContext.init();
        ctx.setUserId(100L);
        ctx.setUsername("admin");
        ctx.setRequestIp("10.0.0.1");

        OpLogContextExtractor.OpLogContext result = extractor.extract();

        assertNotNull(result);
        assertEquals(100L, result.userId());
        assertEquals("admin", result.username());
        assertEquals("10.0.0.1", result.clientIp());
    }

    @Test
    void extract_returns_null_when_not_initialized() {
        assertNull(extractor.extract());
    }

    @Test
    void extract_returns_partial_context() {
        RequestContext ctx = RequestContext.init();
        ctx.setUserId(200L);
        // username and requestIp not set

        OpLogContextExtractor.OpLogContext result = extractor.extract();

        assertNotNull(result);
        assertEquals(200L, result.userId());
        assertNull(result.username());
        assertNull(result.clientIp());
    }
}
