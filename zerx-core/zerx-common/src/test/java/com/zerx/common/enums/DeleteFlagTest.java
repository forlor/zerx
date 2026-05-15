package com.zerx.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeleteFlag Tests")
class DeleteFlagTest {

    // ======================== Enum Values Tests ========================

    @Nested
    @DisplayName("Enum Values")
    class EnumValuesTest {

        @Test
        @DisplayName("Should have exactly 2 values")
        void valueCount() {
            assertEquals(2, DeleteFlag.values().length);
        }

        @Test
        @DisplayName("Should have DELETED and NOT_DELETED values")
        void hasExpectedValues() {
            assertNotNull(DeleteFlag.valueOf("DELETED"));
            assertNotNull(DeleteFlag.valueOf("NOT_DELETED"));
        }

        @Test
        @DisplayName("valueOf() with invalid name should throw")
        void valueOfInvalid() {
            assertThrows(IllegalArgumentException.class, () -> DeleteFlag.valueOf("INVALID"));
        }
    }

    // ======================== isDeleted() / isNotDeleted() Tests ========================

    @Nested
    @DisplayName("isDeleted() / isNotDeleted() Instance Methods")
    class FlagCheckTest {

        @Test
        @DisplayName("DELETED.isDeleted() should return true")
        void deletedIsDeleted() {
            assertTrue(DeleteFlag.DELETED.isDeleted());
        }

        @Test
        @DisplayName("DELETED.isNotDeleted() should return false")
        void deletedIsNotDeleted() {
            assertFalse(DeleteFlag.DELETED.isNotDeleted());
        }

        @Test
        @DisplayName("NOT_DELETED.isDeleted() should return false")
        void notDeletedIsDeleted() {
            assertFalse(DeleteFlag.NOT_DELETED.isDeleted());
        }

        @Test
        @DisplayName("NOT_DELETED.isNotDeleted() should return true")
        void notDeletedIsNotDeleted() {
            assertTrue(DeleteFlag.NOT_DELETED.isNotDeleted());
        }
    }

    // ======================== fromCode() Tests ========================

    @Nested
    @DisplayName("fromCode() Tests")
    class FromCodeTest {

        @Test
        @DisplayName("fromCode(1) should return DELETED")
        void fromCode1() {
            assertEquals(DeleteFlag.DELETED, DeleteFlag.fromCode(1));
        }

        @Test
        @DisplayName("fromCode(0) should return NOT_DELETED")
        void fromCode0() {
            assertEquals(DeleteFlag.NOT_DELETED, DeleteFlag.fromCode(0));
        }

        @Test
        @DisplayName("fromCode(null) should return null")
        void fromCodeNull() {
            assertNull(DeleteFlag.fromCode(null));
        }

        @Test
        @DisplayName("fromCode(99) should return null for unknown code")
        void fromCodeUnknown() {
            assertNull(DeleteFlag.fromCode(99));
        }

        @Test
        @DisplayName("fromCode(-1) should return null for negative code")
        void fromCodeNegative() {
            assertNull(DeleteFlag.fromCode(-1));
        }
    }

    // ======================== Static isDeleted() / isNotDeleted() Tests ========================

    @Nested
    @DisplayName("Static isDeleted() / isNotDeleted() Tests")
    class StaticFlagCheckTest {

        @Test
        @DisplayName("Static isDeleted(1) should return true")
        void staticIsDeleted1() {
            assertTrue(DeleteFlag.isDeleted(1));
        }

        @Test
        @DisplayName("Static isDeleted(0) should return false")
        void staticIsDeleted0() {
            assertFalse(DeleteFlag.isDeleted(0));
        }

        @Test
        @DisplayName("Static isNotDeleted(0) should return true")
        void staticIsNotDeleted0() {
            assertTrue(DeleteFlag.isNotDeleted(0));
        }

        @Test
        @DisplayName("Static isNotDeleted(1) should return false")
        void staticIsNotDeleted1() {
            assertFalse(DeleteFlag.isNotDeleted(1));
        }

        @Test
        @DisplayName("Static isDeleted(null) should return false")
        void staticIsDeletedNull() {
            assertFalse(DeleteFlag.isDeleted(null));
        }

        @Test
        @DisplayName("Static isNotDeleted(null) should return false")
        void staticIsNotDeletedNull() {
            assertFalse(DeleteFlag.isNotDeleted(null));
        }
    }

    // ======================== getCode() / getDescription() Tests ========================

    @Nested
    @DisplayName("getCode() / getDescription() Tests")
    class AccessorTest {

        @Test
        @DisplayName("DELETED.getCode() should return 1")
        void deletedGetCode() {
            assertEquals(1, DeleteFlag.DELETED.getCode());
        }

        @Test
        @DisplayName("NOT_DELETED.getCode() should return 0")
        void notDeletedGetCode() {
            assertEquals(0, DeleteFlag.NOT_DELETED.getCode());
        }

        @Test
        @DisplayName("DELETED.getDescription() should return '已删除'")
        void deletedGetDescription() {
            assertEquals("已删除", DeleteFlag.DELETED.getDescription());
        }

        @Test
        @DisplayName("NOT_DELETED.getDescription() should return '未删除'")
        void notDeletedGetDescription() {
            assertEquals("未删除", DeleteFlag.NOT_DELETED.getDescription());
        }
    }
}
