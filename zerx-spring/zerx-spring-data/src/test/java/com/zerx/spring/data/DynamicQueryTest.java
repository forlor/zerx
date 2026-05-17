package com.zerx.spring.data;

import com.zerx.common.model.PageRequest;
import com.zerx.common.model.PageResult;
import com.zerx.spring.data.query.DynamicQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
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
                    deleted INT DEFAULT 0,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);

        jdbcTemplate.execute("DELETE FROM test_user");

        // 插入测试数据
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age, deleted) VALUES (1, 'alice', 'alice@test.com', 'ACTIVE', 25, 0)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age, deleted) VALUES (2, 'bob', 'bob@test.com', 'ACTIVE', 30, 0)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age, deleted) VALUES (3, 'charlie', 'charlie@test.com', 'DISABLED', 28, 0)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age, deleted) VALUES (4, 'deleted_user', 'del@test.com', 'ACTIVE', 22, 1)");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email, status, age, deleted) VALUES (5, 'eve', 'eve@test.com', 'ACTIVE', 35, 0)");
    }

    private final RowMapper<Map<String, Object>> mapRowMapper = (rs, rowNum) -> {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("username", rs.getString("username"));
        map.put("email", rs.getString("email"));
        map.put("status", rs.getString("status"));
        map.put("age", rs.getInt("age"));
        map.put("deleted", rs.getInt("deleted"));
        return map;
    };

    @Test
    void select_all() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(4, result.size());
    }

    @Test
    void select_columns() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .select("username", "email")
                .eq("deleted", 0)
                .list();

        assertEquals(4, result.size());
        // 只包含 select 的列
        assertTrue(result.getFirst().containsKey("username"));
        assertTrue(result.getFirst().containsKey("email"));
    }

    @Test
    void eq_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(3, result.size());
        result.forEach(r -> assertEquals("ACTIVE", r.get("status")));
    }

    @Test
    void ne_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .ne("status", "ACTIVE")
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(1, result.size());
        assertEquals("charlie", result.getFirst().get("username"));
    }

    @Test
    void like_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .like("username", "ali")
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(1, result.size());
        assertEquals("alice", result.getFirst().get("username"));
    }

    @Test
    void in_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .in("username", List.of("alice", "bob", "eve"))
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(3, result.size());
    }

    @Test
    void between_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .between("age", 25, 30)
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(3, result.size());
    }

    @Test
    void ge_le_conditions() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .ge("age", 30)
                .le("age", 35)
                .eq("deleted", 0)
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
                .eq("deleted", 0)
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
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(4, result.size());
        result.forEach(r -> assertNotNull(r.get("email")));
    }

    @Test
    void raw_condition() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .raw("username LIKE ?", "a%")
                .eq("deleted", 0)
                .list(mapRowMapper);

        assertEquals(1, result.size());
        assertEquals("alice", result.getFirst().get("username"));
    }

    @Test
    void order_by_asc() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("deleted", 0)
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
                .eq("deleted", 0)
                .orderBy("age", false)
                .list(mapRowMapper);

        for (int i = 1; i < result.size(); i++) {
            int prev = (int) result.get(i - 1).get("age");
            int curr = (int) result.get(i).get("age");
            assertTrue(prev >= curr, "DESC order violated: " + prev + " < " + curr);
        }
    }

    @Test
    void limit_offset() {
        var result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("deleted", 0)
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
                .eq("u.deleted", 0)
                .list();

        assertEquals(4, result.size()); // LEFT JOIN, 2 users with orders + 2 without
    }

    @Test
    void count() {
        long count = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .eq("deleted", 0)
                .count();

        assertEquals(3, count);
    }

    @Test
    void one_found() {
        Optional<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("username", "alice")
                .eq("deleted", 0)
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
                .eq("deleted", 0)
                .orderBy("id", true)
                .page(pageReq, mapRowMapper);

        assertEquals(4, result.total());
        assertEquals(1, result.page());
        assertEquals(2, result.size());
        assertEquals(2, result.records().size());
        assertEquals(2, result.totalPages());
    }

    @Test
    void page_secondPage() {
        PageRequest pageReq = new PageRequest(2, 2);
        PageResult<Map<String, Object>> result = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("deleted", 0)
                .orderBy("id", true)
                .page(pageReq, mapRowMapper);

        assertEquals(4, result.total());
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
                .eq("deleted", 0)
                .groupBy("status")
                .having("COUNT(*) > ?", 1)
                .list();

        // ACTIVE status has 3 users
        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.getFirst().get("status"));
    }

    @Test
    void getSql_and_getParams() {
        var dq = DynamicQuery.from(jdbcTemplate, "test_user")
                .eq("status", "ACTIVE")
                .eq("deleted", 0)
                .orderBy("id", true);

        String sql = dq.getSql();
        Object[] params = dq.getParams();

        assertTrue(sql.contains("SELECT * FROM test_user"));
        assertTrue(sql.contains("WHERE"));
        assertTrue(sql.contains("ORDER BY id ASC"));
        assertArrayEquals(new Object[]{"ACTIVE", 0}, params);
    }
}
