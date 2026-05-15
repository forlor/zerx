package com.zerx.common.exception;

import com.zerx.common.enums.BaseEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCode Tests")
class ErrorCodeTest {

    // ======================== Predefined Constants Tests ========================

    @Nested
    @DisplayName("Predefined Constants - Success (0xxxx)")
    class SuccessConstantsTest {

        @Test
        @DisplayName("SUCCESS should have code 00000, message '操作成功', httpStatus 200")
        void success() {
            assertEquals("00000", ErrorCode.SUCCESS.code());
            assertEquals("操作成功", ErrorCode.SUCCESS.message());
            assertEquals(200, ErrorCode.SUCCESS.httpStatus());
        }
    }

    @Nested
    @DisplayName("Predefined Constants - System Errors (1xxxx)")
    class SystemErrorConstantsTest {

        @Test
        @DisplayName("SYSTEM_ERROR should have code 10001, httpStatus 500")
        void systemError() {
            assertEquals("10001", ErrorCode.SYSTEM_ERROR.code());
            assertEquals("系统内部错误", ErrorCode.SYSTEM_ERROR.message());
            assertEquals(500, ErrorCode.SYSTEM_ERROR.httpStatus());
        }

        @Test
        @DisplayName("NETWORK_ERROR should have code 10002, httpStatus 503")
        void networkError() {
            assertEquals("10002", ErrorCode.NETWORK_ERROR.code());
            assertEquals(503, ErrorCode.NETWORK_ERROR.httpStatus());
        }

        @Test
        @DisplayName("SERVICE_UNAVAILABLE should have code 10003, httpStatus 503")
        void serviceUnavailable() {
            assertEquals("10003", ErrorCode.SERVICE_UNAVAILABLE.code());
            assertEquals(503, ErrorCode.SERVICE_UNAVAILABLE.httpStatus());
        }

        @Test
        @DisplayName("TOO_MANY_REQUESTS should have code 10004, httpStatus 429")
        void tooManyRequests() {
            assertEquals("10004", ErrorCode.TOO_MANY_REQUESTS.code());
            assertEquals(429, ErrorCode.TOO_MANY_REQUESTS.httpStatus());
        }

        @Test
        @DisplayName("SERIALIZATION_ERROR should have code 10005, httpStatus 500")
        void serializationError() {
            assertEquals("10005", ErrorCode.SERIALIZATION_ERROR.code());
            assertEquals(500, ErrorCode.SERIALIZATION_ERROR.httpStatus());
        }

        @Test
        @DisplayName("DATABASE_ERROR should have code 10006, httpStatus 500")
        void databaseError() {
            assertEquals("10006", ErrorCode.DATABASE_ERROR.code());
            assertEquals(500, ErrorCode.DATABASE_ERROR.httpStatus());
        }
    }

    @Nested
    @DisplayName("Predefined Constants - Business Errors (2xxxx)")
    class BusinessErrorConstantsTest {

        @Test
        @DisplayName("BUSINESS_ERROR should have code 20001, httpStatus 400")
        void businessError() {
            assertEquals("20001", ErrorCode.BUSINESS_ERROR.code());
            assertEquals(400, ErrorCode.BUSINESS_ERROR.httpStatus());
        }

        @Test
        @DisplayName("DATA_NOT_FOUND should have code 20002, httpStatus 404")
        void dataNotFound() {
            assertEquals("20002", ErrorCode.DATA_NOT_FOUND.code());
            assertEquals(404, ErrorCode.DATA_NOT_FOUND.httpStatus());
        }

        @Test
        @DisplayName("DATA_ALREADY_EXISTS should have code 20003, httpStatus 409")
        void dataAlreadyExists() {
            assertEquals("20003", ErrorCode.DATA_ALREADY_EXISTS.code());
            assertEquals(409, ErrorCode.DATA_ALREADY_EXISTS.httpStatus());
        }

        @Test
        @DisplayName("OPERATION_FAILED should have code 20004, httpStatus 400")
        void operationFailed() {
            assertEquals("20004", ErrorCode.OPERATION_FAILED.code());
            assertEquals(400, ErrorCode.OPERATION_FAILED.httpStatus());
        }

