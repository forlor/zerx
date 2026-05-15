package com.zerx.common.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 简易内存事件总线实现
 * <p>
 * 基于 ConcurrentHashMap + CopyOnWriteArrayList 实现，
 * 提供同步的发布-订阅能力。适用于单进程内的领域事件分发。
 * </p>
 * <p>
 * 对于分布式场景，应由 Spring 模块提供基于消息队列的实现。
 * </p>
 *
 * @author zerx
 */
public class SimpleEventBus implements EventBus {

    /** 事件类型 → 监听器列表 */
    private final Map<String, CopyOnWriteArrayList<DomainEventListener<?>>> listeners = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }
        CopyOnWriteArrayList<DomainEventListener<?>> eventListeners =
                listeners.get(event.getEventType());
        if (eventListeners == null || eventListeners.isEmpty()) {
            return;
        }
        for (DomainEventListener<?> listener : eventListeners) {
            try {
                ((DomainEventListener<DomainEvent>) listener).onEvent(event);
            } catch (Exception e) {
                // 单个监听器异常不影响其他监听器
                Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), e);
            }
        }
    }

    @Override
    public <T extends DomainEvent> void subscribe(String eventType, DomainEventListener<T> listener) {
        if (eventType == null || listener == null) {
            return;
        }
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    @Override
    public void unsubscribe(String eventType) {
        if (eventType == null) {
            return;
        }
        listeners.remove(eventType);
    }

    @Override
    public <T extends DomainEvent> void unsubscribe(String eventType, DomainEventListener<T> listener) {
        if (eventType == null || listener == null) {
            return;
        }
        CopyOnWriteArrayList<DomainEventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }

    @Override
    public List<DomainEventListener<?>> getListeners(String eventType) {
        if (eventType == null) {
            return List.of();
        }
        CopyOnWriteArrayList<DomainEventListener<?>> eventListeners = listeners.get(eventType);
        if (eventListeners == null || eventListeners.isEmpty()) {
            return List.of();
        }
        return List.copyOf(eventListeners);
    }

    @Override
    public boolean hasListeners(String eventType) {
        if (eventType == null) {
            return false;
        }
        CopyOnWriteArrayList<DomainEventListener<?>> eventListeners = listeners.get(eventType);
        return eventListeners != null && !eventListeners.isEmpty();
    }

    @Override
    public void clear() {
        listeners.clear();
    }
}
