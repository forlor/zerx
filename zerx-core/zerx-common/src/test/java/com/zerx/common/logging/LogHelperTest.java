package com.zerx.common.logging;

import com.zerx.common.exception.BusinessException;
import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogHelper 单元测试
 */
@DisplayName("LogHelper")
class LogHelperTest {

    @Nested
    @DisplayName("formatException - 完整格式化")
    class FormatExceptionTest {

        @Test
        @DisplayName("null 异常应返回 'null'")
        void nullThrowable() {
            assertEquals("null", LogHelper.formatException(null));
        }

        @Test
        @DisplayName("单层异常应包含错误码和描述")
        void singleException() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            String result = LogHelper.formatException(ex);
            assertTrue(result.contains("[20002:数据不存在]"), "应包含错误码前缀");
            assertTrue(result.contains("BusinessException"), "应包含异常类名");
        }

        @Test
        @DisplayName("多层异常应包含 cause chain")
        void causeChain() {
            RuntimeException root = new RuntimeException("数据库连接超时");
            BusinessException mid = new BusinessException(ErrorCode.DATABASE_ERROR, root);
            String result = LogHelper.formatException(mid);
            assertTrue(result.contains("[10006:数据库操作异常]"), "应包含错误码前缀");
            assertTrue(result.contains("Caused by:"), "应包含 Caused by");
            assertTrue(result.contains("数据库连接超时"), "应包含根因消息");
        }

