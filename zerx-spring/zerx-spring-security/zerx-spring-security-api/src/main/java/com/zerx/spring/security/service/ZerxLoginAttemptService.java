package com.zerx.spring.security.service;

/**
 * 登录失败锁定服务接口 — 防暴力破解的 SPI
 * <p>
 * 业务应用实现此接口，提供登录失败计数和账户锁定能力。
 * 框架层仅定义契约，不提供默认实现（因为涉及业务数据模型）。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * @Service
 * public class MyLoginAttemptService implements ZerxLoginAttemptService {
 *     private static final int MAX_ATTEMPTS = 5;
 *     private static final Duration LOCK_DURATION = Duration.ofMinutes(30);
 *
 *     public boolean isLocked(String username) {
 *         LoginAttempt attempt = cache.get("login:attempt:" + username);
 *         return attempt != null && attempt.failCount >= MAX_ATTEMPTS
 *                 && attempt.lastFailTime.plus(LOCK_DURATION).isAfter(Instant.now());
 *     }
 *
 *     public void recordFailure(String username) { ... }
 *     public void recordSuccess(String username) { ... }
 *     public int getRemainingAttempts(String username) { ... }
 * }
 * }</pre>
 *
 * @author zerx
 */
public interface ZerxLoginAttemptService {

    /**
     * 检查账户是否被锁定
     *
     * @param username 用户名（或其他登录标识）
     * @return 被锁定返回 {@code true}，否则返回 {@code false}
     */
    boolean isLocked(String username);

    /**
     * 记录一次登录失败
     * <p>
     * 累计失败次数，达到阈值后自动锁定账户。
     * </p>
     *
     * @param username 用户名
     */
    void recordFailure(String username);

    /**
     * 记录一次登录成功
     * <p>
     * 清除该用户的失败计数和锁定状态。
     * </p>
     *
     * @param username 用户名
     */
    void recordSuccess(String username);

    /**
     * 获取剩余登录尝试次数
     *
     * @param username 用户名
     * @return 剩余次数，已锁定时返回 0
     */
    int getRemainingAttempts(String username);
}
