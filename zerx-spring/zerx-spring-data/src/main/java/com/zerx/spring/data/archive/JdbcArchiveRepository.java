package com.zerx.spring.data.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于数据库归档表的 {@link Archiver} 默认实现。
 * <p>
 * 在物理删除前将数据完整复制到归档表（表名 = 原表名 + 后缀），然后从主表删除。
 * 归档表需要手动或通过 Flyway 迁移脚本提前创建，结构与主表完全一致。
 * </p>
 *
 * <h3>归档表 DDL 示例：</h3>
 * <pre>{@code
 * CREATE TABLE sys_user_archive LIKE sys_user;
 * -- 归档表额外添加归档时间字段
 * ALTER TABLE sys_user_archive ADD COLUMN archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
 * ALTER TABLE sys_user_archive ADD COLUMN archived_by BIGINT;
 * }</pre>
 *
 * <h3>线程安全：</h3>
 * <p>此类使用 {@link JdbcTemplate}，是线程安全的。</p>
 *
 * @param <T> 实体类型（必须继承 {@code BaseEntity}）
 * @author zerx
 */
public class JdbcArchiveRepository<T> implements Archiver<T> {

    private static final Logger log = LoggerFactory.getLogger(JdbcArchiveRepository.class);

    private final JdbcTemplate jdbcTemplate;
    private final ArchiveProperties properties;
    private final Class<T> entityClass;
    private final String mainTable;
    private final String archiveTable;

    /**
     * 构造基于数据库归档表的归档仓库。
     *
     * @param jdbcTemplate JDBC 模板
     * @param properties   归档配置属性
     * @param entityClass  实体类型
     */
    public JdbcArchiveRepository(JdbcTemplate jdbcTemplate, ArchiveProperties properties,
                                 Class<T> entityClass) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.entityClass = entityClass;
        this.mainTable = resolveTableName(entityClass);
        this.archiveTable = mainTable + properties.getTableSuffix();
    }

    @Override
    public void archive(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity must not be null");
        }

        Object id = extractId(entity);
        if (id == null) {
            throw new IllegalArgumentException(
                    "Cannot archive entity with null ID: " + entityClass.getSimpleName());
        }

        try {
            // 先检查归档表中是否已存在该记录（幂等性保证）
            long exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + archiveTable + " WHERE id = ?",
                    Long.class, id);
            if (exists > 0) {
                log.warn("[Zerx] Archive record already exists for {}[id={}], skipping archive",
                        entityClass.getSimpleName(), id);
                return;
            }

            // 将主表数据复制到归档表
            String insertSql = "INSERT INTO " + archiveTable +
                    " SELECT *, CURRENT_TIMESTAMP AS archived_at, NULL AS archived_by" +
                    " FROM " + mainTable + " WHERE id = ?";

            int rows = jdbcTemplate.update(insertSql, id);
            if (rows == 0) {
                throw new ArchiveException(entityClass, id,
                        "No record found in main table for archiving");
            }

            log.info("[Zerx] Archived {}[id={}] to {}", entityClass.getSimpleName(), id, archiveTable);
        } catch (ArchiveException e) {
            throw e;
        } catch (Exception e) {
            throw new ArchiveException(entityClass, id,
                    "Failed to archive data to " + archiveTable, e);
        }
    }

    @Override
    public Optional<T> restore(Object id) {
        if (id == null) {
            return Optional.empty();
        }

        try {
            // 从归档表查询原始数据
            Map<String, Object> archivedData = jdbcTemplate.queryForMap(
                    "SELECT * FROM " + archiveTable + " WHERE id = ?", id);
            if (archivedData.isEmpty()) {
                return Optional.empty();
            }

            // 从归档表删除已恢复的记录
            jdbcTemplate.update("DELETE FROM " + archiveTable + " WHERE id = ?", id);

            log.info("[Zerx] Restored {}[id={}] from {}", entityClass.getSimpleName(), id, archiveTable);

            // 返回 Map 形式的归档数据，由业务层负责重新插入主表
            // 注意：由于泛型擦除，直接反序列化为实体类型需要反射或自定义 RowMapper
            // 此处返回一个 Optional 包装的 Map，业务层可使用 DynamicQuery 插入
            @SuppressWarnings("unchecked")
            T result = (T) archivedData;
            return Optional.of(result);
        } catch (Exception e) {
            throw new ArchiveException(entityClass, id,
                    "Failed to restore data from " + archiveTable, e);
        }
    }

    /**
     * 根据主键查询归档数据（不删除）。
     *
     * @param id 实体主键
     * @return 归档数据的 Map，不存在时返回 empty
     */
    public Optional<Map<String, Object>> findArchived(Object id) {
        try {
            Map<String, Object> data = jdbcTemplate.queryForMap(
                    "SELECT * FROM " + archiveTable + " WHERE id = ?", id);
            return Optional.of(data);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 分页查询归档数据。
     *
     * @param limit  每页数量
     * @param offset 偏移量
     * @return 归档数据列表
     */
    public List<Map<String, Object>> listArchived(int limit, int offset) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM " + archiveTable + " ORDER BY archived_at DESC LIMIT ? OFFSET ?",
                limit, offset);
    }

    /**
     * 清理超过保留天数的归档数据。
     *
     * @return 清理的记录数
     */
    public int purgeExpired() {
        int deleted = jdbcTemplate.update(
                "DELETE FROM " + archiveTable +
                        " WHERE archived_at < DATE_SUB(NOW(), INTERVAL ? DAY)",
                properties.getRetainDays());
        if (deleted > 0) {
            log.info("[Zerx] Purged {} expired archive records from {}", deleted, archiveTable);
        }
        return deleted;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return entityClass.isAssignableFrom(clazz);
    }

    /**
     * 解析实体类对应的表名。
     * <p>
     * 优先使用 {@code @Table} 注解指定的表名，
     * 否则使用类名的 snake_case 转换。
     * </p>
     *
     * @param clazz 实体类
     * @return 表名
     */
    private String resolveTableName(Class<?> clazz) {
        var tableAnnotation = clazz.getAnnotation(org.springframework.data.relational.core.mapping.Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isBlank()) {
            return tableAnnotation.value();
        }
        // 默认使用类名转 snake_case
        return camelToSnake(clazz.getSimpleName());
    }

    /**
     * 从实体中提取 ID。
     * <p>
     * 通过反射调用 {@code getId()} 方法获取主键值。
     * </p>
     *
     * @param entity 实体对象
     * @return 主键值，获取失败返回 null
     */
    private Object extractId(T entity) {
        try {
            var method = entity.getClass().getMethod("getId");
            return method.invoke(entity);
        } catch (Exception e) {
            log.warn("[Zerx] Failed to extract ID from {}: {}",
                    entityClass.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * 驼峰命名转下划线命名
     */
    private static String camelToSnake(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