        @Test
        @DisplayName("STATE_CONFLICT should have code 20005, httpStatus 409")
        void stateConflict() {
            assertEquals("20005", ErrorCode.STATE_CONFLICT.code());
            assertEquals(409, ErrorCode.STATE_CONFLICT.httpStatus());
        }

        @Test
        @DisplayName("VERSION_CONFLICT should have code 20006, httpStatus 409")
        void versionConflict() {
            assertEquals("20006", ErrorCode.VERSION_CONFLICT.code());
            assertEquals(409, ErrorCode.VERSION_CONFLICT.httpStatus());
        }

        @Test
        @DisplayName("BALANCE_NOT_ENOUGH should have code 20007, httpStatus 400")
        void balanceNotEnough() {
            assertEquals("20007", ErrorCode.BALANCE_NOT_ENOUGH.code());
            assertEquals(400, ErrorCode.BALANCE_NOT_ENOUGH.httpStatus());
        }

        @Test
        @DisplayName("STOCK_NOT_ENOUGH should have code 20008, httpStatus 400")
        void stockNotEnough() {
            assertEquals("20008", ErrorCode.STOCK_NOT_ENOUGH.code());
            assertEquals(400, ErrorCode.STOCK_NOT_ENOUGH.httpStatus());
        }
    }

    @Nested
    @DisplayName("Predefined Constants - Param Errors (3xxxx)")
    class ParamErrorConstantsTest {

        @Test
        @DisplayName("PARAM_REQUIRED should have code 30001, httpStatus 400")
        void paramRequired() {
            assertEquals("30001", ErrorCode.PARAM_REQUIRED.code());
            assertEquals(400, ErrorCode.PARAM_REQUIRED.httpStatus());
        }

        @Test
        @DisplayName("PARAM_FORMAT_ERROR should have code 30002, httpStatus 400")
        void paramFormatError() {
            assertEquals("30002", ErrorCode.PARAM_FORMAT_ERROR.code());
            assertEquals(400, ErrorCode.PARAM_FORMAT_ERROR.httpStatus());
        }

        @Test
        @DisplayName("PARAM_OUT_OF_RANGE should have code 30003, httpStatus 400")
        void paramOutOfRange() {
            assertEquals("30003", ErrorCode.PARAM_OUT_OF_RANGE.code());
            assertEquals(400, ErrorCode.PARAM_OUT_OF_RANGE.httpStatus());
        }

        @Test
        @DisplayName("PARAM_TYPE_ERROR should have code 30004, httpStatus 400")
        void paramTypeError() {
            assertEquals("30004", ErrorCode.PARAM_TYPE_ERROR.code());
            assertEquals(400, ErrorCode.PARAM_TYPE_ERROR.httpStatus());
        }

        @Test
        @DisplayName("PARAM_INVALID should have code 30005, httpStatus 400")
        void paramInvalid() {
            assertEquals("30005", ErrorCode.PARAM_INVALID.code());
            assertEquals(400, ErrorCode.PARAM_INVALID.httpStatus());
        }

        @Test
        @DisplayName("BODY_REQUIRED should have code 30006, httpStatus 400")
        void bodyRequired() {
            assertEquals("30006", ErrorCode.BODY_REQUIRED.code());
            assertEquals(400, ErrorCode.BODY_REQUIRED.httpStatus());
        }
    }

    @Nested
    @DisplayName("Predefined Constants - Auth Errors (4xxxx)")
    class AuthErrorConstantsTest {

        @Test
        @DisplayName("UNAUTHORIZED should have code 40001, httpStatus 401")
        void unauthorized() {
            assertEquals("40001", ErrorCode.UNAUTHORIZED.code());
            assertEquals(401, ErrorCode.UNAUTHORIZED.httpStatus());
        }

        @Test
        @DisplayName("FORBIDDEN should have code 40002, httpStatus 403")
        void forbidden() {
            assertEquals("40002", ErrorCode.FORBIDDEN.code());
            assertEquals(403, ErrorCode.FORBIDDEN.httpStatus());
        }

