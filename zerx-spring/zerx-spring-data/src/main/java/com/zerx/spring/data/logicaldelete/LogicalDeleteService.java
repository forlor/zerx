package com.zerx.spring.data.logicaldelete;

import com.zerx.spring.data.util.NamingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.Collection;

/**
 * 逻辑删除服务。
 * <p>
 * 为标注 {@link LogicalDelete @LogicalDelete} 的实体提供逻辑删除、恢复、物理删除等操作。
 * 删除操作通过 UPDATE 设置删除标记实现，而非物理 DELETE，确保数据可恢复。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 逻辑删除
 * logicalDeleteService.deleteById(userId, User.class);
 *
 * // 批量逻辑删除
 * logicalDeleteService.batchDeleteByIds(userIds, User.class);
 *
 * // 恢复已删除记录
 * logicalDeleteService.restoreById(userId, User.class);
 *
 * // 物理删除（管理操作，谨慎使用）
 * logicalDeleteService.hardDeleteById(userId, User.class);
 * }</pre>
 *
 * @author zerx
 * @see LogicalDelete
 */
public class LogicalDeleteService {

    private static final Logger log = LoggerFactory.getLogger(LogicalDeleteService.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造逻辑删除服务。
     *
     * @param dataSource 数据源
     */
    public LogicalDeleteService(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource must not be null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("[Zerx] LogicalDeleteService initialized");
    }

    /**
     * 逻辑删除指定 ID 的记录。
     * <p>
     * 将删除标记列更新为 {@code deletedValue}，而非物理删除。
     * </p>
     *
     * @param id          主键 ID
     * @param entityClass 实体类型（必须标注 {@link LogicalDelete}）
     * @param <T>         实体类型
     */
    public <T> void deleteById(Long id, Class<T> entityClass) {
        Assert.notNull(id, "ID must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");
        validateLogicalDelete(entityClass);

        LogicalDelete annotation = entityClass.getAnnotation(LogicalDelete.class);
        String tableName = NamingUtils.resolveTableName(entityClass);
        String sql = "UPDATE " + tableName + " SET " + annotation.column() + " = ? WHERE id = ?";

        int affected = jdbcTemplate.update(sql, annotation.deletedValue(), id);
        if (affected > 0) {
            log.info("[Zerx] Logical deleted {}[id={}] — column={}, value={}",
                    entityClass.getSimpleName(), id, annotation.column(), annotation.deletedValue());
        } else {
            log.warn("[Zerx] Logical delete affected 0 rows for {}[id={}]", entityClass.getSimpleName(), id);
        }
    }

    /**
     * 批量逻辑删除指定 ID 的记录。
     *
     * @param ids         主键 ID 集合
     * @param entityClass 实体类型（必须标注 {@link LogicalDelete}）
     * @param <T>         实体类型
     */
    public <T> void batchDeleteByIds(Collection<Long> ids, Class<T> entityClass) {
        Assert.notNull(ids, "IDs must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");
        if (ids.isEmpty()) {
            return;
        }
        validateLogicalDelete(entityClass);

        LogicalDelete annotation = entityClass.getAnnotation(LogicalDelete.class);
        String tableName = NamingUtils.resolveTableName(entityClass);
        String placeholders = ids.stream().map(v -> "?").collect(java.util.stream.Collectors.joining(","));
        String sql = "UPDATE " + tableName + " SET " + annotation.column() + " = ? WHERE id IN (" + placeholders + ")";

        // 构建参数数组：第一个参数是 deletedValue，后面是所有 id
        Object[] params = new Object[ids.size() + 1];
        params[0] = annotation.deletedValue();
        int i = 1;
        for (Long id : ids) {
            params[i++] = id;
        }

        int affected = jdbcTemplate.update(sql, params);
        log.info("[Zerx] Batch logical deleted {} records for {} — column={}, value={}",
                affected, entityClass.getSimpleName(), annotation.column(), annotation.deletedValue());
    }

    /**
     * 恢复指定 ID 的已删除记录。
     * <p>
     * 将删除标记列更新为 {@code notDeletedValue}。
     * </p>
     *
     * @param id          主键 ID
     * @param entityClass 实体类型（必须标注 {@link LogicalDelete}）
     * @param <T>         实体类型
     */
    public <T> void restoreById(Long id, Class<T> entityClass) {
        Assert.notNull(id, "ID must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");
        validateLogicalDelete(entityClass);

        LogicalDelete annotation = entityClass.getAnnotation(LogicalDelete.class);
        String tableName = NamingUtils.resolveTableName(entityClass);
        String sql = "UPDATE " + tableName + " SET " + annotation.column() + " = ? WHERE id = ?";

        int affected = jdbcTemplate.update(sql, annotation.notDeletedValue(), id);
        if (affected > 0) {
            log.info("[Zerx] Restored {}[id={}] — column={}, value={}",
                    entityClass.getSimpleName(), id, annotation.column(), annotation.notDeletedValue());
        } else {
            log.warn("[Zerx] Restore affected 0 rows for {}[id={}]", entityClass.getSimpleName(), id);
        }
    }

    /**
     * 物理删除指定 ID 的记录（管理操作，谨慎使用）。
     * <p>
     * 直接执行 DELETE 语句，不可恢复。仅用于管理后台或数据清理场景。
     * 会以 WARN 级别记录日志。
     * </p>
     *
     * @param id          主键 ID
     * @param entityClass 实体类型
     * @param <T>         实体类型
     */
    public <T> void hardDeleteById(Long id, Class<T> entityClass) {
        Assert.notNull(id, "ID must not be null");
        Assert.notNull(entityClass, "Entity class must not be null");

        String tableName = NamingUtils.resolveTableName(entityClass);
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";

        log.warn("[Zerx] Hard (physical) deleting {}[id={}] — this operation is irreversible",
                entityClass.getSimpleName(), id);
        int affected = jdbcTemplate.update(sql, id);
        if (affected > 0) {
            log.warn("[Zerx] Hard deleted {}[id={}]", entityClass.getSimpleName(), id);
        } else {
            log.warn("[Zerx] Hard delete affected 0 rows for {}[id={}]", entityClass.getSimpleName(), id);
        }
    }

    /**
     * 检查实体类是否支持逻辑删除。
     *
     * @param entityClass 实体类型
     * @return 如果标注了 {@link LogicalDelete} 返回 {@code true}
     */
    public boolean isLogicalDelete(Class<?> entityClass) {
        return entityClass != null && entityClass.isAnnotationPresent(LogicalDelete.class);
    }

    /**
     * 获取未删除条件 SQL 片段。
     * <p>
     * 返回形如 {@code "deleted = '0'"} 的字符串，用于拼接到查询 SQL 的 WHERE 条件中。
     * </p>
     *
     * @param entityClass 实体类型（必须标注 {@link LogicalDelete}）
     * @return SQL 条件片段，例如 {@code "deleted = '0'"}
     */
    public String getNotDeletedCondition(Class<?> entityClass) {
        Assert.notNull(entityClass, "Entity class must not be null");
        validateLogicalDelete(entityClass);

        LogicalDelete annotation = entityClass.getAnnotation(LogicalDelete.class);
        return annotation.column() + " = '" + annotation.notDeletedValue() + "'";
    }

    /**
     * 为现有 SQL 自动追加 {@code WHERE deleted = '0'} 过滤条件。
     * <p>
     * 如果 SQL 已包含 WHERE 子句，则追加 AND 条件；否则追加 WHERE 子句。
     * 如果实体未标注 {@link LogicalDelete}，则原样返回 SQL。
     * </p>
     *
     * @param sql         原始 SQL
     * @param entityClass 实体类型
     * @return 追加了逻辑删除过滤条件的 SQL
     */
    public String appendNotDeletedFilter(String sql, Class<?> entityClass) {
        Assert.hasText(sql, "SQL must not be empty");
        Assert.notNull(entityClass, "Entity class must not be null");

        if (!isLogicalDelete(entityClass)) {
            return sql;
        }

        String condition = getNotDeletedCondition(entityClass);
        String upperSql = sql.toUpperCase();

        // 检测是否已包含 WHERE 子句（需要排除 ORDER BY 中的 "WHERE" 误判）
        boolean hasWhere = upperSql.contains(" WHERE ");

        if (hasWhere) {
            return sql + " AND " + condition;
        } else {
            return sql + " WHERE " + condition;
        }
    }

    /**
     * 验证实体类是否标注了 {@link LogicalDelete}。
     *
     * @param entityClass 实体类型
     * @throws IllegalArgumentException 如果未标注
     */
    private void validateLogicalDelete(Class<?> entityClass) {
        if (!entityClass.isAnnotationPresent(LogicalDelete.class)) {
            throw new IllegalArgumentException(
                    "Entity " + entityClass.getSimpleName() + " is not annotated with @LogicalDelete");
        }
    }
}
