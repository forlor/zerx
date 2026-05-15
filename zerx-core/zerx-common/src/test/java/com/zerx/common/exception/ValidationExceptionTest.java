package com.zerx.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationException Tests")
class ValidationExceptionTest {

    // ======================== Constructor Tests ========================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Constructor with ErrorCode should set error code and null field")
        void constructorWithErrorCode() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED);
            assertEquals(ErrorCode.PARAM_REQUIRED, ex.getErrorCode());
            assertEquals("30001", ex.getCode());
            assertEquals(400, ex.getHttpStatus());
            assertEquals("必填参数不能为空", ex.getMessage());
            assertNull(ex.getField());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and message should set custom message and null field")
        void constructorWithErrorCodeAndMessage() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_FORMAT_ERROR, "手机号格式不正确");
            assertEquals(ErrorCode.PARAM_FORMAT_ERROR, ex.getErrorCode());
            assertEquals("手机号格式不正确", ex.getMessage());
            assertNull(ex.getField());
        }

        @Test
        @DisplayName("Constructor with ErrorCode, message, and field should set field")
        void constructorWithErrorCodeMessageAndField() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED, "不能为空", "username");
            assertEquals(ErrorCode.PARAM_REQUIRED, ex.getErrorCode());
            assertEquals("不能为空", ex.getMessage());
            assertEquals("username", ex.getField());
        }

        @Test
        @DisplayName("Field can be set to any string value")
        void fieldCanBeAnyString() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_TYPE_ERROR, "类型错误", "user.age");
            assertEquals("user.age", ex.getField());
        }

        @Test
        @DisplayName("Field can be empty string")
        void fieldCanBeEmpty() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED, "必填", "");
            assertEquals("", ex.getField());
        }
    }

    // ======================== getField() Tests ========================

    @Nested
    @DisplayName("getField() Tests")
    class GetFieldTest {

        @Test
        @DisplayName("getField() should return null when field not set")
        void fieldNullWhenNotSet() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED);
            assertNull(ex.getField());
        }

        @Test
        @DisplayName("getField() should return null when using 2-arg constructor")
        void fieldNullWithTwoArgConstructor() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_FORMAT_ERROR, "format error");
            assertNull(ex.getField());
        }

        @Test
        @DisplayName("getField() should return field name when using 3-arg constructor")
        void fieldReturnsName() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED, "required", "email");
            assertEquals("email", ex.getField());
        }
    }

    // ======================== Inheritance Tests ========================

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTest {

        @Test
        @DisplayName("Should extend ZerxException")
        void extendsZerxException() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED);
            assertInstanceOf(ZerxException.class, ex);
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void isRuntimeException() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED);
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Should inherit getErrorCode, getCode, getHttpStatus from ZerxException")
        void inheritsZerxExceptionMethods() {
            ValidationException ex = new ValidationException(ErrorCode.PARAM_OUT_OF_RANGE, "out of range", "age");
            assertEquals(ErrorCode.PARAM_OUT_OF_RANGE, ex.getErrorCode());
            assertEquals("30003", ex.getCode());
            assertEquals(400, ex.getHttpStatus());
        }

        @Test
        @DisplayName("Should be catchable as ZerxException")
        void catchableAsZerxException() {
            ZerxException caught = null;
            try {
                throw new ValidationException(ErrorCode.PARAM_REQUIRED, "test", "field");
            } catch (ZerxException e) {
                caught = e;
            }
            assertNotNull(caught);
            assertInstanceOf(ValidationException.class, caught);
        }
    }
}
