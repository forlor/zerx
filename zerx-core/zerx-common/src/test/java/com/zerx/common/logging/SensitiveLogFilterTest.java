package com.zerx.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SensitiveLogFilter 单元测试
 */
@DisplayName("SensitiveLogFilter")
class SensitiveLogFilterTest {

    @Nested
    @DisplayName("filter - 综合脱敏")
    class FilterTest {

        @Test
        @DisplayName("null 和空字符串应原样返回")
        void nullAndEmpty() {
            assertNull(SensitiveLogFilter.filter(null));
            assertEquals("", SensitiveLogFilter.filter(""));
        }

        @Test
        @DisplayName("无敏感信息的消息应原样返回")
        void noSensitiveData() {
            String input = "这是一条普通日志消息，没有敏感信息";
            assertEquals(input, SensitiveLogFilter.filter(input));
        }

        @Test
        @DisplayName("手机号应被脱敏")
        void mobile() {
            String input = "用户手机号13812345678已注册";
            String result = SensitiveLogFilter.filter(input);
            assertTrue(result.contains("138****5678"), "手机号应被脱敏: " + result);
            assertFalse(result.contains("13812345678"), "原始手机号不应出现");
        }

        @Test
        @DisplayName("邮箱应被脱敏")
        void email() {
            String input = "用户邮箱zhangsan@example.com验证通过";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("zhangsan@example.com"), "原始邮箱不应出现");
            assertTrue(result.contains("@example.com"), "域名应保留");
        }

