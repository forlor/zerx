package com.zerx.spring.security.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxPasswordValidator} 接口契约测试
 * <p>
 * 使用简单实现验证接口方法的语义契约。
 * </p>
 *
 * @author zerx
 */
class ZerxPasswordValidatorTest {

    /**
     * 简单的密码校验实现，仅用于接口契约测试
     */
    static class SimplePasswordValidator implements ZerxPasswordValidator {

        private static final int MIN_LENGTH = 8;

        @Override
        public List<String> validate(String rawPassword, String username) {
            List<String> errors = new java.util.ArrayList<>();
            if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
                errors.add("密码长度不能少于" + MIN_LENGTH + "位");
            }
            if (rawPassword != null && rawPassword.equalsIgnoreCase(username)) {
                errors.add("密码不能与用户名相同");
            }
            return errors;
        }
    }

    private ZerxPasswordValidator validator;

    @Nested
    @DisplayName("校验通过")
    class PassTest {

        @Test
        @DisplayName("满足最小长度且不等于用户名时返回空列表")
        void validPassword() {
            validator = new SimplePasswordValidator();
            List<String> errors = validator.validate("StrongPass123", "admin");
            assertTrue(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("校验失败")
    class FailTest {

        @Test
        @DisplayName("密码过短返回错误")
        void tooShort() {
            validator = new SimplePasswordValidator();
            List<String> errors = validator.validate("short", "admin");
            assertFalse(errors.isEmpty());
            assertTrue(errors.get(0).contains("8"));
        }

        @Test
        @DisplayName("密码与用户名相同返回错误")
        void sameAsUsername() {
            validator = new SimplePasswordValidator();
            List<String> errors = validator.validate("longpassword", "longpassword");
            assertFalse(errors.isEmpty());
            assertTrue(errors.stream().anyMatch(e -> e.contains("用户名")));
        }

        @Test
        @DisplayName("密码为 null 时返回错误")
        void nullPassword() {
            validator = new SimplePasswordValidator();
            List<String> errors = validator.validate(null, "admin");
            assertFalse(errors.isEmpty());
        }
    }
}
