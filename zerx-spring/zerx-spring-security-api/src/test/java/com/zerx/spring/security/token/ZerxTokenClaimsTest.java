package com.zerx.spring.security.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxTokenClaims} record 契约测试
 *
 * @author zerx
 */
class ZerxTokenClaimsTest {

    @Nested
    @DisplayName("构造与访问")
    class ConstructorTest {

        @Test
        @DisplayName("完整构造 — 所有字段正确")
        void fullConstruction() {
            Instant now = Instant.now();
            var claims = new ZerxTokenClaims(1L, "jti-001", now, now.plusSeconds(3600),
                    "access", List.of("admin", "editor"));

            assertEquals(1L, claims.userId());
            assertEquals("jti-001", claims.jti());
            assertEquals(now, claims.issuedAt());
            assertEquals(now.plusSeconds(3600), claims.expiresAt());
            assertEquals("access", claims.tokenType());
            assertEquals(2, claims.roles().size());
            assertEquals("admin", claims.roles().get(0));
            assertEquals("editor", claims.roles().get(1));
        }

        @Test
        @DisplayName("roles 为空列表时正常")
        void emptyRoles() {
            var claims = new ZerxTokenClaims(2L, "jti-002", Instant.now(), Instant.now(),
                    "access", List.of());

            assertNotNull(claims.roles());
            assertTrue(claims.roles().isEmpty());
        }

        @Test
        @DisplayName("record 自动生成 equals 和 hashCode")
        void equalsAndHashCode() {
            Instant now = Instant.now();
            var claims1 = new ZerxTokenClaims(1L, "jti-001", now, now.plusSeconds(3600),
                    "access", List.of("admin"));
            var claims2 = new ZerxTokenClaims(1L, "jti-001", now, now.plusSeconds(3600),
                    "access", List.of("admin"));

            assertEquals(claims1, claims2);
            assertEquals(claims1.hashCode(), claims2.hashCode());
        }

        @Test
        @DisplayName("不同字段导致不等")
        void notEquals() {
            Instant now = Instant.now();
            var claims1 = new ZerxTokenClaims(1L, "jti-001", now, now.plusSeconds(3600),
                    "access", List.of("admin"));
            var claims2 = new ZerxTokenClaims(2L, "jti-002", now, now.plusSeconds(3600),
                    "access", List.of("admin"));

            assertNotEquals(claims1, claims2);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTest {

        @Test
        @DisplayName("toString 包含关键字段")
        void toString_containsFields() {
            var claims = new ZerxTokenClaims(1L, "jti-001", Instant.now(), Instant.now(),
                    "access", List.of("admin"));

            String str = claims.toString();
            assertTrue(str.contains("jti-001"));
            assertTrue(str.contains("access"));
            assertTrue(str.contains("admin"));
        }
    }
}
