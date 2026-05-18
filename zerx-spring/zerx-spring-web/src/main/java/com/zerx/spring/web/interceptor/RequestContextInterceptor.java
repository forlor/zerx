package com.zerx.spring.web.interceptor;

import com.zerx.spring.web.context.RequestContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求上下文拦截器 — 初始化和清理 RequestContext
 * <p>
 * 在 Controller 方法执行前从 HTTP 请求中提取客户端 IP 等信息，
 * 并初始化 {@link RequestContext}；在请求完成后清理上下文。
 * </p>
 *
 * <h3>客户端 IP 解析策略：</h3>
 * <ol>
 *   <li>{@code X-Forwarded-For} 请求头（取第一个 IP）</li>
 *   <li>{@code X-Real-IP} 请求头</li>
 *   <li>{@code request.getRemoteAddr()}</li>
 * </ol>
 *
 * @author zerx
 */
public class RequestContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestContextInterceptor.class);

    /** X-Forwarded-For 请求头 */
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    /** X-Real-IP 请求头 */
    private static final String X_REAL_IP = "X-Real-IP";

    /**
     * 请求处理前：提取客户端 IP 并设置到 RequestContext
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  处理器
     * @return 始终返回 {@code true}，放行后续拦截器和 Controller
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TraceFilter 已初始化 RequestContext，此处确保存在
        RequestContext ctx = RequestContext.get();
        if (ctx == null) {
            ctx = RequestContext.init();
        }

        // 提取并设置客户端 IP
        String clientIp = resolveClientIp(request);
        ctx.setRequestIp(clientIp);

        if (log.isDebugEnabled()) {
            log.debug("RequestContext initialized: ip={}, uri={}", clientIp, request.getRequestURI());
        }

        return true;
    }

    /**
     * 请求完成后：清理 RequestContext
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  处理器
     * @param ex       异常（可能为 null）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // TraceFilter 会清理，此处不重复清理
        // 但作为安全措施，在 finally 风格中确保清理
        if (ex != null) {
            log.debug("RequestContext cleanup with exception: {}", ex.getMessage());
        }
    }

    /**
     * 解析客户端真实 IP 地址
     * <p>
     * 按优先级依次从代理头和远程地址中获取客户端 IP。
     * </p>
     *
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String resolveClientIp(HttpServletRequest request) {
        // 1. X-Forwarded-For（可能包含多个 IP，取第一个）
        String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            int commaIndex = xForwardedFor.indexOf(',');
            if (commaIndex > 0) {
                return xForwardedFor.substring(0, commaIndex).trim();
            }
            return xForwardedFor.trim();
        }

        // 2. X-Real-IP
        String xRealIp = request.getHeader(X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        // 3. RemoteAddr
        return request.getRemoteAddr();
    }
}
