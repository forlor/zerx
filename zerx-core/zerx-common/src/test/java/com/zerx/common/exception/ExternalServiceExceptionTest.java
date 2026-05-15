package com.zerx.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExternalServiceException Tests")
class ExternalServiceExceptionTest {

    // ======================== Constructor Tests ========================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTest {

        @Test
        @DisplayName("Constructor with ErrorCode should set error code, null service name")
        void constructorWithErrorCode() {
            ExternalServiceException ex = new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
            assertEquals("50001", ex.getCode());
            assertEquals(502, ex.getHttpStatus());
            assertEquals("外部服务调用失败", ex.getMessage());
            assertNull(ex.getServiceName());
        }

        @Test
        @DisplayName("Constructor with ErrorCode and message should set custom message, null service name")
        void constructorWithErrorCodeAndMessage() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_TIMEOUT, "短信服务调用超时");
            assertEquals(ErrorCode.EXTERNAL_SERVICE_TIMEOUT, ex.getErrorCode());
            assertEquals("短信服务调用超时", ex.getMessage());
            assertNull(ex.getServiceName());
        }

        @Test
        @DisplayName("Constructor with ErrorCode, message, and cause should set all, null service name")
        void constructorWithErrorCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("connection timeout");
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "支付网关扣款失败", cause);
            assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
            assertEquals("支付网关扣款失败", ex.getMessage());
            assertSame(cause, ex.getCause());
            assertNull(ex.getServiceName());
        }

        @Test
        @DisplayName("Constructor with ErrorCode, message, and serviceName should set service name")
        void constructorWithErrorCodeMessageAndServiceName() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_TIMEOUT, "短信服务调用超时", "SMS");
            assertEquals(ErrorCode.EXTERNAL_SERVICE_TIMEOUT, ex.getErrorCode());
            assertEquals("短信服务调用超时", ex.getMessage());
            assertEquals("SMS", ex.getServiceName());
            assertNull(ex.getCause());
        }

        @Test
        @DisplayName("Constructor with ErrorCode, message, cause, and serviceName should set all")
        void constructorWithAllParams() {
            RuntimeException cause = new RuntimeException("connection refused");
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "支付网关连接失败", cause, "PaymentGateway");
            assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR, ex.getErrorCode());
            assertEquals("支付网关连接失败", ex.getMessage());
            assertSame(cause, ex.getCause());
            assertEquals("PaymentGateway", ex.getServiceName());
        }
    }

    // ======================== getServiceName() Tests ========================

    @Nested
    @DisplayName("getServiceName() Tests")
    class GetServiceNameTest {

        @Test
        @DisplayName("getServiceName() should return null when not set")
        void serviceNameNullWhenNotSet() {
            ExternalServiceException ex = new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            assertNull(ex.getServiceName());
        }

        @Test
        @DisplayName("getServiceName() should return null with 2-arg constructor")
        void serviceNameNullWithTwoArgsConstructor() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "error");
            assertNull(ex.getServiceName());
        }

        @Test
        @DisplayName("getServiceName() should return null with 3-arg (cause) constructor")
        void serviceNameNullWithCauseConstructor() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "error", new RuntimeException());
            assertNull(ex.getServiceName());
        }

        @Test
        @DisplayName("getServiceName() should return service name when set")
        void serviceNameReturnsValue() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "error", "EmailService");
            assertEquals("EmailService", ex.getServiceName());
        }

        @Test
        @DisplayName("getServiceName() should return service name with cause constructor")
        void serviceNameWithCauseConstructor() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "error", new RuntimeException(), "PushService");
            assertEquals("PushService", ex.getServiceName());
        }

        @Test
        @DisplayName("Service name can be empty string")
        void serviceNameCanBeEmpty() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_ERROR, "error", "");
            assertEquals("", ex.getServiceName());
        }
    }

    // ======================== Inheritance Tests ========================

    @Nested
    @DisplayName("Inheritance Tests")
    class InheritanceTest {

        @Test
        @DisplayName("Should extend ZerxException")
        void extendsZerxException() {
            ExternalServiceException ex = new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            assertInstanceOf(ZerxException.class, ex);
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void isRuntimeException() {
            ExternalServiceException ex = new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Should inherit getErrorCode, getCode, getHttpStatus from ZerxException")
        void inheritsZerxExceptionMethods() {
            ExternalServiceException ex = new ExternalServiceException(
                    ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT, "限流", "SMS");
            assertEquals(ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT, ex.getErrorCode());
            assertEquals("50003", ex.getCode());
            assertEquals(429, ex.getHttpStatus());
        }

        @Test
        @DisplayName("Should be catchable as ZerxException")
        void catchableAsZerxException() {
            ZerxException caught = null;
            try {
                throw new ExternalServiceException(ErrorCode.EXTERNAL_SERVICE_ERROR, "fail", "Service");
            } catch (ZerxException e) {
                caught = e;
            }
            assertNotNull(caught);
            assertInstanceOf(ExternalServiceException.class, caught);
        }
    }

    // ======================== Different External Error Codes ========================

    @Nested
    @DisplayName("External ErrorCode Scenarios")
    class ExternalScenariosTest {

        @Test
        @DisplayName("Should work with all external service error codes")
        void allExternalErrorCodes() {
            ErrorCode[] codes = {
                    ErrorCode.EXTERNAL_SERVICE_ERROR, ErrorCode.EXTERNAL_SERVICE_TIMEOUT,
                    ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT, ErrorCode.EXTERNAL_SERVICE_DATA_ERROR
            };
            for (ErrorCode code : codes) {
                ExternalServiceException ex = new ExternalServiceException(code, "error", "Service");
                assertEquals(code, ex.getErrorCode());
            }
        }
    }
}
