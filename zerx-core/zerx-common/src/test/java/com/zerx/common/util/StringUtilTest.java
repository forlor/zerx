package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link StringUtil} 单元测试
 */
@DisplayName("StringUtil 字符串工具类测试")
class StringUtilTest {

    // ======================== EMPTY 常量 ========================

    @Test
    @DisplayName("EMPTY 常量应为空字符串")
    void emptyConstant() {
        assertEquals("", StringUtil.EMPTY);
        assertTrue(StringUtil.EMPTY.isEmpty());
    }

    // ======================== 判空相关 ========================

    @Nested
    @DisplayName("isBlank / isNotBlank 测试")
    class IsBlankTests {

        @Test
        @DisplayName("null 应为 blank")
        void isBlank_null() {
            assertTrue(StringUtil.isBlank(null));
        }

        @Test
        @DisplayName("空字符串应为 blank")
        void isBlank_empty() {
            assertTrue(StringUtil.isBlank(""));
        }

        @Test
        @DisplayName("纯空格应为 blank")
        void isBlank_whitespace() {
            assertTrue(StringUtil.isBlank("   "));
            assertTrue(StringUtil.isBlank("\t"));
            assertTrue(StringUtil.isBlank("\n"));
        }

        @Test
        @DisplayName("正常字符串不应为 blank")
        void isBlank_normal() {
            assertFalse(StringUtil.isBlank("hello"));
            assertFalse(StringUtil.isBlank(" hello "));
        }

        @Test
        @DisplayName("isNotBlank 应与 isBlank 相反")
        void isNotBlank() {
            assertTrue(StringUtil.isNotBlank("hello"));
            assertFalse(StringUtil.isNotBlank(null));
            assertFalse(StringUtil.isNotBlank(""));
            assertFalse(StringUtil.isNotBlank("  "));
        }
    }

    @Nested
    @DisplayName("isEmpty 测试")
    class IsEmptyTests {

        @Test
        @DisplayName("null 应为 empty")
        void isEmpty_null() {
            assertTrue(StringUtil.isEmpty(null));
        }

        @Test
        @DisplayName("空字符串应为 empty")
        void isEmpty_empty() {
            assertTrue(StringUtil.isEmpty(""));
        }

        @Test
        @DisplayName("空格字符串不应为 empty（与 isBlank 不同，不做 trim）")
        void isEmpty_whitespace() {
            assertFalse(StringUtil.isEmpty("  "));
        }

        @Test
        @DisplayName("正常字符串不应为 empty")
        void isEmpty_normal() {
            assertFalse(StringUtil.isEmpty("hello"));
        }
    }

    @Nested
    @DisplayName("isAllBlank / isAnyBlank / isNoneBlank 测试")
    class MultiBlankTests {

        @Test
        @DisplayName("isAllBlank - 全部为空返回 true")
        void isAllBlank_allBlank() {
            assertTrue(StringUtil.isAllBlank(null, "", "  "));
        }

        @Test
        @DisplayName("isAllBlank - 数组为 null 返回 true")
        void isAllBlank_nullArray() {
            assertTrue(StringUtil.isAllBlank((String[]) null));
        }

        @Test
        @DisplayName("isAllBlank - 有非空元素返回 false")
        void isAllBlank_hasNonBlank() {
            assertFalse(StringUtil.isAllBlank("", "hello"));
        }

        @Test
        @DisplayName("isAllBlank - 空数组返回 true")
        void isAllBlank_emptyArray() {
            assertTrue(StringUtil.isAllBlank());
        }

        @Test
        @DisplayName("isAnyBlank - 任一为空返回 true")
        void isAnyBlank_hasBlank() {
            assertTrue(StringUtil.isAnyBlank("hello", "", "world"));
        }

        @Test
        @DisplayName("isAnyBlank - 数组为 null 返回 true")
        void isAnyBlank_nullArray() {
            assertTrue(StringUtil.isAnyBlank((String[]) null));
        }

