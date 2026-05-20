package com.zerx.spring.security.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的登录失败锁定服务 — 默认实现。
 * <p>
 * 提供开箱即用的防暴力破解能力：连续失败达到阈值后锁定指定时长。
 * 基于 ConcurrentHashMap 存储，适用于单节点部署。
 * 多节点部署建议使用基于缓存（CacheOps）的自定义实现。
 * </p>
 *
 * @author zerx
 */
public class DefaultLoginAttemptService implements ZerxLoginAttemptService {

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /**
     * 使用默认配置构造（5 次锁定，30 分钟解锁）
     */
    public DefaultLoginAttemptService() {
        this(5, Duration.ofMinutes(30));
    }

    /**
     * @param maxAttempts    最大失败次数
     * @param lockDuration   锁定时长
     */
    public DefaultLoginAttemptService(int maxAttempts, Duration lockDuration) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
    }

    @Override
    public boolean isLocked(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) {
            return false;
        }
        if (record.failCount < maxAttempts) {
            return false;
        }
        // 检查锁定是否已过期
        if (record.lastFailTime.plus(lockDuration).isBefore(Instant.now())) {
            attempts.remove(username, record);
            return false;
        }
        return true;
    }

    @Override
    public void recordFailure(String username) {
        attempts.compute(username, (k, existing) -> {
            if (existing == null) {
                return new AttemptRecord(1, Instant.now());
            }
            // 如果上次失败已超过锁定时长，重新计数
            if (existing.lastFailTime.plus(lockDuration).isBefore(Instant.now())) {
                return new AttemptRecord(1, Instant.now());
            }
            return new AttemptRecord(existing.failCount + 1, Instant.now());
        });
    }

    @Override
    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    @Override
    public int getRemainingAttempts(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null) {
            return maxAttempts;
        }
        if (record.lastFailTime.plus(lockDuration).isBefore(Instant.now())) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - record.failCount);
    }

    private record AttemptRecord(int failCount, Instant lastFailTime) {}
}
