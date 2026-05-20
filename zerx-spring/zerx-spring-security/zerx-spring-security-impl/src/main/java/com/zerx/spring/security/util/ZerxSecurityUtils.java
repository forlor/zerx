package com.zerx.spring.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 安全工具类 — 便捷获取当前认证用户信息
 * <p>
 * 基于 {@link SecurityContextHolder} 封装常用的安全上下文操作，
 * 为业务层提供简洁的静态方法，避免直接操作 Spring Security API。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取当前用户 ID
 * Long userId = ZerxSecurityUtils.getCurrentUserId();
 *
 * // 获取当前用户角色列表
 * List<String> roles = ZerxSecurityUtils.getCurrentRoles();
 *
 * // 检查是否具有指定角色
 * boolean isAdmin = ZerxSecurityUtils.hasRole("ADMIN");
 *
 * // 检查是否具有指定权限
 * boolean canCreate = ZerxSecurityUtils.hasPermission("user:create");
 *
 * // 检查是否具有任一权限
 * boolean canExport = ZerxSecurityUtils.hasAnyPermission("order:export", "order:exportAll");
 *
 * // 判断是否已认证
 * boolean authenticated = ZerxSecurityUtils.isAuthenticated();
 * }</pre>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>所有方法从当前线程的 {@link SecurityContextHolder} 获取认证信息</li>
 *   <li>在 {@code @Async} 方法中调用前，需确保 SecurityContext 已传播（框架已自动配置）</li>
 *   <li>未认证时，各方法返回安全的默认值（null / 空列表 / false）</li>
 * </ul>
 *
 * @author zerx
 */
public final class ZerxSecurityUtils {

    private ZerxSecurityUtils() {
        // 工具类不允许实例化
    }

    /**
     * 获取当前认证用户 ID
     * <p>
     * 从 {@link SecurityContextHolder} 中提取 principal，
     * 如果 principal 为 {@code Long} 类型直接返回，否则尝试转换为 {@code Long}。
     * </p>
     *
     * @return 当前用户 ID，未认证时返回 {@code null}
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Number num) {
            return num.longValue();
        }
        if (principal instanceof String str && !str.isEmpty()) {
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取当前认证用户 ID（Optional 版本）
     *
     * @return 当前用户 ID 的 Optional，未认证时返回 {@link Optional#empty()}
     */
    public static Optional<Long> getCurrentUserIdOptional() {
        return Optional.ofNullable(getCurrentUserId());
    }

    /**
     * 获取当前用户的角色列表
     * <p>
     * 从 {@link Authentication#getAuthorities()} 中提取权限列表，
     * 自动去除 {@code ROLE_} 前缀后返回。
     * </p>
     *
     * @return 角色编码列表（不含 ROLE_ 前缀），未认证时返回空列表
     */
    public static List<String> getCurrentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return List.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authStr -> {
                    if (authStr.startsWith("ROLE_")) {
                        return authStr.substring(5);
                    }
                    return authStr;
                })
                .toList();
    }

    /**
     * 判断当前用户是否具有指定角色
     * <p>
     * 角色比较不区分大小写，自动处理 {@code ROLE_} 前缀。
     * </p>
     *
     * @param role 角色编码（不含 ROLE_ 前缀）
     * @return 具有该角色返回 {@code true}，否则返回 {@code false}
     */
    public static boolean hasRole(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String target = role.startsWith("ROLE_") ? role.substring(5) : role;
        return getCurrentRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase(target));
    }

    /**
     * 判断当前用户是否已认证
     * <p>
     * 当 {@link SecurityContextHolder} 中存在非空的 {@link Authentication}，
     * 且 principal 不为匿名用户时，视为已认证。
     * </p>
     *
     * @return 已认证返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }
        String name = auth.getPrincipal().toString();
        // Spring Security 匿名用户的 principal 为 "anonymousUser"
        return !"anonymousUser".equals(name);
    }

    /** 权限 authority 前缀，与 {@code ZerxJwtAuthenticationFilter} 中的格式一致 */
    private static final String PERM_PREFIX = "PERM_";

    /**
     * 获取当前用户的权限列表
     * <p>
     * 从 {@link Authentication#getAuthorities()} 中提取以 {@code PERM_} 为前缀的权限，
     * 自动去除前缀后返回原始权限编码。
     * </p>
     *
     * @return 权限编码列表（不含 PERM_ 前缀），未认证或无权限时返回空列表
     */
    public static List<String> getPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return List.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authStr -> authStr.startsWith(PERM_PREFIX))
                .map(authStr -> authStr.substring(PERM_PREFIX.length()))
                .toList();
    }

    /**
     * 判断当前用户是否拥有指定权限
     *
     * @param permission 权限编码（如 {@code user:create}），不含 PERM_ 前缀
     * @return 拥有该权限返回 {@code true}，否则返回 {@code false}
     */
    public static boolean hasPermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return false;
        }
        return getPermissions().contains(permission);
    }

    /**
     * 判断当前用户是否拥有任一指定权限
     *
     * @param permissions 权限编码列表，不含 PERM_ 前缀
     * @return 拥有其中任一权限返回 {@code true}，否则返回 {@code false}
     */
    public static boolean hasAnyPermission(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> currentPermissions = Set.copyOf(getPermissions());
        for (String permission : permissions) {
            if (permission != null && !permission.isBlank() && currentPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前认证对象
     *
     * @return 当前 Authentication，未认证时返回 {@code null}
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