        @Test
        @DisplayName("isAnyBlank - 全部非空返回 false")
        void isAnyBlank_allNotBlank() {
            assertFalse(StringUtil.isAnyBlank("hello", "world"));
        }

        @Test
        @DisplayName("isNoneBlank - 全部非空返回 true")
        void isNoneBlank_allNotBlank() {
            assertTrue(StringUtil.isNoneBlank("hello", "world"));
        }

        @Test
        @DisplayName("isNoneBlank - 有空元素返回 false")
        void isNoneBlank_hasBlank() {
            assertFalse(StringUtil.isNoneBlank("hello", ""));
        }

        @Test
        @DisplayName("isNoneBlank - null 数组返回 false")
        void isNoneBlank_nullArray() {
            assertFalse(StringUtil.isNoneBlank((String[]) null));
        }
    }

    // ======================== 裁剪与清理 ========================

    @Nested
    @DisplayName("defaultIfBlank 测试")
    class DefaultIfBlankTests {

        @Test
        @DisplayName("null 返回默认值")
        void defaultIfBlank_null() {
            assertEquals("default", StringUtil.defaultIfBlank(null, "default"));
        }

        @Test
        @DisplayName("空字符串返回默认值")
        void defaultIfBlank_empty() {
            assertEquals("default", StringUtil.defaultIfBlank("", "default"));
        }

        @Test
        @DisplayName("纯空格返回默认值")
        void defaultIfBlank_whitespace() {
            assertEquals("default", StringUtil.defaultIfBlank("  ", "default"));
        }

        @Test
        @DisplayName("非空字符串返回原值")
        void defaultIfBlank_normal() {
            assertEquals("hello", StringUtil.defaultIfBlank("hello", "default"));
        }

        @Test
        @DisplayName("默认值也可以为 null")
        void defaultIfBlank_defaultNull() {
            assertNull(StringUtil.defaultIfBlank(null, null));
        }
    }

    @Nested
    @DisplayName("truncate 测试")
    class TruncateTests {

        @Test
        @DisplayName("null 返回 null")
        void truncate_null() {
            assertNull(StringUtil.truncate(null, 5));
        }

        @Test
        @DisplayName("字符串长度小于最大长度，不截断")
        void truncate_shorter() {
            assertEquals("hi", StringUtil.truncate("hi", 5));
        }

        @Test
        @DisplayName("字符串长度等于最大长度，不截断")
        void truncate_equal() {
            assertEquals("hello", StringUtil.truncate("hello", 5));
        }

        @Test
        @DisplayName("正常截断并加省略号")
        void truncate_normal() {
            assertEquals("hel...", StringUtil.truncate("hello world", 6));
        }

        @Test
        @DisplayName("maxLength <= 3 时不加省略号，直接截断")
        void truncate_shortMax() {
            assertEquals("he", StringUtil.truncate("hello", 2));
            assertEquals("h", StringUtil.truncate("hello", 1));
            assertEquals("", StringUtil.truncate("hello", 0));
        }

        @Test
        @DisplayName("maxLength 为负数时当作 0 处理")
        void truncate_negative() {
            assertEquals("", StringUtil.truncate("hello", -1));
        }

        @Test
        @DisplayName("maxLength 为 3 时截取3个字符")
        void truncate_exactlyThree() {
            assertEquals("hel", StringUtil.truncate("hello world", 3));
        }
    }

    @Nested
    @DisplayName("cleanInvisibleChars 测试")
    class CleanInvisibleCharsTests {

        @Test
        @DisplayName("null 返回 null")
        void cleanInvisibleChars_null() {
            assertNull(StringUtil.cleanInvisibleChars(null));
        }

        @Test
        @DisplayName("清除零宽空格 (\\u200B)")
        void cleanInvisibleChars_zeroWidthSpace() {
            assertEquals("hello", StringUtil.cleanInvisibleChars("hel\u200Blo"));
        }

        @Test
        @DisplayName("清除零宽不连字符 (\\u200C)")
        void cleanInvisibleChars_zeroWidthNonJoiner() {
            assertEquals("hello", StringUtil.cleanInvisibleChars("hel\u200Clo"));
        }

