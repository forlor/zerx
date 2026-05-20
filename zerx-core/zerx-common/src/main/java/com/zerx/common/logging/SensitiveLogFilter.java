package com.zerx.common.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zerx.common.util.SensitiveDataUtil;

/**
 * 日志敏感数据过滤器
 * <p>
 * 对日志消息进行自动脱敏扫描，将手机号、邮箱、身份证号、银行卡号、密码等
 * 敏感信息替换为掩码形式，防止敏感数据泄露到日志文件中。
 * 内置常见敏感数据的匹配规则，同时支持自定义脱敏规则。
 * </p>
 *
 * <h3>内置脱敏规则：</h3>
 * <ul>
 *   <li>手机号：连续 11 位数字，1 开头，如 138****5678</li>
 *   <li>邮箱：user@domain 格式，如 t****r@example.com</li>
 *   <li>身份证号：连续 15 或 18 位（含末位 X），如 110***********1234</li>
 *   <li>银行卡号：连续 13-19 位纯数字，如 6222***********0123</li>
 *   <li>密码：password=xxx / pwd=xxx 等键值对形式</li>
 *   <li>IPv4：标准点分十进制格式，如 192.168.*.*</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 自动扫描脱敏
 * String safe = SensitiveLogFilter.filter("用户手机13812345678登录，密码=pwd123");
 * // → "用户手机138****5678登录，密码=******"
 *
 * // 注册自定义规则
 * SensitiveLogFilter.addRule("apiKey",
 *     Pattern.compile("ak_[a-zA-Z0-9]{16,}"),
 *     m -> "ak_" + "*".repeat(m.group().length() - 3));
 * }</pre>
 *
 * @author zerx
 */
public final class SensitiveLogFilter {

    /**
     * 脱敏规则定义
     *
     * @param name    规则名称（用于标识和管理）
     * @param pattern 匹配正则表达式
     * @param masker  脱敏函数，接收匹配到的字符串，返回脱敏后的字符串
     */
    public record Rule(String name, Pattern pattern, Function<String, String> masker) {
    }

    /** 手机号正则：1 开头，连续 11 位数字（前后不能是数字） */
    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("(?<![0-9])1[3-9]\\d{9}(?![0-9])");

    /** 邮箱正则 */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    /** 身份证号正则：15 位或 18 位（末位可以是数字或 X/x） */
    private static final Pattern IDCARD_PATTERN =
            Pattern.compile("(?<![0-9Xx])[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx](?![0-9Xx])"
                    + "|(?<![0-9])[1-9]\\d{5}\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}(?![0-9])");

    /** 银行卡号正则：13-19 位纯数字（前后不能是数字，排除手机号和身份证） */
    private static final Pattern BANKCARD_PATTERN =
            Pattern.compile("(?<![0-9])(?:6[0-9]{15,18})(?![0-9])");

    /** 密码键值对正则：匹配 password=xxx / pwd=xxx / secret=xxx 等形式 */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("(?i)(password|passwd|pwd|secret|token|apikey|api_key)\\s*[=:]\\s*\\S+");

    /** IPv4 正则：标准点分十进制 */
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\b");

    /** 自定义规则列表（线程安全） */
    private static final List<Rule> CUSTOM_RULES = new ArrayList<>();

    /** 内置规则列表（不可变） */
    private static final List<Rule> BUILTIN_RULES = List.of(
            new Rule("mobile", MOBILE_PATTERN, SensitiveDataUtil::maskMobile),
            new Rule("email", EMAIL_PATTERN, SensitiveDataUtil::maskEmail),
            new Rule("idcard", IDCARD_PATTERN, SensitiveDataUtil::maskIdCard),
            new Rule("bankcard", BANKCARD_PATTERN, SensitiveDataUtil::maskBankCard),
            new Rule("ipv4", IPV4_PATTERN, SensitiveDataUtil::maskIpv4),
            new Rule("password", PASSWORD_PATTERN, SensitiveLogFilter::maskPasswordValue)
    );

    private SensitiveLogFilter() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 核心方法 ========================