        @Test
        @DisplayName("TOKEN_EXPIRED should have code 40003, httpStatus 401")
        void tokenExpired() {
            assertEquals("40003", ErrorCode.TOKEN_EXPIRED.code());
            assertEquals(401, ErrorCode.TOKEN_EXPIRED.httpStatus());
        }

        @Test
        @DisplayName("TOKEN_INVALID should have code 40004, httpStatus 401")
        void tokenInvalid() {
            assertEquals("40004", ErrorCode.TOKEN_INVALID.code());
            assertEquals(401, ErrorCode.TOKEN_INVALID.httpStatus());
        }

        @Test
        @DisplayName("ACCOUNT_DISABLED should have code 40005, httpStatus 403")
        void accountDisabled() {
            assertEquals("40005", ErrorCode.ACCOUNT_DISABLED.code());
            assertEquals(403, ErrorCode.ACCOUNT_DISABLED.httpStatus());
        }

        @Test
        @DisplayName("LOGIN_ATTEMPT_EXCEEDED should have code 40006, httpStatus 429")
        void loginAttemptExceeded() {
            assertEquals("40006", ErrorCode.LOGIN_ATTEMPT_EXCEEDED.code());
            assertEquals(429, ErrorCode.LOGIN_ATTEMPT_EXCEEDED.httpStatus());
        }
    }

    @Nested
    @DisplayName("Predefined Constants - External Errors (5xxxx)")
    class ExternalErrorConstantsTest {

        @Test
        @DisplayName("EXTERNAL_SERVICE_ERROR should have code 50001, httpStatus 502")
        void externalServiceError() {
            assertEquals("50001", ErrorCode.EXTERNAL_SERVICE_ERROR.code());
            assertEquals(502, ErrorCode.EXTERNAL_SERVICE_ERROR.httpStatus());
        }

        @Test
        @DisplayName("EXTERNAL_SERVICE_TIMEOUT should have code 50002, httpStatus 504")
        void externalServiceTimeout() {
            assertEquals("50002", ErrorCode.EXTERNAL_SERVICE_TIMEOUT.code());
            assertEquals(504, ErrorCode.EXTERNAL_SERVICE_TIMEOUT.httpStatus());
        }

        @Test
        @DisplayName("EXTERNAL_SERVICE_RATE_LIMIT should have code 50003, httpStatus 429")
        void externalServiceRateLimit() {
            assertEquals("50003", ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT.code());
            assertEquals(429, ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT.httpStatus());
        }

        @Test
        @DisplayName("EXTERNAL_SERVICE_DATA_ERROR should have code 50004, httpStatus 502")
        void externalServiceDataError() {
            assertEquals("50004", ErrorCode.EXTERNAL_SERVICE_DATA_ERROR.code());
            assertEquals(502, ErrorCode.EXTERNAL_SERVICE_DATA_ERROR.httpStatus());
        }
    }

    // ======================== of() Tests ========================

    @Nested
    @DisplayName("of() Custom Creation")
    class OfTest {

        @Test
        @DisplayName("of() should create a custom ErrorCode")
        void ofCreatesCustom() {
            ErrorCode custom = ErrorCode.of("60001", "自定义错误", 400);
            assertEquals("60001", custom.code());
            assertEquals("自定义错误", custom.message());
            assertEquals(400, custom.httpStatus());
        }

        @Test
        @DisplayName("of() with same code should return same instance (registry)")
        void ofSameCodeReturnsSameInstance() {
            ErrorCode custom1 = ErrorCode.of("70001", "错误1", 400);
            ErrorCode custom2 = ErrorCode.of("70001", "错误2", 500);
            assertSame(custom1, custom2);
            assertEquals("错误1", custom2.message()); // first registration wins
        }
    }

    // ======================== fromCode() Tests ========================

    @Nested
    @DisplayName("fromCode() Tests")
    class FromCodeTest {

