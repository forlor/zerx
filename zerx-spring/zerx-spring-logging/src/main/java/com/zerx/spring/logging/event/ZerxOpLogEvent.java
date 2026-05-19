package com.zerx.spring.logging.event;

import com.zerx.spring.logging.annotation.ZerxOpLog;

import java.time.Instant;
import java.util.Map;

/**
 * 操作日志领域事件
 * <p>
 * 由 {@link com.zerx.spring.logging.aspect.ZerxOpLogAspect} 在方法执行后构建。
 * 包含完整的操作上下文信息，业务层通过监听此事件实现持久化。
 * </p>
 *
 * @author zerx
 */
public record ZerxOpLogEvent(
        /** 链路追踪 ID */
        String traceId,
        /** 操作用户 ID */
        Long userId,
        /** 操作用户名 */
        String username,
        /** 操作模块 */
        String module,
        /** 操作类型 */
        ZerxOpLog.Type type,
        /** 操作描述 */
        String description,
        /** 目标类名 */
        String className,
        /** 目标方法名 */
        String methodName,
        /** 参数名数组 */
        String[] paramNames,
        /** 参数值数组 */
        Object[] paramValues,
        /** 返回值（失败时为 null） */
        Object result,
        /** 执行耗时（毫秒） */
        long durationMs,
        /** 异常信息（成功时为 null） */
        Throwable exception,
        /** 客户端 IP */
        String clientIp,
        /** 事件时间戳 */
        Instant timestamp,
        /** 扩展信息 */
        Map<String, Object> extra
) {

    /**
     * 创建成功操作日志
     *
     * @param traceId     链路追踪 ID
     * @param userId      操作用户 ID
     * @param username    操作用户名
     * @param module      操作模块
     * @param type        操作类型
     * @param description 操作描述
     * @param className   目标类名
     * @param methodName  目标方法名
     * @param paramNames  参数名数组
     * @param paramValues 参数值数组
     * @param result      返回值
     * @param durationMs  执行耗时（毫秒）
     * @param clientIp    客户端 IP
     * @param extra       扩展信息
     * @return 操作日志事件
     */
    public static ZerxOpLogEvent success(String traceId, Long userId, String username,
                                         String module, ZerxOpLog.Type type, String description,
                                         String className, String methodName,
                                         String[] paramNames, Object[] paramValues,
                                         Object result, long durationMs,
                                         String clientIp, Map<String, Object> extra) {
        return new ZerxOpLogEvent(traceId, userId, username, module, type, description,
                className, methodName, paramNames, paramValues, result, durationMs,
                null, clientIp, Instant.now(), extra);
    }

    /**
     * 创建失败操作日志
     *
     * @param traceId     链路追踪 ID
     * @param userId      操作用户 ID
     * @param username    操作用户名
     * @param module      操作模块
     * @param type        操作类型
     * @param description 操作描述
     * @param className   目标类名
     * @param methodName  目标方法名
     * @param paramNames  参数名数组
     * @param paramValues 参数值数组
     * @param exception   异常信息
     * @param durationMs  执行耗时（毫秒）
     * @param clientIp    客户端 IP
     * @param extra       扩展信息
     * @return 操作日志事件
     */
    public static ZerxOpLogEvent failure(String traceId, Long userId, String username,
                                         String module, ZerxOpLog.Type type, String description,
                                         String className, String methodName,
                                         String[] paramNames, Object[] paramValues,
                                         Throwable exception, long durationMs,
                                         String clientIp, Map<String, Object> extra) {
        return new ZerxOpLogEvent(traceId, userId, username, module, type, description,
                className, methodName, paramNames, paramValues, null, durationMs,
                exception, clientIp, Instant.now(), extra);
    }

    /**
     * 判断是否为成功操作（无异常）
     *
     * @return true 表示操作成功
     */
    public boolean isSuccess() {
        return exception == null;
    }
}
