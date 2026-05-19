package com.zerx.spring.security.filter;

import com.zerx.spring.security.props.ZerxSecurityProperties;
import com.zerx.spring.security.token.ZerxRoleService;
import com.zerx.spring.security.token.ZerxTokenClaims;
import com.zerx.spring.security.token.ZerxTokenService;
import com.zerx.spring.web.context.RequestContext;
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
import java.util.List;

/**
 * JWT 认证过滤器 — 从请求头中提取并验证 JWT 令牌
 * <p>
 * 继承 {@link OncePerRequestFilter}，确保每个请求只执行一次过滤逻辑。
 * 从 {@code Authorization: Bearer <token>} 请求头中提取令牌，
 * 验证后将用户身份和 RBAC 角色注入 Spring Security 上下文和请求上下文。
 * </p>
 *
 * <h3>角色加载策略：</h3>
 * <ol>
 *   <li>如果存在 {@link ZerxRoleService} Bean → 从 SPI 按需加载角色（推荐）</li>
 *   <li>否则 → 从 JWT claims 中的 roles 字段获取（向后兼容）</li>
 * </ol>
 *
 * <h3>处理流程：</h3>
 * <ol>
 *   <li>从请求头提取 Bearer Token</li>
 *   <li>使用 {@link ZerxTokenService} 解析并验证令牌</li>
 *   <li>加载角色：优先 SPI，回退到 JWT claims</li>
 *   <li>将角色转换为 Spring Security {@link GrantedAuthority}</li>
 *   <li>创建 {@link UsernamePasswordAuthenticationToken} 并设置到 {@link SecurityContextHolder}</li>
 *   <li>将 userId 设置到 {@link RequestContext}</li>
 *   <li>验证失败时清除 SecurityContext，继续过滤器链（由 Spring Security 处理 401）</li>
 * </ol>
 *
 * @author zerx
 */
public class ZerxJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ZerxJwtAuthenticationFilter.class);

    /** 仅允许用于 API 认证的令牌类型 */
    private static final String ALLOWED_TOKEN_TYPE = "access";

    private final ZerxTokenService tokenService;
    private final ZerxSecurityProperties properties;
    @Nullable
    private final ZerxRoleService roleService;

    /**
     * 构造 JWT 认证过滤器（无角色服务，从 JWT claims 加载角色）
     *
     * @param tokenService JWT 令牌服务
     * @param properties   安全配置属性
     */
    public ZerxJwtAuthenticationFilter(ZerxTokenService tokenService,
                                       ZerxSecurityProperties properties) {
        this(tokenService, properties, null);
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
        this.tokenService = tokenService;
        this.properties = properties;
        this.roleService = roleService;
        if (roleService != null) {
            log.info("JWT filter: using ZerxRoleService for on-demand role loading");
        } else {
            log.info("JWT filter: using JWT claims for role loading (backward compatible)");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 从请求头提取 JWT 令牌，验证后将用户身份和角色注入安全上下文。
     * 任何验证失败都不阻塞请求，仅清除安全上下文，交由后续过滤器处理。
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
                if (tokenService.validateToken(token)) {
                    ZerxTokenClaims claims = tokenService.parseToken(token);

                    // 校验令牌类型：仅 access token 可用于 API 认证
                    if (!ALLOWED_TOKEN_TYPE.equals(claims.tokenType())) {
                        log.debug("Rejected non-access token: tokenType={}, jti={}",
                                claims.tokenType(), claims.jti());
                        SecurityContextHolder.clearContext();
                    } else {
                        // 加载角色：优先 SPI 按需加载，回退到 JWT claims
                        List<String> roles = loadRoles(claims);

                        // 将 roles 转换为 Spring Security GrantedAuthority，自动添加 ROLE_ 前缀
                        List<GrantedAuthority> authorities = roles.stream()
                                .map(role -> (GrantedAuthority) () -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                .toList();

                        var authentication = new UsernamePasswordAuthenticationToken(
                                claims.userId(), null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // 将用户 ID 设置到请求上下文
                        if (RequestContext.get() != null) {
                            RequestContext.setUserId(claims.userId());
                        }
                    }

                } else {
                    // 令牌无效，清除安全上下文
                    SecurityContextHolder.clearContext();
                }
            }
        } catch (Exception ex) {
            log.debug("JWT authentication failed: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 加载用户角色
     * <p>
     * 如果 {@link ZerxRoleService} 可用，从 SPI 按需加载角色；
     * 否则从 JWT claims 中的 roles 字段获取（向后兼容）。
     * </p>
     *
     * @param claims 令牌声明
     * @return 角色列表
     */
    private List<String> loadRoles(ZerxTokenClaims claims) {
        if (roleService != null) {
            try {
                return roleService.getRoles(claims.userId());
            } catch (Exception ex) {
                log.warn("ZerxRoleService failed to load roles for userId={}, falling back to JWT claims: {}",
                        claims.userId(), ex.getMessage());
                return claims.roles();
            }
        }
        return claims.roles();
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
