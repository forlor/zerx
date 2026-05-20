package com.zerx.spring.web.context;

import com.zerx.common.util.UuidUtil;

/**
 * 请求上下文 — 基于 ThreadLocal 的请求级别信息载体
 * <p>
 * 在请求进入时通过拦截器初始化，在请求完成后清理，存储当前请求的
 * 用户身份、租户信息、链路追踪 ID、客户端 IP 等数据。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在拦截器或过滤器中初始化
 * RequestContext.init();
 * RequestContext.setUserId(1001L);
 * RequestContext.setTraceId("abc123");
 *
 * // 在业务代码中读取
 * Long userId = RequestContext.getUserId();
 * String traceId = RequestContext.getTraceId();
 *
 * // 在 finally 中清理
 * RequestContext.clear();
 * }</pre>
 *
 * @author zerx
 */
public final class RequestContext {

    /** ThreadLocal 持有器 */
    private static final ThreadLocal<RequestContext> HOLDER = new ThreadLocal<>();

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 租户 ID */
    private String tenantId;

    /** 链路追踪 ID */
    private String traceId;

    /** 客户端 IP 地址 */
    private String requestIp;

    /** 请求唯一标识 */
    private String requestId;

    /**
     * 私有构造，防止外部实例化
     */
    private RequestContext() {
    }

    /**
     * 初始化请求上下文
     * <p>
     * 每次请求进入时应调用此方法创建新的上下文实例。
     * 如果当前线程已有上下文，将先清除再创建。
     * </p>
     *
     * @return 新创建的请求上下文实例
     */
    public static RequestContext init() {
        clear();
        var ctx = new RequestContext();
        ctx.requestId = UuidUtil.uuidv7Hex();
        HOLDER.set(ctx);
        return ctx;
    }

    /**
     * 获取当前线程的请求上下文
     *
     * @return 当前请求上下文，可能为 {@code null}
     */
    public static RequestContext get() {
        return HOLDER.get();
    }

    /**
     * 清除当前线程的请求上下文
     * <p>
     * 请求处理完成后（finally 块中）必须调用，防止内存泄漏。
     * </p>
     */
    public static void clear() {
        HOLDER.remove();
    }

    // ======================== 静态便捷方法 ========================

    /**
     * 获取用户 ID
     *
     * @return 用户 ID，可能为 {@code null}
     */
    public static Long getUserId() {
        var ctx = get();
        return ctx != null ? ctx.userId : null;
    }

    /**
     * 设置用户 ID
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        requireContext().userId = userId;
    }

    /**
     * 获取用户名
     *
     * @return 用户名，可能为 {@code null}
     */
    public static String getUsername() {
        var ctx = get();
        return ctx != null ? ctx.username : null;
    }

    /**
     * 设置用户名
     *
     * @param username 用户名
     */
    public static void setUsername(String username) {
        requireContext().username = username;
    }

    /**
     * 获取租户 ID
     *
     * @return 租户 ID，可能为 {@code null}
     */
    public static String getTenantId() {
        var ctx = get();
        return ctx != null ? ctx.tenantId : null;
    }

    /**
     * 设置租户 ID
     *
     * @param tenantId 租户 ID
     */
    public static void setTenantId(String tenantId) {
        requireContext().tenantId = tenantId;
    }

    /**
     * 获取链路追踪 ID
     *
     * @return 链路追踪 ID，可能为 {@code null}
     */
    public static String getTraceId() {
        var ctx = get();
        return ctx != null ? ctx.traceId : null;
    }

    /**
     * 设置链路追踪 ID
     *
     * @param traceId 链路追踪 ID
     */
    public static void setTraceId(String traceId) {
        requireContext().traceId = traceId;
    }

    /**
     * 获取客户端 IP 地址
     *
     * @return 客户端 IP，可能为 {@code null}
     */
    public static String getRequestIp() {
        var ctx = get();
        return ctx != null ? ctx.requestIp : null;
    }

    /**
     * 设置客户端 IP 地址
     *
     * @param requestIp 客户端 IP
     */
    public static void setRequestIp(String requestIp) {
        requireContext().requestIp = requestIp;
    }

    /**
     * 获取请求唯一标识
     *
     * @return 请求 ID，可能为 {@code null}
     */
    public static String getRequestId() {
        var ctx = get();
        return ctx != null ? ctx.requestId : null;
    }

    /**
     * 确保当前线程存在请求上下文，否则抛出异常
     *
     * @return 当前请求上下文
     * @throws IllegalStateException 当前线程未初始化请求上下文
     */
    private static RequestContext requireContext() {
        var ctx = get();
        if (ctx == null) {
            throw new IllegalStateException("RequestContext not initialized. Call RequestContext.init() first.");
        }
        return ctx;
    }
}
