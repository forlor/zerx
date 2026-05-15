package com.zerx.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("NotFoundException Tests")
class NotFoundExceptionTest {

    // ======================== Constructor Tests ========================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Constructor with ErrorCode should set error code and default message")
        void constructorWithErrorCode() {
            NotFoundException ex = new NotFoundException(ErrorCode.DATA_NOT_FOUND);
            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
            assertEquals("20002", ex.getCode());
            assertEquals(404, ex.getHttpStatus());
            assertEquals("数据不存在", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and custom message should override default")
        void constructorWithErrorCodeAndMessage() {
            NotFoundException ex = new NotFoundException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with message only should use DATA_NOT_FOUND error code")
        void constructorWithMessageOnly() {
            NotFoundException ex = new NotFoundException("订单不存在: ORD-12345");
            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
            assertEquals("20002", ex.getCode());
            assertEquals(404, ex.getHttpStatus());
            assertEquals("订单不存在: ORD-12345", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with message only should use DATA_NOT_FOUND error code even for other codes")
        void messageOnlyUsesDataNotFound() {
            NotFoundException ex = new NotFoundException("资源已删除");
            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
        }
    }

    // ======================== Inheritance Tests ========================

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTest {

        @Test
        @DisplayName("Should extend ZerxException")
        void extendsZerxException() {
            NotFoundException ex = new NotFoundException(ErrorCode.DATA_NOT_FOUND);
            assertInstanceOf(ZerxException.class, ex);
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void isRuntimeException() {
            NotFoundException ex = new NotFoundException("not found");
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Should inherit getErrorCode, getCode, getHttpStatus from ZerxException")
        void inheritsZerxExceptionMethods() {
            NotFoundException ex = new NotFoundException(ErrorCode.DATA_NOT_FOUND, "not found");
            assertEquals(ErrorCode.DATA_NOT_FOUND, ex.getErrorCode());
            assertEquals("20002", ex.getCode());
            assertEquals(404, ex.getHttpStatus());
        }

        @Test
        @DisplayName("Should be catchable as ZerxException")
        void catchableAsZerxException() {
            ZerxException caught = null;
            try {
                throw new NotFoundException("resource not found");
            } catch (ZerxException e) {
                caught = e;
            }
            assertNotNull(caught);
            assertInstanceOf(NotFoundException.class, caught);
        }
    }
}
