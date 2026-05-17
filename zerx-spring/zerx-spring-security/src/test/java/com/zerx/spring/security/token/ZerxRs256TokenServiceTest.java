package com.zerx.spring.security.token;

import com.zerx.spring.cache.ops.CacheOps;
import com.zerx.spring.security.props.ZerxSecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxRs256TokenService} 单元测试
 * <p>
 * 使用程序化生成的 RSA 密钥对进行 JWT 操作，使用内存 Map 模拟缓存。
 * </p>
 *
 * @author zerx
 */
class ZerxRs256TokenServiceTest {

    private ZerxRs256TokenService tokenService;
    private Map<String, Object> cacheStore;
    private ZerxSecurityProperties properties;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        // 程序化生成 RSA 密钥对
        var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        // 将密钥转为 Base64 配置
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        properties = new ZerxSecurityProperties();
        properties.getJwt().setAlgorithm("RS256");
        properties.getJwt().setAccessTokenExpire(Duration.ofMinutes(30));
        properties.getJwt().setRefreshTokenExpire(Duration.ofDays(1));
        properties.getJwt().getRsa().setPublicKey(publicKeyBase64);
        properties.getJwt().getRsa().setPrivateKey(privateKeyBase64);

        // 使用内存 Map 模拟 CacheOps
        cacheStore = new HashMap<>();
        CacheOps cacheOps = new InMemoryCacheOps(cacheStore);

