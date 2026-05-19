package com.zerx.spring.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxPasswordService} 单元测试
 *
 * @author zerx
 */
class ZerxPasswordServiceTest {

    private ZerxPasswordService passwordService;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        passwordService = new ZerxPasswordService(passwordEncoder);
    }

    @Nested
    @DisplayName("哈希测试")
    class HashTest {

        @Test
        @DisplayName("hash 产生非空结果")
        void hash_notNull() {
            String hash = passwordService.hash("password123");
            assertNotNull(hash);
            assertFalse(hash.isBlank());
        }

        @Test
        @DisplayName("相同密码每次哈希产生不同值（BCrypt 盐值）")
        void hash_samePassword_differentResults() {
            String raw = "mySecretPassword";
            String hash1 = passwordService.hash(raw);
            String hash2 = passwordService.hash(raw);

            assertNotNull(hash1);
            assertNotNull(hash2);
            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("哈希值以 $2a$ 或 $2b$ 开头（BCrypt 格式）")
        void hash_hasBcryptPrefix() {
            String hash = passwordService.hash("test");
            assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"),
                    "Hash should start with BCrypt prefix");
        }
    }

    @Nested
    @DisplayName("验证测试")
    class MatchesTest {

        @Test
        @DisplayName("正确密码验证通过")
        void matches_correctPassword_returnsTrue() {
            String raw = "correctPassword";
            String hash = passwordService.hash(raw);
            assertTrue(passwordService.matches(raw, hash));
        }

        @Test
        @DisplayName("错误密码验证失败")
        void matches_wrongPassword_returnsFalse() {
            String hash = passwordService.hash("correctPassword");
            assertFalse(passwordService.matches("wrongPassword", hash));
        }

        @Test
        @DisplayName("空字符串密码验证失败")
        void matches_emptyString_returnsFalse() {
            String hash = passwordService.hash("nonEmptyPassword");
            assertFalse(passwordService.matches("", hash));
        }

        @Test
        @DisplayName("不同密码的哈希值不匹配")
        void matches_differentPasswords_doNotMatch() {
            String hash1 = passwordService.hash("passwordA");
            String hash2 = passwordService.hash("passwordB");
            assertFalse(passwordService.matches("passwordA", hash2));
        }
    }
}
