package com.zerx.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ZerxException Tests")
class ZerxExceptionTest {

    /**
     * Using BusinessException as the concrete subclass for testing
     * since ZerxException is abstract.
     */

    // ======================== getErrorCode() Tests ========================

    @Nested
    @DisplayName("getErrorCode() Tests")
    class GetErrorCodeTest {

        @Test
        @DisplayName("Should return the ErrorCode passed in constructor")
        void returnsErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertEquals(ErrorCode.BUSINESS_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should return different ErrorCode for different exceptions")
        void returnsDifferentErrorCode() {
            BusinessException ex1 = new BusinessException(ErrorCode.BUSINESS_ERROR);
            BusinessException ex2 = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            assertNotEquals(ex1.getErrorCode(), ex2.getErrorCode());
        }
    }

    // ======================== getCode() Tests ========================

    @Nested
    @DisplayName("getCode() Tests")
    class GetCodeTest {

        @Test
        @DisplayName("Should return the error code string from ErrorCode")
        void returnsCodeString() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertEquals("20001", ex.getCode());
        }

        @Test
        @DisplayName("Should return correct code for different ErrorCode")
        void returnsCorrectCodeForDifferentErrorCode() {
            BusinessException ex = new BusinessException(ErrorCode.PARAM_REQUIRED);
            assertEquals("30001", ex.getCode());
        }

        @Test
        @DisplayName("getCode() should match getErrorCode().code()")
        void codeMatchesErrorCodeCode() {
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR);
            assertEquals(ex.getErrorCode().code(), ex.getCode());
        }
    }

    // ======================== getHttpStatus() Tests ========================

    @Nested
    @DisplayName("getHttpStatus() Tests")
    class GetHttpStatusTest {

        @Test
        @DisplayName("Should return the HTTP status from ErrorCode")
        void returnsHttpStatus() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("Should return correct HTTP status for different ErrorCode")
        void returnsCorrectHttpStatus() {
            BusinessException ex = new BusinessException(ErrorCode.DATA_NOT_FOUND);
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("getHttpStatus() should match getErrorCode().httpStatus()")
        void httpStatusMatchesErrorCodeHttpStatus() {
            BusinessException ex = new BusinessException(ErrorCode.UNAUTHORIZED);
            assertEquals(ex.getErrorCode().httpStatus(), ex.getHttpStatus());
        }
    }

    // ======================== Cause Chain Tests ========================

    @Nested
    @DisplayName("Cause Chain Tests")
    class CauseChainTest {

        @Test
        @DisplayName("Exception with cause should have cause set")
        void exceptionWithCause() {
            NullPointerException npe = new NullPointerException("original NPE");
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR, npe);
            assertSame(npe, ex.getCause());
            assertEquals("original NPE", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("Exception without cause should have null cause")
        void exceptionWithoutCause() {
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR);
            assertNull(ex.getCause());
        }

        @Test
        @DisplayName("Cause chain should be preserved through wrapping")
        void causeChainPreserved() {
            IOException ioEx = new IOException("IO failure");
            RuntimeException rtEx = new RuntimeException("Runtime wrapper", ioEx);
            BusinessException bizEx = new BusinessException(ErrorCode.EXTERNAL_SERVICE_ERROR, "外部调用失败", rtEx);
            assertEquals("Runtime wrapper", bizEx.getCause().getMessage());
            assertEquals("IO failure", bizEx.getCause().getCause().getMessage());
        }

        @Test
        @DisplayName("Exception with cause should have ErrorCode message as default message")
        void exceptionWithCauseDefaultMessage() {
            NullPointerException npe = new NullPointerException("NPE");
            BusinessException ex = new BusinessException(ErrorCode.SYSTEM_ERROR, npe);
            assertEquals("系统内部错误", ex.getMessage());
        }
    }

    // ======================== Custom Message Tests ========================

    @Nested
    @DisplayName("Custom Message Tests")
    class CustomMessageTest {

        @Test
        @DisplayName("Exception with custom message should use custom message")
        void customMessage() {
            BusinessException ex = new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH, "余额不足，当前: 0");
            assertEquals("余额不足，当前: 0", ex.getMessage());
        }

        @Test
        @DisplayName("Exception without custom message should use ErrorCode default message")
        void defaultMessage() {
            BusinessException ex = new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
            assertEquals("余额不足", ex.getMessage());
        }

        @Test
        @DisplayName("Exception with custom message and cause should use custom message")
        void customMessageWithCause() {
            RuntimeException cause = new RuntimeException("cause");
            BusinessException ex = new BusinessException(ErrorCode.OPERATION_FAILED, "操作失败: 具体原因", cause);
            assertEquals("操作失败: 具体原因", ex.getMessage());
            assertSame(cause, ex.getCause());
        }
    }

    // ======================== Inheritance Tests ========================

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTest {

        @Test
        @DisplayName("Should be a RuntimeException")
        void isRuntimeException() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Should be a ZerxException")
        void isZerxException() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertInstanceOf(ZerxException.class, ex);
        }

        @Test
        @DisplayName("Should be a Throwable")
        void isThrowable() {
            BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
            assertInstanceOf(Throwable.class, ex);
        }

        @Test
        @DisplayName("Different subclasses should share same ErrorCode methods")
        void subclassesShareErrorCodeMethods() {
            ZerxException biz = new BusinessException(ErrorCode.SYSTEM_ERROR);
            ZerxException notFound = new NotFoundException(ErrorCode.DATA_NOT_FOUND);
            ZerxException validation = new ValidationException(ErrorCode.PARAM_REQUIRED);

            assertEquals(ErrorCode.SYSTEM_ERROR, biz.getErrorCode());
            assertEquals(ErrorCode.DATA_NOT_FOUND, notFound.getErrorCode());
            assertEquals(ErrorCode.PARAM_REQUIRED, validation.getErrorCode());
        }
    }
}