        tokenService = new ZerxRs256TokenService(properties, cacheOps);
    }

    @Nested
    @DisplayName("令牌生成测试")
    class GenerateTokenTest {

        @Test
        @DisplayName("生成 RS256 访问令牌不为空")
        void generateAccessToken_notNull() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-access-001");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("生成 RS256 刷新令牌不为空")
        void generateRefreshToken_notNull() {
            String token = tokenService.generateRefreshToken(1001L, "jti-rs-refresh-001");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("不同用户生成的 RS256 令牌不同")
        void differentUsers_differentTokens() {
            String token1 = tokenService.generateAccessToken(1001L, "jti-rs-001");
            String token2 = tokenService.generateAccessToken(1002L, "jti-rs-002");
            assertNotEquals(token1, token2);
        }

        @Test
        @DisplayName("使用 generateTokenPair 生成 RS256 令牌对")
        void generateTokenPair() {
            ZerxTokenPair pair = tokenService.generateTokenPair(1001L);
            assertNotNull(pair.accessToken());
            assertNotNull(pair.refreshToken());
            assertNotEquals(pair.accessToken(), pair.refreshToken());
            assertEquals(1800, pair.accessExpiresIn());     // 30 minutes
            assertEquals(86400, pair.refreshExpiresIn());   // 1 day
            assertNotNull(pair.jti());
            assertFalse(pair.jti().isBlank());
        }

        @Test
        @DisplayName("使用 generateTokenPair 带角色生成令牌对")
        void generateTokenPairWithRoles() {
            var roles = List.of("ADMIN", "SUPERVISOR");
            ZerxTokenPair pair = tokenService.generateTokenPair(1001L, roles);
            assertNotNull(pair.accessToken());

            var claims = tokenService.parseToken(pair.accessToken());
            assertEquals(2, claims.roles().size());
            assertTrue(claims.roles().contains("ADMIN"));
            assertTrue(claims.roles().contains("SUPERVISOR"));
        }
    }

    @Nested
    @DisplayName("令牌解析测试")
    class ParseTokenTest {

        @Test
        @DisplayName("解析 RS256 访问令牌返回正确声明")
        void parseAccessToken_correctClaims() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-parse-001");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(1001L, claims.userId());
            assertEquals("jti-rs-parse-001", claims.jti());
            assertEquals("access", claims.tokenType());
            assertNotNull(claims.issuedAt());
            assertNotNull(claims.expiresAt());
            assertTrue(claims.expiresAt().isAfter(claims.issuedAt()));
        }

        @Test
        @DisplayName("解析 RS256 刷新令牌返回正确声明")
        void parseRefreshToken_correctClaims() {
            String token = tokenService.generateRefreshToken(2002L, "jti-rs-parse-002");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(2002L, claims.userId());
            assertEquals("jti-rs-parse-002", claims.jti());
            assertEquals("refresh", claims.tokenType());
        }

        @Test
        @DisplayName("解析无效 RS256 令牌抛出异常")
        void parseInvalidToken_throwsException() {
            assertThrows(Exception.class, () -> tokenService.parseToken("invalid-token-string"));
        }

        @Test
        @DisplayName("解析篡改签名的 RS256 令牌抛出异常")
        void parseTamperedToken_throwsException() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-tamper-001");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertThrows(Exception.class, () -> tokenService.parseToken(tampered));
        }
    }

    @Nested
    @DisplayName("RBAC 角色测试")
    class RolesTest {

        @Test
        @DisplayName("生成带角色的 RS256 访问令牌并正确解析")
        void generateAccessTokenWithRoles_parseCorrectly() {
            var roles = List.of("ADMIN", "EDITOR");
            String token = tokenService.generateAccessToken(1001L, "jti-rs-roles-001", roles);
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertEquals(1001L, claims.userId());
            assertEquals(2, claims.roles().size());
            assertEquals(roles, claims.roles());
        }

        @Test
        @DisplayName("不带角色的 RS256 访问令牌解析后角色为空列表")
        void generateAccessTokenWithoutRoles_emptyRolesList() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-no-roles-001");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertNotNull(claims.roles());
            assertTrue(claims.roles().isEmpty());
        }
    }

    @Nested
    @DisplayName("令牌校验测试")
    class ValidateTokenTest {

        @Test
        @DisplayName("有效的 RS256 令牌校验通过")
        void validToken_returnsTrue() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-valid-001");
            assertTrue(tokenService.validateToken(token));
        }

        @Test
        @DisplayName("无效的 RS256 令牌校验失败")
        void invalidToken_returnsFalse() {
            assertFalse(tokenService.validateToken("invalid-token"));
        }

        @Test
        @DisplayName("空 RS256 令牌校验失败")
        void emptyToken_returnsFalse() {
            assertFalse(tokenService.validateToken(""));
        }

        @Test
        @DisplayName("篡改签名的 RS256 令牌校验失败")
        void tamperedToken_returnsFalse() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-tamper-002");
            String tampered = token.substring(0, token.length() - 3) + "XXX";
            assertFalse(tokenService.validateToken(tampered));
        }
    }

    @Nested
    @DisplayName("黑名单测试")
    class BlacklistTest {

        @Test
        @DisplayName("RS256 令牌加入黑名单后校验失败")
        void blacklistedToken_validationFails() {
            String token = tokenService.generateAccessToken(1001L, "jti-rs-blacklist-001");
            ZerxTokenClaims claims = tokenService.parseToken(token);

            assertTrue(tokenService.validateToken(token));

            tokenService.blacklistToken(claims.jti(), claims.expiresAt());
            assertTrue(tokenService.isBlacklisted(claims.jti()));

            assertFalse(tokenService.validateToken(token));
        }

        @Test
        @DisplayName("未黑名单的 RS256 JTI 返回 false")
        void notBlacklisted_returnsFalse() {
            assertFalse(tokenService.isBlacklisted("non-existent-jti"));
        }

        @Test
        @DisplayName("已过期 RS256 令牌加入黑名单被跳过")
        void expiredToken_skipBlacklist() {
            Instant past = Instant.now().minus(Duration.ofHours(1));
            tokenService.blacklistToken("jti-rs-expired-001", past);
            assertFalse(tokenService.isBlacklisted("jti-rs-expired-001"));
        }

        @Test
        @DisplayName("未过期 RS256 令牌成功加入黑名单")
        void nonExpiredToken_addedToBlacklist() {
            Instant future = Instant.now().plus(Duration.ofHours(1));
            tokenService.blacklistToken("jti-rs-future-001", future);
            assertTrue(tokenService.isBlacklisted("jti-rs-future-001"));
        }
    }

    @Nested
    @DisplayName("密钥加载测试")
    class KeyLoadingTest {

        @Test
        @DisplayName("使用 Base64 编码的 DER 密钥成功初始化")
        void base64DerKeys_works() {
            // 上面的 setUp 已经使用 Base64 DER 格式成功初始化
            assertNotNull(tokenService);
            String token = tokenService.generateAccessToken(1001L, "jti-base64-001");
            assertTrue(tokenService.validateToken(token));
        }

        @Test
        @DisplayName("无效密钥初始化抛出异常")
        void invalidKey_throwsException() {
            var props = new ZerxSecurityProperties();
            props.getJwt().setAlgorithm("RS256");
            props.getJwt().getRsa().setPublicKey("invalid-base64!!!");
            props.getJwt().getRsa().setPrivateKey("invalid-base64!!!");

            CacheOps cacheOps = new InMemoryCacheOps(new HashMap<>());

            assertThrows(IllegalArgumentException.class,
                    () -> new ZerxRs256TokenService(props, cacheOps));
        }

        @Test
        @DisplayName("公钥私钥不匹配时解析令牌失败")
        void mismatchedKeys_parseFails() throws Exception {
            // 生成另一对密钥，使用不匹配的密钥初始化
            var kpGen = KeyPairGenerator.getInstance("RSA");
            kpGen.initialize(2048);
            KeyPair otherPair = kpGen.generateKeyPair();

            var props = new ZerxSecurityProperties();
            props.getJwt().setAlgorithm("RS256");
            // 用正确的私钥签名，用错误的公钥验证
            props.getJwt().getRsa().setPublicKey(
                    Base64.getEncoder().encodeToString(otherPair.getPublic().getEncoded()));
            props.getJwt().getRsa().setPrivateKey(
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));

            CacheOps cacheOps = new InMemoryCacheOps(new HashMap<>());
            var mismatchedService = new ZerxRs256TokenService(props, cacheOps);

            String token = mismatchedService.generateAccessToken(1001L, "jti-mismatch-001");
            // 签名用私钥没问题，但验证用公钥会失败
            assertFalse(mismatchedService.validateToken(token));
        }
    }

    // ======================== 内存缓存模拟实现 ========================

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

        private record CacheEntry(Object value, long expiresAt) {
            boolean isExpired() {
                return System.currentTimeMillis() > expiresAt;
            }
        }
    }
}
