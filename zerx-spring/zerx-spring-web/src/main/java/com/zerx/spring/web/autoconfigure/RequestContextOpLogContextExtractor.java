package com.zerx.spring.web.autoconfigure;

import com.zerx.common.logging.OpLogContextExtractor;
import com.zerx.spring.web.context.RequestContext;

/**
 * 基于 {@link RequestContext} 的操作日志上下文提取器
 * <p>
 * 从当前线程的 {@link RequestContext} 中提取用户 ID、用户名和客户端 IP，
 * 供 {@link com.zerx.spring.logging.aspect.ZerxOpLogAspect} 操作日志切面使用。
 * 当 RequestContext 未初始化时（如非 Web 请求场景），返回 {@code null}。
 * </p>
 *
 * @author zerx
 */
class RequestContextOpLogContextExtractor implements OpLogContextExtractor {

    @Override
    public OpLogContext extract() {
        RequestContext ctx = RequestContext.get();
        if (ctx == null) {
            return null;
        }
        return new OpLogContext(ctx.getUserId(), ctx.getUsername(), ctx.getRequestIp());
    }
}
