package com.zerx.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthorizationException Tests")
class AuthorizationExceptionTest {

    // ======================== Constructor Tests ========================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Constructor with ErrorCode should set error code and default message")
        void constructorWithErrorCode() {
            AuthorizationException ex = new AuthorizationException(ErrorCode.UNAUTHORIZED);
            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
            assertEquals("40001", ex.getCode());
            assertEquals(401, ex.getHttpStatus());
            assertEquals("未登录或认证已过期", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and cause should set cause")
        void constructorWithErrorCodeAndCause() {
            RuntimeException cause = new RuntimeException("token parse error");
            AuthorizationException ex = new AuthorizationException(ErrorCode.TOKEN_INVALID, cause);
            assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
            assertSame(cause, ex.getCause());
            assertEquals("认证令牌无效", ex.getMessage());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and custom message should override default message")
        void constructorWithErrorCodeAndMessage() {
            AuthorizationException ex = new AuthorizationException(ErrorCode.FORBIDDEN, "需要管理员权限");
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            assertEquals("需要管理员权限", ex.getMessage());
            assertNull(ex.getCause());
        }
    }

    // ======================== Inheritance Tests ========================

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTest {

        @Test
        @DisplayName("Should extend ZerxException")
        void extendsZerxException() {
            AuthorizationException ex = new AuthorizationException(ErrorCode.UNAUTHORIZED);
            assertInstanceOf(ZerxException.class, ex);
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void isRuntimeException() {
            AuthorizationException ex = new AuthorizationException(ErrorCode.FORBIDDEN);
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Should inherit getErrorCode, getCode, getHttpStatus from ZerxException")
        void inheritsZerxExceptionMethods() {
            AuthorizationException ex = new AuthorizationException(ErrorCode.TOKEN_EXPIRED);
            assertEquals(ErrorCode.TOKEN_EXPIRED, ex.getErrorCode());
            assertEquals("40003", ex.getCode());
            assertEquals(401, ex.getHttpStatus());
        }

        @Test
        @DisplayName("Should be catchable as ZerxException")
        void catchableAsZerxException() {
            ZerxException caught = null;
            try {
                throw new AuthorizationException(ErrorCode.ACCOUNT_DISABLED, "账号已禁用");
            } catch (ZerxException e) {
                caught = e;
            }
            assertNotNull(caught);
            assertInstanceOf(AuthorizationException.class, caught);
        }
    }

    // ======================== Different Auth ErrorCode Scenarios ========================

    @Nested
    @DisplayName("Auth ErrorCode Scenarios")
    class AuthScenariosTest {

        @Test
        @DisplayName("Should work with all auth error codes")
        void allAuthErrorCodes() {
            ErrorCode[] codes = {
                    ErrorCode.UNAUTHORIZED, ErrorCode.FORBIDDEN, ErrorCode.TOKEN_EXPIRED,
                    ErrorCode.TOKEN_INVALID, ErrorCode.ACCOUNT_DISABLED, ErrorCode.LOGIN_ATTEMPT_EXCEEDED
            };
            for (ErrorCode code : codes) {
                AuthorizationException ex = new AuthorizationException(code);
                assertEquals(code, ex.getErrorCode());
            }
        }
    }
}
