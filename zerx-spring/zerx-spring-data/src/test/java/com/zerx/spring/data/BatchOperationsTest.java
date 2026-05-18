package com.zerx.spring.data;

import com.zerx.spring.data.repository.BatchOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BatchOperations 测试。
 * <p>
 * 使用 H2 内存数据库验证批量插入、更新、删除功能。
 * </p>
 */
class BatchOperationsTest {

    private JdbcTemplate jdbcTemplate;
    private BatchOperations batchOperations;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:batch_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        jdbcTemplate = new JdbcTemplate(dataSource);
        batchOperations = new BatchOperations(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_batch_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    status INT DEFAULT 1
                )
                """);
        jdbcTemplate.execute("DELETE FROM test_batch_user");
    }

    // ======================== batchInsert 测试 ========================

    @Test
    void batchInsert_singleBatch() {
        List<Object[]> rows = List.of(
                new Object[]{"user1", "a@test.com", 1},
                new Object[]{"user2", "b@test.com", 1},
                new Object[]{"user3", "c@test.com", 0},
                new Object[]{"user4", "d@test.com", 1},
                new Object[]{"user5", "e@test.com", 0}
        );

        int[] result = batchOperations.batchInsert("test_batch_user",
                List.of("username", "email", "status"), rows);

        assertEquals(5, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_batch_user", Long.class);
        assertEquals(5L, count);
    }

    @Test
    void batchInsert_largeBatch() {
        // 默认 batch size 200，插入 500 行，验证分批处理正确
        List<Object[]> rows = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            rows.add(new Object[]{"user_" + i, "user" + i + "@test.com", 1});
        }

        int[] result = batchOperations.batchInsert("test_batch_user",
                List.of("username", "email", "status"), rows);

        assertEquals(500, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_batch_user", Long.class);
        assertEquals(500L, count);
    }

    @Test
    void batchInsert_emptyRows_throwsException() {
        List<Object[]> emptyRows = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () ->
                batchOperations.batchInsert("test_batch_user",
                        List.of("username"), emptyRows));
    }

    @Test
    void batchInsert_emptyColumns_throwsException() {
        Object[][] nonEmptyRows = {new Object[]{"user1"}};

        assertThrows(IllegalArgumentException.class, () ->
                batchOperations.batchInsert("test_batch_user",
                        List.<String>of(), Arrays.asList(nonEmptyRows)));
    }

    // ======================== batchUpdate 测试 ========================

    @Test
    void batchUpdate_singleColumn() {
        // 先插入 3 条数据
        jdbcTemplate.update("INSERT INTO test_batch_user (username, email, status) VALUES (?, ?, ?)",
                "user1", "a@test.com", 1);
        jdbcTemplate.update("INSERT INTO test_batch_user (username, email, status) VALUES (?, ?, ?)",
                "user2", "b@test.com", 1);
        jdbcTemplate.update("INSERT INTO test_batch_user (username, email, status) VALUES (?, ?, ?)",
                "user3", "c@test.com", 1);

        // 批量更新 status = 0
        Long id1 = jdbcTemplate.queryForObject("SELECT id FROM test_batch_user WHERE username = 'user1'", Long.class);
        Long id2 = jdbcTemplate.queryForObject("SELECT id FROM test_batch_user WHERE username = 'user2'", Long.class);
        Long id3 = jdbcTemplate.queryForObject("SELECT id FROM test_batch_user WHERE username = 'user3'", Long.class);

        int[] result = batchOperations.batchUpdate("test_batch_user",
                List.of("status"),
                List.of("id"),
                List.of(
                        new Object[]{0, id1},
                        new Object[]{0, id2},
                        new Object[]{0, id3}
                ));

        assertEquals(3, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        // 验证全部更新为 0
        Long disabledCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_batch_user WHERE status = 0", Long.class);
        assertEquals(3L, disabledCount);
    }

    @Test
    void batchUpdate_multipleColumns() {
        jdbcTemplate.update("INSERT INTO test_batch_user (username, email, status) VALUES (?, ?, ?)",
                "user1", "a@test.com", 1);
        jdbcTemplate.update("INSERT INTO test_batch_user (username, email, status) VALUES (?, ?, ?)",
                "user2", "b@test.com", 1);

        Long id1 = jdbcTemplate.queryForObject("SELECT id FROM test_batch_user WHERE username = 'user1'", Long.class);
        Long id2 = jdbcTemplate.queryForObject("SELECT id FROM test_batch_user WHERE username = 'user2'", Long.class);

        int[] result = batchOperations.batchUpdate("test_batch_user",
                List.of("username", "email"),
                List.of("id"),
                List.of(
                        new Object[]{"updated1", "updated1@test.com", id1},
                        new Object[]{"updated2", "updated2@test.com", id2}
                ));

        assertEquals(2, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        String name = jdbcTemplate.queryForObject(
                "SELECT username FROM test_batch_user WHERE id = ?", String.class, id1);
        assertEquals("updated1", name);

        String email = jdbcTemplate.queryForObject(
                "SELECT email FROM test_batch_user WHERE id = ?", String.class, id2);
        assertEquals("updated2@test.com", email);
    }

    @Test
    void batchUpdate_noAffectedRows() {
        // 表为空，更新不存在的 ID
        int[] result = batchOperations.batchUpdate("test_batch_user",
                List.of("status"),
                List.of("id"),
                List.of(
                        new Object[]{0, 99999L},
                        new Object[]{0, 88888L}
                ));

        assertEquals(2, result.length);
        for (int count : result) {
            assertEquals(0, count);
        }
    }

    // ======================== batchDelete 测试 ========================

    @Test
    void batchDelete_byIds() {
        // 插入 5 条数据
        List<Long> ids = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            jdbcTemplate.update("INSERT INTO test_batch_user (username, email, status) VALUES (?, ?, ?)",
                    "user" + i, "user" + i + "@test.com", 1);
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM test_batch_user WHERE username = ?", Long.class, "user" + i);
            ids.add(id);
        }

        // 删除前 3 条
        int[] result = batchOperations.batchDelete("test_batch_user",
                List.of("id"),
                List.of(
                        new Object[]{ids.get(0)},
                        new Object[]{ids.get(1)},
                        new Object[]{ids.get(2)}
                ));

        assertEquals(3, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        Long remaining = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_batch_user", Long.class);
        assertEquals(2L, remaining);
    }

    @Test
    void batchDelete_nonExistent() {
        // 删除不存在的 ID，不应报错
        int[] result = batchOperations.batchDelete("test_batch_user",
                List.of("id"),
                List.of(
                        new Object[]{99999L},
                        new Object[]{88888L}
                ));

        assertEquals(2, result.length);
        for (int count : result) {
            assertEquals(0, count);
        }

        Long remaining = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_batch_user", Long.class);
        assertEquals(0L, remaining);
    }

    // ======================== 自定义 batch size 测试 ========================

    @Test
    void batchInsert_customBatchSize() {
        DataSource dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:batch_custom;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        JdbcTemplate customJdbc = new JdbcTemplate(dataSource);
        BatchOperations customBatch = new BatchOperations(dataSource, 50);

        customJdbc.execute("""
                CREATE TABLE IF NOT EXISTS test_batch_custom (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(50),
                    val INT
                )
                """);

        assertEquals(50, customBatch.getBatchSize());

        List<Object[]> rows = new ArrayList<>(120);
        for (int i = 0; i < 120; i++) {
            rows.add(new Object[]{"item_" + i, i});
        }

        int[] result = customBatch.batchInsert("test_batch_custom",
                List.of("name", "val"), rows);

        assertEquals(120, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        Long count = customJdbc.queryForObject("SELECT COUNT(*) FROM test_batch_custom", Long.class);
        assertEquals(120L, count);
    }

    // ======================== 实体感知 API 测试 ========================

    /**
     * 测试用实体类，模拟 @Table 注解场景。
     */
    @org.springframework.data.relational.core.mapping.Table("test_batch_user")
    static class TestUserEntity extends com.zerx.spring.data.domain.BaseEntity {
        private String username;
        private String email;
        private Integer status;

        public TestUserEntity() {}
        public TestUserEntity(String username, String email, Integer status) {
            this.username = username;
            this.email = email;
            this.status = status;
        }
    }

    @Test
    void batchInsert_byEntityClass() {
        List<Object[]> rows = List.of(
                new Object[]{"entityUser1", "e1@test.com", 1},
                new Object[]{"entityUser2", "e2@test.com", 0}
        );

        int[] result = batchOperations.batchInsert(TestUserEntity.class,
                List.of("username", "email", "status"), rows);

        assertEquals(2, result.length);
        for (int count : result) {
            assertEquals(1, count);
        }

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_batch_user", Long.class);
        assertEquals(2L, count);
    }

    @Test
    void batchInsertAndReturnKeys_returnsGeneratedIds() {
        List<Object[]> rows = List.of(
                new Object[]{"keyUser1", "k1@test.com", 1},
                new Object[]{"keyUser2", "k2@test.com", 0},
                new Object[]{"keyUser3", "k3@test.com", 1}
        );

        List<Map<String, Object>> keys = batchOperations.batchInsertAndReturnKeys(TestUserEntity.class,
                List.of("username", "email", "status"), rows);

        assertEquals(3, keys.size());
        // H2 AUTO_INCREMENT should return generated "id" key
        for (Map<String, Object> key : keys) {
            assertNotNull(key.get("ID"), "Generated key 'ID' should not be null");
        }

        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_batch_user", Long.class);
        assertEquals(3L, count);
    }

    @Test
    void batchInsertAndReturnKeys_largeBatch() {
        List<Object[]> rows = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            rows.add(new Object[]{"large_" + i, "large" + i + "@test.com", 1});
        }

        List<Map<String, Object>> keys = batchOperations.batchInsertAndReturnKeys(TestUserEntity.class,
                List.of("username", "email", "status"), rows);

        assertEquals(500, keys.size());
        for (Map<String, Object> key : keys) {
            assertNotNull(key.get("ID"), "Generated key should not be null for large batch");
        }
    }

    @Test
    void batchInsert_nullEntityClass_throwsException() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"test", "test@test.com", 1});
        assertThrows(IllegalArgumentException.class, () ->
                batchOperations.batchInsert((Class<?>) null, List.of("username"), rows));
    }

    @Test
    void batchInsertAndReturnKeys_nullEntityClass_throwsException() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"test", "test@test.com", 1});
        assertThrows(IllegalArgumentException.class, () ->
                batchOperations.batchInsertAndReturnKeys((Class<?>) null, List.of("username"), rows));
    }
}
