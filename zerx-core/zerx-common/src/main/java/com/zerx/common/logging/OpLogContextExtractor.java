package com.zerx.common.logging;

/**
 * 操作日志上下文提取器
 * <p>
 * 从当前运行环境中提取操作日志所需的用户上下文信息（用户 ID、用户名、客户端 IP）。
 * 不同运行环境（Web、消息队列、定时任务等）可提供各自的实现，
 * 使得日志模块不依赖任何特定的上下文载体。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * // 在 Web 环境中，由 zerx-spring-web 自动注册：
 * // OpLogContextExtractor 实现 → 从 RequestContext 提取
 *
 * // 在非 Web 环境中，可不注册，userId/username/clientIp 均为 null
 * // 或自行注册一个从 SecurityContext / TenantContext 提取的实现
 * }</pre>
 *
 * @author zerx
 * @see com.zerx.spring.logging.aspect.ZerxOpLogAspect
 */
@FunctionalInterface
public interface OpLogContextExtractor {

    /**
     * 提取当前操作日志上下文
     *
     * @return 操作日志上下文，如果无法提取则返回 {@code null}
     */
    OpLogContext extract();

    /**
     * 操作日志上下文数据
     *
     * @param userId   操作用户 ID，可为 {@code null}
     * @param username 操作用户名，可为 {@code null}
     * @param clientIp 客户端 IP 地址，可为 {@code null}
     */
    record OpLogContext(Long userId, String username, String clientIp) {
    }
}
