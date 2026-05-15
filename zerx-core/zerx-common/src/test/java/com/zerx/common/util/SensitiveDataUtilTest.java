package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SensitiveDataUtil} 单元测试
 */
@DisplayName("SensitiveDataUtil - 数据脱敏工具类测试")
class SensitiveDataTest {

    // ======================== 通用脱敏 ========================

    @Test
    @DisplayName("mask(str, prefixLen, suffixLen, maskChar) - 正常脱敏")
    void mask_normal() {
        assertEquals("he**o", SensitiveDataUtil.mask("hello", 2, 1, '*'));
    }

    @Test
    @DisplayName("mask - 保留位数之和 >= 总长度时不脱敏")
    void mask_noMaskNeeded() {
        assertEquals("hi", SensitiveDataUtil.mask("hi", 1, 1, '*'));
        assertEquals("a", SensitiveDataUtil.mask("a", 1, 0, '*'));
    }

    @Test
    @DisplayName("mask - null 返回 null")
    void mask_null() {
        assertNull(SensitiveDataUtil.mask(null, 2, 1, '*'));
    }

    @Test
    @DisplayName("mask - 空字符串返回空字符串")
    void mask_empty() {
        assertEquals("", SensitiveDataUtil.mask("", 2, 1, '*'));
    }

    @Test
    @DisplayName("mask - 空白字符串返回原值")
    void mask_blank() {
        assertEquals("   ", SensitiveDataUtil.mask("   ", 1, 1, '*'));
    }

    @Test
    @DisplayName("mask - 自定义脱敏字符")
    void mask_customChar() {
        assertEquals("ab##e", SensitiveDataUtil.mask("abcde", 2, 1, '#'));
        assertEquals("abXXe", SensitiveDataUtil.mask("abcde", 2, 1, 'X'));
    }

    @Test
    @DisplayName("mask - prefix=0 全部保留后缀")
    void mask_prefixZero() {
        assertEquals("****o", SensitiveDataUtil.mask("hello", 0, 1, '*'));
    }

    @Test
    @DisplayName("mask - suffix=0 全部保留前缀")
    void mask_suffixZero() {
        assertEquals("he***", SensitiveDataUtil.mask("hello", 2, 0, '*'));
    }

    @Test
    @DisplayName("mask - 长字符串脱敏")
    void mask_longString() {
        String original = "12345678901234567890";
        String masked = SensitiveDataUtil.mask(original, 3, 3, '*');
        assertEquals(20, masked.length());
        assertEquals("123", masked.substring(0, 3));
        assertEquals("890", masked.substring(17));
    }

    @Test
    @DisplayName("mask(str, prefixLen, suffixLen) - 使用默认脱敏字符")
    void mask_defaultChar() {
        assertEquals("he**o", SensitiveDataUtil.mask("hello", 2, 1));
    }

    @Test
    @DisplayName("maskKeep - 仅保留前 N 位")
    void maskKeep_normal() {
        assertEquals("12****", SensitiveDataUtil.maskKeep("123456", 2, '*'));
        assertEquals("ab###", SensitiveDataUtil.maskKeep("abcde", 2, '#'));
    }

    @Test
    @DisplayName("maskKeep - keepLen=0 全部脱敏")
    void maskKeep_zero() {
        assertEquals("*****", SensitiveDataUtil.maskKeep("abcde", 0, '*'));
    }

    @Test
    @DisplayName("maskKeep - null/空")
    void maskKeep_nullEmpty() {
        assertNull(SensitiveDataUtil.maskKeep(null, 2, '*'));
        assertEquals("", SensitiveDataUtil.maskKeep("", 2, '*'));
    }

    @Test
    @DisplayName("maskAll - 全部脱敏")
    void maskAll_normal() {
        assertEquals("******", SensitiveDataUtil.maskAll("secret", '*'));
        assertEquals("####", SensitiveDataUtil.maskAll("text", '#'));
    }

    @Test
    @DisplayName("maskAll - null/空")
    void maskAll_nullEmpty() {
        assertNull(SensitiveDataUtil.maskAll(null, '*'));
        assertEquals("", SensitiveDataUtil.maskAll("", '*'));
    }

    @Test
    @DisplayName("maskAll - 单字符")
    void maskAll_singleChar() {
        assertEquals("?", SensitiveDataUtil.maskAll("x", '?'));
    }

    // ======================== 手机号 ========================

    @Test
    @DisplayName("maskMobile - 标准手机号脱敏")
    void maskMobile_normal() {
        assertEquals("138****5678", SensitiveDataUtil.maskMobile("13812345678"));
    }

