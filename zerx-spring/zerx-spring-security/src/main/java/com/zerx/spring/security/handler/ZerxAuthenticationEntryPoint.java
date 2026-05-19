package com.zerx.spring.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.model.Result;
import com.zerx.spring.security.filter.ZerxJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Map;

/**
 * 未认证处理器 — 处理 401 Unauthorized 响应
 * <p>
 * 当未认证用户访问受保护资源时，Spring Security 调用此处理器，
 * 以 JSON 格式返回统一的错误响应。
 * </p>
 *
 * <h3>错误粒度：</h3>
 * <p>
 * 根据过滤器设置的请求属性 {@link ZerxJwtAuthenticationFilter#ATTR_AUTH_ERROR}
 * 区分不同的认证失败原因，返回对应的错误码和消息：
 * </p>
 * <ul>
 *   <li>{@code token_expired} — 令牌已过期</li>
 *   <li>{@code token_invalid} — 令牌签名无效或格式错误</li>
 *   <li>{@code token_blacklisted} — 令牌已被加入黑名单（已注销）</li>
 *   <li>{@code token_type_rejected} — 令牌类型不匹配</li>
 *   <li>无属性（默认）— 未提供认证凭据</li>
 * </ul>
 *
 * @author zerx
 */
public class ZerxAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(ZerxAuthenticationEntryPoint.class);

    /** 共享 ObjectMapper 实例（线程安全） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void commence(@SuppressWarnings("NullableProblems") HttpServletRequest request,
                         @SuppressWarnings("NullableProblems") HttpServletResponse response,
                         @SuppressWarnings("NullableProblems") AuthenticationException authException)
            throws IOException {
        String errorCode = (String) request.getAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR);

        String code;
        String message;
        if (errorCode == null) {
            // 无令牌或未携带认证信息
            code = "401";
            message = "未认证，请先登录";
        } else {
            code = switch (errorCode) {
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_EXPIRED -> "TOKEN_EXPIRED";
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_BLACKLISTED -> "TOKEN_BLACKLISTED";
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_INVALID -> "TOKEN_INVALID";
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_TYPE_REJECTED -> "TOKEN_INVALID";
                default -> "401";
            };
            message = switch (errorCode) {
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_EXPIRED -> "令牌已过期，请重新登录";
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_BLACKLISTED -> "令牌已失效，请重新登录";
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_INVALID -> "无效的认证令牌";
                case ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_TYPE_REJECTED -> "无效的认证令牌";
                default -> "未认证，请先登录";
            };
        }

        log.debug("Unauthorized access: {}, errorCode={}", request.getRequestURI(), errorCode);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(
                Result.fail(code, message)));
    }
}
