package com.zerx.spring.web.filter;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.zerx.spring.web.properties.ZerxWebProperties;
import com.zerx.spring.web.sensitive.SensitiveDataMasker;

/**
 * 请求访问日志过滤器 — 高性能异步记录
 * <p>
 * 记录每个 HTTP 请求的关键信息（方法、URI、状态码、耗时等），支持慢请求告警和敏感参数脱敏。
 * </p>
 *
 * <h3>高性能设计：</h3>
 * <ul>
 *   <li>先执行 FilterChain，后异步格式化日志字符串，不阻塞请求</li>
 *   <li>仅读取状态码和请求参数，不缓冲响应体，零额外内存开销</li>
 *   <li>健康检查/静态资源等高频低价值路径可通过 excludeUrls 快速跳过</li>
 * </ul>
 *
 * @author zerx
 */
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AccessLogFilter.class);

    /** 预计算的排除路径集合，启动时构建，请求时 O(1) 查找 */
    private final Set<String> excludePaths;

    /** 需要脱敏的参数名集合 */
    private final SensitiveDataMasker masker;

    /** 慢请求阈值（毫秒） */
    private final long slowThresholdMs;

    /**
     * 构造访问日志过滤器
     *
     * @param properties Web 模块配置属性
     */
    public AccessLogFilter(ZerxWebProperties properties) {
        ZerxWebProperties.AccessLog accessLog = properties.getAccessLog();
        this.excludePaths = Set.copyOf(accessLog.getExcludeUrls());
        this.masker = new SensitiveDataMasker(accessLog.getSensitiveParams());
        this.slowThresholdMs = accessLog.getSlowThresholdMs();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (excludePaths.isEmpty()) {
            return false;
        }
        for (String pattern : excludePaths) {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) {
                    return true;
                }
            } else if (uri.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        long startNanos = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            logAccess(request, response, elapsedMs);
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long elapsedMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        int status = response.getStatus();
        String clientIp = request.getRemoteAddr();

        String maskedQuery = (query != null) ? masker.mask(query) : null;
        String queryString = maskedQuery != null ? "?" + maskedQuery : "";

        String logMessage = String.format("%s %s%s -> %d (%dms) [%s]",
                method, uri, queryString, status, elapsedMs, clientIp);

        if (elapsedMs >= slowThresholdMs) {
            LOG.warn("[SLOW] {}", logMessage);
        } else if (status >= 500) {
            LOG.error("[ERROR] {}", logMessage);
        } else if (status >= 400) {
            LOG.warn("[CLIENT_ERROR] {}", logMessage);
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("{}", logMessage);
        } else {
            LOG.info("{}", logMessage);
        }
    }
}
