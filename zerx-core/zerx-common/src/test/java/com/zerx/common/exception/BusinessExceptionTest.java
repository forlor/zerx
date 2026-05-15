package com.zerx.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BusinessException Tests")
class BusinessExceptionTest {

    // ======================== Constructor Tests ========================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Constructor with ErrorCode should set error code and default message")
        void constructorWithErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertEquals(ErrorCode.BUSINESS_ERROR, ex.getErrorCode());
            assertEquals("20001", ex.getCode());
            assertEquals(400, ex.getHttpStatus());
            assertEquals("业务处理失败", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and cause should set cause")
        void constructorWithErrorCodeAndCause() {
            RuntimeException cause = new RuntimeException("root cause");
            BusinessException ex = new BusinessException(ErrorCode.OPERATION_FAILED, cause);
            assertEquals(ErrorCode.OPERATION_FAILED, ex.getErrorCode());
            assertSame(cause, ex.getCause());
            assertEquals("操作失败", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and custom message should override default message")
        void constructorWithErrorCodeAndMessage() {
            BusinessException ex = new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH, "余额不足，当前: 50");
            assertEquals(ErrorCode.BALANCE_NOT_ENOUGH, ex.getErrorCode());
            assertEquals("余额不足，当前: 50", ex.getMessage());
            assertNull(ex.getCause());
        }

        @Test
        @DisplayName("Constructor with ErrorCode, message, and cause should set all")
        void constructorWithErrorCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("payment failed");
            BusinessException ex = new BusinessException(ErrorCode.OPERATION_FAILED, "支付处理失败", cause);
            assertEquals(ErrorCode.OPERATION_FAILED, ex.getErrorCode());
            assertEquals("支付处理失败", ex.getMessage());
            assertSame(cause, ex.getCause());
        }
    }

    // ======================== Inheritance Tests ========================

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTest {

        @Test
        @DisplayName("Should extend ZerxException")
        void extendsZerxException() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertInstanceOf(ZerxException.class, ex);
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void isRuntimeException() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Should inherit getErrorCode, getCode, getHttpStatus from ZerxException")
        void inheritsMethods() {
            BusinessException ex = new BusinessException(ErrorCode.STATE_CONFLICT);
            assertEquals("20005", ex.getCode());
            assertEquals(409, ex.getHttpStatus());
            assertEquals(ErrorCode.STATE_CONFLICT, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should be catchable as ZerxException")
        void catchableAsZerxException() {
            ZerxException caught = null;
            try {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR);
            } catch (ZerxException e) {
                caught = e;
            }
            assertNotNull(caught);
            assertInstanceOf(BusinessException.class, caught);
        }
    }

    // ======================== Different ErrorCode Tests ========================

    @Nested
    @DisplayName("Different ErrorCode Scenarios")
    class ErrorCodeScenariosTest {

        @Test
        @DisplayName("Should work with all business error codes")
        void allBusinessErrorCodes() {
            ErrorCode[] codes = {
                    ErrorCode.BUSINESS_ERROR, ErrorCode.DATA_NOT_FOUND, ErrorCode.DATA_ALREADY_EXISTS,
                    ErrorCode.OPERATION_FAILED, ErrorCode.STATE_CONFLICT, ErrorCode.VERSION_CONFLICT,
                    ErrorCode.BALANCE_NOT_ENOUGH, ErrorCode.STOCK_NOT_ENOUGH
            };
            for (ErrorCode code : codes) {
                BusinessException ex = new BusinessException(code);
                assertEquals(code, ex.getErrorCode());
            }
        }

        @Test
        @DisplayName("Should work with non-business error codes too")
        void nonBusinessErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR);
            assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        }
    }
}
