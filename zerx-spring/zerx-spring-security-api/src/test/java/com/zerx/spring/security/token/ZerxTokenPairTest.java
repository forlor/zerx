package com.zerx.spring.security.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxTokenPair} record 契约测试
 *
 * @author zerx
 */
class ZerxTokenPairTest {

    @Nested
    @DisplayName("构造与访问")
    class ConstructorTest {

        @Test
        @DisplayName("完整构造 — 所有字段正确")
        void fullConstruction() {
            var pair = new ZerxTokenPair("access-token", "refresh-token", 7200, 604800, "jti-001");

            assertEquals("access-token", pair.accessToken());
            assertEquals("refresh-token", pair.refreshToken());
            assertEquals(7200, pair.accessExpiresIn());
            assertEquals(604800, pair.refreshExpiresIn());
            assertEquals("jti-001", pair.jti());
        }

        @Test
        @DisplayName("record 自动生成 equals 和 hashCode")
        void equalsAndHashCode() {
            var pair1 = new ZerxTokenPair("a", "r", 7200, 604800, "jti-001");
            var pair2 = new ZerxTokenPair("a", "r", 7200, 604800, "jti-001");

            assertEquals(pair1, pair2);
            assertEquals(pair1.hashCode(), pair2.hashCode());
        }

        @Test
        @DisplayName("不同字段导致不等")
        void notEquals() {
            var pair1 = new ZerxTokenPair("a1", "r1", 7200, 604800, "jti-001");
            var pair2 = new ZerxTokenPair("a2", "r2", 7200, 604800, "jti-002");

            assertNotEquals(pair1, pair2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 包含关键字段")
        void toString_containsFields() {
            var pair = new ZerxTokenPair("my-access", "my-refresh", 3600, 86400, "jti-abc");
            String str = pair.toString();
            assertTrue(str.contains("my-access"));
            assertTrue(str.contains("jti-abc"));
        }
    }
}
