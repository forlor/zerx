package com.zerx.spring.data;

import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.logicaldelete.LogicalDelete;
import com.zerx.spring.data.logicaldelete.LogicalDeleteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Table;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogicalDeleteService 集成测试。
 * <p>
 * 使用 H2 内存数据库测试逻辑删除、恢复、物理删除、批量操作等功能。
 * </p>
 */
class LogicalDeleteServiceTest {

    private DataSource dataSource;
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private LogicalDeleteService logicalDeleteService;

    @BeforeEach
    void setUp() {
        dataSource = new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:logical_delete_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);

        // 创建测试表（使用默认 @LogicalDelete 配置：deleted 列，0/1 值）
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS logical_delete_test_entity (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    deleted VARCHAR(1) DEFAULT '0',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    create_by BIGINT,
                    update_by BIGINT,
                    version BIGINT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM logical_delete_test_entity");

        // 插入测试数据
        for (int i = 1; i <= 5; i++) {
            jdbcTemplate.update(
                    "INSERT INTO logical_delete_test_entity (id, name, deleted) VALUES (?, ?, ?)",
                    i, "Entity-" + i, "0");
        }

        // 创建自定义配置测试表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS custom_delete_entity (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    is_deleted VARCHAR(1) DEFAULT 'N',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    create_by BIGINT,
                    update_by BIGINT,
                    version BIGINT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM custom_delete_entity");

        for (int i = 1; i <= 3; i++) {
            jdbcTemplate.update(
                    "INSERT INTO custom_delete_entity (id, name, is_deleted) VALUES (?, ?, ?)",
                    i, "Custom-" + i, "N");
        }

        logicalDeleteService = new LogicalDeleteService(dataSource);
    }

    // ======================== deleteById 测试 ========================

    @Test
    void deleteById_marks_record_as_deleted() {
        logicalDeleteService.deleteById(1L, LogicalDeleteTestEntity.class);

        Map<String, Object> record = jdbcTemplate.queryForMap(
                "SELECT * FROM logical_delete_test_entity WHERE id = 1");
        assertEquals("1", record.get("DELETED"));
    }

    @Test
    void deleteById_other_records_unchanged() {
        logicalDeleteService.deleteById(1L, LogicalDeleteTestEntity.class);

        Map<String, Object> record2 = jdbcTemplate.queryForMap(
                "SELECT * FROM logical_delete_test_entity WHERE id = 2");
        assertEquals("0", record2.get("DELETED"));
    }

    @Test
    void deleteById_nonexistent_id_no_error() {
        // 不应抛出异常
        assertDoesNotThrow(() -> logicalDeleteService.deleteById(999L, LogicalDeleteTestEntity.class));
    }