        @Test
        @DisplayName("身份证号应被脱敏（18位）")
        void idCard18() {
            String input = "身份证号110101199001011234验证中";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("110101199001011234"), "原始身份证不应出现");
            assertTrue(result.contains("110***********1234"), "应显示脱敏格式");
        }

        @Test
        @DisplayName("银行卡号应被脱敏")
        void bankCard() {
            String input = "银行卡6222021234567890123扣款成功";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("6222021234567890123"), "原始银行卡号不应出现");
            assertTrue(result.contains("6222***********0123"), "应显示脱敏格式");
        }

        @Test
        @DisplayName("密码键值对应被脱敏")
        void password() {
            String input = "登录 password=password123 验证成功";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("password123"), "密码值不应出现: " + result);
            assertTrue(result.contains("******"), "应显示脱敏密码: " + result);
        }

        @Test
        @DisplayName("pwd= 格式也应被脱敏")
        void pwd() {
            String input = "pwd=mypassword123";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("mypassword123"));
        }

        @Test
        @DisplayName("token= 格式也应被脱敏")
        void token() {
            String input = "token=abcdef1234567890";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("abcdef1234567890"));
        }

        @Test
        @DisplayName("IPv4 应被脱敏")
        void ipv4() {
            String input = "客户端IP: 192.168.1.100";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("192.168.1.100"), "原始IP不应出现");
            assertTrue(result.contains("192.168.*.*"), "应显示脱敏格式");
        }

        @Test
        @DisplayName("多种敏感信息混合脱敏")
        void mixed() {
            String input = "用户张三手机13812345678邮箱test@example.com登录成功IP为192.168.1.100";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("13812345678"));
            assertFalse(result.contains("test@example.com"));
            assertFalse(result.contains("192.168.1.100"));
        }
    }

    @Nested
    @DisplayName("filterWithBuiltinRules - 仅内置规则")
    class BuiltinOnlyTest {

        @Test
        @DisplayName("仅使用内置规则不应用自定义规则")
        void builtinOnly() {
            SensitiveLogFilter.addRule("testRule",
                    Pattern.compile("CUSTOM_[A-Z]+"),
                    m -> "MASKED");

            try {
                String input = "手机13812345678和CUSTOM_SECRET";
                String result = SensitiveLogFilter.filterWithBuiltinRules(input);
                assertFalse(result.contains("13812345678"), "内置规则应生效");
                assertTrue(result.contains("CUSTOM_SECRET"), "自定义规则不应生效");
            } finally {
                SensitiveLogFilter.clearCustomRules();
            }
        }
    }

    @Nested
    @DisplayName("自定义规则管理")
    class CustomRuleTest {

        @Test
        @DisplayName("添加和执行自定义规则")
        void addAndApply() {
            SensitiveLogFilter.addRule("apiKey",
                    Pattern.compile("ak_[a-zA-Z0-9]{10,}"),
                    m -> "ak_***REDACTED***");

            try {
                String input = "请求携带apiKey ak_abcdefghijklmnopqrstuvwxyz123";
                String result = SensitiveLogFilter.filter(input);
                assertTrue(result.contains("ak_***REDACTED***"), "自定义规则应生效");
                assertFalse(result.contains("ak_abcdefghijklmnopqrstuvwxyz123"));
            } finally {
                SensitiveLogFilter.clearCustomRules();
            }
        }

        @Test
        @DisplayName("同名规则应覆盖旧规则")
        void overrideRule() {
            SensitiveLogFilter.addRule("test", Pattern.compile("TEST123"), m -> "MASKED_V1");
            SensitiveLogFilter.addRule("test", Pattern.compile("TEST456"), m -> "MASKED_V2");

            try {
                List<SensitiveLogFilter.Rule> rules = SensitiveLogFilter.getCustomRules();
                assertEquals(1, rules.size(), "同名规则应只有一个");
                assertEquals("test", rules.getFirst().name());

                String input = "TEST456 value";
                String result = SensitiveLogFilter.filter(input);
                assertTrue(result.contains("MASKED_V2"));
            } finally {
                SensitiveLogFilter.clearCustomRules();
            }
        }

        @Test
        @DisplayName("移除指定规则")
        void removeRule() {
            SensitiveLogFilter.addRule("temp", Pattern.compile("TEMP"), m -> "MASKED");
            boolean removed = SensitiveLogFilter.removeRule("temp");
            assertTrue(removed, "应成功移除");
            assertEquals(0, SensitiveLogFilter.getCustomRules().size());

            boolean removedAgain = SensitiveLogFilter.removeRule("temp");
            assertFalse(removedAgain, "重复移除应返回 false");
        }

        @Test
        @DisplayName("清除所有自定义规则")
        void clearCustomRules() {
            SensitiveLogFilter.addRule("r1", Pattern.compile("R1"), m -> "M1");
            SensitiveLogFilter.addRule("r2", Pattern.compile("R2"), m -> "M2");
            SensitiveLogFilter.clearCustomRules();
            assertEquals(0, SensitiveLogFilter.getCustomRules().size());
        }

        @Test
        @DisplayName("参数为 null 时应抛异常")
        void nullParams() {
            assertThrows(NullPointerException.class,
                    () -> SensitiveLogFilter.addRule(null, Pattern.compile("x"), m -> "y"));
            assertThrows(NullPointerException.class,
                    () -> SensitiveLogFilter.addRule("name", null, m -> "y"));
            assertThrows(NullPointerException.class,
                    () -> SensitiveLogFilter.addRule("name", Pattern.compile("x"), null));
        }
    }

    @Nested
    @DisplayName("内置规则列表")
    class BuiltinRulesTest {

        @Test
        @DisplayName("内置规则应包含 6 条规则")
        void builtinRuleCount() {
            List<SensitiveLogFilter.Rule> rules = SensitiveLogFilter.getBuiltinRules();
            assertEquals(6, rules.size());
        }

        @Test
        @DisplayName("内置规则列表应不可变")
        void immutable() {
            List<SensitiveLogFilter.Rule> rules = SensitiveLogFilter.getBuiltinRules();
            assertThrows(UnsupportedOperationException.class,
                    () -> rules.add(new SensitiveLogFilter.Rule("x", Pattern.compile("x"), m -> "y")));
        }
    }

    @Nested
    @DisplayName("自定义规则列表")
    class CustomRulesListTest {

        @Test
        @DisplayName("获取的自定义规则应为快照（不可变）")
        void snapshotImmutable() {
            SensitiveLogFilter.addRule("r1", Pattern.compile("R1"), m -> "M1");
            try {
                List<SensitiveLogFilter.Rule> snapshot = SensitiveLogFilter.getCustomRules();
                assertThrows(UnsupportedOperationException.class,
                        () -> snapshot.add(new SensitiveLogFilter.Rule("x", Pattern.compile("x"), m -> "y")));
            } finally {
                SensitiveLogFilter.clearCustomRules();
            }
        }
    }

    @Nested
    @DisplayName("边界场景")
    class EdgeCasesTest {

        @Test
        @DisplayName("数字中嵌入的11位数字（非手机号格式）不应被误匹配")
        void embeddedDigits() {
            String input = "订单编号1234567890123已创建";
            String result = SensitiveLogFilter.filter(input);
            // 1234567890123 不是以1[3-9]开头，不应被当作手机号
            assertTrue(result.contains("1234567890123"));
        }

        @Test
        @DisplayName("密码= 前面有空格格式")
        void passwordWithSpace() {
            String input = "password = secretvalue";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("secretvalue"));
        }

        @Test
        @DisplayName("大小写混合的密码键")
        void passwordCaseInsensitive() {
            String input = "PASSWORD=secret123";
            String result = SensitiveLogFilter.filter(input);
            assertFalse(result.contains("secret123"));
        }

        @Test
        @DisplayName("多条日志独立脱敏不影响")
        void independentMessages() {
            String msg1 = "用户手机号13812345678已注册";
            String msg2 = "用户手机号13987654321已注册";
            String r1 = SensitiveLogFilter.filter(msg1);
            String r2 = SensitiveLogFilter.filter(msg2);
            assertTrue(r1.contains("138****5678"), "第一条日志应脱敏: " + r1);
            assertTrue(r2.contains("139****4321"), "第二条日志应脱敏: " + r2);
        }
    }
}