        @Test
        @DisplayName("清除零宽连字符 (\\u200D)")
        void cleanInvisibleChars_zeroWidthJoiner() {
            assertEquals("hello", StringUtil.cleanInvisibleChars("hel\u200Dlo"));
        }

        @Test
        @DisplayName("清除 BOM (\\uFEFF)")
        void cleanInvisibleChars_bom() {
            assertEquals("hello", StringUtil.cleanInvisibleChars("\uFEFFhello"));
        }

        @Test
        @DisplayName("清除软连字符 (\\u00AD)")
        void cleanInvisibleChars_softHyphen() {
            assertEquals("hello", StringUtil.cleanInvisibleChars("hel\u00ADlo"));
        }

        @Test
        @DisplayName("清除多种不可见字符")
        void cleanInvisibleChars_mixed() {
            assertEquals("abc", StringUtil.cleanInvisibleChars("\u200B\uFEFFabc\u200D\u00AD"));
        }

        @Test
        @DisplayName("无可见不可见字符，保持不变")
        void cleanInvisibleChars_normal() {
            assertEquals("hello world", StringUtil.cleanInvisibleChars("hello world"));
        }

        @Test
        @DisplayName("空字符串保持不变")
        void cleanInvisibleChars_empty() {
            assertEquals("", StringUtil.cleanInvisibleChars(""));
        }
    }

    @Nested
    @DisplayName("removePrefix / removeSuffix 测试")
    class RemovePrefixSuffixTests {

        @Test
        @DisplayName("removePrefix - null 输入返回原值")
        void removePrefix_nullStr() {
            assertNull(StringUtil.removePrefix(null, "pre"));
        }

        @Test
        @DisplayName("removePrefix - null 前缀返回原值")
        void removePrefix_nullPrefix() {
            assertEquals("hello", StringUtil.removePrefix("hello", null));
        }

        @Test
        @DisplayName("removePrefix - 大小写不敏感移除")
        void removePrefix_caseInsensitive() {
            // removePrefix is case-insensitive but does NOT lowercase the result
            assertEquals("FIX", StringUtil.removePrefix("PREFIX", "pre"));
            assertEquals("fix", StringUtil.removePrefix("prefix", "PRE"));
            assertEquals("fix", StringUtil.removePrefix("prefix", "Pre"));
        }

        @Test
        @DisplayName("removePrefix - 不匹配时返回原值")
        void removePrefix_noMatch() {
            assertEquals("hello", StringUtil.removePrefix("hello", "xyz"));
        }

        @Test
        @DisplayName("removePrefix - 空前缀返回原值")
        void removePrefix_empty() {
            assertEquals("hello", StringUtil.removePrefix("hello", ""));
        }

        @Test
        @DisplayName("removeSuffix - null 输入返回原值")
        void removeSuffix_nullStr() {
            assertNull(StringUtil.removeSuffix(null, "fix"));
        }

        @Test
        @DisplayName("removeSuffix - null 后缀返回原值")
        void removeSuffix_nullSuffix() {
            assertEquals("hello", StringUtil.removeSuffix("hello", null));
        }

        @Test
        @DisplayName("removeSuffix - 大小写不敏感移除")
        void removeSuffix_caseInsensitive() {
            // removeSuffix is case-insensitive but does NOT lowercase the result
            assertEquals("PRE", StringUtil.removeSuffix("PREFIX", "fix"));
            assertEquals("pre", StringUtil.removeSuffix("prefix", "FIX"));
            assertEquals("pre", StringUtil.removeSuffix("prefix", "Fix"));
        }

        @Test
        @DisplayName("removeSuffix - 不匹配时返回原值")
        void removeSuffix_noMatch() {
            assertEquals("hello", StringUtil.removeSuffix("hello", "xyz"));
        }

