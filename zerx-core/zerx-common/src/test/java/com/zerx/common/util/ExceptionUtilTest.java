package com.zerx.common.util;

import com.zerx.common.exception.BusinessException;
import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ZerxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ExceptionUtil} 单元测试
 */
@DisplayName("ExceptionUtil 异常工具类测试")
class ExceptionUtilTest {

    // ======================== 异常信息提取 ========================

    @Nested
    @DisplayName("getStackTrace 测试")
    class GetStackTraceTests {

        @Test
        @DisplayName("null 返回空字符串")
        void getStackTrace_null() {
            assertEquals("", ExceptionUtil.getStackTrace(null));
        }

        @Test
        @DisplayName("正常异常返回非空堆栈字符串")
        void getStackTrace_normal() {
            Exception ex = new RuntimeException("test error");
            String stackTrace = ExceptionUtil.getStackTrace(ex);
            assertNotNull(stackTrace);
            assertFalse(stackTrace.isEmpty());
            assertTrue(stackTrace.contains("java.lang.RuntimeException"));
            assertTrue(stackTrace.contains("test error"));
        }

        @Test
        @DisplayName("包含异常类名和方法名")
        void getStackTrace_containsClassAndMethod() {
            Exception ex = new RuntimeException("boom");
            String stackTrace = ExceptionUtil.getStackTrace(ex);
            assertTrue(stackTrace.contains("ExceptionUtilTest"));
        }
    }

    @Nested
    @DisplayName("getSimpleMessage 测试")
    class GetSimpleMessageTests {

        @Test
        @DisplayName("null 返回 \"null\"")
        void getSimpleMessage_null() {
            assertEquals("null", ExceptionUtil.getSimpleMessage(null));
        }

        @Test
        @DisplayName("有消息时返回 \"类名: 消息\"")
        void getSimpleMessage_withMessage() {
            RuntimeException ex = new RuntimeException("出错了");
            String simple = ExceptionUtil.getSimpleMessage(ex);
            assertEquals("RuntimeException: 出错了", simple);
        }

        @Test
        @DisplayName("无消息时仅返回类名")
        void getSimpleMessage_noMessage() {
            RuntimeException ex = new RuntimeException((String) null);
            String simple = ExceptionUtil.getSimpleMessage(ex);
            assertEquals("RuntimeException", simple);
        }

        @Test
        @DisplayName("空消息时仅返回类名")
        void getSimpleMessage_emptyMessage() {
            RuntimeException ex = new RuntimeException("");
            String simple = ExceptionUtil.getSimpleMessage(ex);
            // "" is blank, so should return class name only
            assertEquals("RuntimeException", simple);
        }

        @Test
        @DisplayName("嵌套异常类名")
        void getSimpleMessage_nestedException() {
            IOException ex = new IOException("IO error");
            String simple = ExceptionUtil.getSimpleMessage(ex);
            assertEquals("IOException: IO error", simple);
        }
    }

    // ======================== 异常链 ========================

    @Nested
    @DisplayName("getRootCause 测试")
    class GetRootCauseTests {

        @Test
        @DisplayName("null 返回 null")
        void getRootCause_null() {
            assertNull(ExceptionUtil.getRootCause(null));
        }

        @Test
        @DisplayName("无 cause 的异常返回自身")
        void getRootCause_noCause() {
            RuntimeException ex = new RuntimeException("top");
            assertSame(ex, ExceptionUtil.getRootCause(ex));
        }

        @Test
        @DisplayName("有 cause 的异常返回根因")
        void getRootCause_withCause() {
            IOException root = new IOException("root cause");
            RuntimeException mid = new RuntimeException("mid", root);
            RuntimeException top = new RuntimeException("top", mid);
            assertSame(root, ExceptionUtil.getRootCause(top));
        }

        @Test
        @DisplayName("单层 cause")
        void getRootCause_singleCause() {
            IOException root = new IOException("root");
            RuntimeException top = new RuntimeException("top", root);
            assertSame(root, ExceptionUtil.getRootCause(top));
        }

        @Test
        @DisplayName("深层嵌套 cause")
        void getRootCause_deepChain() {
            SQLException root = new SQLException("db error");
            IOException mid2 = new IOException("io", root);
            RuntimeException mid1 = new RuntimeException("rt", mid2);
            RuntimeException top = new RuntimeException("top", mid1);
            assertSame(root, ExceptionUtil.getRootCause(top));
        }
    }

    @Nested
    @DisplayName("findCause 测试")
    class FindCauseTests {

        @Test
        @DisplayName("null 异常返回 null")
        void findCause_nullThrowable() {
            assertNull(ExceptionUtil.findCause(null, IOException.class));
        }

        @Test
        @DisplayName("null 类型返回 null")
        void findCause_nullType() {
            assertNull(ExceptionUtil.findCause(new RuntimeException("test"), null));
        }

