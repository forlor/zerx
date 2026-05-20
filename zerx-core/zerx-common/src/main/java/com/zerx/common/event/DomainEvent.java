package com.zerx.common.event;

import java.io.Serializable;
import java.time.Instant;

import com.zerx.common.util.UuidUtil;

/**
 * 领域事件基类
 * <p>
 * 所有领域事件都应继承此基类，提供事件 ID、时间戳、聚合根标识等通用能力。
 * 领域事件代表领域中发生的重要业务事实，是不可变的值对象。
 * </p>
 * <p>
 * 设计原则：
 * </p>
 * <ul>
 *   <li>事件是不可变的（所有字段为 final）</li>
 *   <li>事件携带足够的上下文信息，使消费者无需回溯源头</li>
 *   <li>事件命名使用过去时态（如 OrderCreated、UserRegistered）</li>
 * </ul>
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public record OrderCreatedEvent(
 *         Long orderId,
 *         Long userId,
 *         BigDecimal amount
 * ) extends DomainEvent {
 *     public OrderCreatedEvent() {
 *         super("order", "OrderCreated");
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 */
public abstract class DomainEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件唯一标识 */
    private final String eventId;

    /** 事件类型（如 "OrderCreated"、"UserRegistered"） */
    private final String eventType;

    /** 事件发生的聚合根类型（如 "order"、"user"） */
    private final String aggregateType;

    /** 事件发生时间 */
    private final Instant occurredAt;

    /** 事件版本号（用于事件演进） */
    private final int version;

    /**
     * 创建领域事件
     *
     * @param aggregateType 聚合根类型
     * @param eventType     事件类型
     */
    protected DomainEvent(String aggregateType, String eventType) {
        this(UuidUtil.uuidv7String(), aggregateType, eventType,
                Instant.now(), 1);
    }

    /**
     * 创建领域事件（完整参数）
     *
     * @param eventId       事件 ID
     * @param aggregateType 聚合根类型
     * @param eventType     事件类型
     * @param occurredAt    发生时间
     * @param version       版本号
     */
    protected DomainEvent(String eventId, String aggregateType, String eventType,
                          Instant occurredAt, int version) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.version = version;
    }

    // ======================== Getter ========================

    /**
     * 获取事件唯一标识
     *
     * @return 事件 ID（UUIDv7 字符串）
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * 获取事件类型
     *
     * @return 事件类型名称
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * 获取聚合根类型
     *
     * @return 聚合根类型名称
     */
    public String getAggregateType() {
        return aggregateType;
    }

    /**
     * 获取事件发生时间
     *
     * @return 发生时间（Instant）
     */
    public Instant getOccurredAt() {
        return occurredAt;
    }

    /**
     * 获取事件版本号
     *
     * @return 版本号
     */
    public int getVersion() {
        return version;
    }

    // ======================== 判断 ========================

    /**
     * 判断事件是否为指定类型
     *
     * @param type 事件类型
     * @return 匹配返回 true
     */
    public boolean isType(String type) {
        return eventType != null && eventType.equals(type);
    }

    /**
     * 判断事件是否属于指定聚合根类型
     *
     * @param type 聚合根类型
     * @return 匹配返回 true
     */
    public boolean isAggregateType(String type) {
        return aggregateType != null && aggregateType.equals(type);
    }

    /**
     * 判断事件是否发生在指定时间之前
     *
     * @param instant 时间点
     * @return 事件时间早于指定时间返回 true
     */
    public boolean isBefore(Instant instant) {
        return occurredAt.isBefore(instant);
    }

    /**
     * 判断事件是否发生在指定时间之后
     *
     * @param instant 时间点
     * @return 事件时间晚于指定时间返回 true
     */
    public boolean isAfter(Instant instant) {
        return occurredAt.isAfter(instant);
    }

    @Override
    public String toString() {
        return "DomainEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", aggregateType='" + aggregateType + '\'' +
                ", occurredAt=" + occurredAt +
                ", version=" + version +
                '}';
    }
}