        @Test
        @DisplayName("fromCode() should find predefined SUCCESS")
        void fromCodeSuccess() {
            ErrorCode found = ErrorCode.fromCode("00000");
            assertSame(ErrorCode.SUCCESS, found);
        }

        @Test
        @DisplayName("fromCode() should find predefined SYSTEM_ERROR")
        void fromCodeSystemError() {
            ErrorCode found = ErrorCode.fromCode("10001");
            assertSame(ErrorCode.SYSTEM_ERROR, found);
        }

        @Test
        @DisplayName("fromCode() should find predefined BUSINESS_ERROR")
        void fromCodeBusinessError() {
            ErrorCode found = ErrorCode.fromCode("20001");
            assertSame(ErrorCode.BUSINESS_ERROR, found);
        }

        @Test
        @DisplayName("fromCode() should find predefined PARAM_REQUIRED")
        void fromCodeParamRequired() {
            ErrorCode found = ErrorCode.fromCode("30001");
            assertSame(ErrorCode.PARAM_REQUIRED, found);
        }

        @Test
        @DisplayName("fromCode() should find predefined UNAUTHORIZED")
        void fromCodeUnauthorized() {
            ErrorCode found = ErrorCode.fromCode("40001");
            assertSame(ErrorCode.UNAUTHORIZED, found);
        }

        @Test
        @DisplayName("fromCode() should find predefined EXTERNAL_SERVICE_ERROR")
        void fromCodeExternalServiceError() {
            ErrorCode found = ErrorCode.fromCode("50001");
            assertSame(ErrorCode.EXTERNAL_SERVICE_ERROR, found);
        }

        @Test
        @DisplayName("fromCode() should find custom registered error code")
        void fromCodeCustom() {
            ErrorCode custom = ErrorCode.of("99999", "自定义", 400);
            ErrorCode found = ErrorCode.fromCode("99999");
            assertSame(custom, found);
        }

        @Test
        @DisplayName("fromCode() should return null for unknown code")
        void fromCodeUnknown() {
            ErrorCode found = ErrorCode.fromCode("99988");
            assertNull(found);
        }

        @Test
        @DisplayName("fromCode() should throw NullPointerException for null")
        void fromCodeNull() {
            assertThrows(NullPointerException.class, () -> ErrorCode.fromCode(null));
        }
    }

    // ======================== predefinedValues() Tests ========================

    @Nested
    @DisplayName("predefinedValues() Tests")
    class PredefinedValuesTest {

        @Test
        @DisplayName("predefinedValues() should return all 31 predefined constants")
        void predefinedValuesCount() {
            List<ErrorCode> values = ErrorCode.predefinedValues();
            assertEquals(31, values.size());
        }

