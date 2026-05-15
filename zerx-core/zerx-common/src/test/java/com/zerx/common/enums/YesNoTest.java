package com.zerx.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("YesNo Tests")
class YesNoTest {

    // ======================== Enum Values Tests ========================

    @Nested
    @DisplayName("Enum Values")
    class EnumValuesTest {

        @Test
        @DisplayName("Should have exactly 2 values")
        void valueCount() {
            assertEquals(2, YesNo.values().length);
        }

        @Test
        @DisplayName("Should have YES and NO values")
        void hasExpectedValues() {
            assertNotNull(YesNo.valueOf("YES"));
            assertNotNull(YesNo.valueOf("NO"));
        }

        @Test
        @DisplayName("valueOf() with invalid name should throw")
        void valueOfInvalid() {
            assertThrows(IllegalArgumentException.class, () -> YesNo.valueOf("MAYBE"));
        }
    }

    // ======================== isYes() / isNo() Instance Methods ========================

    @Nested
    @DisplayName("isYes() / isNo() Instance Methods")
    class ValueCheckTest {

        @Test
        @DisplayName("YES.isYes() should return true")
        void yesIsYes() {
            assertTrue(YesNo.YES.isYes());
        }

        @Test
        @DisplayName("YES.isNo() should return false")
        void yesIsNo() {
            assertFalse(YesNo.YES.isNo());
        }

        @Test
        @DisplayName("NO.isYes() should return false")
        void noIsYes() {
            assertFalse(YesNo.NO.isYes());
        }

        @Test
        @DisplayName("NO.isNo() should return true")
        void noIsNo() {
            assertTrue(YesNo.NO.isNo());
        }
    }

    // ======================== fromCode() Tests ========================

    @Nested
    @DisplayName("fromCode() Tests")
    class FromCodeTest {

        @Test
        @DisplayName("fromCode(\"Y\") should return YES")
        void fromCodeY() {
            assertEquals(YesNo.YES, YesNo.fromCode("Y"));
        }

        @Test
        @DisplayName("fromCode(\"N\") should return NO")
        void fromCodeN() {
            assertEquals(YesNo.NO, YesNo.fromCode("N"));
        }

        @Test
        @DisplayName("fromCode(\"y\") should return YES (case-insensitive)")
        void fromCodeLowercaseY() {
            assertEquals(YesNo.YES, YesNo.fromCode("y"));
        }

        @Test
        @DisplayName("fromCode(\"n\") should return NO (case-insensitive)")
        void fromCodeLowercaseN() {
            assertEquals(YesNo.NO, YesNo.fromCode("n"));
        }

        @Test
        @DisplayName("fromCode(\"Y\") should match uppercase (case-insensitive)")
        void fromCodeUppercaseY() {
            assertEquals(YesNo.YES, YesNo.fromCode("Y"));
        }

        @Test
        @DisplayName("fromCode(null) should return null")
        void fromCodeNull() {
            assertNull(YesNo.fromCode(null));
        }

        @Test
        @DisplayName("fromCode(\"X\") should return null for unknown code")
        void fromCodeUnknown() {
            assertNull(YesNo.fromCode("X"));
        }

        @Test
        @DisplayName("fromCode(\"\") should return null for empty string")
        void fromCodeEmpty() {
            assertNull(YesNo.fromCode(""));
        }

        @Test
        @DisplayName("fromCode(code, defaultValue) should return default for unknown code")
        void fromCodeWithDefault() {
            assertEquals(YesNo.YES, YesNo.fromCode("X", YesNo.YES));
        }

        @Test
        @DisplayName("fromCode(code, defaultValue) should return found value when known")
        void fromCodeWithDefaultFound() {
            assertEquals(YesNo.NO, YesNo.fromCode("n", YesNo.YES));
        }

        @Test
        @DisplayName("fromCode(null, defaultValue) should return default")
        void fromCodeNullWithDefault() {
            assertEquals(YesNo.NO, YesNo.fromCode(null, YesNo.NO));
        }
    }

    // ======================== of(boolean) Tests ========================

    @Nested
    @DisplayName("of(boolean) Tests")
    class OfBooleanTest {

        @Test
        @DisplayName("of(true) should return YES")
        void ofTrue() {
            assertEquals(YesNo.YES, YesNo.of(true));
        }

        @Test
        @DisplayName("of(false) should return NO")
        void ofFalse() {
            assertEquals(YesNo.NO, YesNo.of(false));
        }
    }

    // ======================== Static isYes() / isNo() Tests ========================

    @Nested
    @DisplayName("Static isYes() / isNo() Tests")
    class StaticValueCheckTest {

        @Test
        @DisplayName("Static isYes(\"Y\") should return true")
        void staticIsYesY() {
            assertTrue(YesNo.isYes("Y"));
        }

        @Test
        @DisplayName("Static isYes(\"y\") should return true (case-insensitive)")
        void staticIsYesLowercaseY() {
            assertTrue(YesNo.isYes("y"));
        }

        @Test
        @DisplayName("Static isYes(\"N\") should return false")
        void staticIsYesN() {
            assertFalse(YesNo.isYes("N"));
        }

        @Test
        @DisplayName("Static isNo(\"N\") should return true")
        void staticIsNoN() {
            assertTrue(YesNo.isNo("N"));
        }

        @Test
        @DisplayName("Static isNo(\"n\") should return true (case-insensitive)")
        void staticIsNoLowercaseN() {
            assertTrue(YesNo.isNo("n"));
        }

        @Test
        @DisplayName("Static isNo(\"Y\") should return false")
        void staticIsNoY() {
            assertFalse(YesNo.isNo("Y"));
        }

        @Test
        @DisplayName("Static isYes(null) should return false")
        void staticIsYesNull() {
            assertFalse(YesNo.isYes(null));
        }

        @Test
        @DisplayName("Static isNo(null) should return false")
        void staticIsNoNull() {
            assertFalse(YesNo.isNo(null));
        }
    }

    // ======================== getCode() / getDescription() Tests ========================

    @Nested
    @DisplayName("getCode() / getDescription() Tests")
    class AccessorTest {

        @Test
        @DisplayName("YES.getCode() should return \"Y\"")
        void yesGetCode() {
            assertEquals("Y", YesNo.YES.getCode());
        }

        @Test
        @DisplayName("NO.getCode() should return \"N\"")
        void noGetCode() {
            assertEquals("N", YesNo.NO.getCode());
        }

        @Test
        @DisplayName("YES.getDescription() should return '是'")
        void yesGetDescription() {
            assertEquals("是", YesNo.YES.getDescription());
        }

        @Test
        @DisplayName("NO.getDescription() should return '否'")
        void noGetDescription() {
            assertEquals("否", YesNo.NO.getDescription());
        }
    }
}
