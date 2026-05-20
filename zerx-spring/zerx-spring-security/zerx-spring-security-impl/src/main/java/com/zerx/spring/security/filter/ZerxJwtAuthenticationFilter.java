package com.zerx.spring.security.filter;

import com.zerx.spring.security.properties.ZerxSecurityProperties;
import com.zerx.spring.security.token.ZerxPermissionService;
import com.zerx.spring.security.token.ZerxRoleService;
import com.zerx.spring.security.token.ZerxTokenClaims;
import com.zerx.spring.security.token.ZerxTokenService;
import com.zerx.spring.web.context.RequestContext;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.annotation.Nullable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器 — 从请求头中提取并验证 JWT 令牌
 * <p>
 * 继承 {@link OncePerRequestFilter}，确保每个请求只执行一次过滤逻辑。
 * 从 {@code Authorization: Bearer <token>} 请求头中提取令牌，
 * 验证后将用户身份、RBAC 角色和细粒度权限注入 Spring Security 上下文和请求上下文。
 * </p>
 *
 * <h3>角色加载策略：</h3>
 * <ol>
 *   <li>如果存在 {@link ZerxRoleService} Bean → 从 SPI 按需加载角色（推荐）</li>
 *   <li>否则 → 从 JWT claims 中的 roles 字段获取（向后兼容）</li>
 * </ol>
 *
 * <h3>权限加载策略：</h3>
 * <ol>
 *   <li>如果存在 {@link ZerxPermissionService} Bean → 从 SPI 实时加载权限</li>
 *   <li>否则 → 无细粒度权限（仅保留角色）</li>
 * </ol>
 *
 * <h3>处理流程：</h3>
 * <ol>
 *   <li>从请求头提取 Bearer Token</li>
 *   <li>解析并验证令牌（仅解析一次，同时完成签名+过期+黑名单校验）</li>
 *   <li>校验令牌类型（仅 access token 可用于 API 认证）</li>
 *   <li>加载角色：优先 SPI，回退到 JWT claims</li>
 *   <li>加载权限：如果存在 ZerxPermissionService SPI，实时加载权限编码</li>
 *   <li>将角色和权限转换为 Spring Security {@link GrantedAuthority}</li>
 *   <li>创建 {@link UsernamePasswordAuthenticationToken} 并设置到 {@link SecurityContextHolder}</li>
 *   <li>将 userId 设置到 {@link RequestContext}</li>
 *   <li>验证失败时清除 SecurityContext，通过请求属性传递失败原因给 EntryPoint</li>
 * </ol>
 *
 * @author zerx
 */
