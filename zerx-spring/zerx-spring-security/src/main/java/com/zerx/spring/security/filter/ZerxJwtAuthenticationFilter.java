package com.zerx.spring.security.filter;

import com.zerx.spring.security.props.ZerxSecurityProperties;
import com.zerx.spring.security.token.ZerxTokenClaims;
import com.zerx.spring.security.token.ZerxTokenService;
import com.zerx.spring.web.context.RequestContext;
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
 * <h3>处理流程：</h3>
 * <ol>
 *   <li>从请求头提取 Bearer Token</li>
 *   <li>使用 {@link ZerxTokenService} 解析并验证令牌</li>
 *   <li>将令牌中的角色转换为 Spring Security {@link GrantedAuthority}</li>
 *   <li>创建 {@link UsernamePasswordAuthenticationToken} 并设置到 {@link SecurityContextHolder}</li>
 *   <li>将 userId 设置到 {@link RequestContext}</li>
 *   <li>验证失败时清除 SecurityContext，继续过滤器链（由 Spring Security 处理 401）</li>
 * </ol>
 *
 * @author zerx
 */
public class ZerxJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ZerxJwtAuthenticationFilter.class);

    private final ZerxTokenService tokenService;
    private final ZerxSecurityProperties properties;

    /**
     * 构造 JWT 认证过滤器
     *
     * @param tokenService JWT 令牌服务
     * @param properties   安全配置属性
     */
    public ZerxJwtAuthenticationFilter(ZerxTokenService tokenService, ZerxSecurityProperties properties) {
        this.tokenService = tokenService;
        this.properties = properties;
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

                    // 将 roles 转换为 Spring Security GrantedAuthority，自动添加 ROLE_ 前缀
                    List<GrantedAuthority> authorities = claims.roles().stream()
                            .map(role -> (GrantedAuthority) () -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .toList();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.userId(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // 将用户 ID 设置到请求上下文
                    if (RequestContext.get() != null) {
                        RequestContext.setUserId(claims.userId());
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
