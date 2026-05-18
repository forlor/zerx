package com.zerx.spring.web.filter;

import com.zerx.spring.web.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪过滤器 — 为每个请求生成并传播 TraceID
 * <p>
 * 在请求进入时生成唯一的链路追踪 ID，设置到 {@link RequestContext} 和 SLF4J {@link MDC} 中，
 * 便于日志聚合和分布式链路追踪。请求完成后自动清理。
 * </p>
 *
 * <h3>TraceID 生成策略：</h3>
 * <ul>
 *   <li>优先从请求头 {@code X-Trace-Id} 中获取（支持上游服务传递）</li>
 *   <li>若无则生成 UUID（去除连字符的短格式）</li>
 * </ul>
 *
 * @author zerx
 */
public class TraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);

    /** 追踪 ID 请求头名称 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /** MDC 中追踪 ID 的键名 */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * 为每个请求生成或传递 TraceID，设置到 RequestContext 和 MDC
     *
     * @param request     当前 HTTP 请求
     * @param response    当前 HTTP 响应
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        try {
            // 初始化请求上下文
            var ctx = RequestContext.init();

            // 生成或获取 TraceID
            String traceId = resolveTraceId(request);
            ctx.setTraceId(traceId);

            // 设置到 MDC，方便日志框架自动输出
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 将 TraceID 写入响应头，方便客户端排查
            response.setHeader(TRACE_ID_HEADER, traceId);

            if (log.isDebugEnabled()) {
                log.debug("TraceID [{}] assigned for request: {} {}", traceId,
                        request.getMethod(), request.getRequestURI());
            }

            filterChain.doFilter(request, response);
        } finally {
            // 清理 MDC 和 RequestContext
            MDC.remove(TRACE_ID_MDC_KEY);
            RequestContext.clear();
        }
    }

    /**
     * 解析 TraceID
     * <p>
     * 优先从请求头获取（支持上游服务传递），若不存在则自动生成。
     * </p>
     *
     * @param request HTTP 请求
     * @return TraceID 字符串
     */
    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId != null && !traceId.isBlank()) {
            return traceId.trim();
        }
        // 生成短 UUID（32 位十六进制字符串）
        return UUID.randomUUID().toString().replace("-", "");
    }
}
