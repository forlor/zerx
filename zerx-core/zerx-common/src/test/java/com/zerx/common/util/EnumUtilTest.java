package com.zerx.common.util;

import com.zerx.common.enums.BaseEnum;
import com.zerx.common.model.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link EnumUtil} 单元测试
 */
@DisplayName("EnumUtil 枚举工具类测试")
class EnumUtilTest {

    /**
     * 测试用枚举，实现 BaseEnum<Integer>
     */
    enum TestEnum implements BaseEnum<Integer> {
        A(1, "Alpha"),
        B(2, "Beta"),
        C(3, "Gamma");

        private final Integer code;
        private final String description;

        TestEnum(Integer code, String description) {
            this.code = code;
            this.description = description;
        }

        @Override
        public Integer getCode() {
            return code;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    // ======================== BaseEnum 操作 ========================

    @Nested
    @DisplayName("findByCode 测试")
    class FindByCodeTests {

        @Test
        @DisplayName("通过 code 找到枚举")
        void findByCode_found() {
            assertEquals(TestEnum.A, EnumUtil.findByCode(TestEnum.class, 1));
            assertEquals(TestEnum.B, EnumUtil.findByCode(TestEnum.class, 2));
            assertEquals(TestEnum.C, EnumUtil.findByCode(TestEnum.class, 3));
        }

        @Test
        @DisplayName("code 不存在返回 null")
        void findByCode_notFound() {
            assertNull(EnumUtil.findByCode(TestEnum.class, 99));
        }

        @Test
        @DisplayName("null 枚举类返回 null")
        void findByCode_nullClass() {
            assertNull(EnumUtil.findByCode(null, 1));
        }

        @Test
        @DisplayName("null code 返回 null")
        void findByCode_nullCode() {
            assertNull(EnumUtil.findByCode(TestEnum.class, null));
        }
    }

    @Nested
    @DisplayName("requireByCode 测试")
    class RequireByCodeTests {

        @Test
        @DisplayName("通过 code 找到枚举")
        void requireByCode_found() {
            assertEquals(TestEnum.A, EnumUtil.requireByCode(TestEnum.class, 1));
        }

        @Test
        @DisplayName("code 不存在抛出 IllegalArgumentException")
        void requireByCode_notFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> EnumUtil.requireByCode(TestEnum.class, 99));
            assertTrue(ex.getMessage().contains("99"));
            assertTrue(ex.getMessage().contains("TestEnum"));
        }
    }

    @Nested
    @DisplayName("findByDescription 测试")
    class FindByDescriptionTests {

        @Test
        @DisplayName("通过 description 找到枚举")
        void findByDescription_found() {
            assertEquals(TestEnum.A, EnumUtil.findByDescription(TestEnum.class, "Alpha"));
            assertEquals(TestEnum.B, EnumUtil.findByDescription(TestEnum.class, "Beta"));
        }

        @Test
        @DisplayName("description 不存在返回 null")
        void findByDescription_notFound() {
            assertNull(EnumUtil.findByDescription(TestEnum.class, "Unknown"));
        }

        @Test
        @DisplayName("null 枚举类返回 null")
        void findByDescription_nullClass() {
            assertNull(EnumUtil.findByDescription(null, "Alpha"));
        }

        @Test
        @DisplayName("null description 返回 null")
        void findByDescription_nullDesc() {
            assertNull(EnumUtil.findByDescription(TestEnum.class, null));
        }
    }

    @Nested
    @DisplayName("isValidCode 测试")
    class IsValidCodeTests {

        @Test
        @DisplayName("有效 code 返回 true")
        void isValidCode_true() {
            assertTrue(EnumUtil.isValidCode(TestEnum.class, 1));
            assertTrue(EnumUtil.isValidCode(TestEnum.class, 2));
            assertTrue(EnumUtil.isValidCode(TestEnum.class, 3));
        }

        @Test
        @DisplayName("无效 code 返回 false")
        void isValidCode_false() {
            assertFalse(EnumUtil.isValidCode(TestEnum.class, 99));
        }

        @Test
        @DisplayName("null code 返回 false")
        void isValidCode_null() {
            assertFalse(EnumUtil.isValidCode(TestEnum.class, null));
        }
    }

    @Nested
    @DisplayName("getDescription 测试")
    class GetDescriptionTests {

        @Test
        @DisplayName("通过 code 获取描述（有默认值）")
        void getDescription_withDefault() {
            assertEquals("Alpha", EnumUtil.getDescription(TestEnum.class, 1, "未知"));
        }

