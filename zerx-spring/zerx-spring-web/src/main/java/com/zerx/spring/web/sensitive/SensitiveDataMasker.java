package com.zerx.spring.web.sensitive;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏器 — 预编译正则，线程安全
 * <p>
 * 在构造时编译所有脱敏正则并缓存，请求时的脱敏操作仅需 Pattern.matcher() 匹配替换。
 * Pattern.matcher() 是轻量操作（仅初始化几个字段），真正的编译开销在 Pattern 编译阶段已完成。
 * </p>
 *
 * <h3>性能设计：</h3>
 * <ul>
 *   <li>正则在构造时一次性编译，后续请求零编译开销</li>
 *   <li>支持两种匹配模式：URL query string (key=value) 和 JSON ("key":"value")</li>
 *   <li>线程安全：Pattern 不可变，Matcher 每次创建新实例</li>
 * </ul>
 *
 * @author zerx
 */
public final class SensitiveDataMasker {

    /** 默认脱敏掩码 */
    static final String MASK = "******";

    /** 预编译的规则列表 */
    private final List<Rule> rules;

    public SensitiveDataMasker(List<String> sensitiveParams) {
        this.rules = new ArrayList<>(sensitiveParams.size());
        for (String param : sensitiveParams) {
            if (param == null || param.isBlank()) continue;
            String quoted = Pattern.quote(param);
            // URL query string 模式：password=xxx& → password=******&
            // 使用前瞻确保匹配的是 key= 后面的值
            Pattern kvPattern = Pattern.compile(
                    "(\\b" + quoted + "=)([^&]*)",
                    Pattern.CASE_INSENSITIVE
            );
            // JSON key:value 模式："password":"xxx" → "password":"******"
            Pattern jsonPattern = Pattern.compile(
                    "(\"[^\"]*" + quoted + "[^\"]*\"\\s*:\\s*\")([^\"]*?)(\")",
                    Pattern.CASE_INSENSITIVE
            );
            rules.add(new Rule(param, kvPattern, jsonPattern));
        }
    }

    /**
     * 对输入字符串进行敏感数据脱敏
     * <p>
     * 依次应用所有编译后的脱敏规则，将匹配到的敏感值替换为掩码。
     * </p>
     *
     * @param input 原始字符串
     * @return 脱敏后的字符串
     */
    public String mask(String input) {
        if (input == null || input.isEmpty() || rules.isEmpty()) {
            return input;
        }
        String result = input;
        for (Rule rule : rules) {
            result = rule.kvPattern.matcher(result).replaceAll("$1" + MASK);
            result = rule.jsonPattern.matcher(result).replaceAll("$1" + MASK + "$3");
        }
        return result;
    }

    /**
     * 编译后的脱敏规则
     *
     * @param paramName 参数名
     * @param kvPattern URL query string 匹配模式
     * @param jsonPattern JSON key:value 匹配模式
     */
    private record Rule(String paramName, Pattern kvPattern, Pattern jsonPattern) {}
}