    @Test
    void deleteById_null_id_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> logicalDeleteService.deleteById(null, LogicalDeleteTestEntity.class));
    }

    @Test
    void deleteById_null_class_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> logicalDeleteService.deleteById(1L, null));
    }

    // ======================== batchDeleteByIds 测试 ========================

    @Test
    void batchDeleteByIds_marks_all_records() {
        logicalDeleteService.batchDeleteByIds(List.of(1L, 2L, 3L), LogicalDeleteTestEntity.class);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM logical_delete_test_entity WHERE id IN (1, 2, 3)");
        assertEquals(3, records.size());
        for (Map<String, Object> record : records) {
            assertEquals("1", record.get("DELETED"));
        }
    }

    @Test
    void batchDeleteByIds_empty_collection_no_op() {
        int countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logical_delete_test_entity WHERE deleted = '1'", Integer.class);
        assertEquals(0, countBefore);

        logicalDeleteService.batchDeleteByIds(List.of(), LogicalDeleteTestEntity.class);

        int countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM logical_delete_test_entity WHERE deleted = '1'", Integer.class);
        assertEquals(0, countAfter);
    }

    @Test
    void batchDeleteByIds_null_collection_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> logicalDeleteService.batchDeleteByIds(null, LogicalDeleteTestEntity.class));
    }

    // ======================== restoreById 测试 ========================

    @Test
    void restoreById_restores_deleted_record() {
        // 先删除
        logicalDeleteService.deleteById(1L, LogicalDeleteTestEntity.class);
        Map<String, Object> deleted = jdbcTemplate.queryForMap(
                "SELECT * FROM logical_delete_test_entity WHERE id = 1");
        assertEquals("1", deleted.get("DELETED"));

        // 恢复
        logicalDeleteService.restoreById(1L, LogicalDeleteTestEntity.class);
        Map<String, Object> restored = jdbcTemplate.queryForMap(
                "SELECT * FROM logical_delete_test_entity WHERE id = 1");
        assertEquals("0", restored.get("DELETED"));
    }

    @Test
    void restoreById_nonexistent_id_no_error() {
        assertDoesNotThrow(() -> logicalDeleteService.restoreById(999L, LogicalDeleteTestEntity.class));
    }

    // ======================== hardDeleteById 测试 ========================

    @Test
    void hardDeleteById_physically_deletes_record() {
        logicalDeleteService.hardDeleteById(1L, LogicalDeleteTestEntity.class);

        List<Map<String, Object>> remaining = jdbcTemplate.queryForList(
                "SELECT * FROM logical_delete_test_entity WHERE id = 1");
        assertTrue(remaining.isEmpty());
    }

    @Test
    void hardDeleteById_other_records_unchanged() {
        logicalDeleteService.hardDeleteById(1L, LogicalDeleteTestEntity.class);

        List<Map<String, Object>> remaining = jdbcTemplate.queryForList(
                "SELECT * FROM logical_delete_test_entity");
        assertEquals(4, remaining.size());
    }

    // ======================== isLogicalDelete 测试 ========================

    @Test
    void isLogicalDelete_returns_true_for_annotated_class() {
        assertTrue(logicalDeleteService.isLogicalDelete(LogicalDeleteTestEntity.class));
    }

    @Test
    void isLogicalDelete_returns_false_for_non_annotated_class() {
        assertFalse(logicalDeleteService.isLogicalDelete(NonLogicalDeleteEntity.class));
    }

    @Test
    void isLogicalDelete_returns_false_for_null() {
        assertFalse(logicalDeleteService.isLogicalDelete(null));
    }

    // ======================== getNotDeletedCondition 测试 ========================

    @Test
    void getNotDeletedCondition_default_config() {
        String condition = logicalDeleteService.getNotDeletedCondition(LogicalDeleteTestEntity.class);
        assertEquals("deleted = '0'", condition);
    }

    @Test
    void getNotDeletedCondition_custom_config() {
        String condition = logicalDeleteService.getNotDeletedCondition(CustomDeleteEntity.class);
        assertEquals("is_deleted = 'N'", condition);
    }

    @Test
    void getNotDeletedCondition_non_annotated_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> logicalDeleteService.getNotDeletedCondition(NonLogicalDeleteEntity.class));
    }

    // ======================== appendNotDeletedFilter 测试 ========================

    @Test
    void appendNotDeletedFilter_without_where() {
        String sql = "SELECT * FROM logical_delete_test_entity";
        String result = logicalDeleteService.appendNotDeletedFilter(sql, LogicalDeleteTestEntity.class);
        assertEquals("SELECT * FROM logical_delete_test_entity WHERE deleted = '0'", result);
    }

    @Test
    void appendNotDeletedFilter_with_where() {
        String sql = "SELECT * FROM logical_delete_test_entity WHERE name = ?";
        String result = logicalDeleteService.appendNotDeletedFilter(sql, LogicalDeleteTestEntity.class);
        assertEquals("SELECT * FROM logical_delete_test_entity WHERE name = ? AND deleted = '0'", result);
    }

    @Test
    void appendNotDeletedFilter_non_annotated_returns_original() {
        String sql = "SELECT * FROM non_logical_delete_entity";
        String result = logicalDeleteService.appendNotDeletedFilter(sql, NonLogicalDeleteEntity.class);
        assertEquals(sql, result);
    }

    @Test
    void appendNotDeletedFilter_with_order_by() {
        // 包含 WHERE 和 ORDER BY 的 SQL
        String sql = "SELECT * FROM logical_delete_test_entity WHERE name = ? ORDER BY id DESC";
        String result = logicalDeleteService.appendNotDeletedFilter(sql, LogicalDeleteTestEntity.class);
        assertEquals("SELECT * FROM logical_delete_test_entity WHERE name = ? ORDER BY id DESC AND deleted = '0'", result);
    }

    @Test
    void appendNotDeletedFilter_empty_sql_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> logicalDeleteService.appendNotDeletedFilter("", LogicalDeleteTestEntity.class));
    }

    // ======================== 自定义注解配置测试 ========================

    @Test
    void customAnnotation_deleteById_uses_custom_values() {
        logicalDeleteService.deleteById(1L, CustomDeleteEntity.class);

        Map<String, Object> record = jdbcTemplate.queryForMap(
                "SELECT * FROM custom_delete_entity WHERE id = 1");
        assertEquals("Y", record.get("IS_DELETED"));
    }

    @Test
    void customAnnotation_restoreById_uses_custom_values() {
        // 先逻辑删除
        logicalDeleteService.deleteById(1L, CustomDeleteEntity.class);

        // 恢复
        logicalDeleteService.restoreById(1L, CustomDeleteEntity.class);

        Map<String, Object> record = jdbcTemplate.queryForMap(
                "SELECT * FROM custom_delete_entity WHERE id = 1");
        assertEquals("N", record.get("IS_DELETED"));
    }

    @Test
    void customAnnotation_batchDeleteByIds() {
        logicalDeleteService.batchDeleteByIds(List.of(1L, 2L), CustomDeleteEntity.class);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM custom_delete_entity WHERE id IN (1, 2)");
        assertEquals(2, records.size());
        for (Map<String, Object> record : records) {
            assertEquals("Y", record.get("IS_DELETED"));
        }
    }

    @Test
    void customAnnotation_getNotDeletedCondition() {
        String condition = logicalDeleteService.getNotDeletedCondition(CustomDeleteEntity.class);
        assertEquals("is_deleted = 'N'", condition);
    }

    @Test
    void customAnnotation_appendNotDeletedFilter() {
        String sql = "SELECT * FROM custom_delete_entity";
        String result = logicalDeleteService.appendNotDeletedFilter(sql, CustomDeleteEntity.class);
        assertEquals("SELECT * FROM custom_delete_entity WHERE is_deleted = 'N'", result);
    }

    @Test
    void customAnnotation_hardDeleteById() {
        logicalDeleteService.hardDeleteById(1L, CustomDeleteEntity.class);

        List<Map<String, Object>> remaining = jdbcTemplate.queryForList(
                "SELECT * FROM custom_delete_entity WHERE id = 1");
        assertTrue(remaining.isEmpty());
    }

    // ======================== 非逻辑删除实体操作测试 ========================

    @Test
    void nonLogicalDeleteEntity_deleteById_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> logicalDeleteService.deleteById(1L, NonLogicalDeleteEntity.class));
    }

    // ======================== 测试实体 ========================

    /**
     * 使用默认 @LogicalDelete 配置的测试实体
     */
    @LogicalDelete
    static class LogicalDeleteTestEntity extends BaseEntity {
        private String name;
        private String deleted;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDeleted() { return deleted; }
        public void setDeleted(String deleted) { this.deleted = deleted; }
    }

    /**
     * 使用自定义 @LogicalDelete 配置的测试实体
     */
    @LogicalDelete(column = "is_deleted", deletedValue = "Y", notDeletedValue = "N")
    static class CustomDeleteEntity extends BaseEntity {
        private String name;
        private String isDeleted;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIsDeleted() { return isDeleted; }
        public void setIsDeleted(String isDeleted) { this.isDeleted = isDeleted; }
    }

    /**
     * 未标注 @LogicalDelete 的测试实体
     */
    static class NonLogicalDeleteEntity extends BaseEntity {
    }
}