    @Test
    @DisplayName("maskMobile - 自定义脱敏字符")
    void maskMobile_customChar() {
        assertEquals("138####5678", SensitiveDataUtil.maskMobile("13812345678", '#'));
    }

    @Test
    @DisplayName("maskMobile - null 返回 null")
    void maskMobile_null() {
        assertNull(SensitiveDataUtil.maskMobile(null));
    }

    @Test
    @DisplayName("maskMobile - 空返回空")
    void maskMobile_empty() {
        assertEquals("", SensitiveDataUtil.maskMobile(""));
    }

    @Test
    @DisplayName("maskMobile - 短于 7 位不脱敏")
    void maskMobile_tooShort() {
        assertEquals("123", SensitiveDataUtil.maskMobile("123"));
    }

    // ======================== 固定电话 ========================

    @Test
    @DisplayName("maskPhone - 标准固定电话脱敏")
    void maskPhone_normal() {
        assertEquals("0755****5678", SensitiveDataUtil.maskPhone("075512345678"));
    }

    @Test
    @DisplayName("maskPhone - null 返回 null")
    void maskPhone_null() {
        assertNull(SensitiveDataUtil.maskPhone(null));
    }

    @Test
    @DisplayName("maskPhone - 空返回空")
    void maskPhone_empty() {
        assertEquals("", SensitiveDataUtil.maskPhone(""));
    }

    // ======================== 邮箱 ========================

    @Test
    @DisplayName("maskEmail - 标准邮箱脱敏")
    void maskEmail_normal() {
        String result = SensitiveDataUtil.maskEmail("testuser@example.com");
        assertEquals("t******r@example.com", result);
    }

    @Test
    @DisplayName("maskEmail - 短用户名邮箱")
    void maskEmail_shortUsername() {
        String result = SensitiveDataUtil.maskEmail("ab@example.com");
        // username "ab" length > 1, so keeps first and last char: "a" + "" + "b" = "ab"
        assertEquals("ab@example.com", result);
    }

    @Test
    @DisplayName("maskEmail - 单字符用户名")
    void maskEmail_singleCharUsername() {
        String result = SensitiveDataUtil.maskEmail("a@example.com");
        assertEquals("a***@example.com", result);
    }

    @Test
    @DisplayName("maskEmail - null 返回 null")
    void maskEmail_null() {
        assertNull(SensitiveDataUtil.maskEmail(null));
    }

    @Test
    @DisplayName("maskEmail - 空返回空")
    void maskEmail_empty() {
        assertEquals("", SensitiveDataUtil.maskEmail(""));
    }

    @Test
    @DisplayName("maskEmail - 无 @ 符号按通用规则")
    void maskEmail_noAtSign() {
        String result = SensitiveDataUtil.maskEmail("plainstring");
        // mask("plainstring", 1, 0) → prefixLen=1, suffixLen=0, maskLen=10
        assertEquals("p**********", result);
    }

    @Test
    @DisplayName("maskEmail - @ 在开头")
    void maskEmail_atStart() {
        String result = SensitiveDataUtil.maskEmail("@example.com");
        // atIndex=0, which is <= 0, so mask("@example.com", 1, 0)
        assertEquals("@***********", result);
    }

    // ======================== 身份证号 ========================

    @Test
    @DisplayName("maskIdCard - 18 位身份证号脱敏")
    void maskIdCard_normal() {
        String result = SensitiveDataUtil.maskIdCard("110101199001011234");
        assertEquals("110***********1234", result);
    }

    @Test
    @DisplayName("maskIdCard - null 返回 null")
    void maskIdCard_null() {
        assertNull(SensitiveDataUtil.maskIdCard(null));
    }

    @Test
    @DisplayName("maskIdCard - 空返回空")
    void maskIdCard_empty() {
        assertEquals("", SensitiveDataUtil.maskIdCard(""));
    }

    // ======================== 银行卡号 ========================

    @Test
    @DisplayName("maskBankCard - 标准银行卡号脱敏")
    void maskBankCard_normal() {
        String result = SensitiveDataUtil.maskBankCard("6222021234567890123");
        assertEquals("6222***********0123", result);
    }

    @Test
    @DisplayName("maskBankCard - null 返回原值")
    void maskBankCard_null() {
        assertNull(SensitiveDataUtil.maskBankCard(null));
    }

    @Test
    @DisplayName("maskBankCardFormatted - 格式化银行卡号脱敏")
    void maskBankCardFormatted_normal() {
        String result = SensitiveDataUtil.maskBankCardFormatted("6222021234567890123");
        // 19 digits: masked is "6222***********0123", then spaced every 4 chars
        assertEquals("6222 **** **** ***0 123", result);
    }

