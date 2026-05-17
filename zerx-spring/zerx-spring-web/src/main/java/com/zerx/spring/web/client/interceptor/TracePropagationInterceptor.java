package com.zerx.spring.web.client.interceptor;

import com.zerx.spring.web.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * 链路追踪透传拦截器
 * <p>
 * 从当前线程的 {@link RequestContext} 中提取 TraceID，
 * 自动注入到出站请求的 {@code X-Trace-Id} Header 中，
 * 实现跨服务调用时的全链路追踪闭环。
 * </p>
 *
 * <h3>性能设计：</h3>
 * <ul>
 *   <li>ThreadLocal 读取为 O(1) 操作，零分配开销</li>
 *   <li>无 RequestContext 时直接跳过，不产生额外对象</li>
 *   <li>不影响已有 Header 值（仅在新请求中注入）</li>
 * </ul>
 *
 * @author zerx
 */
public class TracePropagationInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TracePropagationInterceptor.class);

    /** 链路追踪 Header 名称 */
    static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {
        String traceId = RequestContext.getTraceId();
        if (traceId != null && !traceId.isBlank()) {
            if (!request.getHeaders().containsKey(TRACE_ID_HEADER)) {
                request.getHeaders().add(TRACE_ID_HEADER, traceId);
            }
            if (log.isTraceEnabled()) {
                log.trace("Propagating traceId={} to {}", traceId, request.getURI());
            }
        }
        return execution.execute(request, body);
    }
}
