package com.zerx.common.retry;

import com.zerx.common.functional.ThrowableRunnable;
import com.zerx.common.functional.ThrowableSupplier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * 通用重试工具
 * <p>
 * 提供可配置的重试策略，支持最大次数、退避策略、异常过滤等。
 * 基于 JDK 原生实现，无第三方依赖。
 * </p>
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 简单重试 3 次
 * String result = Retryer.of(String.class)
 *         .maxAttempts(3)
 *         .retryOn(IOException.class)
 *         .execute(() -> callRemoteService());
 *
 * // 指数退避重试
 * String result = Retryer.of(String.class)
 *         .maxAttempts(5)
 *         .backoff(Duration.ofMillis(100), Duration.ofSeconds(5))
 *         .execute(() -> callRemoteService());
 * }</pre>
 *
 * @param <T> 返回值类型
 * @author zerx
 */
public final class Retryer<T> {

    /** 默认最大重试次数 */
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    /** 默认退避策略 */
    private static final BackoffStrategy DEFAULT_BACKOFF = BackoffStrategy.NONE;

    /** 无退避时长 */
    private static final Duration NO_DELAY = Duration.ZERO;

    private final int maxAttempts;
    private final BackoffStrategy backoffStrategy;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final Predicate<Exception> retryPredicate;
    private final boolean jitterEnabled;

    private Retryer(Builder<T> builder) {
        this.maxAttempts = Math.max(1, builder.maxAttempts);
        this.backoffStrategy = builder.backoffStrategy;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.retryPredicate = builder.retryPredicate;
        this.jitterEnabled = builder.jitterEnabled;
    }

    // ======================== 工厂方法 ========================

    /**
     * 创建一个重试器构建器
     *
     * @param <T> 返回值类型
     * @return 重试器构建器
     */
    public static <T> Builder<T> of(Class<T> type) {
        return new Builder<>();
    }

    /**
     * 创建一个重试器构建器
     *
     * @param <T> 返回值类型
     * @return 重试器构建器
     */
    public static <T> Builder<T> of() {
        return new Builder<>();
    }

    // ======================== 执行 ========================

