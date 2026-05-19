package com.zerx.spring.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxSecurityUtils} 单元测试
 *
 * @author zerx
 */
class ZerxSecurityUtilsTest {

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUserId 测试")
    class GetCurrentUserIdTest {

        @Test
        @DisplayName("未认证时返回 null")
        void notAuthenticated_returnsNull() {
            assertNull(ZerxSecurityUtils.getCurrentUserId());
        }

        @Test
        @DisplayName("Long 类型的 principal 直接返回")
        void longPrincipal_returnsDirectly() {
            setAuth(1001L, List.of("USER"));
            assertEquals(1001L, ZerxSecurityUtils.getCurrentUserId());
        }

        @Test
        @DisplayName("String 类型的 principal 尝试转换为 Long")
        void stringPrincipal_convertsToLong() {
            var auth = new UsernamePasswordAuthenticationToken("2048", null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertEquals(2048L, ZerxSecurityUtils.getCurrentUserId());
        }

        @Test
        @DisplayName("Integer 类型的 principal 转换为 Long")
        void integerPrincipal_convertsToLong() {
            var auth = new UsernamePasswordAuthenticationToken(123, null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertEquals(123L, ZerxSecurityUtils.getCurrentUserId());
        }

        @Test
        @DisplayName("非数字字符串返回 null")
        void nonNumericString_returnsNull() {
            var auth = new UsernamePasswordAuthenticationToken("abc", null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertNull(ZerxSecurityUtils.getCurrentUserId());
        }

        @Test
        @DisplayName("getCurrentUserIdOptional 包装正确")
        void optional_wrapsCorrectly() {
            assertNull(ZerxSecurityUtils.getCurrentUserId());
            assertFalse(ZerxSecurityUtils.getCurrentUserIdOptional().isPresent());

            setAuth(42L, List.of());
            assertEquals(42L, ZerxSecurityUtils.getCurrentUserIdOptional().orElse(-1L));
        }
    }

    @Nested
    @DisplayName("getCurrentRoles 测试")
    class GetCurrentRolesTest {

        @Test
        @DisplayName("未认证时返回空列表")
        void notAuthenticated_returnsEmptyList() {
            assertTrue(ZerxSecurityUtils.getCurrentRoles().isEmpty());
        }

        @Test
        @DisplayName("自动去除 ROLE_ 前缀")
        void rolesWithPrefix_stripsPrefix() {
            setAuth(1L, List.of("ROLE_ADMIN", "ROLE_USER"));
            var roles = ZerxSecurityUtils.getCurrentRoles();
            assertEquals(2, roles.size());
            assertTrue(roles.contains("ADMIN"));
            assertTrue(roles.contains("USER"));
        }

        @Test
        @DisplayName("无 ROLE_ 前缀的角色保持原样")
        void rolesWithoutPrefix_keptAsIs() {
            setAuth(1L, List.of("ADMIN", "USER"));
            var roles = ZerxSecurityUtils.getCurrentRoles();
            assertEquals(2, roles.size());
            assertTrue(roles.contains("ADMIN"));
            assertTrue(roles.contains("USER"));
        }

        @Test
        @DisplayName("混合前缀的角色正确处理")
        void mixedPrefix_handlesCorrectly() {
            setAuth(1L, List.of("ADMIN", "ROLE_EDITOR"));
            var roles = ZerxSecurityUtils.getCurrentRoles();
            assertEquals(2, roles.size());
            assertTrue(roles.contains("ADMIN"));
            assertTrue(roles.contains("EDITOR"));
        }
    }

    @Nested
    @DisplayName("hasRole 测试")
    class HasRoleTest {

        @Test
        @DisplayName("未认证时返回 false")
        void notAuthenticated_returnsFalse() {
            assertFalse(ZerxSecurityUtils.hasRole("ADMIN"));
        }

        @Test
        @DisplayName("具有角色返回 true")
        void hasRole_returnsTrue() {
            setAuth(1L, List.of("ADMIN", "USER"));
            assertTrue(ZerxSecurityUtils.hasRole("ADMIN"));
            assertTrue(ZerxSecurityUtils.hasRole("admin")); // 不区分大小写
        }

        @Test
        @DisplayName("不具有角色返回 false")
        void notHaveRole_returnsFalse() {
            setAuth(1L, List.of("USER"));
            assertFalse(ZerxSecurityUtils.hasRole("ADMIN"));
        }

        @Test
        @DisplayName("null 和空字符串返回 false")
        void nullAndBlank_returnsFalse() {
            assertFalse(ZerxSecurityUtils.hasRole(null));
            assertFalse(ZerxSecurityUtils.hasRole(""));
            assertFalse(ZerxSecurityUtils.hasRole("  "));
        }

        @Test
        @DisplayName("ROLE_ 前缀自动处理")
        void roleWithPrefix_handledCorrectly() {
            setAuth(1L, List.of("ROLE_ADMIN"));
            assertTrue(ZerxSecurityUtils.hasRole("ADMIN"));
            assertTrue(ZerxSecurityUtils.hasRole("ROLE_ADMIN"));
        }
    }

    @Nested
    @DisplayName("isAuthenticated 测试")
    class IsAuthenticatedTest {

        @Test
        @DisplayName("无认证信息返回 false")
        void noAuth_returnsFalse() {
            assertFalse(ZerxSecurityUtils.isAuthenticated());
        }

        @Test
        @DisplayName("有认证信息返回 true")
        void hasAuth_returnsTrue() {
            setAuth(1L, List.of());
            assertTrue(ZerxSecurityUtils.isAuthenticated());
        }

        @Test
        @DisplayName("anonymousUser 返回 false")
        void anonymousUser_returnsFalse() {
            var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
            SecurityContextHolder.getContext().setAuthentication(auth);
            assertFalse(ZerxSecurityUtils.isAuthenticated());
        }
    }

    private void setAuth(Object principal, List<String> roles) {
        var authorities = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .toList();
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