        @Test
        @DisplayName("三层异常链应完整输出")
        void threeLayerChain() {
            SQLException sub = new SQLException("connection timeout");
            RuntimeException mid = new RuntimeException("wrapper error", sub);
            BusinessException top = new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, mid);
            String result = LogHelper.formatException(top);
            assertTrue(result.contains("Caused by:"), "应包含 cause chain 标记");
        }

        @Test
        @DisplayName("带业务上下文的格式化")
        void withContextVarargs() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            String result = LogHelper.formatException(ex,
                    "orderId", "202605140001",
                    "userId", "10086");
            assertTrue(result.contains("Context:"), "应包含上下文标签");
            assertTrue(result.contains("orderId=202605140001"), "应包含订单ID");
            assertTrue(result.contains("userId=10086"), "应包含用户ID");
        }

        @Test
        @DisplayName("带 Map 上下文的格式化")
        void withContextMap() {
            BusinessException ex = new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
            String result = LogHelper.formatException(ex, Map.of(
                    "accountId", "ACC001",
                    "amount", 100.50));
            assertTrue(result.contains("accountId=ACC001"), "应包含账户ID");
            assertTrue(result.contains("amount=100.5"), "应包含金额");
        }

        @Test
        @DisplayName("上下文值为 null 时应输出 'null'")
        void contextNullValue() {
            BusinessException ex = new BusinessException(ErrorCode.PARAM_REQUIRED);
            String result = LogHelper.formatException(ex, "field", null);
            assertTrue(result.contains("field=null"), "null 值应显示为 'null'");
        }

        @Test
        @DisplayName("上下文值为 Throwable 时应输出简洁描述")
        void contextThrowableValue() {
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR);
            RuntimeException ctxEx = new RuntimeException("ctx error");
            String result = LogHelper.formatException(ex, "cause", ctxEx);
            assertTrue(result.contains("RuntimeException: ctx error"), "异常值应输出简洁描述");
        }

        @Test
        @DisplayName("上下文参数为奇数应抛异常")
        void oddContextParams() {
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR);
            assertThrows(IllegalArgumentException.class,
                    () -> LogHelper.formatException(ex, "key1", "value1", "key2"));
        }

        @Test
        @DisplayName("上下文 key 为 null 应抛异常")
        void nullContextKey() {
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR);
            assertThrows(IllegalArgumentException.class,
                    () -> LogHelper.formatException(ex, null, "value"));
        }

        @Test
        @DisplayName("空上下文不应输出 Context 行")
        void emptyContext() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            String result = LogHelper.formatException(ex, Map.of());
            assertFalse(result.contains("Context:"), "空上下文不应输出");
        }

        @Test
        @DisplayName("IllegalArgumentException 应自动映射为参数校验错误码")
        void illegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("参数不能为空");
            String result = LogHelper.formatException(ex);
            assertTrue(result.contains("[30005:"), "应映射到 PARAM_INVALID 错误码");
        }

        @Test
        @DisplayName("普通 RuntimeException 应映射为系统错误码")
        void runtimeException() {
            RuntimeException ex = new RuntimeException("some error");
            String result = LogHelper.formatException(ex);
            assertTrue(result.contains("[10001:"), "应映射到 SYSTEM_ERROR 错误码");
        }

        @Test
        @DisplayName("ValidationException 应包含对应的错误码")
        void validationException() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_FORMAT_ERROR);
            String result = LogHelper.formatException(ex);
            assertTrue(result.contains("[30002:参数格式错误]"));
        }
    }

    @Nested
    @DisplayName("formatBrief - 简洁格式化")
    class FormatBriefTest {

        @Test
        @DisplayName("null 返回 'null'")
        void nullThrowable() {
            assertEquals("null", LogHelper.formatBrief(null));
        }

        @Test
        @DisplayName("单层异常仅输出类名和消息")
        void singleException() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            String result = LogHelper.formatBrief(ex);
            assertEquals("BusinessException: 数据不存在", result);
        }

        @Test
        @DisplayName("多层异常用分隔符连接")
        void causeChain() {
            RuntimeException root = new RuntimeException("DB timeout");
            BusinessException top = new BusinessException(ErrorCode.DATABASE_ERROR, root);
            String result = LogHelper.formatBrief(top);
            assertTrue(result.contains("BusinessException:"));
            assertTrue(result.contains("RuntimeException: DB timeout"));
            assertTrue(result.contains(" | "), "应用分隔符连接");
        }
    }

    @Nested
    @DisplayName("formatBriefWithCode - 带错误码的简洁格式")
    class FormatBriefWithCodeTest {

        @Test
        @DisplayName("null 返回 'null'")
        void nullThrowable() {
            assertEquals("null", LogHelper.formatBriefWithCode(null));
        }

        @Test
        @DisplayName("应包含错误码前缀和简洁描述")
        void withCode() {
            BusinessException ex = new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
            String result = LogHelper.formatBriefWithCode(ex);
            assertTrue(result.startsWith("[20007:"), "应以错误码前缀开始");
            assertTrue(result.contains("BusinessException:"));
        }
    }

    @Nested
    @DisplayName("rootCauseMessage - 根因提取")
    class RootCauseMessageTest {

        @Test
        @DisplayName("null 返回 'null'")
        void nullThrowable() {
            assertEquals("null", LogHelper.rootCauseMessage(null));
        }

        @Test
        @DisplayName("无 cause 的异常应返回自身描述")
        void noCause() {
            RuntimeException ex = new RuntimeException("direct error");
            assertEquals("RuntimeException: direct error", LogHelper.rootCauseMessage(ex));
        }

        @Test
        @DisplayName("应提取最深层的 cause")
        void deepCause() {
            RuntimeException root = new RuntimeException("root error");
            RuntimeException mid = new RuntimeException("mid error", root);
            RuntimeException top = new RuntimeException("top error", mid);
            assertEquals("RuntimeException: root error", LogHelper.rootCauseMessage(top));
        }
    }

    @Nested
    @DisplayName("chainDepth - 链深度")
    class ChainDepthTest {

        @Test
        @DisplayName("null 返回 0")
        void nullThrowable() {
            assertEquals(0, LogHelper.chainDepth(null));
        }

        @Test
        @DisplayName("单层异常深度为 1")
        void single() {
            assertEquals(1, LogHelper.chainDepth(new RuntimeException("e")));
        }

        @Test
        @DisplayName("三层异常深度为 3")
        void threeLayers() {
            RuntimeException r3 = new RuntimeException("l3");
            RuntimeException r2 = new RuntimeException("l2", r3);
            RuntimeException r1 = new RuntimeException("l1", r2);
            assertEquals(3, LogHelper.chainDepth(r1));
        }
    }

    /**
     * 模拟 SQLException（不在 zerx-common 中）
     */
    private static class SQLException extends Exception {
        SQLException(String message) {
            super(message);
        }
    }
}