        @Test
        @DisplayName("removeSuffix - 空后缀返回原值")
        void removeSuffix_empty() {
            assertEquals("hello", StringUtil.removeSuffix("hello", ""));
        }
    }

    // ======================== 驼峰转换 ========================

    @Nested
    @DisplayName("camelToUnderscore 测试")
    class CamelToUnderscoreTests {

        @Test
        @DisplayName("null 返回 null")
        void camelToUnderscore_null() {
            assertNull(StringUtil.camelToUnderscore(null));
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void camelToUnderscore_empty() {
            assertEquals("", StringUtil.camelToUnderscore(""));
        }

        @Test
        @DisplayName("正常驼峰转下划线")
        void camelToUnderscore_normal() {
            assertEquals("user_name", StringUtil.camelToUnderscore("userName"));
        }

        @Test
        @DisplayName("首字母大写的驼峰转下划线")
        void camelToUnderscore_startsWithUpper() {
            assertEquals("http_servlet_request", StringUtil.camelToUnderscore("HttpServletRequest"));
        }

        @Test
        @DisplayName("连续大写字母处理")
        void camelToUnderscore_consecutiveUpper() {
            assertEquals("u_r_l", StringUtil.camelToUnderscore("URL"));
        }

        @Test
        @DisplayName("纯小写字母保持不变")
        void camelToUnderscore_allLower() {
            assertEquals("hello", StringUtil.camelToUnderscore("hello"));
        }

        @Test
        @DisplayName("单字符")
        void camelToUnderscore_single() {
            assertEquals("a", StringUtil.camelToUnderscore("a"));
        }
    }

    @Nested
    @DisplayName("underscoreToCamel 测试")
    class UnderscoreToCamelTests {

        @Test
        @DisplayName("null 返回 null")
        void underscoreToCamel_null() {
            assertNull(StringUtil.underscoreToCamel(null));
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void underscoreToCamel_empty() {
            assertEquals("", StringUtil.underscoreToCamel(""));
        }

        @Test
        @DisplayName("正常下划线转驼峰")
        void underscoreToCamel_normal() {
            assertEquals("userName", StringUtil.underscoreToCamel("user_name"));
        }

        @Test
        @DisplayName("以下划线开头")
        void underscoreToCamel_startsWithUnderscore() {
            assertEquals("PrivateField", StringUtil.underscoreToCamel("_private_field"));
        }

        @Test
        @DisplayName("连续下划线")
        void underscoreToCamel_consecutiveUnderscore() {
            assertEquals("helloWorld", StringUtil.underscoreToCamel("hello__world"));
        }

        @Test
        @DisplayName("纯小写保持不变")
        void underscoreToCamel_allLower() {
            assertEquals("hello", StringUtil.underscoreToCamel("hello"));
        }

        @Test
        @DisplayName("下划线结尾")
        void underscoreToCamel_endsWithUnderscore() {
            assertEquals("hello", StringUtil.underscoreToCamel("hello_"));
        }

        @Test
        @DisplayName("双向转换一致性")
        void underscoreToCamel_roundTrip() {
            String original = "userName";
            assertEquals(original, StringUtil.underscoreToCamel(StringUtil.camelToUnderscore(original)));
        }
    }

    @Nested
    @DisplayName("capitalize / uncapitalize 测试")
    class CapitalizeTests {

        @Test
        @DisplayName("capitalize - null 返回 null")
        void capitalize_null() {
            assertNull(StringUtil.capitalize(null));
        }

        @Test
        @DisplayName("capitalize - 空字符串返回空字符串")
        void capitalize_empty() {
            assertEquals("", StringUtil.capitalize(""));
        }

        @Test
        @DisplayName("capitalize - 小写首字母大写")
        void capitalize_normal() {
            assertEquals("Hello", StringUtil.capitalize("hello"));
        }

        @Test
        @DisplayName("capitalize - 已经大写保持不变")
        void capitalize_alreadyUpper() {
            assertEquals("Hello", StringUtil.capitalize("Hello"));
        }