    @Test
    @DisplayName("maskBankCardFormatted - 已有空格的银行卡号")
    void maskBankCardFormatted_withSpaces() {
        String result = SensitiveDataUtil.maskBankCardFormatted("6222 0212 3456 7890 123");
        // 19 digits after cleaning: masked spaced every 4 chars
        assertEquals("6222 **** **** ***0 123", result);
    }

    @Test
    @DisplayName("maskBankCardFormatted - null 返回 null")
    void maskBankCardFormatted_null() {
        assertNull(SensitiveDataUtil.maskBankCardFormatted(null));
    }

    @Test
    @DisplayName("maskBankCardFormatted - 空返回空")
    void maskBankCardFormatted_empty() {
        assertEquals("", SensitiveDataUtil.maskBankCardFormatted(""));
    }

    @Test
    @DisplayName("maskBankCardFormatted - 短于 8 位")
    void maskBankCardFormatted_short() {
        String result = SensitiveDataUtil.maskBankCardFormatted("1234567");
        assertNotNull(result);
    }

    // ======================== 姓名 ========================

    @Test
    @DisplayName("maskName - 两个字姓名")
    void maskName_twoChars() {
        assertEquals("张*", SensitiveDataUtil.maskName("张三"));
    }

    @Test
    @DisplayName("maskName - 三个字姓名")
    void maskName_threeChars() {
        assertEquals("张**", SensitiveDataUtil.maskName("张三丰"));
    }

    @Test
    @DisplayName("maskName - 复姓")
    void maskName_complexSurname() {
        assertEquals("欧**", SensitiveDataUtil.maskName("欧阳锋"));
    }

    @Test
    @DisplayName("maskName - 单字姓名")
    void maskName_singleChar() {
        assertEquals("张*", SensitiveDataUtil.maskName("张"));
    }

    @Test
    @DisplayName("maskName - null 返回 null")
    void maskName_null() {
        assertNull(SensitiveDataUtil.maskName(null));
    }

    @Test
    @DisplayName("maskName - 空返回空")
    void maskName_empty() {
        assertEquals("", SensitiveDataUtil.maskName(""));
    }

    @Test
    @DisplayName("maskName - 英文姓名")
    void maskName_english() {
        // maskName keeps first char only, rest masked: "A" + 8 asterisks
        assertEquals("A********", SensitiveDataUtil.maskName("Alexander"));
    }

    // ======================== 地址 ========================

    @Test
    @DisplayName("maskAddress - 长地址脱敏")
    void maskAddress_long() {
        String result = SensitiveDataUtil.maskAddress("北京市朝阳区建国路88号SOHO现代城");
        assertNotNull(result);
        assertFalse(result.equals("北京市朝阳区建国路88号SOHO现代城"));
        assertTrue(result.startsWith("北京市朝阳区"));
    }

    @Test
    @DisplayName("maskAddress - 短地址")
    void maskAddress_short() {
        String result = SensitiveDataUtil.maskAddress("北京市");
        assertNotNull(result);
    }

    @Test
    @DisplayName("maskAddress - null 返回 null")
    void maskAddress_null() {
        assertNull(SensitiveDataUtil.maskAddress(null));
    }

    @Test
    @DisplayName("maskAddress - 空返回空")
    void maskAddress_empty() {
        assertEquals("", SensitiveDataUtil.maskAddress(""));
    }

    // ======================== 密码 ========================

    @Test
    @DisplayName("maskPassword - 统一显示 6 个星号")
    void maskPassword_normal() {
        assertEquals("******", SensitiveDataUtil.maskPassword("mypassword"));
        assertEquals("******", SensitiveDataUtil.maskPassword("123"));
        assertEquals("******", SensitiveDataUtil.maskPassword("verylongpassword123"));
    }

    @Test
    @DisplayName("maskPassword - null 返回 null")
    void maskPassword_null() {
        assertNull(SensitiveDataUtil.maskPassword(null));
    }

    @Test
    @DisplayName("maskPassword - 空返回空")
    void maskPassword_empty() {
        assertEquals("", SensitiveDataUtil.maskPassword(""));
    }

    // ======================== IP 地址 ========================

    @Test
    @DisplayName("maskIpv4 - 标准IPv4脱敏")
    void maskIpv4_normal() {
        assertEquals("192.168.*.*", SensitiveDataUtil.maskIpv4("192.168.1.100"));
        assertEquals("10.0.*.*", SensitiveDataUtil.maskIpv4("10.0.0.1"));
    }

    @Test
    @DisplayName("maskIpv4 - null 返回 null")
    void maskIpv4_null() {
        assertNull(SensitiveDataUtil.maskIpv4(null));
    }

