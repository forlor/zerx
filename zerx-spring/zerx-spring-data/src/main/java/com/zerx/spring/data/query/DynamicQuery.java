package com.zerx.spring.data.query;

import com.zerx.common.model.PageRequest;
import com.zerx.common.model.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 链式 SQL 构建器，用于解决 Spring Data JDBC 在复杂查询场景下的能力不足。
 * <p>
 * DynamicQuery 底层委托 {@link JdbcTemplate} 执行，与 Spring Data JDBC 共享同一个
 * DataSource，保证事务一致性。所有 SQL 参数通过 PreparedStatement 绑定，防止 SQL 注入。
 * </p>
 *
 * <p>实例不是线程安全的，每次查询应创建新实例。通常用法：</p>
 * <pre>{@code
 * List<UserVO> users = DynamicQuery.from(jdbcTemplate, "sys_user")
 *     .select("id", "username", "email")
 *     .eq("status", "ACTIVE")
 *     .like("username", keyword)
 *     .between("create_time", startTime, endTime)
 *     .notIn("role", List.of("BANNED", "DELETED"))
 *     .orderBy("create_time", false)
 *     .limit(20)
 *     .offset(0)
 *     .list(rowMapper);
 * }</pre>
 *
 * <h3>支持的特性：</h3>
 * <ul>
 *     <li>WHERE 条件：eq / ne / like / notLike / in / notIn / between / ge / gt / le / lt / isNull / isNotNull / raw</li>
 *     <li>OR 条件组：or / endOr</li>
 *     <li>JOIN：leftJoin / innerJoin / rightJoin / crossJoin</li>
 *     <li>GROUP BY / HAVING</li>
 *     <li>ORDER BY（支持多列排序）</li>
 *     <li>DISTINCT</li>
 *     <li>分页：limit / offset / page</li>
 * </ul>
 *
 * @author zerx
 */
public class DynamicQuery {

    private final JdbcTemplate jdbcTemplate;
    private final StringBuilder sql;
    private final java.util.ArrayList<Object> params;
    private boolean whereAppended;
    private boolean inOrGroup;
    private final String table;
    private boolean selectOverridden;
    private int groupByCount;
    private int orderByCount;

    /**
     * 表名后是否刚追加了 " ("，用于 OR 组内第一个条件的判断。
     */
    private boolean afterOrOpenParen;

    private DynamicQuery(JdbcTemplate jdbcTemplate, String table) {
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
        this.sql = new StringBuilder("SELECT * FROM ").append(table);
        this.params = new java.util.ArrayList<>();
        this.whereAppended = false;
        this.inOrGroup = false;
        this.afterOrOpenParen = false;
        this.selectOverridden = false;
        this.groupByCount = 0;
        this.orderByCount = 0;
    }

    /**
     * 创建 DynamicQuery 实例
     *
     * @param jdbcTemplate JDBC 模板
     * @param table        表名（可带别名，如 "sys_user u"）
     * @return 新的 DynamicQuery 实例
     */
    public static DynamicQuery from(JdbcTemplate jdbcTemplate, String table) {
        return new DynamicQuery(jdbcTemplate, table);
    }

    // ======================== SELECT 子句 ========================

    /**
     * 指定查询列，覆盖默认的 SELECT *
     *
     * @param columns 列名列表
     * @return this
     */
    public DynamicQuery select(String... columns) {
        sql.setLength(0);
        sql.append("SELECT ").append(String.join(", ", columns)).append(" FROM ").append(table);
        selectOverridden = true;
        return this;
    }

    /**
     * 添加 DISTINCT 关键字
     *
     * @return this
     */
    public DynamicQuery distinct() {
        if (!selectOverridden) {
            sql.setLength(0);
            sql.append("SELECT DISTINCT * FROM ").append(table);
            selectOverridden = true;
        } else {
            // 替换已有的 SELECT 为 SELECT DISTINCT
            String current = sql.toString();
            sql.setLength(0);
            sql.append(current.replaceFirst("(?i)SELECT\\s+", "SELECT DISTINCT "));
        }
        return this;
    }

    // ======================== WHERE 条件（null 安全，null 值不追加条件） ========================

