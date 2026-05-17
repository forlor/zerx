package com.zerx.spring.data;

import com.zerx.common.model.PageRequest;
import com.zerx.common.model.PageResult;
import com.zerx.spring.data.query.DynamicQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DynamicQuery 链式 SQL 构建器测试。
 * <p>
 * 使用 H2 内存数据库验证 SQL 构建逻辑和查询结果。
 * </p>
 */
class DynamicQueryTest {

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 使用 H2 内存数据库
        var dataSource = new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:dq_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    age INT,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("DELETE FROM test_user");

        // 插入测试数据
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age) VALUES (1, 'alice', 'alice@test.com', 'ACTIVE', 25)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age) VALUES (2, 'bob', 'bob@test.com', 'ACTIVE', 30)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age) VALUES (3, 'charlie', 'charlie@test.com', 'DISABLED', 28)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age) VALUES (4, 'diana', 'diana@test.com', 'ACTIVE', 22)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age) VALUES (5, 'eve', 'eve@test.com', 'ACTIVE', 35)");
    }

    private final RowMapper<Map<String, Object>> mapRowMapper = (rs, rowNum) -> {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("username", rs.getString("username"));
        map.put("email", rs.getString("email"));
        map.put("status", rs.getString("status"));
        map.put("age", rs.getInt("age"));
        return map;
    };

    @Test
    void select_all() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .list(mapRowMapper);

        assertEquals(5, result.size());
    }

    @Test
    void select_columns() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .select("username", "email")
                .list();

        assertEquals(5, result.size());
        // 只包含 select 的列
        assertTrue(result.getFirst().containsKey("username"));
        assertTrue(result.getFirst().containsKey("email"));
    }

    @Test
    void distinct() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .select("status")
                .distinct()
                .list();

        assertEquals(2, result.size());
    }

    @Test
    void eq_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .list(mapRowMapper);

        assertEquals(4, result.size());
        result.forEach(r -> assertEquals("ACTIVE", r.get("status")));
    }

    @Test
    void ne_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .ne("status", "ACTIVE")
                .list(mapRowMapper);

        assertEquals(1, result.size());
        assertEquals("charlie", result.getFirst().get("username"));
    }

    @Test
    void like_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .like("username", "ali")
                .list(mapRowMapper);

        assertEquals(1, result.size());
        assertEquals("alice", result.getFirst().get("username"));
    }

    @Test
    void notLike_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .notLike("username", "a")
                .list(mapRowMapper);

        assertEquals(2, result.size());
        result.forEach(r -> {
            String name = (String) r.get("username");
            assertFalse(name.contains("a"));
        });
    }

    @Test
    void in_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .in("username", List.of("alice", "bob", "eve"))
                .list(mapRowMapper);

        assertEquals(3, result.size());
    }

    @Test
    void notIn_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .notIn("status", List.of("DISABLED"))
                .list(mapRowMapper);

        assertEquals(4, result.size());
        result.forEach(r -> assertEquals("ACTIVE", r.get("status")));
    }

    @Test
    void between_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .between("age", 25, 30)
                .list(mapRowMapper);

        assertEquals(3, result.size());
    }

    @Test
    void ge_le_conditions() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .ge("age", 30)
                .le("age", 35)
                .list(mapRowMapper);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> {
            int age = (int) r.get("age");
            return age >= 30 && age <= 35;
        }));
    }

    @Test
    void gt_lt_conditions() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .gt("age", 25)
                .lt("age", 35)
                .list(mapRowMapper);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> {
            int age = (int) r.get("age");
            return age > 25 && age < 35;
        }));
    }

    @Test
    void null_conditions_skipped() {
        // null 值不追加条件
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", null)
                .like("username", null)
                .between("age", null, null);

        String sql = dq.getSql();
        // 不应包含 WHERE 子句（所有条件都是 null）
        assertFalse(sql.contains("WHERE"));
    }

    @Test
    void empty_like_skipped() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .like("username", "")
                .like("email", "   ");

        String sql = dq.getSql();
        assertFalse(sql.contains("WHERE"));
    }

    @Test
    void isNull_isNotNull() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .isNotNull("email")
                .list(mapRowMapper);

        assertEquals(5, result.size());
        result.forEach(r -> assertNotNull(r.get("email")));
    }

    @Test
    void raw_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .raw("username LIKE ?", "a%")
                .list(mapRowMapper);

        assertEquals(1, result.size());
        assertEquals("alice", result.getFirst().get("username"));
    }

    @Test
    void or_condition_group() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .or()
                .eq("username", "charlie")
                .orCondition()
                .eq("username", "eve")
                .endOr();

        String sql = dq.getSql();
        // Expected: SELECT * FROM test_user WHERE status = ? AND (username = ? OR username = ?)
        assertTrue(sql.contains("AND (username = ? OR username = ?)"));

        var result = dq.list(mapRowMapper);

        // SQL: WHERE status = ? AND (username = ? OR username = ?)
        // ACTIVE users: alice, bob, diana, eve — only eve matches username IN (charlie, eve)
        assertEquals(1, result.size());
        assertEquals("eve", result.getFirst().get("username"));
    }

    @Test
    void order_by_asc() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .orderBy("age", true)
                .list(mapRowMapper);

        for (int i = 1; i < result.size(); i++) {
            int prev = (int) result.get(i - 1).get("age");
            int curr = (int) result.get(i).get("age");
            assertTrue(prev <= curr, "ASC order violated: " + prev + " > " + curr);
        }
    }

    @Test
    void order_by_desc() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .orderBy("age", false)
                .list(mapRowMapper);

        for (int i = 1; i < result.size(); i++) {
            int prev = (int) result.get(i - 1).get("age");
            int curr = (int) result.get(i).get("age");
            assertTrue(prev >= curr, "DESC order violated: " + prev + " < " + curr);
        }
    }

    @Test
    void multi_column_order_by() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .orderBy("status", true)
                .orderBy("age", false)
                .list(mapRowMapper);

        // 第一个 ACTIVE 用户 age 应该最大
        String firstStatus = (String) result.getFirst().get("status");
        assertEquals("ACTIVE", firstStatus);
    }

    @Test
    void limit_offset() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .orderBy("id", true)
                .limit(2)
                .offset(1)
                .list(mapRowMapper);

        assertEquals(2, result.size());
        assertEquals("bob", result.getFirst().get("username"));
        assertEquals("charlie", result.get(1).get("username"));
    }

    @Test
    void left_join() {
        // 创建关联表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_order (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    amount DECIMAL(10,2) DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM test_order");
        jdbcTemplate.update("INSERT INTO test_order (id, user_id, amount) VALUES (1, 1, 100.00)");
        jdbcTemplate.update("INSERT INTO test_order (id, user_id, amount) VALUES (2, 2, 200.00)");

        var result = DynamicQuery.from(jdbcTemplate, "test_user u")
                .select("u.username", "o.amount")
                .leftJoin("test_order o", "o.user_id = u.id")
                .list();

        assertEquals(5, result.size()); // LEFT JOIN, 2 users with orders + 3 without
    }

    @Test
    void inner_join() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_order (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    amount DECIMAL(10,2) DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM test_order");
        jdbcTemplate.update("INSERT INTO test_order (id, user_id, amount) VALUES (1, 1, 100.00)");
        jdbcTemplate.update("INSERT INTO test_order (id, user_id, amount) VALUES (2, 2, 200.00)");

        var result = DynamicQuery.from(jdbcTemplate, "test_user u")
                .select("u.username", "o.amount")
                .innerJoin("test_order o", "o.user_id = u.id")
                .list();

        assertEquals(2, result.size()); // INNER JOIN, only users with orders
    }

    @Test
    void right_join() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_order (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    amount DECIMAL(10,2) DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM test_order");
        jdbcTemplate.update("INSERT INTO test_order (id, user_id, amount) VALUES (1, 1, 100.00)");
        jdbcTemplate.update("INSERT INTO test_order (id, user_id, amount) VALUES (2, 99, 200.00)");

        var result = DynamicQuery.from(jdbcTemplate, "test_user u")
                .select("u.username", "o.amount")
                .rightJoin("test_order o", "o.user_id = u.id")
                .list();

        assertEquals(2, result.size());
    }

    @Test
    void count() {
        long count = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .count();

        assertEquals(4, count);
    }

    @Test
    void one_found() {
        Optional<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("username", "alice")
                .one(mapRowMapper);

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().get("username"));
    }

    @Test
    void one_not_found() {
        Optional<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("username", "nonexistent")
                .one(mapRowMapper);

        assertFalse(result.isPresent());
    }

    @Test
    void page_firstPage() {
        PageRequest pageReq = new PageRequest(1, 2);
        PageResult<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .orderBy("id", true)
                .page(pageReq, mapRowMapper);

        assertEquals(5, result.total());
        assertEquals(1, result.page());
        assertEquals(2, result.size());
        assertEquals(2, result.records().size());
        assertEquals(3, result.totalPages());
    }

    @Test
    void page_secondPage() {
        PageRequest pageReq = new PageRequest(2, 2);
        PageResult<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .orderBy("id", true)
                .page(pageReq, mapRowMapper);

        assertEquals(5, result.total());
        assertEquals(2, result.page());
        assertEquals(2, result.records().size());
    }

    @Test
    void page_emptyResult() {
        PageRequest pageReq = new PageRequest(1, 10);
        PageResult<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "NONEXISTENT")
                .page(pageReq, mapRowMapper);

        assertEquals(0, result.total());
        assertTrue(result.isEmpty());
        assertFalse(result.hasNext());
    }

    @Test
    void groupBy_having() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .select("status", "COUNT(*) AS cnt")
                .groupBy("status")
                .having("COUNT(*) > ?", 1)
                .list();

        // ACTIVE status has 4 users
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.getFirst().get("status"));
    }

    @Test
    void getSql_and_getParams() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .orderBy("id", true);

        String sql = dq.getSql();
        Object[] params = dq.getParams();

        assertTrue(sql.contains("SELECT * FROM test_user"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("ORDER BY id ASC"));
        assertArrayEquals(new Object[]{"ACTIVE"}, params);
    }

    @Test
    void empty_collection_in_notAppended() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .in("username", List.of());

        String sql = dq.getSql();
        assertFalse(sql.contains("WHERE"));
        assertFalse(sql.contains("IN"));
    }

    @Test
    void null_collection_in_notAppended() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .in("username", (java.util.Collection<?>) null);

        String sql = dq.getSql();
        assertFalse(sql.contains("WHERE"));
    }

    @Test
    void notIn_empty_collection_notAppended() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .notIn("status", List.of());

        String sql = dq.getSql();
        assertFalse(sql.contains("WHERE"));
        assertFalse(sql.contains("NOT IN"));
    }
}
