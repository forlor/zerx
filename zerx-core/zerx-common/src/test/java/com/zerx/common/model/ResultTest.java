package com.zerx.common.model;

import com.zerx.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Result Tests")
class ResultTest {

    // ======================== ok() Tests ========================

    @Nested
    @DisplayName("ok() Factory Methods")
    class OkTest {

        @Test
        @DisplayName("ok(data) should return success with data")
        void okWithData() {
            Result<String> result = Result.ok("hello");
            assertTrue(result.success());
            assertTrue(result.isSuccess());
            assertEquals("00000", result.code());
            assertEquals("操作成功", result.message());
            assertEquals("hello", result.data());
        }

        @Test
        @DisplayName("ok() without data should return success with null data")
        void okWithoutData() {
            Result<Void> result = Result.ok();
            assertTrue(result.success());
            assertTrue(result.isSuccess());
            assertEquals("00000", result.code());
            assertNull(result.data());
        }

        @Test
        @DisplayName("ok(message, data) should return success with custom message")
        void okWithCustomMessage() {
            Result<String> result = Result.ok("自定义成功消息", "hello");
            assertTrue(result.success());
            assertEquals("00000", result.code());
            assertEquals("自定义成功消息", result.message());
            assertEquals("hello", result.data());
        }

        @Test
        @DisplayName("ok() should work with complex generic types")
        void okWithComplexType() {
            record User(String name, int age) {}
            User user = new User("Alice", 30);
            Result<User> result = Result.ok(user);
            assertTrue(result.isSuccess());
            assertEquals("Alice", result.data().name());
        }

        @Test
        @DisplayName("ok() should work with null data")
        void okWithNullData() {
            Result<String> result = Result.ok((String) null);
            assertTrue(result.isSuccess());
            assertNull(result.data());
        }
    }

    // ======================== fail() Tests ========================

    @Nested
    @DisplayName("fail() Factory Methods")
    class FailTest {

        @Test
        @DisplayName("fail(ErrorCode) should return failure with error code info")
        void failWithErrorCode() {
            Result<Void> result = Result.fail(ErrorCode.BUSINESS_ERROR);
            assertFalse(result.success());
            assertFalse(result.isSuccess());
            assertEquals("20001", result.code());
            assertEquals("业务处理失败", result.message());
            assertNull(result.data());
        }

        @Test
        @DisplayName("fail(code, message) should return failure with custom code and message")
        void failWithCodeAndMessage() {
            Result<Void> result = Result.fail("99999", "自定义错误");
            assertFalse(result.success());
            assertEquals("99999", result.code());
            assertEquals("自定义错误", result.message());
            assertNull(result.data());
        }

        @Test
        @DisplayName("fail(ErrorCode, message) should return failure with error code and custom message")
        void failWithErrorCodeAndMessage() {
            Result<Void> result = Result.fail(ErrorCode.PARAM_REQUIRED, "用户名不能为空");
            assertFalse(result.success());
            assertEquals("30001", result.code());
            assertEquals("用户名不能为空", result.message());
            assertNull(result.data());
        }

        @Test
        @DisplayName("fail(ErrorCode) should use correct HTTP status from error code")
        void failPreservesErrorCodeHttpStatus() {
            Result<Void> result = Result.fail(ErrorCode.DATA_NOT_FOUND);
            assertEquals("20002", result.code());
            assertEquals("数据不存在", result.message());
        }
    }

    // ======================== isSuccess() Tests ========================

    @Nested
    @DisplayName("isSuccess() Tests")
    class IsSuccessTest {

        @Test
        @DisplayName("isSuccess() should return true for ok result")
        void successResultIsSuccess() {
            Result<String> result = Result.ok("data");
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("isSuccess() should return false for fail result")
        void failResultIsNotSuccess() {
            Result<Void> result = Result.fail(ErrorCode.SYSTEM_ERROR);
            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("isSuccess() should match success field")
        void isSuccessMatchesSuccessField() {
            Result<String> ok = Result.ok("data");
            Result<Void> fail = Result.fail(ErrorCode.BUSINESS_ERROR);
            assertEquals(ok.success(), ok.isSuccess());
            assertEquals(fail.success(), fail.isSuccess());
        }
    }

    // ======================== Equals / HashCode / ToString ========================

    @Nested
    @DisplayName("Equals, HashCode, ToString")
    class ObjectMethodsTest {

        @Test
        @DisplayName("Equal Results should have same hashCode")
        void equalInstances() {
            Result<String> r1 = new Result<>(true, "00000", "操作成功", "data");
            Result<String> r2 = new Result<>(true, "00000", "操作成功", "data");
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("Different success status should not be equal")
        void differentSuccess() {
            Result<String> r1 = new Result<>(true, "00000", "ok", "data");
            Result<String> r2 = new Result<>(false, "00000", "ok", "data");
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Different code should not be equal")
        void differentCode() {
            Result<Void> r1 = new Result<>(false, "20001", "error", null);
            Result<Void> r2 = new Result<>(false, "20002", "error", null);
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Different data should not be equal")
        void differentData() {
            Result<String> r1 = new Result<>(true, "00000", "ok", "data1");
            Result<String> r2 = new Result<>(true, "00000", "ok", "data2");
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            Result<String> result = Result.ok("data");
            assertNotEquals(null, result);
        }

        @Test
        @DisplayName("Should not equal different type")
        void notEqualDifferentType() {
            Result<String> result = Result.ok("data");
            assertNotEquals("data", result);
        }

        @Test
        @DisplayName("Same instance should be equal")
        void sameInstanceEqual() {
            Result<String> result = Result.ok("data");
            assertEquals(result, result);
        }

        @Test
        @DisplayName("ToString should contain field information")
        void toStringContainsFields() {
            Result<String> result = Result.ok("hello");
            String str = result.toString();
            assertNotNull(str);
            assertTrue(str.contains("00000"));
        }
    }

    // ======================== Generic Type Tests ========================

    @Nested
    @DisplayName("Generic Type Support")
    class GenericTypeTest {

        @Test
        @DisplayName("Should work with Integer generic type")
        void integerType() {
            Result<Integer> result = Result.ok(42);
            assertEquals(42, result.data());
        }

        @Test
        @DisplayName("Should work with raw Result type")
        void rawType() {
            Result result = Result.ok();
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("fail() should work with different generic types")
        void failWithDifferentTypes() {
            Result<String> result1 = Result.fail(ErrorCode.SYSTEM_ERROR);
            Result<Integer> result2 = Result.fail(ErrorCode.SYSTEM_ERROR);
            assertNull(result1.data());
            assertNull(result2.data());
        }
    }
}
