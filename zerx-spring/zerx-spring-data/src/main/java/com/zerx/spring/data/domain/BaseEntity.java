package com.zerx.spring.data.domain;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 聚合根基类。
 * <p>
 * 所有 Spring Data JDBC 实体必须继承此类，提供统一的 ID 策略、审计字段和乐观锁支持。
 * 使用 Spring Data 注解实现自动审计填充和版本控制。
 * </p>
 *
 * <h3>建模规范：</h3>
 * <ul>
 *     <li>实体类名与表名遵循 UpperCamelCase → snake_case 自动映射（可通过 {@link Table} 显式指定）</li>
 *     <li>子实体（非聚合根）不继承 BaseEntity，使用普通 POJO + {@code @MappedCollection}</li>
 *     <li>值对象使用 JDK 21 record，通过 {@code @MappedCollection} 嵌入聚合根</li>
 *     <li>避免 Lombok {@code @Data}，推荐 {@code @Getter} + {@code @Setter}</li>
 *     <li>删除操作通过归档机制管理，主表不保留已删除数据，避免大表查询性能劣化</li>
 * </ul>
 *
 * @author zerx
 * @see com.zerx.spring.data.archive.Archiver
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
     * 乐观锁版本号。
     * <p>
     * 每次更新时自动递增，用于检测并发修改冲突。
     * Spring Data JDBC 在 UPDATE 时自动追加 {@code WHERE version = ?} 条件，
     * 当版本不匹配时抛出 {@link org.springframework.dao.OptimisticLockingFailureException}。
     * </p>
     */
    @Version
    private Long version;

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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
