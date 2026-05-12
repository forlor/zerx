package com.zerx.common.functional;

import java.util.function.Function;

/**
 * 支持抛出受检异常的 Function
 * <p>
 * JDK 自带的 {@link Function#apply(Object)} 不允许抛出受检异常（Checked Exception），
 * 但在实际业务中（如文件 IO、数据库操作、网络调用等），lambda 中抛出受检异常是非常常见的场景。
 * 该接口弥补了这一缺陷，并提供了与标准 {@link Function} 互转的便捷方法。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 在 Stream 中处理可能抛异常的操作
 * List<String> lines = files.stream()
 *     .map(ThrowableFunction.of(file -> Files.readString(file)))
 *     .toList();
 *
 * // 转为标准 Function
 * Function<String, Integer> fn = ThrowableFunction.of(str -> Integer.parseInt(str));
 * }</pre>
 *
 * @param <T> 输入类型
 * @param <R> 返回类型
 * @author zerx
 * @see ThrowableConsumer
 * @see ThrowableSupplier
 * @see ThrowableRunnable
 */
@FunctionalInterface
public interface ThrowableFunction<T, R> {

    /**
     * 执行带有受检异常的操作
     *
     * @param t 输入参数
     * @return 操作结果
     * @throws Throwable 任意受检异常
     */
    R apply(T t) throws Throwable;

    /**
     * 将此函数转换为标准 {@link Function}，
     * 受检异常将被包装为 {@link RuntimeException} 重新抛出
     *
     * @return 标准 Function
     */
    default Function<T, R> unchecked() {
        return t -> {
            try {
                return apply(t);
            } catch (Throwable e) {
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        };
    }

    /**
     * 将标准 {@link Function} 转换为 {@link ThrowableFunction}
     *
     * @param function 标准 Function
     * @param <T>      输入类型
     * @param <R>      返回类型
     * @return ThrowableFunction
     */
    static <T, R> ThrowableFunction<T, R> of(Function<T, R> function) {
        return function::apply;
    }

    /**
     * 包装一个可能抛出受检异常的 lambda 为标准 {@link Function}
     * <p>
     * 便捷方法，可直接在 Stream API 中使用。
     * </p>
     *
     * @param function ThrowableFunction 实例
     * @param <T>      输入类型
     * @param <R>      返回类型
     * @return 标准 Function，异常包装为 RuntimeException
     */
    static <T, R> Function<T, R> wrap(ThrowableFunction<T, R> function) {
        return function.unchecked();
    }

    /**
     * 组合两个 ThrowableFunction，先执行当前函数，再将结果作为参数传给 after
     *
     * @param after 后续函数
     * @param <V>   后续函数输入类型
     * @return 组合后的函数
     */
    default <V> ThrowableFunction<T, V> andThen(ThrowableFunction<R, V> after) {
        return t -> after.apply(apply(t));
    }
}