        @Test
        @DisplayName("uncapitalize - null 返回 null")
        void uncapitalize_null() {
            assertNull(StringUtil.uncapitalize(null));
        }

        @Test
        @DisplayName("uncapitalize - 空字符串返回空字符串")
        void uncapitalize_empty() {
            assertEquals("", StringUtil.uncapitalize(""));
        }

        @Test
        @DisplayName("uncapitalize - 大写首字母小写")
        void uncapitalize_normal() {
            assertEquals("hello", StringUtil.uncapitalize("Hello"));
        }

        @Test
        @DisplayName("uncapitalize - 已经小写保持不变")
        void uncapitalize_alreadyLower() {
            assertEquals("hello", StringUtil.uncapitalize("hello"));
        }

        @Test
        @DisplayName("capitalize 和 uncapitalize 互逆")
        void capitalize_uncapitalize_roundTrip() {
            assertEquals("hello", StringUtil.uncapitalize(StringUtil.capitalize("hello")));
            assertEquals("Hello", StringUtil.capitalize(StringUtil.uncapitalize("Hello")));
        }
    }

    // ======================== 正则匹配 ========================

    @Nested
    @DisplayName("matches 测试")
    class MatchesTests {

        @Test
        @DisplayName("null 字符串返回 false")
        void matches_nullStr() {
            assertFalse(StringUtil.matches(null, ".*"));
        }

        @Test
        @DisplayName("null 正则返回 false")
        void matches_nullRegex() {
            assertFalse(StringUtil.matches("hello", null));
        }

        @Test
        @DisplayName("正常匹配")
        void matches_normal() {
            assertTrue(StringUtil.matches("hello", "h.*"));
            assertFalse(StringUtil.matches("hello", "x.*"));
        }

        @Test
        @DisplayName("完全匹配正则")
        void matches_exact() {
            assertTrue(StringUtil.matches("12345", "\\d{5}"));
            assertFalse(StringUtil.matches("1234", "\\d{5}"));
        }
    }

    @Nested
    @DisplayName("isChinese 测试")
    class IsChineseTests {

        @Test
        @DisplayName("null 返回 false")
        void isChinese_null() {
            assertFalse(StringUtil.isChinese(null));
        }

        @Test
        @DisplayName("空字符串返回 false")
        void isChinese_empty() {
            assertFalse(StringUtil.isChinese(""));
        }

        @Test
        @DisplayName("纯中文返回 true")
        void isChinese_pureChinese() {
            assertTrue(StringUtil.isChinese("你好世界"));
        }

        @Test
        @DisplayName("中英文混合返回 false")
        void isChinese_mixed() {
            assertFalse(StringUtil.isChinese("hello你好"));
        }

        @Test
        @DisplayName("纯英文返回 false")
        void isChinese_english() {
            assertFalse(StringUtil.isChinese("hello"));
        }

        @Test
        @DisplayName("中文前后有空格（trim 后判断）")
        void isChinese_withSpaces() {
            assertTrue(StringUtil.isChinese(" 你好 "));
        }
    }

    @Nested
    @DisplayName("isMobile 测试")
    class IsMobileTests {

        @Test
        @DisplayName("null 返回 false")
        void isMobile_null() {
            assertFalse(StringUtil.isMobile(null));
        }

        @Test
        @DisplayName("空字符串返回 false")
        void isMobile_empty() {
            assertFalse(StringUtil.isMobile(""));
        }

        @Test
        @DisplayName("有效手机号")
        void isMobile_valid() {
            assertTrue(StringUtil.isMobile("13800138000"));
            assertTrue(StringUtil.isMobile("13912345678"));
            assertTrue(StringUtil.isMobile("19999999999"));
        }

        @Test
        @DisplayName("无效手机号 - 不以 1 开头")
        void isMobile_invalidStart() {
            assertFalse(StringUtil.isMobile("23800138000"));
        }

        @Test
        @DisplayName("无效手机号 - 位数不对")
        void isMobile_invalidLength() {
            assertFalse(StringUtil.isMobile("1380013800"));
            assertFalse(StringUtil.isMobile("138001380001"));
        }