        @Test
        @DisplayName("predefinedValues() should contain all known constants")
        void predefinedValuesContains() {
            List<ErrorCode> values = ErrorCode.predefinedValues();
            assertTrue(values.contains(ErrorCode.SUCCESS));
            assertTrue(values.contains(ErrorCode.SYSTEM_ERROR));
            assertTrue(values.contains(ErrorCode.NETWORK_ERROR));
            assertTrue(values.contains(ErrorCode.SERVICE_UNAVAILABLE));
            assertTrue(values.contains(ErrorCode.TOO_MANY_REQUESTS));
            assertTrue(values.contains(ErrorCode.SERIALIZATION_ERROR));
            assertTrue(values.contains(ErrorCode.DATABASE_ERROR));
            assertTrue(values.contains(ErrorCode.BUSINESS_ERROR));
            assertTrue(values.contains(ErrorCode.DATA_NOT_FOUND));
            assertTrue(values.contains(ErrorCode.DATA_ALREADY_EXISTS));
            assertTrue(values.contains(ErrorCode.OPERATION_FAILED));
            assertTrue(values.contains(ErrorCode.STATE_CONFLICT));
            assertTrue(values.contains(ErrorCode.VERSION_CONFLICT));
            assertTrue(values.contains(ErrorCode.BALANCE_NOT_ENOUGH));
            assertTrue(values.contains(ErrorCode.STOCK_NOT_ENOUGH));
            assertTrue(values.contains(ErrorCode.PARAM_REQUIRED));
            assertTrue(values.contains(ErrorCode.PARAM_FORMAT_ERROR));
            assertTrue(values.contains(ErrorCode.PARAM_OUT_OF_RANGE));
            assertTrue(values.contains(ErrorCode.PARAM_TYPE_ERROR));
            assertTrue(values.contains(ErrorCode.PARAM_INVALID));
            assertTrue(values.contains(ErrorCode.BODY_REQUIRED));
            assertTrue(values.contains(ErrorCode.UNAUTHORIZED));
            assertTrue(values.contains(ErrorCode.FORBIDDEN));
            assertTrue(values.contains(ErrorCode.TOKEN_EXPIRED));
            assertTrue(values.contains(ErrorCode.TOKEN_INVALID));
            assertTrue(values.contains(ErrorCode.ACCOUNT_DISABLED));
            assertTrue(values.contains(ErrorCode.LOGIN_ATTEMPT_EXCEEDED));
            assertTrue(values.contains(ErrorCode.EXTERNAL_SERVICE_ERROR));
            assertTrue(values.contains(ErrorCode.EXTERNAL_SERVICE_TIMEOUT));
            assertTrue(values.contains(ErrorCode.EXTERNAL_SERVICE_RATE_LIMIT));
            assertTrue(values.contains(ErrorCode.EXTERNAL_SERVICE_DATA_ERROR));
        }
    }

    // ======================== BaseEnum Interface Tests ========================

    @Nested
    @DisplayName("BaseEnum Interface Implementation")
    class BaseEnumTest {

        @Test
        @DisplayName("ErrorCode should implement BaseEnum<String>")
        void implementsBaseEnum() {
            assertInstanceOf(BaseEnum.class, ErrorCode.SUCCESS);
        }

        @Test
        @DisplayName("getCode() should return same as code()")
        void getCodeMatchesCode() {
            assertEquals(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.getCode());
        }

        @Test
        @DisplayName("getDescription() should return same as message()")
        void getDescriptionMatchesMessage() {
            assertEquals(ErrorCode.SUCCESS.message(), ErrorCode.SUCCESS.getDescription());
        }
    }

    // ======================== Equals / HashCode / ToString ========================

    @Nested
    @DisplayName("Equals, HashCode, ToString")
    class ObjectMethodsTest {

        @Test
        @DisplayName("Equal ErrorCodes should have same hashCode (by code)")
        void equalByCode() {
            ErrorCode ec1 = ErrorCode.of("80001", "msg1", 400);
            ErrorCode ec2 = ErrorCode.of("80002", "msg2", 500);
            // predefined vs custom with same code won't happen, so test predefined equals
            ErrorCode ec3 = ErrorCode.fromCode("00000");
            assertEquals(ErrorCode.SUCCESS, ec3);
            assertEquals(ErrorCode.SUCCESS.hashCode(), ec3.hashCode());
        }

        @Test
        @DisplayName("Different ErrorCodes should not be equal")
        void notEqual() {
            assertNotEquals(ErrorCode.SUCCESS, ErrorCode.SYSTEM_ERROR);
            assertNotEquals(ErrorCode.SUCCESS.hashCode(), ErrorCode.SYSTEM_ERROR.hashCode());
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            assertNotEquals(null, ErrorCode.SUCCESS);
        }

        @Test
        @DisplayName("Should not equal different type")
        void notEqualDifferentType() {
            assertNotEquals("00000", ErrorCode.SUCCESS);
        }

        @Test
        @DisplayName("Same instance should be equal")
        void sameInstanceEqual() {
            assertEquals(ErrorCode.SUCCESS, ErrorCode.SUCCESS);
        }

        @Test
        @DisplayName("toString() should contain code, message, and httpStatus")
        void toStringFormat() {
            String str = ErrorCode.SUCCESS.toString();
            assertTrue(str.contains("00000"));
            assertTrue(str.contains("操作成功"));
            assertTrue(str.contains("200"));
        }
    }
}
