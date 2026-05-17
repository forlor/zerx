package com.zerx.spring.data.domain;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.time.LocalDateTime;

/**
 * 聚合根基类。
 * <p>
 * 所有 Spring Data JDBC 实体必须继承此类，提供统一的 ID 策略、审计字段和逻辑删除支持。
 * 使用 Spring Data 注解实现自动审计填充，使用 {@link ReadOnlyProperty} 标记逻辑删除字段
 * 不直接映射到 INSERT/UPDATE SQL。
 * </p>
 *
 * <h3>建模规范：</h3>
 * <ul>
 *     <li>实体类名与表名遵循 UpperCamelCase → snake_case 自动映射（可通过 {@code @Table} 显式指定）</li>
 *     <li>子实体（非聚合根）不继承 BaseEntity，使用普通 POJO + {@code @MappedCollection}</li>
 *     <li>值对象使用 JDK 21 record，通过 {@code @MappedCollection} 嵌入聚合根</li>
 *     <li>避免 Lombok {@code @Data}，推荐 {@code @Getter} + {@code @Setter}</li>
 * </ul>
 *
 * @author zerx
 */
public abstract class BaseEntity {

    /**
     * 主键 ID
     */
    @Id
    private Long id;

    /**
     * 创建时间（自动填充）
     */
    @CreatedDate
    private LocalDateTime createTime;

    /**
     * 更新时间（自动填充）
     */
    @LastModifiedDate
    private LocalDateTime updateTime;

    /**
     * 创建人 ID（自动填充）
     */
    @CreatedBy
    private Long createBy;

    /**
     * 更新人 ID（自动填充）
     */
    @LastModifiedBy
    private Long updateBy;

    /**
     * 逻辑删除标记。不映射到 INSERT/UPDATE，由逻辑删除机制控制。
     */
    @ReadOnlyProperty
    private Boolean deleted = false;

    // ======================== 行为方法 ========================

    /**
     * 标记为已删除。由 SoftDeleteCallback 触发，业务代码不应直接调用。
     */
    public void markDeleted() {
        this.deleted = true;
    }

    /**
     * 判断是否已删除
     *
     * @return 已删除返回 true
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }

    // ======================== getter/setter ========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
