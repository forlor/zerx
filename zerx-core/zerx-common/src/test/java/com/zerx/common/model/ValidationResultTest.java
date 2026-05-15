package com.zerx.common.model;

import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationResult Tests")
class ValidationResultTest {

    // ======================== Factory Method Tests ========================

    @Nested
    @DisplayName("create() Factory Method")
    class CreateTest {

        @Test
        @DisplayName("create() should return a valid empty ValidationResult")
        void createReturnsValidResult() {
            ValidationResult result = ValidationResult.create();
            assertTrue(result.isValid());
            assertFalse(result.hasErrors());
            assertEquals(0, result.errorCount());
            assertNull(result.firstError());
        }
    }

    // ======================== check() Tests ========================

    @Nested
    @DisplayName("check() Tests")
    class CheckTest {

        @Test
        @DisplayName("check(true) should not add error")
        void checkTrueDoesNotAddError() {
            ValidationResult result = ValidationResult.create().check(true, "should not appear");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("check(false) should add error")
        void checkFalseAddsError() {
            ValidationResult result = ValidationResult.create().check(false, "error message");
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
            assertEquals(1, result.errorCount());
            assertEquals("error message", result.firstError());
        }

        @Test
        @DisplayName("check() should support chaining")
        void checkChaining() {
            ValidationResult result = ValidationResult.create()
                    .check(true, "error1")
                    .check(false, "error2")
                    .check(false, "error3");
            assertEquals(2, result.errorCount());
        }

        @Test
        @DisplayName("check() with null message should throw NPE")
        void checkNullMessageThrows() {
            ValidationResult result = ValidationResult.create();
            assertThrows(NullPointerException.class, () -> result.check(false, null));
        }
    }

    // ======================== notNull() Tests ========================

    @Nested
    @DisplayName("notNull() Tests")
    class NotNullTest {

