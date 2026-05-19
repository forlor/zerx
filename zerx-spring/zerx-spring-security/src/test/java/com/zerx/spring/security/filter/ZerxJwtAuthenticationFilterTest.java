package com.zerx.spring.security.filter;

import com.zerx.spring.security.props.ZerxSecurityProperties;
import com.zerx.spring.security.token.ZerxTokenClaims;
import com.zerx.spring.security.token.ZerxTokenService;
import com.zerx.spring.web.context.RequestContext;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * {@link ZerxJwtAuthenticationFilter} 单元测试
 *
 * @author zerx
 */
class ZerxJwtAuthenticationFilterTest {

    private static final Long TEST_USER_ID = 1001L;
    private static final String TEST_JTI = "test-jti-001";
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String INVALID_TOKEN = "invalid.jwt.token";

    private ZerxTokenService tokenService;
    private ZerxSecurityProperties properties;
    private ZerxJwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        tokenService = mock(ZerxTokenService.class);
        properties = new ZerxSecurityProperties();
        filter = new ZerxJwtAuthenticationFilter(tokenService, properties);
        RequestContext.init();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContext.clear();
    }

    @Nested
    @DisplayName("令牌提取测试")
    class TokenExtractionTest {

        @Test
        @DisplayName("从 Authorization 头提取 Bearer Token")
        void extractBearerToken() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(TEST_USER_ID, auth.getPrincipal());
        }

        @Test
        @DisplayName("无 Authorization 头时跳过认证")
        void noAuthorizationHeader_skipAuth() throws Exception {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(tokenService, never()).parseToken(anyString());
        }

        @Test
        @DisplayName("Authorization 头不是 Bearer 前缀时跳过认证")
        void nonBearerPrefix_skipAuth() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
            var response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            verify(tokenService, never()).parseToken(anyString());
        }

        @Test
        @DisplayName("Bearer 前缀后无令牌值时跳过认证")
        void bearerWithEmptyToken_skipAuth() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer ");
            var response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("令牌校验测试")
    class TokenValidationTest {

        @Test
        @DisplayName("有效令牌设置 SecurityContext")
        void validToken_setAuthentication() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(TEST_USER_ID, auth.getPrincipal());
            assertTrue(auth.getAuthorities().isEmpty());
        }

        @Test
        @DisplayName("无效令牌清除 SecurityContext 并设置错误属性")
        void invalidToken_clearContext() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);
            var response = new MockHttpServletResponse();

            when(tokenService.parseToken(INVALID_TOKEN)).thenThrow(new RuntimeException("bad token"));

            // 先设置一个认证信息
            SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "prev-user", null));

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNull(auth);
            assertEquals(ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_INVALID,
                    request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR));
        }

        @Test
        @DisplayName("过期令牌设置 TOKEN_EXPIRED 错误属性")
        void expiredToken_setsExpiredError() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            when(tokenService.parseToken(VALID_TOKEN))
                    .thenThrow(new ExpiredJwtException(null, null, "JWT expired"));

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_EXPIRED,
                    request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR));
        }

        @Test
        @DisplayName("黑名单令牌清除 SecurityContext 并设置错误属性")
        void blacklistedToken_clearContextAndSetError() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(true);

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_BLACKLISTED,
                    request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR));
        }
    }

    @Nested
    @DisplayName("RBAC 角色权限测试")
    class RbacAuthorityTest {

        @Test
        @DisplayName("角色转换为 GrantedAuthority 并自动添加 ROLE_ 前缀")
        void roles_convertedToAuthoritiesWithPrefix() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access",
                    List.of("ADMIN", "USER"));
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            var authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            assertEquals(2, authorities.size());
            assertTrue(authorities.contains("ROLE_ADMIN"));
            assertTrue(authorities.contains("ROLE_USER"));
        }

        @Test
        @DisplayName("已有 ROLE_ 前缀的角色不会重复添加")
        void rolesWithExistingPrefix_noDoublePrefix() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access",
                    List.of("ROLE_ADMIN", "SUPER_USER"));
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            var authorities = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            assertEquals(2, authorities.size());
            assertTrue(authorities.contains("ROLE_ADMIN"));    // 不变成 ROLE_ROLE_ADMIN
            assertTrue(authorities.contains("ROLE_SUPER_USER"));
        }

        @Test
        @DisplayName("空角色列表产生空的权限集合")
        void emptyRoles_emptyAuthorities() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access",
                    List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertTrue(auth.getAuthorities().isEmpty());
        }
    }

    @Nested
    @DisplayName("请求上下文集成测试")
    class RequestContextIntegrationTest {

        @Test
        @DisplayName("有效令牌设置 RequestContext userId")
        void validToken_setRequestContextUserId() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertEquals(TEST_USER_ID, RequestContext.getUserId());
        }

        @Test
        @DisplayName("无效令牌不修改 RequestContext userId")
        void invalidToken_doesNotModifyRequestContext() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);
            var response = new MockHttpServletResponse();

            when(tokenService.parseToken(INVALID_TOKEN)).thenThrow(new RuntimeException("bad token"));

            var originalUserId = 9999L;
            RequestContext.setUserId(originalUserId);

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertEquals(originalUserId, RequestContext.getUserId());
        }

        @Test
        @DisplayName("RequestContext 未初始化时不抛异常")
        void noRequestContext_noException() throws Exception {
            RequestContext.clear();

            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            assertDoesNotThrow(() ->
                    filter.doFilterInternal(request, response, (req, res) -> {}));
        }
    }

    @Nested
    @DisplayName("令牌类型校验测试")
    class TokenTypeValidationTest {

        @Test
        @DisplayName("refresh token 被拒绝，不设置 SecurityContext")
        void refreshToken_rejected() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            // tokenType 为 refresh，应被拒绝
            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "refresh", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_TYPE_REJECTED,
                    request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR));
        }

        @Test
        @DisplayName("refresh token 被拒绝时不修改 RequestContext userId")
        void refreshToken_doesNotModifyRequestContext() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "refresh", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            Long originalUserId = 9999L;
            RequestContext.setUserId(originalUserId);

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertEquals(originalUserId, RequestContext.getUserId());
        }

        @Test
        @DisplayName("access token 正常通过令牌类型校验")
        void accessToken_passesTokenTypeCheck() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access",
                    List.of("ADMIN"));
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(TEST_USER_ID, auth.getPrincipal());
        }
    }

    @Nested
    @DisplayName("自定义请求头配置测试")
    class CustomHeaderConfigTest {

        @Test
        @DisplayName("使用自定义 headerName 和 headerPrefix")
        void customHeaderNameAndPrefix() throws Exception {
            properties.getJwt().setHeaderName("X-Token");
            properties.getJwt().setHeaderPrefix("Token ");

            filter = new ZerxJwtAuthenticationFilter(tokenService, properties);

            var request = new MockHttpServletRequest();
            request.addHeader("X-Token", "Token " + VALID_TOKEN);
            var response = new MockHttpServletResponse();

            var claims = new ZerxTokenClaims(TEST_USER_ID, TEST_JTI,
                    Instant.now(), Instant.now().plusSeconds(3600), "access", List.of());
            when(tokenService.parseToken(VALID_TOKEN)).thenReturn(claims);
            when(tokenService.isBlacklisted(TEST_JTI)).thenReturn(false);

            filter.doFilterInternal(request, response, (req, res) -> {});

            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(auth);
            assertEquals(TEST_USER_ID, auth.getPrincipal());
        }
    }

    @Nested
    @DisplayName("错误属性传递测试")
    class ErrorAttributeTest {

        @Test
        @DisplayName("无令牌时不设置错误属性（由 EntryPoint 返回默认 401）")
        void noToken_noErrorAttribute() throws Exception {
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertNull(request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR));
        }

        @Test
        @DisplayName("签名无效令牌设置 TOKEN_INVALID 属性")
        void invalidSignature_setsInvalidError() throws Exception {
            var request = new MockHttpServletRequest();
            request.addHeader("Authorization", "Bearer " + INVALID_TOKEN);
            var response = new MockHttpServletResponse();

            when(tokenService.parseToken(INVALID_TOKEN))
                    .thenThrow(new io.jsonwebtoken.SignatureException("invalid signature"));

            filter.doFilterInternal(request, response, (req, res) -> {});

            assertEquals(ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_INVALID,
                    request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR));
        }
    }
}
