package com.zerx.spring.data;

import com.zerx.common.model.PageRequest;
import com.zerx.common.model.PageResult;
import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.repository.ZerxRepositoryHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.mapping.Table;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZerxRepositoryHelper 测试。
 */
class ZerxRepositoryHelperTest {

    private DataSource dataSource;
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private ZerxRepositoryHelper helper;

    @BeforeEach
    void setUp() {
        dataSource = new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:helper_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        jdbcTemplate = new org.springframework.jdbc.core.JdbcTemplate(dataSource);

        // 创建测试表
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS test_product (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    price DECIMAL(10,2),
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    create_by BIGINT,
                    update_by BIGINT,
                    version BIGINT DEFAULT 0
                )
                """);
        jdbcTemplate.execute("DELETE FROM test_product");

        // 插入测试数据
        for (int i = 1; i <= 10; i++) {
            jdbcTemplate.update(
                    "INSERT INTO test_product (id, name, price, status) VALUES (?, ?, ?, ?)",
                    i, "Product-" + i, 100.0 * i, i <= 7 ? "ACTIVE" : "INACTIVE");
        }

        helper = new ZerxRepositoryHelper(dataSource);
    }

    // ======================== countAll 测试 ========================

    @Test
    void countAll_returns_correct_count() {
        long count = helper.countAll(TestProduct.class);
        assertEquals(10, count);
    }

    // ======================== existsByIds 测试 ========================

    @Test
    void existsByIds_all_exist() {
        assertTrue(helper.existsByIds(TestProduct.class, List.of(1L, 2L, 3L)));
    }

    @Test
    void existsByIds_partial_exist() {
        assertFalse(helper.existsByIds(TestProduct.class, List.of(1L, 2L, 999L)));
    }

    @Test
    void existsByIds_none_exist() {
        assertFalse(helper.existsByIds(TestProduct.class, List.of(100L, 200L)));
    }

    @Test
    void existsByIds_empty_collection() {
        assertFalse(helper.existsByIds(TestProduct.class, List.of()));
    }

    @Test
    void existsByIds_null_safe() {
        assertFalse(helper.existsByIds(TestProduct.class, null));
    }

    @Test
    void existsByIds_non_collection_iterable() {
        Iterable<Long> ids = () -> List.of(1L, 2L, 3L).iterator();
        assertTrue(helper.existsByIds(TestProduct.class, ids));
    }

    @Test
    void existsByIds_with_duplicates() {
        assertTrue(helper.existsByIds(TestProduct.class, List.of(1L, 2L, 2L, 3L, 3L)));
    }

    // ======================== findPage 测试 ========================

    @Test
    void findPage_first_page() {
        PageRequest pageReq = new PageRequest(1, 3);
        PageResult<Map<String, Object>> result = helper.findPage(TestProduct.class, pageReq);

        assertEquals(10, result.total());
        assertEquals(1, result.page());
        assertEquals(3, result.size());
        assertEquals(3, result.records().size());
    }

    @Test
    void findPage_last_page() {
        PageRequest pageReq = new PageRequest(4, 3);
        PageResult<Map<String, Object>> result = helper.findPage(TestProduct.class, pageReq);

        assertEquals(10, result.total());
        assertEquals(1, result.records().size());
    }

    @Test
    void findPage_beyond_total() {
        PageRequest pageReq = new PageRequest(100, 3);
        PageResult<Map<String, Object>> result = helper.findPage(TestProduct.class, pageReq);

        assertEquals(10, result.total());
        assertTrue(result.isEmpty());
    }

    @Test
    void findPage_desc_order() {
        PageRequest pageReq = new PageRequest(1, 3);
        PageResult<Map<String, Object>> result = helper.findPage(TestProduct.class, pageReq);

        // ID 倒序，第一页应包含最大的 ID：10, 9, 8
        List<Map<String, Object>> records = result.records();
        assertEquals(3, records.size());
        assertEquals(10L, records.get(0).get("ID"));
        assertEquals(9L, records.get(1).get("ID"));
        assertEquals(8L, records.get(2).get("ID"));
    }

    // ======================== 表名解析测试 ========================

    @Test
    void resolve_table_name_from_annotation() {
        // @Table("test_product") 注解指定表名
        long count = helper.countAll(TestProduct.class);
        assertEquals(10, count);
    }

    @Test
    void resolve_table_name_default_snake_case() {
        // NoAnnotationEntity 没有 @Table 注解，使用类名 snake_case → no_annotation_entity
        // 表不存在会抛异常，用 try-catch 验证表名转换正确
        assertThrows(org.springframework.jdbc.BadSqlGrammarException.class,
                () -> helper.countAll(NoAnnotationEntity.class));
    }

    // ======================== 测试实体 ========================

    /**
     * 测试用实体类，对应 test_product 表
     */
    @Table("test_product")
    static class TestProduct extends BaseEntity {
        private String name;
        private Double price;
        private String status;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    /**
     * 无 @Table 注解的实体，测试默认 snake_case 表名转换
     */
    static class NoAnnotationEntity extends BaseEntity {
    }
}
