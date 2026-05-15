package com.zerx.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaseEnum Tests")
class BaseEnumTest {

    /**
     * Using CommonStatus as the test subject since BaseEnum is an interface.
     */

    // ======================== getCode() Tests ========================

    @Nested
    @DisplayName("getCode() Tests")
    class GetCodeTest {

        @Test
        @DisplayName("ENABLED should return code 1")
        void enabledGetCode() {
            assertEquals(1, CommonStatus.ENABLED.getCode());
        }

        @Test
        @DisplayName("DISABLED should return code 0")
        void disabledGetCode() {
            assertEquals(0, CommonStatus.DISABLED.getCode());
        }

        @Test
        @DisplayName("getCode() should return Integer type")
        void getCodeReturnsInteger() {
            assertInstanceOf(Integer.class, CommonStatus.ENABLED.getCode());
        }
    }

    // ======================== getDescription() Tests ========================

    @Nested
    @DisplayName("getDescription() Tests")
    class GetDescriptionTest {

        @Test
        @DisplayName("ENABLED should return description '启用'")
        void enabledGetDescription() {
            assertEquals("启用", CommonStatus.ENABLED.getDescription());
        }

        @Test
        @DisplayName("DISABLED should return description '禁用'")
        void disabledGetDescription() {
            assertEquals("禁用", CommonStatus.DISABLED.getDescription());
        }

        @Test
        @DisplayName("getDescription() should return String type")
        void getDescriptionReturnsString() {
            assertInstanceOf(String.class, CommonStatus.ENABLED.getDescription());
        }
    }

    // ======================== Interface Contract Tests ========================

    @Nested
    @DisplayName("Interface Contract Tests")
    class InterfaceContractTest {

        @Test
        @DisplayName("CommonStatus should implement BaseEnum<Integer>")
        void commonStatusImplementsBaseEnum() {
            assertInstanceOf(BaseEnum.class, CommonStatus.ENABLED);
            assertInstanceOf(BaseEnum.class, CommonStatus.DISABLED);
        }

        @Test
        @DisplayName("BaseEnum should be generic and accept different code types")
        void baseEnumAcceptsDifferentCodeTypes() {
            // CommonStatus uses Integer
            BaseEnum<Integer> intEnum = CommonStatus.ENABLED;
            assertNotNull(intEnum.getCode());

            // YesNo uses String
            BaseEnum<String> strEnum = com.zerx.common.enums.YesNo.YES;
            assertNotNull(strEnum.getCode());
        }

        @Test
        @DisplayName("getCode() and getDescription() should both be available via interface reference")
        void methodsViaInterfaceReference() {
            BaseEnum<Integer> status = CommonStatus.ENABLED;
            assertNotNull(status.getCode());
            assertNotNull(status.getDescription());
        }

        @Test
        @DisplayName("All CommonStatus values should have non-null code and description")
        void allValuesHaveNonNullCodeAndDescription() {
            for (CommonStatus status : CommonStatus.values()) {
                assertNotNull(status.getCode());
                assertNotNull(status.getDescription());
            }
        }
    }

    // ======================== DeleteFlag as additional BaseEnum test subject ========================

    @Nested
    @DisplayName("DeleteFlag as BaseEnum Implementation")
    class DeleteFlagBaseEnumTest {

        @Test
        @DisplayName("DeleteFlag should implement BaseEnum<Integer>")
        void deleteFlagImplementsBaseEnum() {
            assertInstanceOf(BaseEnum.class, DeleteFlag.DELETED);
            assertInstanceOf(BaseEnum.class, DeleteFlag.NOT_DELETED);
        }

        @Test
        @DisplayName("DeleteFlag.getCode() should return Integer")
        void deleteFlagGetCode() {
            assertEquals(1, DeleteFlag.DELETED.getCode());
            assertEquals(0, DeleteFlag.NOT_DELETED.getCode());
        }

        @Test
        @DisplayName("DeleteFlag.getDescription() should return non-null String")
        void deleteFlagGetDescription() {
            assertEquals("已删除", DeleteFlag.DELETED.getDescription());
            assertEquals("未删除", DeleteFlag.NOT_DELETED.getDescription());
        }
    }

    // ======================== YesNo as additional BaseEnum test subject ========================

    @Nested
    @DisplayName("YesNo as BaseEnum Implementation")
    class YesNoBaseEnumTest {

        @Test
        @DisplayName("YesNo should implement BaseEnum<String>")
        void yesNoImplementsBaseEnum() {
            assertInstanceOf(BaseEnum.class, YesNo.YES);
            assertInstanceOf(BaseEnum.class, YesNo.NO);
        }

        @Test
        @DisplayName("YesNo.getCode() should return String")
        void yesNoGetCode() {
            assertEquals("Y", YesNo.YES.getCode());
            assertEquals("N", YesNo.NO.getCode());
        }

        @Test
        @DisplayName("YesNo.getDescription() should return non-null String")
        void yesNoGetDescription() {
            assertEquals("是", YesNo.YES.getDescription());
            assertEquals("否", YesNo.NO.getDescription());
        }
    }
}