    /**
     * 执行带返回值的重试任务
     *
     * @param supplier 任务提供者
     * @return 任务执行结果
     * @throws Exception 重试耗尽后仍失败时抛出最后一次异常
     */
    public T execute(ThrowableSupplier<T> supplier) throws Exception {
        Objects.requireNonNull(supplier, "任务不能为 null");

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts && shouldRetry(e)) {
                    Duration delay = calculateDelay(attempt);
                    if (!delay.isZero() && !delay.isNegative()) {
                        Thread.sleep(delay.toMillis());
                    }
                }
            }
        }
        throw lastException;
    }

    /**
     * 执行无返回值的重试任务
     *
     * @param task 任务
     * @throws Exception 重试耗尽后仍失败时抛出最后一次异常
     */
    public void executeRunnable(ThrowableRunnable task) throws Exception {
        execute(() -> {
            task.run();
            return null;
        });
    }

    // ======================== 退避策略 ========================

    /**
     * 退避策略枚举
     */
    public enum BackoffStrategy {

        /** 无退避，立即重试 */
        NONE {
            @Override
            long calculateDelay(int attempt, Duration initialDelay, Duration maxDelay, boolean jitter) {
                return 0;
            }
        },

        /** 固定间隔退避 */
        FIXED {
            @Override
            long calculateDelay(int attempt, Duration initialDelay, Duration maxDelay, boolean jitter) {
                long delay = initialDelay.toMillis();
                return jitter ? addJitter(delay) : delay;
            }
        },

        /** 线性增长退避 */
        LINEAR {
            @Override
            long calculateDelay(int attempt, Duration initialDelay, Duration maxDelay, boolean jitter) {
                long delay = initialDelay.toMillis() * attempt;
                long capped = Math.min(delay, maxDelay.toMillis());
                return jitter ? addJitter(capped) : capped;
            }
        },

        /** 指数退避（base 2） */
        EXPONENTIAL {
            @Override
            long calculateDelay(int attempt, Duration initialDelay, Duration maxDelay, boolean jitter) {
                long delay = initialDelay.toMillis() * (1L << Math.min(attempt - 1, 30));
                long capped = Math.min(delay, maxDelay.toMillis());
                return jitter ? addJitter(capped) : capped;
            }
        };

        /**
         * 计算退避延迟（毫秒）
         */
        abstract long calculateDelay(int attempt, Duration initialDelay, Duration maxDelay, boolean jitter);

        /**
         * 添加随机抖动（±25%），避免惊群效应
         */
        protected long addJitter(long delay) {
            if (delay <= 0) {
                return 0;
            }
            long bound = Math.max(1, delay / 4);
            return delay + ThreadLocalRandom.current().nextLong(-bound, bound + 1);
        }
    }

    // ======================== 内部方法 ========================

    private boolean shouldRetry(Exception e) {
        return retryPredicate == null || retryPredicate.test(e);
    }

    private Duration calculateDelay(int attempt) {
        long millis = backoffStrategy.calculateDelay(
                attempt, initialDelay, maxDelay, jitterEnabled);
        return Duration.ofMillis(Math.max(0, millis));
    }

    // ======================== 构建器 ========================

    /**
     * 重试器构建器
     *
     * @param <T> 返回值类型
     */
    public static final class Builder<T> {

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private BackoffStrategy backoffStrategy = DEFAULT_BACKOFF;
        private Duration initialDelay = NO_DELAY;
        private Duration maxDelay = Duration.ofSeconds(30);
        private Predicate<Exception> retryPredicate;
        private boolean jitterEnabled = false;

        private final List<Class<? extends Exception>> retryOnExceptions = new ArrayList<>();

        Builder() {
        }

        /**
         * 设置最大重试次数（包含首次执行）
         *
         * @param maxAttempts 最大次数，最小为 1
         * @return 当前构建器
         */
        public Builder<T> maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * 设置退避策略为固定间隔
         *
         * @param delay 固定延迟时长
         * @return 当前构建器
         */
        public Builder<T> fixedBackoff(Duration delay) {
            this.backoffStrategy = BackoffStrategy.FIXED;
            this.initialDelay = delay;
            return this;
        }

        /**
         * 设置退避策略为线性增长
         *
         * @param initialDelay 初始延迟
         * @param maxDelay     最大延迟
         * @return 当前构建器
         */
        public Builder<T> linearBackoff(Duration initialDelay, Duration maxDelay) {
            this.backoffStrategy = BackoffStrategy.LINEAR;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            return this;
        }

        /**
         * 设置退避策略为指数退避
         *
         * @param initialDelay 初始延迟
         * @param maxDelay     最大延迟
         * @return 当前构建器
         */
        public Builder<T> backoff(Duration initialDelay, Duration maxDelay) {
            this.backoffStrategy = BackoffStrategy.EXPONENTIAL;
            this.initialDelay = initialDelay;
            this.maxDelay = maxDelay;
            return this;
        }

        /**
         * 启用退避抖动（随机化延迟，避免惊群效应）
         *
         * @return 当前构建器
         */
        public Builder<T> jitter() {
            this.jitterEnabled = true;
            return this;
        }

        /**
         * 仅在抛出指定异常类型时重试（可多次调用叠加）
         *
         * @param exceptionType 异常类型
         * @return 当前构建器
         */
        @SafeVarargs
        public final Builder<T> retryOn(Class<? extends Exception>... exceptionType) {
            if (exceptionType != null) {
                for (Class<? extends Exception> type : exceptionType) {
                    if (type != null) {
                        this.retryOnExceptions.add(type);
                    }
                }
            }
            return this;
        }

        /**
         * 设置自定义重试条件
         *
         * @param predicate 判断是否重试的谓词
         * @return 当前构建器
         */
        public Builder<T> retryWhen(Predicate<Exception> predicate) {
            this.retryPredicate = predicate;
            return this;
        }

        /**
         * 构建重试器
         *
         * @return 重试器实例
         */
        public Retryer<T> build() {
            // 如果设置了 retryOn 异常类型，构建重试谓词
            if (!retryOnExceptions.isEmpty() && retryPredicate == null) {
                this.retryPredicate = e -> {
                    for (Class<? extends Exception> type : retryOnExceptions) {
                        if (type.isInstance(e)) {
                            return true;
                        }
                    }
                    return false;
                };
            }
            return new Retryer<>(this);
        }
    }
}