    /**
     * 对日志消息进行自动脱敏
     * <p>
     * 使用内置规则和自定义规则依次对消息进行扫描和替换。
     * 内置规则的匹配顺序为：邮箱、手机号、身份证号、银行卡号、密码、IPv4。
     * 自定义规则在内置规则之后执行。
     * </p>
     *
     * @param message 原始日志消息
     * @return 脱敏后的日志消息，null 返回 null
     */
    public static String filter(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String result = message;
        // 先应用内置规则
        for (Rule rule : BUILTIN_RULES) {
            result = applyRule(result, rule);
        }
        // 再应用自定义规则
        List<Rule> customRules;
        synchronized (CUSTOM_RULES) {
            customRules = List.copyOf(CUSTOM_RULES);
        }
        for (Rule rule : customRules) {
            result = applyRule(result, rule);
        }
        return result;
    }

    /**
     * 仅使用内置规则进行脱敏（不应用自定义规则）
     *
     * @param message 原始日志消息
     * @return 脱敏后的日志消息
     */
    public static String filterWithBuiltinRules(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        String result = message;
        for (Rule rule : BUILTIN_RULES) {
            result = applyRule(result, rule);
        }
        return result;
    }

    // ======================== 规则管理 ========================

    /**
     * 注册自定义脱敏规则
     * <p>
     * 自定义规则会在所有内置规则执行完毕之后依次应用。
     * </p>
     *
     * @param name    规则名称（不可为空，用于标识和移除）
     * @param pattern 匹配正则表达式（不可为空）
     * @param masker  脱敏函数（不可为空），接收匹配到的字符串，返回脱敏后的字符串
     * @throws IllegalArgumentException 如果 name、pattern 或 masker 为 null
     */
    public static void addRule(String name, Pattern pattern, Function<String, String> masker) {
        Objects.requireNonNull(name, "规则名称不能为 null");
        Objects.requireNonNull(pattern, "正则表达式不能为 null");
        Objects.requireNonNull(masker, "脱敏函数不能为 null");
        Rule rule = new Rule(name, pattern, masker);
        synchronized (CUSTOM_RULES) {
            // 如果同名规则已存在，先移除
            CUSTOM_RULES.removeIf(r -> r.name().equals(name));
            CUSTOM_RULES.add(rule);
        }
    }

    /**
     * 移除指定名称的自定义脱敏规则
     *
     * @param name 规则名称
     * @return 是否成功移除（规则不存在时返回 false）
     */
    public static boolean removeRule(String name) {
        synchronized (CUSTOM_RULES) {
            return CUSTOM_RULES.removeIf(r -> r.name().equals(name));
        }
    }

    /**
     * 清除所有自定义脱敏规则
     */
    public static void clearCustomRules() {
        synchronized (CUSTOM_RULES) {
            CUSTOM_RULES.clear();
        }
    }

    /**
     * 获取所有已注册的自定义规则（快照）
     *
     * @return 不可变的自定义规则列表
     */
    public static List<Rule> getCustomRules() {
        synchronized (CUSTOM_RULES) {
            return List.copyOf(CUSTOM_RULES);
        }
    }

    /**
     * 获取所有内置规则（不可变）
     *
     * @return 不可变的内置规则列表
     */
    public static List<Rule> getBuiltinRules() {
        return BUILTIN_RULES;
    }

    // ======================== 内部方法 ========================

    /**
     * 应用单条脱敏规则
     */
    private static String applyRule(String input, Rule rule) {
        Matcher matcher = rule.pattern().matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuffer sb = new StringBuffer();
        matcher.reset();
        while (matcher.find()) {
            String matched = matcher.group();
            String masked = rule.masker().apply(matched);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 密码值脱敏：提取等号或冒号后面的值部分进行脱敏
     */
    private static String maskPasswordValue(String matched) {
        int sepIndex = -1;
        for (int i = 0; i < matched.length(); i++) {
            char c = matched.charAt(i);
            if (c == '=' || c == ':') {
                sepIndex = i;
                break;
            }
        }
        if (sepIndex < 0) {
            return SensitiveDataUtil.maskPassword(matched);
        }
        String key = matched.substring(0, sepIndex + 1);
        // 保留分隔符后的空格
        int valueStart = sepIndex + 1;
        while (valueStart < matched.length() && matched.charAt(valueStart) == ' ') {
            valueStart++;
        }
        if (valueStart >= matched.length()) {
            return key + "******";
        }
        return key + SensitiveDataUtil.maskPassword(matched.substring(valueStart));
    }
}