    @Test
    @DisplayName("maskIpv4 - 空返回空")
    void maskIpv4_empty() {
        assertEquals("", SensitiveDataUtil.maskIpv4(""));
    }

    @Test
    @DisplayName("maskIpv4 - 非4段IP按通用规则")
    void maskIpv4_invalidFormat() {
        String result = SensitiveDataUtil.maskIpv4("invalid-ip");
        assertNotNull(result);
    }

    @Test
    @DisplayName("maskIpv6 - 标准IPv6脱敏")
    void maskIpv6_normal() {
        String result = SensitiveDataUtil.maskIpv6("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        assertEquals("2001:0db8:****:****:****:****:****:****", result);
    }

    @Test
    @DisplayName("maskIpv6 - null 返回 null")
    void maskIpv6_null() {
        assertNull(SensitiveDataUtil.maskIpv6(null));
    }

    @Test
    @DisplayName("maskIpv6 - 空返回空")
    void maskIpv6_empty() {
        assertEquals("", SensitiveDataUtil.maskIpv6(""));
    }

    @Test
    @DisplayName("maskIpv6 - 短IPv6（<=2段）按通用规则")
    void maskIpv6_short() {
        String result = SensitiveDataUtil.maskIpv6("::1");
        assertNotNull(result);
    }

    // ======================== 车牌号 ========================

    @Test
    @DisplayName("maskPlateNumber - 普通车牌号脱敏")
    void maskPlateNumber_normal() {
        assertEquals("京A***45", SensitiveDataUtil.maskPlateNumber("京A12345"));
    }

    @Test
    @DisplayName("maskPlateNumber - 新能源车牌（8位）")
    void maskPlateNumber_newEnergy() {
        // 8 chars: prefix 2 + mask(8-4=4) + suffix 2 = "京A****56"
        assertEquals("京A*****56", SensitiveDataUtil.maskPlateNumber("京AD123456"));
    }

    @Test
    @DisplayName("maskPlateNumber - 短车牌号（<=4位）")
    void maskPlateNumber_short() {
        String result = SensitiveDataUtil.maskPlateNumber("京A12");
        assertNotNull(result);
    }

    @Test
    @DisplayName("maskPlateNumber - null 返回 null")
    void maskPlateNumber_null() {
        assertNull(SensitiveDataUtil.maskPlateNumber(null));
    }

    @Test
    @DisplayName("maskPlateNumber - 空返回空")
    void maskPlateNumber_empty() {
        assertEquals("", SensitiveDataUtil.maskPlateNumber(""));
    }

    // ======================== Token ========================

    @Test
    @DisplayName("maskToken - 长Token脱敏")
    void maskToken_normal() {
        String result = SensitiveDataUtil.maskToken("abcdefghijklmnopqrstuvwxyz");
        // 26 chars: prefix 4 + mask(26-8=18) + suffix 4
        assertEquals("abcd******************wxyz", result);
    }

    @Test
    @DisplayName("maskToken - 32位Token脱敏")
    void maskToken_32chars() {
        String token = "0123456789abcdef0123456789abcdef";
        String result = SensitiveDataUtil.maskToken(token);
        // 32 chars: prefix 4 + mask(32-8=24) + suffix 4
        assertEquals("0123************************cdef", result);
    }

    @Test
    @DisplayName("maskToken - 短Token")
    void maskToken_short() {
        String result = SensitiveDataUtil.maskToken("abcd");
        assertEquals("abcd", result);
    }

    @Test
    @DisplayName("maskToken - null 返回 null")
    void maskToken_null() {
        assertNull(SensitiveDataUtil.maskToken(null));
    }

    @Test
    @DisplayName("maskToken - 空返回空")
    void maskToken_empty() {
        assertEquals("", SensitiveDataUtil.maskToken(""));
    }

    // ======================== 常量验证 ========================

    @Test
    @DisplayName("DEFAULT_MASK_CHAR - 默认脱敏字符为 '*'")
    void defaultMaskChar() {
        assertEquals('*', SensitiveDataUtil.DEFAULT_MASK_CHAR);
    }

    // ======================== 综合测试 ========================

    @Test
    @DisplayName("脱敏后字符串长度不变 - mask 方法")
    void mask_lengthPreserved() {
        String original = "abcdefghij";
        assertEquals(original.length(), SensitiveDataUtil.mask(original, 2, 2, '*').length());
    }

    @Test
    @DisplayName("多次脱敏结果一致")
    void mask_consistent() {
        String phone = "13812345678";
        String result1 = SensitiveDataUtil.maskMobile(phone);
        String result2 = SensitiveDataUtil.maskMobile(phone);
        assertEquals(result1, result2);
    }
}
