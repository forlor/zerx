package com.zerx.common.model;

import java.io.Serializable;
import java.util.Map;

/**
 * 二元组（Pair）
 * <p>
 * 存储两个相关联的元素，适用于需要返回两个值的场景。
 * 相比定义专门的 DTO 类更加轻量，相比 {@link Map.Entry} 更加语义化和类型安全。
 * 两个元素通过 {@code left} 和 {@code right} 命名，不暗示任何键值关系。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 返回名字和年龄
 * Pair<String, Integer> nameAndAge = Pair.of("Alice", 25);
 * System.out.println(nameAndAge.left());  // "Alice"
 * System.out.println(nameAndAge.right()); // 25
 *
 * // 用于分组计算
 * Map<String, Pair<Integer, Double>> stats = Map.of(
 *     "count", Pair.of(100, 95.5)
 * );
 * }</pre>
 *
 * @param left  第一个元素
 * @param right 第二个元素
 * @param <L>   第一个元素类型
 * @param <R>   第二个元素类型
 * @author zerx
 */
public record Pair<L, R>(L left, R right) implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 静态工厂方法，创建一个 Pair
     *
     * @param left  第一个元素
     * @param right 第二个元素
     * @param <L>   第一个元素类型
     * @param <R>   第二个元素类型
     * @return Pair 实例
     */
    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    /**
     * 创建一个 left 为 null 的 Pair
     *
     * @param right 第二个元素
     * @param <L>   第一个元素类型
     * @param <R>   第二个元素类型
     * @return Pair 实例
     */
    public static <L, R> Pair<L, R> ofRight(R right) {
        return new Pair<>(null, right);
    }

    /**
     * 创建一个 right 为 null 的 Pair
     *
     * @param left 第一个元素
     * @param <L>  第一个元素类型
     * @param <R>  第二个元素类型
     * @return Pair 实例
     */
    public static <L, R> Pair<L, R> ofLeft(L left) {
        return new Pair<>(left, null);
    }

    /**
     * 交换 left 和 right 的位置
     *
     * @return 交换后的新 Pair
     */
    public Pair<R, L> swap() {
        return new Pair<>(right, left);
    }

    /**
     * 判断两个元素是否都不为 null
     *
     * @return 两个元素均不为 null 返回 true
     */
    public boolean isFull() {
        return left != null && right != null;
    }

    /**
     * 将 Pair 转为 Map.Entry（兼容 Java 集合 API）
     *
     * @return Map.Entry 实例
     */
    public Map.Entry<L, R> toEntry() {
        return new Map.Entry<>() {
            @Override
            public L getKey() {
                return left;
            }

            @Override
            public R getValue() {
                return right;
            }

            @Override
            public R setValue(R value) {
                throw new UnsupportedOperationException("Pair 不支持修改值");
            }

            @Override
            public boolean equals(Object o) {
                return Pair.this.equals(o);
            }

            @Override
            public int hashCode() {
                return Pair.this.hashCode();
            }
        };
    }
}
