package com.zerx.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommonStatus Tests")
class CommonStatusTest {

    // ======================== Enum Values Tests ========================

    @Nested
    @DisplayName("Enum Values")
    class EnumValuesTest {

        @Test
        @DisplayName("Should have exactly 2 values")
        void valueCount() {
            assertEquals(2, CommonStatus.values().length);
        }

        @Test
        @DisplayName("Should have ENABLED and DISABLED values")
        void hasExpectedValues() {
            assertNotNull(CommonStatus.valueOf("ENABLED"));
            assertNotNull(CommonStatus.valueOf("DISABLED"));
        }

        @Test
        @DisplayName("valueof() with invalid name should throw")
        void valueOfInvalid() {
            assertThrows(IllegalArgumentException.class, () -> CommonStatus.valueOf("INVALID"));
        }
    }

    // ======================== isEnabled() / isDisabled() Tests ========================

    @Nested
    @DisplayName("isEnabled() / isDisabled() Instance Methods")
    class StatusCheckTest {

        @Test
        @DisplayName("ENABLED.isEnabled() should return true")
        void enabledIsEnabled() {
            assertTrue(CommonStatus.ENABLED.isEnabled());
        }

        @Test
        @DisplayName("ENABLED.isDisabled() should return false")
        void enabledIsDisabled() {
            assertFalse(CommonStatus.ENABLED.isDisabled());
        }

        @Test
        @DisplayName("DISABLED.isEnabled() should return false")
        void disabledIsEnabled() {
            assertFalse(CommonStatus.DISABLED.isEnabled());
        }

        @Test
        @DisplayName("DISABLED.isDisabled() should return true")
        void disabledIsDisabled() {
            assertTrue(CommonStatus.DISABLED.isDisabled());
        }
    }

    // ======================== fromCode() Tests ========================

    @Nested
    @DisplayName("fromCode() Tests")
    class FromCodeTest {

        @Test
        @DisplayName("fromCode(1) should return ENABLED")
        void fromCode1() {
            assertEquals(CommonStatus.ENABLED, CommonStatus.fromCode(1));
        }

        @Test
        @DisplayName("fromCode(0) should return DISABLED")
        void fromCode0() {
            assertEquals(CommonStatus.DISABLED, CommonStatus.fromCode(0));
        }

        @Test
        @DisplayName("fromCode(null) should return null")
        void fromCodeNull() {
            assertNull(CommonStatus.fromCode(null));
        }

        @Test
        @DisplayName("fromCode(99) should return null for unknown code")
        void fromCodeUnknown() {
            assertNull(CommonStatus.fromCode(99));
        }

        @Test
        @DisplayName("fromCode(-1) should return null for negative code")
        void fromCodeNegative() {
            assertNull(CommonStatus.fromCode(-1));
        }

        @Test
        @DisplayName("fromCode(code, defaultValue) should return default for unknown code")
        void fromCodeWithDefault() {
            assertEquals(CommonStatus.ENABLED, CommonStatus.fromCode(99, CommonStatus.ENABLED));
        }

        @Test
        @DisplayName("fromCode(code, defaultValue) should return found value when known")
        void fromCodeWithDefaultFound() {
            assertEquals(CommonStatus.DISABLED, CommonStatus.fromCode(0, CommonStatus.ENABLED));
        }

        @Test
        @DisplayName("fromCode(code, defaultValue) with null should return default")
        void fromCodeNullWithDefault() {
            assertEquals(CommonStatus.ENABLED, CommonStatus.fromCode(null, CommonStatus.ENABLED));
        }
    }

    // ======================== Static isEnabled() / isDisabled() Tests ========================

    @Nested
    @DisplayName("Static isEnabled() / isDisabled() Tests")
    class StaticStatusCheckTest {

        @Test
        @DisplayName("Static isEnabled(1) should return true")
        void staticIsEnabled1() {
            assertTrue(CommonStatus.isEnabled(1));
        }

        @Test
        @DisplayName("Static isEnabled(0) should return false")
        void staticIsEnabled0() {
            assertFalse(CommonStatus.isEnabled(0));
        }

        @Test
        @DisplayName("Static isDisabled(0) should return true")
        void staticIsDisabled0() {
            assertTrue(CommonStatus.isDisabled(0));
        }

        @Test
        @DisplayName("Static isDisabled(1) should return false")
        void staticIsDisabled1() {
            assertFalse(CommonStatus.isDisabled(1));
        }

        @Test
        @DisplayName("Static isEnabled(null) should return false")
        void staticIsEnabledNull() {
            assertFalse(CommonStatus.isEnabled(null));
        }

        @Test
        @DisplayName("Static isDisabled(null) should return false")
        void staticIsDisabledNull() {
            assertFalse(CommonStatus.isDisabled(null));
        }
    }

    // ======================== of(boolean) Tests ========================

    @Nested
    @DisplayName("of(boolean) Tests")
    class OfBooleanTest {

        @Test
        @DisplayName("of(true) should return ENABLED")
        void ofTrue() {
            assertEquals(CommonStatus.ENABLED, CommonStatus.of(true));
        }

        @Test
        @DisplayName("of(false) should return DISABLED")
        void ofFalse() {
            assertEquals(CommonStatus.DISABLED, CommonStatus.of(false));
        }
    }

    // ======================== getCode() / getDescription() Tests ========================

    @Nested
    @DisplayName("getCode() / getDescription() Tests")
    class AccessorTest {

        @Test
        @DisplayName("ENABLED.getCode() should return 1")
        void enabledGetCode() {
            assertEquals(1, CommonStatus.ENABLED.getCode());
        }

        @Test
        @DisplayName("DISABLED.getCode() should return 0")
        void disabledGetCode() {
            assertEquals(0, CommonStatus.DISABLED.getCode());
        }

        @Test
        @DisplayName("ENABLED.getDescription() should return '启用'")
        void enabledGetDescription() {
            assertEquals("启用", CommonStatus.ENABLED.getDescription());
        }

        @Test
        @DisplayName("DISABLED.getDescription() should return '禁用'")
        void disabledGetDescription() {
            assertEquals("禁用", CommonStatus.DISABLED.getDescription());
        }
    }
}