    /**
     * 等于条件
     */
    public DynamicQuery eq(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" = ?");
            params.add(value);
        }
        return this;
    }

    /**
     * 不等于条件
     */
    public DynamicQuery ne(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" != ?");
            params.add(value);
        }
        return this;
    }

    /**
     * LIKE 模糊查询（自动添加 % 通配符）
     */
    public DynamicQuery like(String column, String value) {
        if (value != null && !value.isBlank()) {
            appendWhere();
            sql.append(column).append(" LIKE ?");
            params.add("%" + value + "%");
        }
        return this;
    }

    /**
     * NOT LIKE 模糊查询（自动添加 % 通配符）
     */
    public DynamicQuery notLike(String column, String value) {
        if (value != null && !value.isBlank()) {
            appendWhere();
            sql.append(column).append(" NOT LIKE ?");
            params.add("%" + value + "%");
        }
        return this;
    }

    /**
     * IN 条件
     */
    public DynamicQuery in(String column, java.util.Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            appendWhere();
            String placeholders = values.stream()
                    .map(v -> "?")
                    .collect(java.util.stream.Collectors.joining(","));
            sql.append(column).append(" IN (").append(placeholders).append(")");
            params.addAll(values);
        }
        return this;
    }

    /**
     * NOT IN 条件
     */
    public DynamicQuery notIn(String column, java.util.Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            appendWhere();
            String placeholders = values.stream()
                    .map(v -> "?")
                    .collect(java.util.stream.Collectors.joining(","));
            sql.append(column).append(" NOT IN (").append(placeholders).append(")");
            params.addAll(values);
        }
        return this;
    }

    /**
     * BETWEEN 条件
     */
    public DynamicQuery between(String column, Object min, Object max) {
        if (min != null && max != null) {
            appendWhere();
            sql.append(column).append(" BETWEEN ? AND ?");
            params.add(min);
            params.add(max);
        }
        return this;
    }

    /**
     * 大于等于条件
     */
    public DynamicQuery ge(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" >= ?");
            params.add(value);
        }
        return this;
    }

    /**
     * 大于条件
     */
    public DynamicQuery gt(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" > ?");
            params.add(value);
        }
        return this;
    }

    /**
     * 小于等于条件
     */
    public DynamicQuery le(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" <= ?");
            params.add(value);
        }
        return this;
    }

    /**
     * 小于条件
     */
    public DynamicQuery lt(String column, Object value) {
        if (value != null) {
            appendWhere();
            sql.append(column).append(" < ?");
            params.add(value);
        }
        return this;
    }

    /**
     * IS NULL 条件
     */
    public DynamicQuery isNull(String column) {
        appendWhere();
        sql.append(column).append(" IS NULL");
        return this;
    }

    /**
     * IS NOT NULL 条件
     */
    public DynamicQuery isNotNull(String column) {
        appendWhere();
        sql.append(column).append(" IS NOT NULL");
        return this;
    }

    /**
     * 原始条件（自由拼接，用于复杂条件）
     */
    public DynamicQuery raw(String clause, Object... values) {
        appendWhere();
        sql.append(clause);
        if (values != null) {
            params.addAll(List.of(values));
        }
        return this;
    }

    // ======================== OR 条件组 ========================

    /**
     * 开始 OR 条件组。
     * <p>
     * OR 条件组内的条件使用 OR 连接，组间使用 AND 连接。
     * </p>
     * <pre>{@code
     * DynamicQuery.from(jdbc, "user")
     *     .eq("status", "ACTIVE")
     *     .or()
     *     .eq("role", "ADMIN")
     *     .orCondition()
     *     .eq("role", "SUPER_ADMIN")
     *     .endOr()
     *     .list(rowMapper);
     * // SQL: SELECT * FROM user WHERE status = 'ACTIVE' AND (role = 'ADMIN' OR role = 'SUPER_ADMIN')
     * }</pre>
     *
     * @return this
     */
    public DynamicQuery or() {
        appendWhere();
        sql.append('(');
        inOrGroup = true;
        afterOrOpenParen = true;
        return this;
    }

    /**
     * 结束 OR 条件组。
     *
     * @return this
     */
    public DynamicQuery endOr() {
        sql.append(')');
        inOrGroup = false;
        return this;
    }

    /**
     * 在 OR 组内添加 OR 连接符。
     * <p>
     * 配合 {@link #or()} 和 {@link #endOr()} 使用，
     * 在 OR 组内的多个条件之间插入 OR 关键字。
     * </p>
     *
     * @return this
     */
    public DynamicQuery orCondition() {
        sql.append(" OR ");
        return this;
    }

    // ======================== ORDER BY ========================

    /**
     * 排序（支持多次调用，以逗号分隔）
     *
     * @param column 列名
     * @param asc    true 为升序，false 为降序
     * @return this
     */
    public DynamicQuery orderBy(String column, boolean asc) {
        if (orderByCount == 0) {
            sql.append(" ORDER BY ");
        } else {
            sql.append(", ");
        }
        sql.append(column).append(asc ? " ASC" : " DESC");
        orderByCount++;
        return this;
    }

    // ======================== GROUP BY / HAVING ========================

    /**
     * GROUP BY
     */
    public DynamicQuery groupBy(String... columns) {
        if (groupByCount == 0) {
            sql.append(" GROUP BY ");
        } else {
            sql.append(", ");
        }
        sql.append(String.join(", ", columns));
        groupByCount++;
        return this;
    }

    /**
     * HAVING
     */
    public DynamicQuery having(String clause, Object... values) {
        sql.append(" HAVING ").append(clause);
        if (values != null) {
            params.addAll(List.of(values));
        }
        return this;
    }

    // ======================== JOIN ========================

    /**
     * LEFT JOIN
     */
    public DynamicQuery leftJoin(String table, String onClause, Object... joinParams) {
        sql.append(" LEFT JOIN ").append(table).append(" ON ").append(onClause);
        if (joinParams != null) {
            params.addAll(List.of(joinParams));
        }
        return this;
    }

    /**
     * INNER JOIN
     */
    public DynamicQuery innerJoin(String table, String onClause, Object... joinParams) {
        sql.append(" INNER JOIN ").append(table).append(" ON ").append(onClause);
        if (joinParams != null) {
            params.addAll(List.of(joinParams));
        }
        return this;
    }

    /**
     * RIGHT JOIN
     */
    public DynamicQuery rightJoin(String table, String onClause, Object... joinParams) {
        sql.append(" RIGHT JOIN ").append(table).append(" ON ").append(onClause);
        if (joinParams != null) {
            params.addAll(List.of(joinParams));
        }
        return this;
    }

    /**
     * CROSS JOIN
     */
    public DynamicQuery crossJoin(String table) {
        sql.append(" CROSS JOIN ").append(table);
        return this;
    }

    // ======================== 分页 ========================

    /**
     * LIMIT
     */
    public DynamicQuery limit(int limit) {
        sql.append(" LIMIT ?");
        params.add(limit);
        return this;
    }

    /**
     * OFFSET
     */
    public DynamicQuery offset(int offset) {
        sql.append(" OFFSET ?");
        params.add(offset);
        return this;
    }

    // ======================== 执行查询 ========================

    /**
     * 执行查询，返回 Map 列表
     *
     * @return 查询结果列表
     */
    public List<Map<String, Object>> list() {
        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    /**
     * 执行查询，使用 RowMapper 映射为实体列表
     *
     * @param rowMapper 行映射器
     * @param <T>       返回类型
     * @return 查询结果列表
     */
    public <T> List<T> list(RowMapper<T> rowMapper) {
        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }

    /**
     * 执行查询，返回单条记录
     *
     * @param rowMapper 行映射器
     * @param <T>       返回类型
     * @return 单条记录的 Optional，无结果返回 empty
     */
    public <T> Optional<T> one(RowMapper<T> rowMapper) {
        List<T> result = list(rowMapper);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.getFirst());
    }

    /**
     * 统计查询结果数
     *
     * @return 记录数
     */
    public long count() {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") _cnt";
        Long count = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return count != null ? count : 0;
    }

    /**
     * 分页查询，返回 zerx-common 的 {@link PageResult}
     *
     * @param pageRequest 分页请求参数
     * @param rowMapper   行映射器
     * @param <T>         返回类型
     * @return 分页结果
     */
    public <T> PageResult<T> page(PageRequest pageRequest, RowMapper<T> rowMapper) {
        long total = count();
        List<T> records = jdbcTemplate.query(
                sql + " LIMIT ? OFFSET ?",
                rowMapper,
                concatParams(pageRequest.size(), pageRequest.offset())
        );
        return PageResult.of(records, total, pageRequest.page(), pageRequest.size());
    }

    // ======================== 调试 ========================

    /**
     * 获取构建的 SQL 语句
     */
    public String getSql() {
        return sql.toString();
    }

    /**
     * 获取参数列表
     */
    public Object[] getParams() {
        return params.toArray();
    }

    // ======================== 内部方法 ========================

    private void appendWhere() {
        String current = sql.toString();
        if (!whereAppended) {
            sql.append(" WHERE ");
            whereAppended = true;
        } else if (afterOrOpenParen) {
            // OR 组内的第一个条件，紧跟在 "(" 之后，不加任何连接符
            afterOrOpenParen = false;
        } else if (inOrGroup && current.endsWith(" OR ")) {
            // 紧跟在 " OR " 后面的条件，不需要额外分隔符
            // " OR " 已经由 orCondition() 添加
        } else if (!inOrGroup) {
            sql.append(" AND ");
        }
    }

    /**
     * 拼接 LIMIT 和 OFFSET 参数
     */
    private Object[] concatParams(int limit, int offset) {
        Object[] allParams = new Object[params.size() + 2];
        System.arraycopy(params.toArray(), 0, allParams, 0, params.size());
        allParams[params.size()] = limit;
        allParams[params.size() + 1] = offset;
        return allParams;
    }
}
