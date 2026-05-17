package com.zerx.spring.data.config;

import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.properties.ZerxDataProperties;

/**
 * 逻辑删除回调。
 * <p>
 * 在实体持久化前（{@code BeforeConvertCallback}）确保 {@code deleted} 字段具有合理的默认值：
 * 新建实体（{@code id == null}）若 {@code deleted} 为 {@code null}，自动设置为 {@code false}，
 * 保证 INSERT 时数据库列具有确定的初始状态。
 * </p>
 *
 * @author zerx
 */
public class SoftDeleteCallback implements org.springframework.data.relational.core.mapping.event.BeforeConvertCallback<BaseEntity> {

    private final ZerxDataProperties properties;

    public SoftDeleteCallback(ZerxDataProperties properties) {
        this.properties = properties;
    }

    @Override
    public BaseEntity onBeforeConvert(BaseEntity entity) {
        if (properties.getLogicDelete().isEnabled()) {
            if (entity.getId() == null && entity.getDeleted() == null) {
                entity.setDeleted(false);
            }
        }
        return entity;
    }
}