        @Test
        @DisplayName("code 不存在返回默认值")
        void getDescription_default() {
            assertEquals("未知", EnumUtil.getDescription(TestEnum.class, 99, "未知"));
        }

        @Test
        @DisplayName("通过 code 获取描述（无默认值）")
        void getDescription_noDefault() {
            assertEquals("Alpha", EnumUtil.getDescription(TestEnum.class, 1));
            assertEquals("Beta", EnumUtil.getDescription(TestEnum.class, 2));
        }

        @Test
        @DisplayName("code 不存在且无默认值返回 code 字符串")
        void getDescription_noDefault_notFound() {
            assertEquals("99", EnumUtil.getDescription(TestEnum.class, 99));
        }

        @Test
        @DisplayName("null code 且无默认值返回 \"null\"")
        void getDescription_noDefault_nullCode() {
            assertEquals("null", EnumUtil.getDescription(TestEnum.class, (Integer) null));
        }
    }

    // ======================== 转换操作 ========================

    @Nested
    @DisplayName("toMap 测试")
    class ToMapTests {

        @Test
        @DisplayName("正常转换为 code -> description Map")
        void toMap_normal() {
            Map<Integer, String> map = EnumUtil.toMap(TestEnum.class);
            assertEquals(3, map.size());
            assertEquals("Alpha", map.get(1));
            assertEquals("Beta", map.get(2));
            assertEquals("Gamma", map.get(3));
        }

        @Test
        @DisplayName("null 枚举类返回空 Map")
        void toMap_null() {
            assertEquals(Map.of(), EnumUtil.toMap(null));
        }
    }

    @Nested
    @DisplayName("toEnumMap 测试")
    class ToEnumMapTests {

        @Test
        @DisplayName("正常转换为 code -> 枚举 Map")
        void toEnumMap_normal() {
            Map<Integer, TestEnum> map = EnumUtil.toEnumMap(TestEnum.class);
            assertEquals(3, map.size());
            assertEquals(TestEnum.A, map.get(1));
            assertEquals(TestEnum.B, map.get(2));
            assertEquals(TestEnum.C, map.get(3));
        }

        @Test
        @DisplayName("null 枚举类返回空 Map")
        void toEnumMap_null() {
            assertEquals(Map.of(), EnumUtil.toEnumMap(null));
        }
    }

    @Nested
    @DisplayName("toList / toBaseEnumList 测试")
    class ToListTests {

        @Test
        @DisplayName("toList 返回所有枚举值")
        void toList_normal() {
            List<TestEnum> list = EnumUtil.toList(TestEnum.class);
            assertEquals(3, list.size());
            assertEquals(List.of(TestEnum.A, TestEnum.B, TestEnum.C), list);
        }

        @Test
        @DisplayName("toList - null 枚举类返回空列表")
        void toList_null() {
            assertEquals(List.of(), EnumUtil.toList(null));
        }

        @Test
        @DisplayName("toBaseEnumList 等同于 toList")
        void toBaseEnumList_equalsToList() {
            assertEquals(EnumUtil.toList(TestEnum.class), EnumUtil.toBaseEnumList(TestEnum.class));
        }

        @Test
        @DisplayName("toBaseEnumList - null 枚举类返回空列表")
        void toBaseEnumList_null() {
            assertEquals(List.of(), EnumUtil.toBaseEnumList(null));
        }
    }

    @Nested
    @DisplayName("codeList / descriptionList 测试")
    class CodeDescriptionListTests {

        @Test
        @DisplayName("codeList 返回所有 code")
        void codeList_normal() {
            List<Integer> codes = EnumUtil.codeList(TestEnum.class);
            assertEquals(List.of(1, 2, 3), codes);
        }

        @Test
        @DisplayName("codeList - null 枚举类返回空列表")
        void codeList_null() {
            assertEquals(List.of(), EnumUtil.codeList(null));
        }

        @Test
        @DisplayName("descriptionList 返回所有 description")
        void descriptionList_normal() {
            List<String> descriptions = EnumUtil.descriptionList(TestEnum.class);
            assertEquals(List.of("Alpha", "Beta", "Gamma"), descriptions);
        }

        @Test
        @DisplayName("descriptionList - null 枚举类返回空列表")
        void descriptionList_null() {
            assertEquals(List.of(), EnumUtil.descriptionList(null));
        }
    }

    @Nested
    @DisplayName("toOptions 测试")
    class ToOptionsTests {

