package com.zerx.spring.data;

import com.zerx.spring.data.archive.ArchiveException;
import com.zerx.spring.data.archive.ArchiveProperties;
import com.zerx.spring.data.archive.JdbcArchiveRepository;
import com.zerx.spring.data.domain.BaseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JdbcArchiveRepository 集成测试。
 * <p>
 * 使用 H2 内存数据库验证归档操作。
 * </p>
 */
class JdbcArchiveRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private ArchiveProperties properties;

    /**
     * 测试用实体
     */
    static class TestUser extends BaseEntity {
        private String username;
        private String email;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @BeforeEach
    void setUp() {
        var dataSource = new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:archive_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        jdbcTemplate = new JdbcTemplate(dataSource);

        // 创建主表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_user (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    create_time TIMESTAMP,
                    update_time TIMESTAMP,
                    create_by BIGINT,
                    update_by BIGINT,
                    version BIGINT
                )
                """);

        // 创建归档表（结构与主表一致 + 额外归档字段）
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_user_archive (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    create_time TIMESTAMP,
                    update_time TIMESTAMP,
                    create_by BIGINT,
                    update_by BIGINT,
                    version BIGINT,
                    archived_at TIMESTAMP,
                    archived_by BIGINT
                )
                """);

        jdbcTemplate.execute("DELETE FROM test_user");
        jdbcTemplate.execute("DELETE FROM test_user_archive");

        // 插入测试数据
        jdbcTemplate.update("INSERT INTO test_user (id, username, email) VALUES (1, 'alice', 'alice@test.com')");
        jdbcTemplate.update("INSERT INTO test_user (id, username, email) VALUES (2, 'bob', 'bob@test.com')");

        properties = new ArchiveProperties();
        properties.setEnabled(true);
        properties.setTableSuffix("_archive");
    }

    @Test
    void archive_success() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        var entity = new TestUser();
        entity.setId(1L);
        entity.setUsername("alice");

        archiver.archive(entity);

        // 验证归档表中有数据
        Optional<Map<String, Object>> archived = archiver.findArchived(1L);
        assertTrue(archived.isPresent());
        assertEquals("alice", archived.get().get("username"));
    }

    @Test
    void archive_idempotent() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        var entity = new TestUser();
        entity.setId(1L);

        // 归档两次，第二次应该跳过
        archiver.archive(entity);
        archiver.archive(entity);

        long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_user_archive WHERE id = 1", Long.class);
        assertEquals(1, count);
    }

    @Test
    void archive_nonExistent_throwsException() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        var entity = new TestUser();
        entity.setId(999L);

        assertThrows(ArchiveException.class, () -> archiver.archive(entity));
    }

    @Test
    void archive_nullEntity_throwsException() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        assertThrows(IllegalArgumentException.class, () -> archiver.archive(null));
    }

    @Test
    void archive_nullId_throwsException() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        var entity = new TestUser();
        // id is null

        assertThrows(IllegalArgumentException.class, () -> archiver.archive(entity));
    }

    @Test
    void findArchived_notFound() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        Optional<Map<String, Object>> result = archiver.findArchived(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void listArchived() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        // 归档两条记录
        var e1 = new TestUser(); e1.setId(1L);
        var e2 = new TestUser(); e2.setId(2L);
        archiver.archive(e1);
        archiver.archive(e2);

        java.util.List<Map<String, Object>> list = archiver.listArchived(10, 0);
        assertEquals(2, list.size());
    }

    @Test
    void supports_sameClass() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        assertTrue(archiver.supports(TestUser.class));
    }

    @Test
    void supports_subclass() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, BaseEntity.class);

        assertTrue(archiver.supports(TestUser.class));
    }

    @Test
    void supports_differentClass() {
        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        assertFalse(archiver.supports(String.class));
    }

    @Test
    void customTableSuffix() {
        properties.setTableSuffix("_history");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_user_history (
                    id BIGINT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    email VARCHAR(100),
                    create_time TIMESTAMP,
                    update_time TIMESTAMP,
                    create_by BIGINT,
                    update_by BIGINT,
                    version BIGINT,
                    archived_at TIMESTAMP,
                    archived_by BIGINT
                )
                """);

        var archiver = new JdbcArchiveRepository<>(jdbcTemplate, properties, TestUser.class);

        var entity = new TestUser();
        entity.setId(1L);
        archiver.archive(entity);

        long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_user_history WHERE id = 1", Long.class);
        assertEquals(1, count);
    }
}
