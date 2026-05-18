package com.zerx.spring.security.token;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.security.props.ZerxSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxHs256TokenService} 单元测试
 * <p>
 * 使用真实 HmacSHA256 密钥进行 JWT 操作，使用内存 Map 模拟缓存。
 * </p>
 *
 * @author zerx
 */
class ZerxTokenServiceTest {

    private static final String TEST_SECRET = "test-secret-key-for-hs256-must-be-at-least-256-bits-long!!";

    private ZerxTokenService tokenService;
    private Map<String, Object> cacheStore;
    private ZerxSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxSecurityProperties();
        properties.getJwt().setSecret(TEST_SECRET);
        properties.getJwt().setAccessTokenExpire(Duration.ofMinutes(30));
        properties.getJwt().setRefreshTokenExpire(Duration.ofDays(1));

        // 使用内存 Map 模拟 CacheOps
        cacheStore = new HashMap<>();
        CacheOps cacheOps = new InMemoryCacheOps(cacheStore);

        tokenService = new ZerxHs256TokenService(properties, cacheOps);
    }

    @Nested
    @DisplayName("令牌生成测试")
    class GenerateTokenTest {

        @Test
        @DisplayName("生成访问令牌不为空")
        void generateAccessToken_notNull() {
            String token = tokenService.generateAccessToken(1001L, "jti-access-001");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("生成刷新令牌不为空")
        void generateRefreshToken_notNull() {
            String token = tokenService.generateRefreshToken(1001L, "jti-refresh-001");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("不同用户生成的令牌不同")
        void differentUsers_differentTokens() {
            String token1 = tokenService.generateAccessToken(1001L, "jti-001");
            String token2 = tokenService.generateAccessToken(1002L, "jti-002");
            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("使用 generateTokenPair 生成令牌对")
        void generateTokenPair() {
            if (tokenService instanceof ZerxHs256TokenService hs256Service) {
                ZerxTokenPair pair = hs256Service.generateTokenPair(1001L);
                assertNotNull(pair.accessToken());
                assertNotNull(pair.refreshToken());
                assertNotEquals(pair.accessToken(), pair.refreshToken());
                assertEquals(1800, pair.accessExpiresIn());     // 30 minutes
                assertEquals(86400, pair.refreshExpiresIn());   // 1 day
                assertNotNull(pair.jti());
                assertFalse(pair.jti().isBlank());
            }
        }

        @Test
        @DisplayName("使用 generateTokenPair 带角色生成令牌对")
        void generateTokenPairWithRoles() {
            if (tokenService instanceof ZerxHs256TokenService hs256Service) {
                var roles = List.of("ADMIN", "USER");
                ZerxTokenPair pair = hs256Service.generateTokenPair(1001L, roles);
                assertNotNull(pair.accessToken());
                assertNotNull(pair.refreshToken());

                // 解析访问令牌验证角色
                var claims = tokenService.parseToken(pair.accessToken());
                assertEquals(2, claims.roles().size());
                assertTrue(claims.roles().contains("ADMIN"));
                assertTrue(claims.roles().contains("USER"));
            }
        }
    }

    @Nested
    @DisplayName("令牌解析测试")
    class ParseTokenTest {

        @Test
        @DisplayName("解析访问令牌返回正确声明")
        void parseAccessToken_correctClaims() {
            String token = tokenService.generateAccessToken(1001L, "jti-parse-001");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(1001L, claims.userId());
            assertEquals("jti-parse-001", claims.jti());
            assertEquals("access", claims.tokenType());
            assertNotNull(claims.issuedAt());
            assertNotNull(claims.expiresAt());
            assertTrue(claims.expiresAt().isAfter(claims.issuedAt()));
        }

        @Test
        @DisplayName("解析刷新令牌返回正确声明")
        void parseRefreshToken_correctClaims() {
            String token = tokenService.generateRefreshToken(2002L, "jti-parse-002");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(2002L, claims.userId());
            assertEquals("jti-parse-002", claims.jti());
            assertEquals("refresh", claims.tokenType());
        }

        @Test
        @DisplayName("解析无效令牌抛出异常")
        void parseInvalidToken_throwsException() {
            assertThrows(Exception.class, () -> tokenService.parseToken("invalid-token-string"));
        }

        @Test
        @DisplayName("解析空令牌抛出异常")
        void parseEmptyToken_throwsException() {
            assertThrows(Exception.class, () -> tokenService.parseToken(""));
        }

        @Test
        @DisplayName("解析篡改签名令牌抛出异常")
        void parseTamperedToken_throwsException() {
            String token = tokenService.generateAccessToken(1001L, "jti-tamper-001");
            // 篡改令牌的最后几个字符
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertThrows(Exception.class, () -> tokenService.parseToken(tampered));
        }
    }

    @Nested
    @DisplayName("RBAC 角色测试")
    class RolesTest {

        @Test
        @DisplayName("生成带角色的访问令牌并正确解析")
        void generateAccessTokenWithRoles_parseCorrectly() {
            var roles = List.of("ADMIN", "EDITOR", "VIEWER");
            String token = tokenService.generateAccessToken(1001L, "jti-roles-001", roles);
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(1001L, claims.userId());
            assertEquals(3, claims.roles().size());
            assertEquals(roles, claims.roles());
        }

        @Test
        @DisplayName("不带角色的访问令牌解析后角色为空列表（向后兼容）")
        void generateAccessTokenWithoutRoles_emptyRolesList() {
            String token = tokenService.generateAccessToken(1001L, "jti-no-roles-001");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(1001L, claims.userId());
            assertNotNull(claims.roles());
            assertTrue(claims.roles().isEmpty());
        }

        @Test
        @DisplayName("显式传入空角色列表等同于不传角色")
        void generateAccessTokenWithEmptyRoles_sameAsNoRoles() {
            String token1 = tokenService.generateAccessToken(1001L, "jti-001");
            String token2 = tokenService.generateAccessToken(1001L, "jti-002", List.of());

            var claims1 = tokenService.parseToken(token1);
            var claims2 = tokenService.parseToken(token2);

            assertEquals(claims1.roles(), claims2.roles());
            assertTrue(claims1.roles().isEmpty());
            assertTrue(claims2.roles().isEmpty());
        }

        @Test
        @DisplayName("带角色的访问令牌和不带角色的具有相同 JTI 时仍可区分")
        void tokensWithSameJti_differentContent() {
            String token1 = tokenService.generateAccessToken(1001L, "jti-same-001");
            String token2 = tokenService.generateAccessToken(1001L, "jti-same-001", List.of("ADMIN"));

            assertNotEquals(token1, token2);
        }
    }

    @Nested
    @DisplayName("令牌校验测试")
    class ValidateTokenTest {

        @Test
        @DisplayName("有效令牌校验通过")
        void validToken_returnsTrue() {
            String token = tokenService.generateAccessToken(1001L, "jti-valid-001");
            assertTrue(tokenService.validateToken(token));
        }

        @Test
        @DisplayName("带角色的有效令牌校验通过")
        void validTokenWithRoles_returnsTrue() {
            String token = tokenService.generateAccessToken(1001L, "jti-valid-roles-001",
                    List.of("ADMIN"));
            assertTrue(tokenService.validateToken(token));
        }

        @Test
        @DisplayName("无效令牌校验失败")
        void invalidToken_returnsFalse() {
            assertFalse(tokenService.validateToken("invalid-token"));
        }

        @Test
        @DisplayName("空令牌校验失败")
        void emptyToken_returnsFalse() {
            assertFalse(tokenService.validateToken(""));
        }

        @Test
        @DisplayName("篡改签名令牌校验失败")
        void tamperedToken_returnsFalse() {
            String token = tokenService.generateAccessToken(1001L, "jti-tamper-002");
            String tampered = token.substring(0, token.length() - 3) + "XXX";
            assertFalse(tokenService.validateToken(tampered));
        }
    }

    @Nested
    @DisplayName("黑名单测试")
    class BlacklistTest {

        @Test
        @DisplayName("加入黑名单后令牌校验失败")
        void blacklistedToken_validationFails() {
            String token = tokenService.generateAccessToken(1001L, "jti-blacklist-001");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            // 令牌有效
            assertTrue(tokenService.validateToken(token));

            // 加入黑名单
            tokenService.blacklistToken(claims.jti(), claims.expiresAt());
            assertTrue(tokenService.isBlacklisted(claims.jti()));

            // 校验失败
            assertFalse(tokenService.validateToken(token));
        }

        @Test
        @DisplayName("检查未黑名单的 JTI 返回 false")
        void notBlacklisted_returnsFalse() {
            assertFalse(tokenService.isBlacklisted("non-existent-jti"));
        }

        @Test
        @DisplayName("已过期令牌加入黑名单被跳过")
        void expiredToken_skipBlacklist() {
            Instant past = Instant.now().minus(Duration.ofHours(1));
            tokenService.blacklistToken("jti-expired-001", past);
            assertFalse(tokenService.isBlacklisted("jti-expired-001"));
        }

        @Test
        @DisplayName("未过期令牌成功加入黑名单")
        void nonExpiredToken_addedToBlacklist() {
            Instant future = Instant.now().plus(Duration.ofHours(1));
            tokenService.blacklistToken("jti-future-001", future);
            assertTrue(tokenService.isBlacklisted("jti-future-001"));
        }
    }

    // ======================== 内存缓存模拟实现 ========================

    /**
     * 基于 HashMap 的 CacheOps 模拟实现，用于单元测试
     */
    private static class InMemoryCacheOps implements CacheOps {

        private final Map<String, CacheEntry> store;

        InMemoryCacheOps(Map<String, Object> cacheStore) {
            this.store = new HashMap<>();
        }

        @Override
        public <T> T get(String key, java.util.function.Supplier<T> loader, long ttl, TimeUnit timeUnit) {
            if (!store.containsKey(key)) {
                T value = loader.get();
                set(key, value, ttl, timeUnit);
                return value;
            }
            var entry = store.get(key);
            if (entry != null && !entry.isExpired()) {
                @SuppressWarnings("unchecked")
                T result = (T) entry.value;
                return result;
            }
            store.remove(key);
            T value = loader.get();
            set(key, value, ttl, timeUnit);
            return value;
        }

        @Override
        public <T> java.util.Optional<T> getOptional(String key, java.util.function.Supplier<T> loader, long ttl, TimeUnit unit) {
            return java.util.Optional.ofNullable(get(key, loader, ttl, unit));
        }

        @Override
        public <T> T get(String key) {
            var entry = store.get(key);
            if (entry != null && !entry.isExpired()) {
                @SuppressWarnings("unchecked")
                T result = (T) entry.value;
                return result;
            }
            store.remove(key);
            return null;
        }

        @Override
        public void set(String key, Object value, long ttl, TimeUnit timeUnit) {
            store.put(key, new CacheEntry(value, System.currentTimeMillis() + timeUnit.toMillis(ttl)));
        }

        @Override
        public void set(String key, Object value, Duration ttl) {
            set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public void evict(String key) {
            store.remove(key);
        }

        @Override
        public void evictByPrefix(String keyPrefix) {
            store.keySet().removeIf(k -> k.startsWith(keyPrefix));
        }

        @Override
        public boolean hasKey(String key) {
            var entry = store.get(key);
            if (entry == null) {
                return false;
            }
            if (entry.isExpired()) {
                store.remove(key);
                return false;
            }
            return true;
        }

        @Override
        public CacheStore getStore() {
            return null;
        }

        private record CacheEntry(Object value, long expiresAt) {
            boolean isExpired() {
                return System.currentTimeMillis() > expiresAt;
            }
        }
    }
}