        @Test
        @DisplayName("toOptions 返回 Pair 列表")
        void toOptions_normal() {
            List<Pair<Integer, String>> options = EnumUtil.toOptions(TestEnum.class);
            assertEquals(3, options.size());
            assertEquals(1, options.get(0).left());
            assertEquals("Alpha", options.get(0).right());
            assertEquals(2, options.get(1).left());
            assertEquals("Beta", options.get(1).right());
            assertEquals(3, options.get(2).left());
            assertEquals("Gamma", options.get(2).right());
        }

        @Test
        @DisplayName("toOptions - null 枚举类返回空列表")
        void toOptions_null() {
            assertEquals(List.of(), EnumUtil.toOptions(null));
        }
    }

    // ======================== 普通枚举操作 ========================

    @Nested
    @DisplayName("findByName 测试")
    class FindByNameTests {

        @Test
        @DisplayName("通过名称找到枚举（精确匹配）")
        void findByName_found() {
            assertEquals(TestEnum.A, EnumUtil.findByName(TestEnum.class, "A"));
        }

        @Test
        @DisplayName("忽略大小写")
        void findByName_ignoreCase() {
            assertEquals(TestEnum.A, EnumUtil.findByName(TestEnum.class, "a"));
            assertEquals(TestEnum.B, EnumUtil.findByName(TestEnum.class, "b"));
            assertEquals(TestEnum.C, EnumUtil.findByName(TestEnum.class, "C"));
        }

        @Test
        @DisplayName("名称前后空格自动 trim")
        void findByName_trim() {
            assertEquals(TestEnum.A, EnumUtil.findByName(TestEnum.class, "  A  "));
        }

        @Test
        @DisplayName("不存在返回 null")
        void findByName_notFound() {
            assertNull(EnumUtil.findByName(TestEnum.class, "D"));
        }

        @Test
        @DisplayName("null 枚举类返回 null")
        void findByName_nullClass() {
            assertNull(EnumUtil.findByName(null, "A"));
        }

        @Test
        @DisplayName("null name 返回 null")
        void findByName_nullName() {
            assertNull(EnumUtil.findByName(TestEnum.class, null));
        }
    }

    @Nested
    @DisplayName("isValidName 测试")
    class IsValidNameTests {

        @Test
        @DisplayName("有效名称返回 true")
        void isValidName_true() {
            assertTrue(EnumUtil.isValidName(TestEnum.class, "A"));
            assertTrue(EnumUtil.isValidName(TestEnum.class, "a"));
        }

        @Test
        @DisplayName("无效名称返回 false")
        void isValidName_false() {
            assertFalse(EnumUtil.isValidName(TestEnum.class, "D"));
        }
    }

    @Nested
    @DisplayName("filter 测试")
    class FilterTests {

        @Test
        @DisplayName("按条件过滤枚举")
        void filter_normal() {
            List<TestEnum> result = EnumUtil.filter(TestEnum.class, e -> e.getCode() > 1);
            assertEquals(List.of(TestEnum.B, TestEnum.C), result);
        }

        @Test
        @DisplayName("无匹配返回空列表")
        void filter_noMatch() {
            List<TestEnum> result = EnumUtil.filter(TestEnum.class, e -> e.getCode() > 10);
            assertEquals(List.of(), result);
        }

        @Test
        @DisplayName("null 枚举类返回空列表")
        void filter_nullClass() {
            assertEquals(List.of(), EnumUtil.filter(null, e -> true));
        }

        @Test
        @DisplayName("null predicate 返回空列表")
        void filter_nullPredicate() {
            assertEquals(List.of(), EnumUtil.filter(TestEnum.class, null));
        }
    }

    @Nested
    @DisplayName("containsName 测试")
    class ContainsNameTests {

        @Test
        @DisplayName("包含名称返回 true")
        void containsName_true() {
            assertTrue(EnumUtil.containsName(TestEnum.class, "A"));
            assertTrue(EnumUtil.containsName(TestEnum.class, "a"));
        }

        @Test
        @DisplayName("不包含名称返回 false")
        void containsName_false() {
            assertFalse(EnumUtil.containsName(TestEnum.class, "D"));
            assertFalse(EnumUtil.containsName(TestEnum.class, null));
        }

        @Test
        @DisplayName("null 枚举类返回 false")
        void containsName_nullClass() {
            assertFalse(EnumUtil.containsName(null, "A"));
        }
    }
}
