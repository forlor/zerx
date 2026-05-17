package com.zerx.spring.data;

import com.zerx.spring.data.repository.ZerxRepositoryImpl;
import com.zerx.spring.data.properties.ZerxDataProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZerxRepositoryImpl 单元测试
 */
class ZerxRepositoryImplTest {

    private JdbcTemplate jdbcTemplate;
    private ZerxRepositoryImpl<Object, Long> repositoryImpl;

    private final RowMapper<Map<String, Object>> mapRowMapper = (rs, rowNum) -> {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("id", rs.getLong("id"));
        map.put("username", rs.getString("username"));
        map.put("status", rs.getString("status"));
        map.put("deleted", rs.getInt("deleted"));
        return map;
    };

    @BeforeEach
    void setUp() {
        var dataSource = new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:repo_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    deleted INT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM sys_user");
        jdbcTemplate.update("INSERT INTO sys_user (id, username, status, deleted) VALUES (1, 'alice', 'ACTIVE', 0)");
        jdbcTemplate.update("INSERT INTO sys_user (id, username, status, deleted) VALUES (2, 'bob', 'ACTIVE', 0)");
        jdbcTemplate.update("INSERT INTO sys_user (id, username, status, deleted) VALUES (3, 'deleted_user', 'DISABLED', 1)");

        var props = new ZerxDataProperties();
        repositoryImpl = new ZerxRepositoryImpl<>(jdbcTemplate, props);
    }

    @Test
    void findById_found() {
        Optional<Map<String, Object>> result = repositoryImpl.findByIdAndDeletedFalse(
                "sys_user", 1L, mapRowMapper, "id");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().get("username"));
    }

    @Test
    void findById_deleted() {
        Optional<Map<String, Object>> result = repositoryImpl.findByIdAndDeletedFalse(
                "sys_user", 3L, mapRowMapper, "id");

        assertFalse(result.isPresent());
    }

    @Test
    void findById_notExists() {
        Optional<Map<String, Object>> result = repositoryImpl.findByIdAndDeletedFalse(
                "sys_user", 999L, mapRowMapper, "id");

        assertFalse(result.isPresent());
    }

    @Test
    void findAllByDeletedFalse() {
        List<Map<String, Object>> result = repositoryImpl.findAllByDeletedFalse("sys_user", mapRowMapper);

        assertEquals(2, result.size());
        result.forEach(r -> assertEquals(0, r.get("deleted")));
    }

    @Test
    void countByDeletedFalse() {
        long count = repositoryImpl.countByDeletedFalse("sys_user");

        assertEquals(2, count);
    }

    @Test
    void existsByIdAndDeletedFalse_true() {
        assertTrue(repositoryImpl.existsByIdAndDeletedFalse("sys_user", 1L, "id"));
    }

    @Test
    void existsByIdAndDeletedFalse_deleted() {
        assertFalse(repositoryImpl.existsByIdAndDeletedFalse("sys_user", 3L, "id"));
    }

    @Test
    void existsByIdAndDeletedFalse_notExists() {
        assertFalse(repositoryImpl.existsByIdAndDeletedFalse("sys_user", 999L, "id"));
    }
}