        @Test
        @DisplayName("无效手机号 - 含非数字")
        void isMobile_nonNumeric() {
            assertFalse(StringUtil.isMobile("1380013800a"));
        }

        @Test
        @DisplayName("手机号前后有空格（trim 后判断）")
        void isMobile_withSpaces() {
            assertTrue(StringUtil.isMobile(" 13800138000 "));
        }
    }

    @Nested
    @DisplayName("isEmail 测试")
    class IsEmailTests {

        @Test
        @DisplayName("null 返回 false")
        void isEmail_null() {
            assertFalse(StringUtil.isEmail(null));
        }

        @Test
        @DisplayName("空字符串返回 false")
        void isEmail_empty() {
            assertFalse(StringUtil.isEmail(""));
        }

        @Test
        @DisplayName("有效邮箱")
        void isEmail_valid() {
            assertTrue(StringUtil.isEmail("test@example.com"));
            assertTrue(StringUtil.isEmail("user.name+tag@domain.co"));
        }

        @Test
        @DisplayName("无效邮箱 - 缺少 @")
        void isEmail_noAt() {
            assertFalse(StringUtil.isEmail("testexample.com"));
        }

        @Test
        @DisplayName("无效邮箱 - 缺少域名后缀")
        void isEmail_noTld() {
            assertFalse(StringUtil.isEmail("test@domain"));
        }

        @Test
        @DisplayName("无效邮箱 - 空格")
        void isEmail_withSpaces() {
            assertFalse(StringUtil.isEmail("test @domain.com"));
        }
    }

    // ======================== 编码转换 ========================

    @Nested
    @DisplayName("toUtf8Bytes / fromUtf8Bytes 测试")
    class Utf8Tests {

        @Test
        @DisplayName("toUtf8Bytes - null 返回空数组")
        void toUtf8Bytes_null() {
            assertArrayEquals(new byte[0], StringUtil.toUtf8Bytes(null));
        }

        @Test
        @DisplayName("toUtf8Bytes - 正常转换")
        void toUtf8Bytes_normal() {
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), StringUtil.toUtf8Bytes("hello"));
        }

        @Test
        @DisplayName("toUtf8Bytes - 中文字符")
        void toUtf8Bytes_chinese() {
            String str = "你好";
            assertArrayEquals(str.getBytes(StandardCharsets.UTF_8), StringUtil.toUtf8Bytes(str));
        }

        @Test
        @DisplayName("fromUtf8Bytes - null 返回 null")
        void fromUtf8Bytes_null() {
            assertNull(StringUtil.fromUtf8Bytes(null));
        }

