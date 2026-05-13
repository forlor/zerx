package com.zerx.common.functional;

/**
 * 支持抛出受检异常的 Runnable
 * <p>
 * JDK 自带的 {@link Runnable#run()} 不允许抛出受检异常，
 * 该接口弥补了这一缺陷，适用于资源清理、事务提交/回滚、锁释放等需要异常处理的后置操作场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 安全关闭资源
 * Connection conn = ...;
 * ThrowableRunnable.wrap(conn::close).run();
 *
 * // 在 finally 块中执行可能抛异常的清理操作
 * try {
 *     // 业务逻辑
 * } finally {
 *     ThrowableRunnable.wrap(() -> stream.close()).run();
 * }
 *
 * // 忽略异常的静默执行
 * ThrowableRunnable.silent(() -> Files.delete(tempFile)).run();
 * }</pre>
 *
 * @author zerx
 * @see ThrowableFunction
 * @see ThrowableConsumer
 * @see ThrowableSupplier
 */
@FunctionalInterface
public interface ThrowableRunnable {

    /**
     * 执行可能抛出受检异常的操作
     *
     * @throws Throwable 任意受检异常
     */
    void run() throws Throwable;

    /**
     * 将此 Runnable 转换为标准 {@link Runnable}，
     * 受检异常将被包装为 {@link RuntimeException} 重新抛出
     *
     * @return 标准 Runnable
     */
    default Runnable unchecked() {
        return () -> {
            try {
                run();
            } catch (Throwable e) {
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        };
    }

    /**
     * 将标准 {@link Runnable} 转换为 {@link ThrowableRunnable}
     *
     * @param runnable 标准 Runnable
     * @return ThrowableRunnable
     */
    static ThrowableRunnable of(Runnable runnable) {
        return runnable::run;
    }

    /**
     * 包装一个可能抛出受检异常的 lambda 为标准 {@link Runnable}
     *
     * @param runnable ThrowableRunnable 实例
     * @return 标准 Runnable，异常包装为 RuntimeException
     */
    static Runnable wrap(ThrowableRunnable runnable) {
        return runnable.unchecked();
    }

    /**
     * 执行操作，静默忽略所有异常
     * <p>
     * 适用于资源清理、日志记录等即使失败也不应影响主流程的场景。
     * </p>
     *
     * @param runnable ThrowableRunnable 实例
     */
    static void silent(ThrowableRunnable runnable) {
        try {
            runnable.run();
        } catch (Error e) {
            throw e; // Error（如 OutOfMemoryError）不应被吞掉
        } catch (Throwable ignored) {
            // 静默忽略受检异常和 RuntimeException
        }
    }

    /**
     * 执行操作，异常时记录但不抛出
     * <p>
     * 适用于资源清理等场景，异常信息不会丢失但不会中断程序。
     * </p>
     *
     * @param runnable  ThrowableRunnable 实例
     * @param logger    日志消费者，用于接收异常信息
     */
    static void quiet(ThrowableRunnable runnable, java.util.function.Consumer<Throwable> logger) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (logger != null) {
                logger.accept(e);
            }
        }
    }

    /**
     * 执行操作，如果发生异常则执行 fallback 操作
     *
     * @param runnable   主要操作
     * @param fallback   失败时的回退操作
     */
    static void withFallback(ThrowableRunnable runnable, ThrowableRunnable fallback) {
        try {
            runnable.run();
        } catch (Error e) {
            throw e; // Error 不应被吞掉
        } catch (Throwable e) {
            try {
                fallback.run();
            } catch (Error e2) {
                throw e2; // fallback 中的 Error 也不应被吞掉
            } catch (Throwable ignored) {
                // fallback 也失败，静默处理
            }
        }
    }
}
