package com.zerx.spring.data.config;

import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * 驼峰命名策略。
 * <p>
 * 表名使用实体类简名（如 {@code MyEntity}），
 * 列名使用字段名原样（如 {@code userName}），不做下划线转换。
 * </p>
 *
 * @author zerx
 */
public class CamelCaseNamingStrategy implements NamingStrategy {

    @Override
    public String getTableName(Class<?> type) {
        return type.getSimpleName();
    }

    @Override
    public String getColumnName(RelationalPersistentProperty property) {
        return property.getName();
    }
}
