package com.zerx.spring.data.logicaldelete;

import com.zerx.spring.data.domain.BaseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;
import org.springframework.data.relational.core.mapping.event.BeforeDeleteCallback;

/**
 * 逻辑删除安全回调。
 * <p>
 * 当有人误对标注 {@link LogicalDelete @LogicalDelete} 的实体调用 {@code repository.delete()} 时，
 * 此回调会发出 WARN 日志提醒开发者应使用 {@link LogicalDeleteService} 进行逻辑删除。
 * </p>
 *
 * <p>
 * 注意：Spring Data JDBC 的 {@link BeforeDeleteCallback} 不支持阻止删除操作，
 * 因此此回调仅做日志提醒，不会阻止物理删除。业务代码应确保对逻辑删除实体
 * 统一使用 {@link LogicalDeleteService}。
 * </p>
 *
 * @author zerx
 * @see LogicalDelete
 * @see LogicalDeleteService
 */
public class LogicalDeleteCallback implements BeforeDeleteCallback<BaseEntity> {

    private static final Logger log = LoggerFactory.getLogger(LogicalDeleteCallback.class);

    @Override
    public BaseEntity onBeforeDelete(BaseEntity entity,
                                      MutableAggregateChange<BaseEntity> aggregateChange) {
        if (entity == null) {
            return null;
        }

        if (entity.getClass().isAnnotationPresent(LogicalDelete.class)) {
            log.warn("[Zerx] Logical delete entity {}[id={}] was physically deleted. " +
                     "Use LogicalDeleteService.deleteById() instead.",
                    entity.getClass().getSimpleName(),
                    entity.getId());
        }

        return entity;
    }
}
