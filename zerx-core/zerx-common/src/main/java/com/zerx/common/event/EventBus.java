package com.zerx.common.event;

import java.util.List;
import java.util.function.Consumer;

/**
 * 领域事件总线接口
 * <p>
 * 定义领域事件的发布与订阅契约，实现发布-订阅模式。
 * Core 模块定义接口，Spring 模块提供具体实现。
 * </p>
 * <p>
 * 领域事件总线是解耦领域逻辑和副作用（通知、日志、审计等）的关键基础设施。
 * </p>
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 发布事件
 * eventBus.publish(new OrderCreatedEvent(orderId, userId, amount));
 *
 * // 订阅事件
 * eventBus.subscribe("OrderCreated", event -&gt; {
 *     // 处理订单创建后的业务逻辑
 *     notificationService.sendOrderConfirmation(event);
 * });
 * }</pre>
 *
 * @author zerx
 */
public interface EventBus {

    /**
     * 发布领域事件
     * <p>
     * 事件会被同步分发给所有订阅了该事件类型的监听器。
     * </p>
     *
     * @param event 领域事件
     */
    void publish(DomainEvent event);

    /**
     * 订阅指定类型的事件
     *
     * @param eventType 事件类型名称
     * @param listener  事件监听器
     * @param <T>       事件类型
     */
    <T extends DomainEvent> void subscribe(String eventType, DomainEventListener<T> listener);

    /**
     * 取消订阅指定事件类型的所有监听器
     *
     * @param eventType 事件类型名称
     */
    void unsubscribe(String eventType);

    /**
     * 取消订阅指定事件类型的特定监听器
     *
     * @param eventType 事件类型名称
     * @param listener  要移除的监听器
     * @param <T>       事件类型
     */
    <T extends DomainEvent> void unsubscribe(String eventType, DomainEventListener<T> listener);

    /**
     * 获取指定事件类型的所有监听器
     *
     * @param eventType 事件类型名称
     * @return 监听器列表（空列表表示无订阅者）
     */
    List<DomainEventListener<?>> getListeners(String eventType);

    /**
     * 判断指定事件类型是否有订阅者
     *
     * @param eventType 事件类型名称
     * @return 有订阅者返回 true
     */
    boolean hasListeners(String eventType);

    /**
     * 清空所有订阅
     */
    void clear();
}
