package com.zerx.spring.data.archive;

import com.zerx.spring.data.domain.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteCallback;

import java.util.List;

/**
 * 归档生命周期回调。
 * <p>
 * 在实体删除前触发归档操作，确保数据在物理删除前被安全归档。
 * 通过 Spring Data JDBC 的事件回调机制，在 Repository delete 操作执行时自动触发。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *     <li>业务代码调用 {@code repository.delete(entity)}</li>
 *     <li>Spring Data JDBC 触发 {@link BeforeDeleteCallback} → 执行归档</li>
 *     <li>归档成功后，Spring Data JDBC 执行物理 DELETE</li>
 *     <li>如果归档失败，抛出 {@link ArchiveException}，阻止物理删除</li>
 * </ol>
 *
 * @author zerx
 */
public class ArchiveCallback implements BeforeDeleteCallback<BaseEntity> {

    private static final Logger log = LoggerFactory.getLogger(ArchiveCallback.class);

    private final ArchiveProperties properties;
    private final List<Archiver<?>> archivers;

    /**
     * 构造归档回调。
     *
     * @param properties 归档配置
     * @param archivers  已注册的 Archiver 实例列表
     */
    public ArchiveCallback(ArchiveProperties properties, List<Archiver<?>> archivers) {
        this.properties = properties;
        this.archivers = archivers;
    }

    @Override
    public BaseEntity onBeforeDelete(BaseEntity entity,
                                       MutableAggregateChange<BaseEntity> aggregateChange) {
        if (!properties.isEnabled() || entity == null || entity.getId() == null) {
            return entity;
        }

        // 查找匹配的 Archiver
        Archiver<Object> matchedArchiver = findArchiver(entity.getClass());
        if (matchedArchiver == null) {
            // 该实体未配置归档，直接跳过
            return entity;
        }

        log.debug("[Zerx] Archiving {}[id={}] before physical delete",
                entity.getClass().getSimpleName(), entity.getId());

        @SuppressWarnings("unchecked")
        Archiver<Object> archiver = (Archiver<Object>) matchedArchiver;
        archiver.archive(entity);

        return entity;
    }

    /**
     * 查找支持指定实体类型的 Archiver。
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 匹配的 Archiver，未找到返回 null
     */
    @SuppressWarnings("unchecked")
    private <T> Archiver<T> findArchiver(Class<?> entityClass) {
        for (Archiver<?> archiver : archivers) {
            if (archiver.supports(entityClass)) {
                return (Archiver<T>) archiver;
            }
        }
        return null;
    }
}
