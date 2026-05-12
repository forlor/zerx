package com.zerx.common.functional;

import java.util.function.Consumer;

/**
 * 支持抛出受检异常的 Consumer
 * <p>
 * JDK 自带的 {@link Consumer#accept(Object)} 不允许抛出受检异常，
 * 该接口弥补了这一缺陷，适用于 forEach、资源释放等需要处理异常的消费者场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在 forEach 中处理可能抛异常的操作
 * files.forEach(ThrowableConsumer.of(file -> Files.delete(file)));
 *
 * // 资源释放
 * List<Connection> connections = ...;
 * connections.forEach(ThrowableConsumer.of(conn -> conn.close()));
 * }</pre>
 *
 * @param <T> 输入类型
 * @author zerx
 * @see ThrowableFunction
 * @see ThrowableSupplier
 * @see ThrowableRunnable
 */
@FunctionalInterface
public interface ThrowableConsumer<T> {

    /**
     * 执行带有受检异常的消费操作
     *
     * @param t 输入参数
     * @throws Throwable 任意受检异常
     */
    void accept(T t) throws Throwable;

    /**
     * 将此消费者转换为标准 {@link Consumer}，
     * 受检异常将被包装为 {@link RuntimeException} 重新抛出
     *
     * @return 标准 Consumer
     */
    default Consumer<T> unchecked() {
        return t -> {
            try {
                accept(t);
            } catch (Throwable e) {
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        };
    }

    /**
     * 将标准 {@link Consumer} 转换为 {@link ThrowableConsumer}
     *
     * @param consumer 标准 Consumer
     * @param <T>      输入类型
     * @return ThrowableConsumer
     */
    static <T> ThrowableConsumer<T> of(Consumer<T> consumer) {
        return consumer::accept;
    }

    /**
     * 包装一个可能抛出受检异常的 lambda 为标准 {@link Consumer}
     *
     * @param consumer ThrowableConsumer 实例
     * @param <T>      输入类型
     * @return 标准 Consumer，异常包装为 RuntimeException
     */
    static <T> Consumer<T> wrap(ThrowableConsumer<T> consumer) {
        return consumer.unchecked();
    }

    /**
     * 组合两个 ThrowableConsumer，先执行当前消费者，再执行 after
     *
     * @param after 后续消费者
     * @return 组合后的消费者
     */
    default ThrowableConsumer<T> andThen(ThrowableConsumer<T> after) {
        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}