public class ZerxJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ZerxJwtAuthenticationFilter.class);

    /** 仅允许用于 API 认证的令牌类型 */
    private static final String ALLOWED_TOKEN_TYPE = "access";

    /** 请求属性：认证失败原因，供 {@link com.zerx.spring.security.handler.ZerxAuthenticationEntryPoint} 读取 */
    public static final String ATTR_AUTH_ERROR = "zerx.auth.error";

    /** 认证失败原因：令牌已过期 */
    public static final String AUTH_ERROR_TOKEN_EXPIRED = "token_expired";

    /** 认证失败原因：令牌无效（签名错误、格式错误等） */
    public static final String AUTH_ERROR_TOKEN_INVALID = "token_invalid";

    /** 认证失败原因：令牌已被加入黑名单 */
    public static final String AUTH_ERROR_TOKEN_BLACKLISTED = "token_blacklisted";

    /** 认证失败原因：令牌类型不匹配（如 refresh token 用于 API 认证） */
    public static final String AUTH_ERROR_TOKEN_TYPE_REJECTED = "token_type_rejected";

    private final ZerxTokenService tokenService;
    private final ZerxSecurityProperties properties;
    @Nullable
    private final ZerxRoleService roleService;
    @Nullable
    private final ZerxPermissionService permissionService;

    /**
     * 构造 JWT 认证过滤器（无角色服务和权限服务，从 JWT claims 加载角色）
     *
     * @param tokenService JWT 令牌服务
     * @param properties   安全配置属性
     */
    public ZerxJwtAuthenticationFilter(ZerxTokenService tokenService,
                                       ZerxSecurityProperties properties) {
        this(tokenService, properties, null, null);
    }

    /**
     * 构造 JWT 认证过滤器（支持角色服务按需加载）
     *
     * @param tokenService JWT 令牌服务
     * @param properties   安全配置属性
     * @param roleService  角色加载服务（可选，为 null 时从 JWT claims 加载）
     */
    public ZerxJwtAuthenticationFilter(ZerxTokenService tokenService,
                                       ZerxSecurityProperties properties,
                                       @Nullable ZerxRoleService roleService) {
        this(tokenService, properties, roleService, null);
    }

    /**
     * 构造 JWT 认证过滤器（支持角色服务和权限服务按需加载）
     *
     * @param tokenService      JWT 令牌服务
     * @param properties        安全配置属性
     * @param roleService       角色加载服务（可选，为 null 时从 JWT claims 加载）
     * @param permissionService 权限加载服务（可选，为 null 时无细粒度权限）
     */
    public ZerxJwtAuthenticationFilter(ZerxTokenService tokenService,
                                       ZerxSecurityProperties properties,
                                       @Nullable ZerxRoleService roleService,
                                       @Nullable ZerxPermissionService permissionService) {
        this.tokenService = tokenService;
        this.properties = properties;
        this.roleService = roleService;
        this.permissionService = permissionService;
        if (roleService != null) {
            log.info("JWT filter: using ZerxRoleService for on-demand role loading");
        } else {
            log.info("JWT filter: using JWT claims for role loading (backward compatible)");
        }
        if (permissionService != null) {
            log.info("JWT filter: using ZerxPermissionService for on-demand permission loading");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 从请求头提取 JWT 令牌，仅解析一次完成签名+过期+黑名单校验，
     * 验证后将用户身份和角色注入安全上下文。
     * 任何验证失败都不阻塞请求，仅清除安全上下文并设置请求属性传递失败原因，
     * 交由后续 EntryPoint 处理 401 响应。
     * </p>
     */
    @Override
    protected void doFilterInternal(@SuppressWarnings("NullableProblems") HttpServletRequest request,
                                    @SuppressWarnings("NullableProblems") HttpServletResponse response,
                                    @SuppressWarnings("NullableProblems") FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                try {
                    // 仅解析一次：验证签名 + 过期时间，同时提取 claims
                    ZerxTokenClaims claims = tokenService.parseToken(token);

                    // 检查黑名单
                    if (tokenService.isBlacklisted(claims.jti())) {
                        log.debug("Token is blacklisted: jti={}", claims.jti());
                        request.setAttribute(ATTR_AUTH_ERROR, AUTH_ERROR_TOKEN_BLACKLISTED);
                        SecurityContextHolder.clearContext();
                    }
                    // 校验令牌类型：仅 access token 可用于 API 认证
                    else if (!ALLOWED_TOKEN_TYPE.equals(claims.tokenType())) {
                        log.debug("Rejected non-access token: tokenType={}, jti={}",
                                claims.tokenType(), claims.jti());
                        request.setAttribute(ATTR_AUTH_ERROR, AUTH_ERROR_TOKEN_TYPE_REJECTED);
                        SecurityContextHolder.clearContext();
                    }
                    // 认证通过：加载角色和权限并设置安全上下文
                    else {
                        // 加载角色：优先 SPI 按需加载，回退到 JWT claims
                        List<String> roles = loadRoles(claims);

                        // 将 roles 转换为 Spring Security GrantedAuthority，自动添加 ROLE_ 前缀
                        List<GrantedAuthority> authorities = new ArrayList<>(
                                roles.stream()
                                        .map(role -> (GrantedAuthority) () -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                        .toList());

                        // 加载权限：如果存在 ZerxPermissionService SPI，实时加载权限
                        List<String> permissions = loadPermissions(claims.userId());
                        // 将权限转换为 GrantedAuthority，添加 PERM_ 前缀
                        permissions.stream()
                                .map(perm -> (GrantedAuthority) () -> perm.startsWith("PERM_") ? perm : "PERM_" + perm)
                                .forEach(authorities::add);

                        var authentication = new UsernamePasswordAuthenticationToken(
                                claims.userId(), null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 将用户 ID 设置到请求上下文
                        if (RequestContext.get() != null) {
                            RequestContext.setUserId(claims.userId());
                        }
                    }
                } catch (ExpiredJwtException ex) {
                    log.debug("Token expired: {}", ex.getMessage());
                    request.setAttribute(ATTR_AUTH_ERROR, AUTH_ERROR_TOKEN_EXPIRED);
                    SecurityContextHolder.clearContext();
                } catch (Exception ex) {
                    log.debug("JWT authentication failed: {}", ex.getMessage());
                    request.setAttribute(ATTR_AUTH_ERROR, AUTH_ERROR_TOKEN_INVALID);
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            log.debug("JWT authentication unexpected error: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 加载用户角色
     * <p>
     * 如果 {@link ZerxRoleService} 可用，从 SPI 按需加载角色；
     * 否则从 JWT claims 中的 roles 字段获取（向后兼容）。
     * 对 SPI 返回值做 null 防护，避免空指针异常。
     * </p>
     *
     * @param claims 令牌声明
     * @return 角色列表，不会为 null
     */
    private List<String> loadRoles(ZerxTokenClaims claims) {
        if (roleService != null) {
            try {
                List<String> roles = roleService.getRoles(claims.userId());
                if (roles != null) {
                    return roles;
                }
            } catch (Exception ex) {
                log.warn("ZerxRoleService failed to load roles for userId={}, falling back to JWT claims: {}",
                        claims.userId(), ex.getMessage());
            }
        }
        return claims.roles() != null ? claims.roles() : List.of();
    }

    /**
     * 加载用户权限
     * <p>
     * 如果 {@link ZerxPermissionService} 可用，从 SPI 实时加载权限编码；
     * 否则返回空列表（仅保留角色权限）。
     * 对 SPI 返回值做 null 防护，加载异常时返回空列表。
     * </p>
     *
     * @param userId 用户 ID
     * @return 权限编码列表，不会为 null
     */
    private List<String> loadPermissions(Long userId) {
        if (permissionService != null) {
            try {
                List<String> permissions = permissionService.getPermissions(userId);
                if (permissions != null) {
                    return permissions;
                }
            } catch (Exception ex) {
                log.warn("ZerxPermissionService failed to load permissions for userId={}: {}",
                        userId, ex.getMessage());
            }
        }
        return List.of();
    }

    /**
     * 从请求头中提取 Bearer Token
     * <p>
     * 按照配置的 headerName（默认 {@code Authorization}）和
     * headerPrefix（默认 {@code Bearer }）进行提取。
     * </p>
     *
     * @param request HTTP 请求
     * @return 令牌字符串，提取失败返回 {@code null}
     */
    private String extractToken(HttpServletRequest request) {
        var jwtConfig = properties.getJwt();
        String header = request.getHeader(jwtConfig.getHeaderName());
        if (header != null && header.startsWith(jwtConfig.getHeaderPrefix())) {
            return header.substring(jwtConfig.getHeaderPrefix().length());
        }
        return null;
    }
}
