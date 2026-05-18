package com.zerx.spring.data.datascope;

import com.zerx.spring.data.datascope.DataScope.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * 数据权限 SQL 条件生成器。
 * <p>
 * 根据 {@link DataScope} 注解和当前用户上下文，生成对应的 SQL WHERE 条件片段。
 * 通过 JdbcTemplate 的参数化查询注入条件值，防止 SQL 注入。
 * </p>
 *
 * <h3>权限规则：</h3>
 * <ul>
 *     <li>{@link Type#SELF}：column = currentUser.id</li>
 *     <li>{@link Type#DEPT}：column IN (当前用户所在部门的ID列表)</li>
 *     <li>{@link Type#DEPT_AND_CHILD}：column IN (当前部门及子部门的ID列表)</li>
 *     <li>{@link Type#ALL}：不追加条件</li>
 * </ul>
 *
 * @author zerx
 */
public class DataScopeHandler {

    private static final Logger log = LoggerFactory.getLogger(DataScopeHandler.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造数据权限处理器
     *
     * @param jdbcTemplate JDBC 模板（用于查询部门层级关系）
     */
    public DataScopeHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 根据数据权限注解和用户上下文生成 SQL 条件片段。
     *
     * @param dataScope    数据权限注解
     * @param currentUser  当前用户信息
     * @return SQL 条件片段（如 "dept_id IN (?,?,?)"），全部权限时返回 null
     */
    public DataScopeSql generateCondition(DataScope dataScope, DataScopeUser currentUser) {
        if (dataScope == null || currentUser == null || dataScope.type() == Type.ALL) {
            return null;
        }

        String column = dataScope.column();
        List<Object> sqlParams = new java.util.ArrayList<>();

        String condition = switch (dataScope.type()) {
            case SELF -> {
                sqlParams.add(currentUser.userId());
                yield column + " = ?";
            }
            case DEPT -> {
                if (currentUser.deptIds() == null || currentUser.deptIds().isEmpty()) {
                    // 无部门信息，返回永假条件
                    yield "1 = 0";
                }
                sqlParams.addAll(currentUser.deptIds());
                String placeholders = currentUser.deptIds().stream()
                        .map(v -> "?")
                        .collect(java.util.stream.Collectors.joining(","));
                yield column + " IN (" + placeholders + ")";
            }
            case DEPT_AND_CHILD -> {
                if (currentUser.deptIds() == null || currentUser.deptIds().isEmpty()) {
                    yield "1 = 0";
                }
                // 查询所有子部门 ID（包括当前部门）
                List<Long> allDeptIds = resolveChildDeptIds(currentUser.deptIds());
                if (allDeptIds.isEmpty()) {
                    yield "1 = 0";
                }
                sqlParams.addAll(allDeptIds);
                String placeholders = allDeptIds.stream()
                        .map(v -> "?")
                        .collect(java.util.stream.Collectors.joining(","));
                yield column + " IN (" + placeholders + ")";
            }
            default -> null;
        };

        if (condition != null) {
            log.debug("[Zerx] DataScope applied: type={}, condition={}",
                    dataScope.type(), condition);
        }

        return condition != null ? new DataScopeSql(condition, sqlParams) : null;
    }

    /**
     * 查询所有子部门 ID（含自身）。
     * <p>
     * 使用递归 CTE 查询部门树。如果数据库不支持递归 CTE，
     * 业务方需要自行提供子部门列表。
     * </p>
     *
     * @param parentDeptIds 父部门 ID 列表
     * @return 所有部门 ID 列表（含自身和子部门）
     */
    private List<Long> resolveChildDeptIds(List<Long> parentDeptIds) {
        try {
            String placeholders = parentDeptIds.stream()
                    .map(v -> "?")
                    .collect(java.util.stream.Collectors.joining(","));
            String sql = """
                    WITH RECURSIVE dept_tree AS (
                        SELECT id FROM sys_dept WHERE id IN (%s)
                        UNION ALL
                        SELECT d.id FROM sys_dept d
                        INNER JOIN dept_tree dt ON d.parent_id = dt.id
                    )
                    SELECT id FROM dept_tree
                    """.formatted(placeholders);

            return jdbcTemplate.queryForList(sql, Long.class, parentDeptIds.toArray());
        } catch (Exception e) {
            log.warn("[Zerx] Failed to resolve child dept IDs, falling back to parent IDs only: {}",
                    e.getMessage());
            return parentDeptIds;
        }
    }

    /**
     * 数据权限 SQL 条件片段
     *
     * @param condition SQL 条件（如 "dept_id IN (?,?,?)"）
     * @param params    参数列表
     */
    public record DataScopeSql(String condition, List<Object> params) {
    }
}
