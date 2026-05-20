package com.zerx.common.event;

import java.util.function.Consumer;

/**
 * 领域事件监听器接口
 * <p>
 * 用于接收并处理领域事件。每个监听器可以指定关注的事件类型，
 * 事件总线会根据类型进行精确匹配分发。
 * </p>
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 方式一：实现接口
 * public class OrderCreatedListener implements DomainEventListener<OrderCreatedEvent> {
 *     @Override
 *     public void onEvent(OrderCreatedEvent event) {
 *         // 处理订单创建事件
 *     }
 * }
 *
 * // 方式二：使用 Consumer（适合简单场景）
 * Consumer&lt;OrderCreatedEvent&gt; handler = event -&gt; log.info(event.toString());
 * </pre>
 *
 * @param <T> 事件类型
 * @author zerx
 */
@FunctionalInterface
public interface DomainEventListener<T extends DomainEvent> {

    /**
     * 处理领域事件
     *
     * @param event 领域事件
     */
    void onEvent(T event);

    /**
     * 获取此监听器关注的事件类型
     * <p>
     * 事件总线根据此值匹配分发。
     * </p>
     *
     * @return 事件类型名称
     */
    default String eventType() {
        return "";
    }

    /**
     * 将 Consumer 转换为 DomainEventListener
     *
     * @param eventType 事件类型名称
     * @param consumer  事件处理函数
     * @param <T>       事件类型
     * @return 监听器实例
     */
    static <T extends DomainEvent> DomainEventListener<T> of(String eventType, Consumer<T> consumer) {
        return new DomainEventListener<>() {
            @Override
            public void onEvent(T event) {
                consumer.accept(event);
            }

            @Override
            public String eventType() {
                return eventType;
            }
        };
    }
}
