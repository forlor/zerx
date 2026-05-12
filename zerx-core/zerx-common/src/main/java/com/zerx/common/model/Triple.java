package com.zerx.common.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * 三元组（Triple）
 * <p>
 * 存储三个相关联的元素，适用于需要返回三个值的场景。
 * 三个元素通过 {@code first}、{@code second}、{@code third} 命名，
 * 语义清晰，适合替代只为临时返回多值而定义的 DTO 类。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 返回分页结果（数据列表、总条数、总页数）
 * Triple<List<User>, Long, Integer> pageData = Triple.of(users, 100L, 10);
 *
 * // RGB 颜色值
 * Triple<Integer, Integer, Integer> rgb = Triple.of(255, 128, 0);
 * }</pre>
 *
 * @param first  第一个元素
 * @param second 第二个元素
 * @param third  第三个元素
 * @param <F>    第一个元素类型
 * @param <S>    第二个元素类型
 * @param <T>    第三个元素类型
 * @author zerx
 */
public record Triple<F, S, T>(F first, S second, T third) implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 静态工厂方法，创建一个 Triple
     *
     * @param first  第一个元素
     * @param second 第二个元素
     * @param third  第三个元素
     * @param <F>    第一个元素类型
     * @param <S>    第二个元素类型
     * @param <T>    第三个元素类型
     * @return Triple 实例
     */
    public static <F, S, T> Triple<F, S, T> of(F first, S second, T third) {
        return new Triple<>(first, second, third);
    }

    /**
     * 将前两个元素提取为 Pair
     *
     * @return 包含 first 和 second 的 Pair
     */
    public Pair<F, S> firstTwo() {
        return Pair.of(first, second);
    }

    /**
     * 将后两个元素提取为 Pair
     *
     * @return 包含 second 和 third 的 Pair
     */
    public Pair<S, T> lastTwo() {
        return Pair.of(second, third);
    }

    /**
     * 将前两个元素和第三个元素分别拆分为 Pair 和单独值
     *
     * @return 包含 Pair(first, second) 和 third 的 Pair
     */
    public Pair<Pair<F, S>, T> split() {
        return Pair.of(Pair.of(first, second), third);
    }

    /**
     * 判断三个元素是否都不为 null
     *
     * @return 三个元素均不为 null 返回 true
     */
    public boolean isFull() {
        return first != null && second != null && third != null;
    }
}
