package com.zerx.spring.data.repository;

import com.zerx.spring.data.properties.ZerxDataProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * ZerxRepository Fragment 实现类。
 * <p>
 * 通过 {@link JdbcTemplate} 实现逻辑删除感知的通用查询方法。
 * 逻辑删除的字段名和标记值从 {@link ZerxDataProperties} 中读取。
 * </p>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author zerx
 */
@Component
public class ZerxRepositoryImpl<T, ID> {

    private final JdbcTemplate jdbcTemplate;
    private final ZerxDataProperties properties;

    public ZerxRepositoryImpl(JdbcTemplate jdbcTemplate, ZerxDataProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /**
     * 根据 ID 查询（过滤已删除）。
     * <p>
     * 该方法需要子类提供具体的表名和 RowMapper，因此设计为 protected 工具方法。
     * 业务代码应通过 Repository 接口的 default 方法调用。
     * </p>
     */
    public <E> Optional<E> findByIdAndDeletedFalse(String tableName, ID id,
                                                    RowMapper<E> rowMapper, String idColumn) {
        String field = properties.getLogicDelete().getField();
        int notDeleted = properties.getLogicDelete().getNotDeletedValue();
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumn + " = ? AND " + field + " = ?";
        List<E> results = jdbcTemplate.query(sql, rowMapper, id, notDeleted);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * 查询所有未删除记录
     */
    public <E> List<E> findAllByDeletedFalse(String tableName, RowMapper<E> rowMapper) {
        String field = properties.getLogicDelete().getField();
        int notDeleted = properties.getLogicDelete().getNotDeletedValue();
        String sql = "SELECT * FROM " + tableName + " WHERE " + field + " = ?";
        return jdbcTemplate.query(sql, rowMapper, notDeleted);
    }

    /**
     * 统计未删除记录数
     */
    public long countByDeletedFalse(String tableName) {
        String field = properties.getLogicDelete().getField();
        int notDeleted = properties.getLogicDelete().getNotDeletedValue();
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + field + " = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, notDeleted);
        return count != null ? count : 0;
    }

    /**
     * 判断指定 ID 的未删除记录是否存在
     */
    public boolean existsByIdAndDeletedFalse(String tableName, ID id, String idColumn) {
        String field = properties.getLogicDelete().getField();
        int notDeleted = properties.getLogicDelete().getNotDeletedValue();
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + idColumn + " = ? AND " + field + " = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, id, notDeleted);
        return count != null && count > 0;
    }
}
