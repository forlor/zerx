package com.zerx.common.functional;

import java.util.function.Supplier;

/**
 * 支持抛出受检异常的 Supplier
 * <p>
 * JDK 自带的 {@link Supplier#get()} 不允许抛出受检异常，
 * 该接口弥补了这一缺陷，适用于延迟加载、工厂方法、可选值创建等场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 读取配置文件
 * String config = ThrowableSupplier.wrap(() -> Files.readString(configPath)).get();
 *
 * // 作为方法默认参数的替代方案
 * public String getName(Supplier<String> nameSupplier) {
 *     return ThrowableSupplier.wrap(nameSupplier).orElse("default");
 * }
 * }</pre>
 *
 * @param <T> 返回类型
 * @author zerx
 * @see ThrowableFunction
 * @see ThrowableConsumer
 * @see ThrowableRunnable
 */
@FunctionalInterface
public interface ThrowableSupplier<T> {

    /**
     * 获取结果，可能抛出受检异常
     *
     * @return 结果值
     * @throws Throwable 任意受检异常
     */
    T get() throws Throwable;

    /**
     * 将此 Supplier 转换为标准 {@link Supplier}，
     * 受检异常将被包装为 {@link RuntimeException} 重新抛出
     *
     * @return 标准 Supplier
     */
    default Supplier<T> unchecked() {
        return () -> {
            try {
                return get();
            } catch (Throwable e) {
                throw (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            }
        };
    }

    /**
     * 将标准 {@link Supplier} 转换为 {@link ThrowableSupplier}
     *
     * @param supplier 标准 Supplier
     * @param <T>      返回类型
     * @return ThrowableSupplier
     */
    static <T> ThrowableSupplier<T> of(Supplier<T> supplier) {
        return supplier::get;
    }

    /**
     * 包装一个可能抛出受检异常的 lambda 为标准 {@link Supplier}
     *
     * @param supplier ThrowableSupplier 实例
     * @param <T>      返回类型
     * @return 标准 Supplier，异常包装为 RuntimeException
     */
    static <T> Supplier<T> wrap(ThrowableSupplier<T> supplier) {
        return supplier.unchecked();
    }

    /**
     * 获取结果，发生异常时返回默认值
     *
     * @param defaultValue 异常时的默认值
     * @return 结果值或默认值
     */
    default T orElse(T defaultValue) {
        try {
            return get();
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    /**
     * 获取结果，发生异常时调用 fallback Supplier
     *
     * @param fallback 异常时的回退 Supplier
     * @return 结果值或回退值
     */
    default T orElseGet(Supplier<T> fallback) {
        try {
            return get();
        } catch (Throwable e) {
            return fallback.get();
        }
    }

    /**
     * 获取结果，发生异常时抛出指定的 RuntimeException
     *
     * @param exceptionSupplier 异常工厂
     * @param <X>              异常类型
     * @return 结果值
     * @throws X 获取失败时抛出
     */
    default <X extends RuntimeException> T orElseThrow(Supplier<X> exceptionSupplier) throws X {
        try {
            return get();
        } catch (Throwable e) {
            throw exceptionSupplier.get();
        }
    }
}