        @Test
        @DisplayName("notNull() with non-null value should pass")
        void notNullPass() {
            ValidationResult result = ValidationResult.create().notNull("value", "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("notNull() with null value should fail")
        void notNullFail() {
            ValidationResult result = ValidationResult.create().notNull(null, "must not be null");
            assertFalse(result.isValid());
            assertEquals("must not be null", result.firstError());
        }
    }

    // ======================== notBlank() Tests ========================

    @Nested
    @DisplayName("notBlank() Tests")
    class NotBlankTest {

        @Test
        @DisplayName("notBlank() with non-blank string should pass")
        void notBlankPass() {
            ValidationResult result = ValidationResult.create().notBlank("hello", "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("notBlank() with null should fail")
        void notBlankNullFail() {
            ValidationResult result = ValidationResult.create().notBlank(null, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("notBlank() with empty string should fail")
        void notBlankEmptyFail() {
            ValidationResult result = ValidationResult.create().notBlank("", "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("notBlank() with whitespace-only string should fail")
        void notBlankWhitespaceFail() {
            ValidationResult result = ValidationResult.create().notBlank("   ", "error");
            assertFalse(result.isValid());
        }
    }

    // ======================== notEmpty() Tests ========================

    @Nested
    @DisplayName("notEmpty() Tests")
    class NotEmptyTest {

        // String overload
        @Test
        @DisplayName("notEmpty(String) with non-empty string should pass")
        void notEmptyStringPass() {
            ValidationResult result = ValidationResult.create().notEmpty("hello", "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(String) with null should fail")
        void notEmptyStringNullFail() {
            ValidationResult result = ValidationResult.create().notEmpty((String) null, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(String) with empty string should fail")
        void notEmptyStringEmptyFail() {
            ValidationResult result = ValidationResult.create().notEmpty("", "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(String) with whitespace should pass (different from notBlank)")
        void notEmptyStringWhitespacePass() {
            ValidationResult result = ValidationResult.create().notEmpty("   ", "error");
            assertTrue(result.isValid());
        }

        // Collection overload
        @Test
        @DisplayName("notEmpty(Collection) with non-empty collection should pass")
        void notEmptyCollectionPass() {
            ValidationResult result = ValidationResult.create().notEmpty(List.of("a"), "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(Collection) with null collection should fail")
        void notEmptyCollectionNullFail() {
            ValidationResult result = ValidationResult.create().notEmpty((Collection<?>) null, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(Collection) with empty collection should fail")
        void notEmptyCollectionEmptyFail() {
            ValidationResult result = ValidationResult.create().notEmpty(List.of(), "error");
            assertFalse(result.isValid());
        }

        // Array overload
        @Test
        @DisplayName("notEmpty(Array) with non-empty array should pass")
        void notEmptyArrayPass() {
            ValidationResult result = ValidationResult.create().notEmpty(new String[]{"a"}, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(Array) with null array should fail")
        void notEmptyArrayNullFail() {
            ValidationResult result = ValidationResult.create().notEmpty((Object[]) null, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("notEmpty(Array) with empty array should fail")
        void notEmptyArrayEmptyFail() {
            ValidationResult result = ValidationResult.create().notEmpty(new String[]{}, "error");
            assertFalse(result.isValid());
        }
    }

    // ======================== Numeric Comparisons ========================

    @Nested
    @DisplayName("Numeric Comparison Tests")
    class NumericTest {

        @Test
        @DisplayName("gt() should pass when value > min")
        void gtPass() {
            ValidationResult result = ValidationResult.create().gt(10, 5, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("gt() should fail when value <= min")
        void gtFail() {
            ValidationResult result = ValidationResult.create().gt(5, 5, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("gt() should fail when value is null")
        void gtNullFail() {
            ValidationResult result = ValidationResult.create().gt(null, 5, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("ge() should pass when value >= min")
        void gePass() {
            ValidationResult result = ValidationResult.create().ge(5, 5, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("ge() should fail when value < min")
        void geFail() {
            ValidationResult result = ValidationResult.create().ge(4, 5, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("lt() should pass when value < max")
        void ltPass() {
            ValidationResult result = ValidationResult.create().lt(3, 5, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("lt() should fail when value >= max")
        void ltFail() {
            ValidationResult result = ValidationResult.create().lt(5, 5, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("between() should pass when value in range [min, max]")
        void betweenPass() {
            ValidationResult result = ValidationResult.create().between(5, 1, 10, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("between() should pass at boundary min")
        void betweenAtMin() {
            ValidationResult result = ValidationResult.create().between(1, 1, 10, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("between() should pass at boundary max")
        void betweenAtMax() {
            ValidationResult result = ValidationResult.create().between(10, 1, 10, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("between() should fail when value out of range")
        void betweenFail() {
            ValidationResult result = ValidationResult.create().between(11, 1, 10, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Numeric methods should work with different Number types")
        void differentNumberTypes() {
            ValidationResult result = ValidationResult.create()
                    .gt(5.5, 5.0, "error1")
                    .ge(5L, 5, "error2")
                    .lt(3.0, 5, "error3");
            assertTrue(result.isValid());
        }
    }

    // ======================== length() Tests ========================

    @Nested
    @DisplayName("length() Tests")
    class LengthTest {

        @Test
        @DisplayName("length() should pass when string is within range")
        void lengthPass() {
            ValidationResult result = ValidationResult.create().length("hello", 3, 10, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("length() should pass at min boundary")
        void lengthAtMin() {
            ValidationResult result = ValidationResult.create().length("abc", 3, 10, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("length() should pass at max boundary")
        void lengthAtMax() {
            ValidationResult result = ValidationResult.create().length("0123456789", 3, 10, "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("length() should fail when too short")
        void lengthTooShort() {
            ValidationResult result = ValidationResult.create().length("ab", 3, 10, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("length() should fail when too long")
        void lengthTooLong() {
            ValidationResult result = ValidationResult.create().length("01234567890", 3, 10, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("length() should fail when null")
        void lengthNullFail() {
            ValidationResult result = ValidationResult.create().length(null, 3, 10, "error");
            assertFalse(result.isValid());
        }
    }

    // ======================== matches() Tests ========================

    @Nested
    @DisplayName("matches() Tests")
    class MatchesTest {

        @Test
        @DisplayName("matches() should pass when regex matches")
        void matchesPass() {
            ValidationResult result = ValidationResult.create().matches("abc123", "[a-z]+\\d+", "error");
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("matches() should fail when regex does not match")
        void matchesFail() {
            ValidationResult result = ValidationResult.create().matches("ABC", "[a-z]+", "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("matches() should fail when null")
        void matchesNullFail() {
            ValidationResult result = ValidationResult.create().matches(null, "[a-z]+", "error");
            assertFalse(result.isValid());
        }
    }

    // ======================== merge() Tests ========================

    @Nested
    @DisplayName("merge() Tests")
    class MergeTest {

        @Test
        @DisplayName("merge() should combine errors from another ValidationResult")
        void mergeErrors() {
            ValidationResult result1 = ValidationResult.create().check(false, "error1");
            ValidationResult result2 = ValidationResult.create().check(false, "error2");
            result1.merge(result2);
            assertEquals(2, result1.errorCount());
            assertTrue(result1.errors().contains("error1"));
            assertTrue(result1.errors().contains("error2"));
        }

        @Test
        @DisplayName("merge() with valid result should not add errors")
        void mergeValidResult() {
            ValidationResult result1 = ValidationResult.create().check(false, "error1");
            ValidationResult result2 = ValidationResult.create();
            result1.merge(result2);
            assertEquals(1, result1.errorCount());
        }

        @Test
        @DisplayName("merge() with null should not throw")
        void mergeNull() {
            ValidationResult result = ValidationResult.create().check(false, "error1");
            assertDoesNotThrow(() -> result.merge(null));
            assertEquals(1, result.errorCount());
        }

        @Test
        @DisplayName("merge() should return same instance for chaining")
        void mergeReturnsThis() {
            ValidationResult result = ValidationResult.create();
            assertSame(result, result.merge(ValidationResult.create()));
        }
    }

    // ======================== nested() Tests ========================

    @Nested
    @DisplayName("nested() Tests")
    class NestedTest {

        @Test
        @DisplayName("nested() should merge child validation errors")
        void nestedMergesErrors() {
            ValidationResult result = ValidationResult.create()
                    .check(false, "parent error")
                    .nested(() -> ValidationResult.create()
                            .check(false, "child error 1")
                            .check(false, "child error 2"));
            assertEquals(3, result.errorCount());
        }

        @Test
        @DisplayName("nested() with valid child should not add errors")
        void nestedValidChild() {
            ValidationResult result = ValidationResult.create()
                    .check(false, "parent error")
                    .nested(() -> ValidationResult.create());
            assertEquals(1, result.errorCount());
        }

        @Test
        @DisplayName("nested() with null supplier should not throw")
        void nestedNullSupplier() {
            ValidationResult result = ValidationResult.create();
            assertDoesNotThrow(() -> result.nested(null));
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("nested() with supplier returning null should not throw")
        void nestedSupplierReturnsNull() {
            ValidationResult result = ValidationResult.create();
            assertDoesNotThrow(() -> result.nested(() -> null));
            assertTrue(result.isValid());
        }
    }

    // ======================== Result Query Tests ========================

    @Nested
    @DisplayName("Result Query Methods")
    class ResultQueryTest {

        @Test
        @DisplayName("hasErrors() should return true when errors exist")
        void hasErrorsTrue() {
            ValidationResult result = ValidationResult.create().check(false, "error");
            assertTrue(result.hasErrors());
        }

        @Test
        @DisplayName("hasErrors() should return false when no errors")
        void hasErrorsFalse() {
            ValidationResult result = ValidationResult.create();
            assertFalse(result.hasErrors());
        }

        @Test
        @DisplayName("isValid() should return true when no errors")
        void isValidTrue() {
            ValidationResult result = ValidationResult.create();
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("isValid() should return false when errors exist")
        void isValidFalse() {
            ValidationResult result = ValidationResult.create().check(false, "error");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("errorCount() should return correct count")
        void errorCount() {
            ValidationResult result = ValidationResult.create()
                    .check(false, "e1")
                    .check(false, "e2")
                    .check(true, "e3");
            assertEquals(2, result.errorCount());
        }

        @Test
        @DisplayName("firstError() should return first error")
        void firstError() {
            ValidationResult result = ValidationResult.create()
                    .check(false, "first")
                    .check(false, "second");
            assertEquals("first", result.firstError());
        }

        @Test
        @DisplayName("firstError() should return null when no errors")
        void firstErrorNull() {
            ValidationResult result = ValidationResult.create();
            assertNull(result.firstError());
        }

        @Test
        @DisplayName("errors() should return unmodifiable list")
        void errorsUnmodifiable() {
            ValidationResult result = ValidationResult.create().check(false, "error");
            List<String> errors = result.errors();
            assertThrows(UnsupportedOperationException.class, () -> errors.add("another"));
        }
    }

    // ======================== joinErrors() Tests ========================

    @Nested
    @DisplayName("joinErrors() Tests")
    class JoinErrorsTest {

        @Test
        @DisplayName("joinErrors() should join all errors with separator")
        void joinErrorsWithSeparator() {
            ValidationResult result = ValidationResult.create()
                    .check(false, "error1")
                    .check(false, "error2")
                    .check(false, "error3");
            assertEquals("error1; error2; error3", result.joinErrors("; "));
        }

        @Test
        @DisplayName("joinErrors() should return empty string when no errors")
        void joinErrorsEmpty() {
            ValidationResult result = ValidationResult.create();
            assertEquals("", result.joinErrors(", "));
        }
    }

    // ======================== toResult() Tests ========================

    @Nested
    @DisplayName("toResult() Tests")
    class ToResultTest {

        @Test
        @DisplayName("toResult() should return ok Result when valid")
        void toResultValid() {
            ValidationResult validation = ValidationResult.create();
            Result<?> result = validation.toResult();
            assertTrue(result.isSuccess());
        }

        @Test
        @DisplayName("toResult() should return fail Result with code 30001 when invalid")
        void toResultInvalid() {
            ValidationResult validation = ValidationResult.create()
                    .check(false, "error1")
                    .check(false, "error2");
            Result<?> result = validation.toResult();
            assertFalse(result.isSuccess());
            assertEquals("30001", result.code());
            assertEquals("error1; error2", result.message());
        }
    }

    // ======================== throwIfInvalid() Tests ========================

    @Nested
    @DisplayName("throwIfInvalid() Tests")
    class ThrowIfInvalidTest {

        @Test
        @DisplayName("throwIfInvalid() should not throw when valid")
        void throwIfInvalidValid() {
            ValidationResult result = ValidationResult.create();
            assertDoesNotThrow(result::throwIfInvalid);
        }

        @Test
        @DisplayName("throwIfInvalid() should throw ValidationException when invalid")
        void throwIfInvalidInvalid() {
            ValidationResult result = ValidationResult.create()
                    .check(false, "error1")
                    .check(false, "error2");
            ValidationException ex = assertThrows(ValidationException.class, result::throwIfInvalid);
            assertEquals(ErrorCode.PARAM_FORMAT_ERROR, ex.getErrorCode());
        }
    }

    // ======================== toString() Tests ========================

    @Nested
    @DisplayName("toString() Tests")
    class ToStringTest {

        @Test
        @DisplayName("toString() should show valid=true when valid")
        void toStringValid() {
            ValidationResult result = ValidationResult.create();
            assertTrue(result.toString().contains("valid=true"));
        }

        @Test
        @DisplayName("toString() should show valid=false and errors when invalid")
        void toStringInvalid() {
            ValidationResult result = ValidationResult.create().check(false, "test error");
            String str = result.toString();
            assertTrue(str.contains("valid=false"));
            assertTrue(str.contains("test error"));
        }
    }

    // ======================== Accumulative Validation Tests ========================

    @Nested
    @DisplayName("Accumulative Validation Tests")
    class AccumulativeTest {

        @Test
        @DisplayName("Should accumulate all errors before checking")
        void accumulateAllErrors() {
            ValidationResult result = ValidationResult.create();
            result.notNull(null, "name must not be null");
            result.notBlank("", "email must not be blank");
            result.gt(0, 1, "age must be greater than 0");
            result.length("ab", 5, 20, "password length must be 5-20");
            result.matches("xyz", "\\d+", "phone must be numeric");

            assertEquals(5, result.errorCount());
            assertFalse(result.isValid());
            assertTrue(result.hasErrors());
        }

        @Test
        @DisplayName("Should support complex nested validation")
        void complexNestedValidation() {
            ValidationResult addressResult = ValidationResult.create()
                    .notBlank(null, "street is required")
                    .length("A", 2, 100, "city too short");

            ValidationResult result = ValidationResult.create()
                    .notBlank("John", "name required")
                    .nested(() -> addressResult);

            assertEquals(2, result.errorCount());
        }
    }
}