        @Test
        @DisplayName("顶层匹配")
        void findCause_topLevel() {
            RuntimeException ex = new RuntimeException("test");
            assertSame(ex, ExceptionUtil.findCause(ex, RuntimeException.class));
        }

        @Test
        @DisplayName("深层匹配")
        void findCause_deepMatch() {
            IOException cause = new IOException("io");
            RuntimeException ex = new RuntimeException("rt", cause);
            assertSame(cause, ExceptionUtil.findCause(ex, IOException.class));
        }

        @Test
        @DisplayName("未找到返回 null")
        void findCause_notFound() {
            RuntimeException ex = new RuntimeException("rt");
            assertNull(ExceptionUtil.findCause(ex, IOException.class));
        }

        @Test
        @DisplayName("匹配接口类型")
        void findCause_interfaceMatch() {
            IOException cause = new IOException("io");
            RuntimeException ex = new RuntimeException("rt", cause);
            // findCause starts from the outer exception, RuntimeException is-a Exception
            assertSame(ex, ExceptionUtil.findCause(ex, Exception.class));
        }
    }

    @Nested
    @DisplayName("containsCause 测试")
    class ContainsCauseTests {

        @Test
        @DisplayName("包含返回 true")
        void containsCause_true() {
            IOException cause = new IOException("io");
            RuntimeException ex = new RuntimeException("rt", cause);
            assertTrue(ExceptionUtil.containsCause(ex, IOException.class));
        }

        @Test
        @DisplayName("不包含返回 false")
        void containsCause_false() {
            RuntimeException ex = new RuntimeException("rt");
            assertFalse(ExceptionUtil.containsCause(ex, IOException.class));
        }

        @Test
        @DisplayName("null 异常返回 false")
        void containsCause_null() {
            assertFalse(ExceptionUtil.containsCause(null, IOException.class));
        }
    }

    // ======================== Zerx 异常相关 ========================

    @Nested
    @DisplayName("isZerxException 测试")
    class IsZerxExceptionTests {

        @Test
        @DisplayName("ZerxException 子类返回 true")
        void isZerxException_true() {
            assertTrue(ExceptionUtil.isZerxException(new BusinessException(ErrorCode.BUSINESS_ERROR)));
        }

        @Test
        @DisplayName("非 ZerxException 返回 false")
        void isZerxException_false() {
            assertFalse(ExceptionUtil.isZerxException(new RuntimeException("test")));
        }

        @Test
        @DisplayName("null 返回 false")
        void isZerxException_null() {
            assertFalse(ExceptionUtil.isZerxException(null));
        }

        @Test
        @DisplayName("IllegalArgumentException 返回 false")
        void isZerxException_iae() {
            assertFalse(ExceptionUtil.isZerxException(new IllegalArgumentException("test")));
        }
    }

    @Nested
    @DisplayName("extractCode 测试")
    class ExtractCodeTests {

        @Test
        @DisplayName("ZerxException 返回其错误码")
        void extractCode_zerxException() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            assertEquals("20002", ExceptionUtil.extractCode(ex));
        }

        @Test
        @DisplayName("IllegalArgumentException 返回参数校验错误码")
        void extractCode_illegalArgumentException() {
            IllegalArgumentException ex = new IllegalArgumentException("bad param");
            assertEquals("30005", ExceptionUtil.extractCode(ex));
        }

