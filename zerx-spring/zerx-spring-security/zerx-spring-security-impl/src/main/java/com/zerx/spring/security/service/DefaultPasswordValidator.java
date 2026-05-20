package com.zerx.spring.security.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于 OWASP 基线的密码强度校验器 — 默认实现。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>最小长度 8 位</li>
 *   <li>至少包含大写字母</li>
 *   <li>至少包含小写字母</li>
 *   <li>至少包含数字</li>
 *   <li>不得与用户名相同（忽略大小写）</li>
 *   <li>不得为常见弱密码</li>
 * </ul>
 * </p>
 *
 * @author zerx
 */
public class DefaultPasswordValidator implements ZerxPasswordValidator {

    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "123456", "password", "12345678", "qwerty", "123456789",
            "abc123", "111111", "123123", "admin", "letmein"
    );

    private final int minLength;

    /**
     * 使用默认最小长度（8 位）构造
     */
    public DefaultPasswordValidator() {
        this(8);
    }

    /**
     * @param minLength 密码最小长度
     */
    public DefaultPasswordValidator(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public List<String> validate(String rawPassword, String username) {
        List<String> errors = new ArrayList<>();

        if (rawPassword == null || rawPassword.length() < minLength) {
            errors.add("密码长度不能少于" + minLength + "位");
        }
        if (rawPassword != null) {
            if (rawPassword.chars().noneMatch(Character::isUpperCase)) {
                errors.add("密码必须包含大写字母");
            }
            if (rawPassword.chars().noneMatch(Character::isLowerCase)) {
                errors.add("密码必须包含小写字母");
            }
            if (rawPassword.chars().noneMatch(Character::isDigit)) {
                errors.add("密码必须包含数字");
            }
            if (username != null && rawPassword.equalsIgnoreCase(username)) {
                errors.add("密码不能与用户名相同");
            }
            if (WEAK_PASSWORDS.contains(rawPassword.toLowerCase())) {
                errors.add("密码过于简单，请使用更复杂的密码");
            }
        }

        return errors;
    }
}
