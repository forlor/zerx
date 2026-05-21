package com.zerx.component.oss;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link OssException} 单元测试
 *
 * @author zerx
 */
class OssExceptionTest {

    @Test
    @DisplayName("ossError(detail) 创建异常")
    void shouldCreateOssError() {
        OssException ex = OssException.ossError("上传失败: test");
        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("上传失败: test"));
    }

    @Test
    @DisplayName("ossError(detail, cause) 创建异常并保留原因")
    void shouldCreateOssErrorWithCause() {
        Throwable cause = new RuntimeException("io error");
        OssException ex = OssException.ossError("下载失败", cause);
        assertNotNull(ex);
        assertSame(cause, ex.getCause());
        assertTrue(ex.getMessage().contains("下载失败"));
    }

    @Test
    @DisplayName("(String message) 构造器")
    void shouldConstructWithMessage() {
        OssException ex = new OssException("自定义错误消息");
        assertEquals("自定义错误消息", ex.getMessage());
    }

    @Test
    @DisplayName("继承 RuntimeException")
    void shouldExtendRuntimeException() {
        OssException ex = new OssException("test");
        assertTrue(ex instanceof RuntimeException);
    }
}