        @Test
        @DisplayName("其他异常返回系统错误码")
        void extractCode_otherException() {
            RuntimeException ex = new RuntimeException("error");
            assertEquals("10001", ExceptionUtil.extractCode(ex));
        }
    }

    @Nested
    @DisplayName("extractHttpStatus 测试")
    class ExtractHttpStatusTests {

        @Test
        @DisplayName("ZerxException 返回其 HTTP 状态码")
        void extractHttpStatus_zerxException() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            assertEquals(404, ExceptionUtil.extractHttpStatus(ex));
        }

        @Test
        @DisplayName("IllegalArgumentException 返回 400")
        void extractHttpStatus_illegalArgumentException() {
            IllegalArgumentException ex = new IllegalArgumentException("bad param");
            assertEquals(400, ExceptionUtil.extractHttpStatus(ex));
        }

        @Test
        @DisplayName("其他异常返回 500")
        void extractHttpStatus_otherException() {
            RuntimeException ex = new RuntimeException("error");
            assertEquals(500, ExceptionUtil.extractHttpStatus(ex));
        }
    }

    @Nested
    @DisplayName("extractMessage 测试")
    class ExtractMessageTests {

        @Test
        @DisplayName("null 返回 \"未知错误\"")
        void extractMessage_null() {
            assertEquals("未知错误", ExceptionUtil.extractMessage(null));
        }

        @Test
        @DisplayName("有消息返回消息")
        void extractMessage_withMessage() {
            RuntimeException ex = new RuntimeException("具体错误");
            assertEquals("具体错误", ExceptionUtil.extractMessage(ex));
        }

        @Test
        @DisplayName("无消息返回类名")
        void extractMessage_noMessage() {
            RuntimeException ex = new RuntimeException((String) null);
            assertEquals("RuntimeException", ExceptionUtil.extractMessage(ex));
        }

        @Test
        @DisplayName("空消息返回类名")
        void extractMessage_emptyMessage() {
            RuntimeException ex = new RuntimeException("");
            assertEquals("RuntimeException", ExceptionUtil.extractMessage(ex));
        }

        @Test
        @DisplayName("ZerxException 返回消息")
        void extractMessage_zerxException() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            assertEquals("数据不存在", ExceptionUtil.extractMessage(ex));
        }
    }

    // ======================== 异常链操作 ========================

    @Nested
    @DisplayName("getExceptionChain 测试")
    class GetExceptionChainTests {

        @Test
        @DisplayName("null 返回空列表")
        void getExceptionChain_null() {
            assertEquals(List.of(), ExceptionUtil.getExceptionChain(null));
        }

        @Test
        @DisplayName("无 cause 的异常返回单元素列表")
        void getExceptionChain_noCause() {
            RuntimeException ex = new RuntimeException("top");
            List<Throwable> chain = ExceptionUtil.getExceptionChain(ex);
            assertEquals(1, chain.size());
            assertSame(ex, chain.getFirst());
        }

        @Test
        @DisplayName("有 cause 的异常返回完整链")
        void getExceptionChain_withCause() {
            IOException root = new IOException("root");
            RuntimeException mid = new RuntimeException("mid", root);
            RuntimeException top = new RuntimeException("top", mid);

            List<Throwable> chain = ExceptionUtil.getExceptionChain(top);
            assertEquals(3, chain.size());
            assertSame(top, chain.get(0));
            assertSame(mid, chain.get(1));
            assertSame(root, chain.get(2));
        }

        @Test
        @DisplayName("返回不可变列表")
        void getExceptionChain_immutable() {
            RuntimeException ex = new RuntimeException("test");
            List<Throwable> chain = ExceptionUtil.getExceptionChain(ex);
            assertThrows(UnsupportedOperationException.class, () -> chain.add(new RuntimeException()));
        }
    }

    @Nested
    @DisplayName("getExceptionChainDepth 测试")
    class GetExceptionChainDepthTests {

        @Test
        @DisplayName("null 返回 0")
        void getExceptionChainDepth_null() {
            assertEquals(0, ExceptionUtil.getExceptionChainDepth(null));
        }

        @Test
        @DisplayName("无 cause 返回 1")
        void getExceptionChainDepth_noCause() {
            assertEquals(1, ExceptionUtil.getExceptionChainDepth(new RuntimeException("top")));
        }

        @Test
        @DisplayName("多层 cause 返回正确深度")
        void getExceptionChainDepth_withCause() {
            IOException root = new IOException("root");
            RuntimeException mid = new RuntimeException("mid", root);
            RuntimeException top = new RuntimeException("top", mid);
            assertEquals(3, ExceptionUtil.getExceptionChainDepth(top));
        }
    }

    @Nested
    @DisplayName("joinExceptionChainMessages 测试")
    class JoinExceptionChainMessagesTests {

        @Test
        @DisplayName("null 异常返回空字符串")
        void joinExceptionChainMessages_null() {
            // null throwable produces empty chain, joined with separator = ""
            assertEquals("", ExceptionUtil.joinExceptionChainMessages(null, " | "));
        }

        @Test
        @DisplayName("单层异常返回单条消息")
        void joinExceptionChainMessages_single() {
            RuntimeException ex = new RuntimeException("error1");
            String result = ExceptionUtil.joinExceptionChainMessages(ex, " -> ");
            assertEquals("error1", result);
        }

        @Test
        @DisplayName("多层异常用分隔符连接")
        void joinExceptionChainMessages_multi() {
            IOException root = new IOException("db error");
            RuntimeException mid = new RuntimeException("runtime error", root);
            RuntimeException top = new RuntimeException("top error", mid);

            String result = ExceptionUtil.joinExceptionChainMessages(top, " | ");
            assertEquals("top error | runtime error | db error", result);
        }

        @Test
        @DisplayName("自定义分隔符")
        void joinExceptionChainMessages_customSeparator() {
            IOException root = new IOException("root");
            RuntimeException top = new RuntimeException("top", root);

            String result = ExceptionUtil.joinExceptionChainMessages(top, "\n");
            assertTrue(result.contains("\n"));
            assertTrue(result.startsWith("top"));
            assertTrue(result.endsWith("root"));
        }

        @Test
        @DisplayName("无消息的异常层使用类名")
        void joinExceptionChainMessages_noMessageInChain() {
            RuntimeException mid = new RuntimeException((String) null);
            RuntimeException top = new RuntimeException("top", mid);

            String result = ExceptionUtil.joinExceptionChainMessages(top, " -> ");
            assertEquals("top -> RuntimeException", result);
        }
    }
}