        @Test
        @DisplayName("fromUtf8Bytes - 正常转换")
        void fromUtf8Bytes_normal() {
            assertEquals("hello", StringUtil.fromUtf8Bytes("hello".getBytes(StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("双向转换一致性")
        void utf8RoundTrip() {
            String original = "Hello 世界!";
            assertEquals(original, StringUtil.fromUtf8Bytes(StringUtil.toUtf8Bytes(original)));
        }

        @Test
        @DisplayName("fromUtf8Bytes - 空字节数组返回空字符串")
        void fromUtf8Bytes_empty() {
            assertEquals("", StringUtil.fromUtf8Bytes(new byte[0]));
        }
    }

    @Nested
    @DisplayName("toBytes 测试")
    class ToBytesTests {

        @Test
        @DisplayName("toBytes - null 返回空数组")
        void toBytes_null() {
            assertArrayEquals(new byte[0], StringUtil.toBytes(null, StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("toBytes - 指定编码转换")
        void toBytes_charset() {
            String str = "hello";
            assertArrayEquals(str.getBytes(StandardCharsets.ISO_8859_1),
                    StringUtil.toBytes(str, StandardCharsets.ISO_8859_1));
        }
    }

    @Nested
    @DisplayName("toHexString 测试")
    class ToHexStringTests {

        @Test
        @DisplayName("null 返回 null")
        void toHexString_null() {
            assertNull(StringUtil.toHexString(null));
        }

        @Test
        @DisplayName("正常字符串转十六进制")
        void toHexString_normal() {
            assertEquals("48656c6c6f", StringUtil.toHexString("Hello"));
        }

        @Test
        @DisplayName("空字符串转十六进制")
        void toHexString_empty() {
            assertEquals("", StringUtil.toHexString(""));
        }
    }

    // ======================== 重复与填充 ========================

    @Nested
    @DisplayName("repeat 测试")
    class RepeatTests {

        @Test
        @DisplayName("null 返回 null")
        void repeat_null() {
            assertNull(StringUtil.repeat(null, 3));
        }

        @Test
        @DisplayName("count <= 0 返回空字符串")
        void repeat_zeroOrNegative() {
            assertEquals("", StringUtil.repeat("a", 0));
            assertEquals("", StringUtil.repeat("a", -1));
        }

        @Test
        @DisplayName("正常重复")
        void repeat_normal() {
            assertEquals("aaa", StringUtil.repeat("a", 3));
            assertEquals("abab", StringUtil.repeat("ab", 2));
        }

        @Test
        @DisplayName("重复空字符串")
        void repeat_emptyString() {
            assertEquals("", StringUtil.repeat("", 5));
        }
    }

    @Nested
    @DisplayName("reverse 测试")
    class ReverseTests {

        @Test
        @DisplayName("null 返回 null")
        void reverse_null() {
            assertNull(StringUtil.reverse(null));
        }

        @Test
        @DisplayName("正常反转")
        void reverse_normal() {
            assertEquals("olleh", StringUtil.reverse("hello"));
        }

        @Test
        @DisplayName("空字符串返回空字符串")
        void reverse_empty() {
            assertEquals("", StringUtil.reverse(""));
        }

        @Test
        @DisplayName("单字符返回自身")
        void reverse_single() {
            assertEquals("a", StringUtil.reverse("a"));
        }

        @Test
        @DisplayName("回文字符串反转不变")
        void reverse_palindrome() {
            assertEquals("abba", StringUtil.reverse("abba"));
        }
    }

    // ======================== 分割与连接 ========================

    @Nested
    @DisplayName("splitTrim 测试")
    class SplitTrimTests {

        @Test
        @DisplayName("null 返回空数组")
        void splitTrim_null() {
            assertArrayEquals(new String[0], StringUtil.splitTrim(null, ","));
        }

        @Test
        @DisplayName("空字符串返回空数组")
        void splitTrim_empty() {
            assertArrayEquals(new String[0], StringUtil.splitTrim("", ","));
        }

        @Test
        @DisplayName("正常分割并去除空白")
        void splitTrim_normal() {
            assertArrayEquals(new String[]{"a", "b", "c"}, StringUtil.splitTrim("a, b , c", ","));
        }

        @Test
        @DisplayName("分割后元素含空格被 trim")
        void splitTrim_withSpaces() {
            assertArrayEquals(new String[]{"hello", "world"}, StringUtil.splitTrim(" hello | world ", "\\|"));
        }
    }

    @Nested
    @DisplayName("join 测试")
    class JoinTests {

        @Test
        @DisplayName("null 数组返回空字符串")
        void join_null() {
            assertEquals("", StringUtil.join(",", (String[]) null));
        }

        @Test
        @DisplayName("空数组返回空字符串")
        void join_empty() {
            assertEquals("", StringUtil.join(",", new String[0]));
        }

        @Test
        @DisplayName("正常连接")
        void join_normal() {
            assertEquals("a,b,c", StringUtil.join(",", "a", "b", "c"));
        }

        @Test
        @DisplayName("单个元素")
        void join_single() {
            assertEquals("a", StringUtil.join(",", "a"));
        }

        @Test
        @DisplayName("空分隔符")
        void join_emptySeparator() {
            assertEquals("abc", StringUtil.join("", "a", "b", "c"));
        }
    }
}
