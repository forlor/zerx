package com.zerx.spring.data;

import com.zerx.spring.data.datascope.DataScope;
import com.zerx.spring.data.datascope.DataScopeHandler;
import com.zerx.spring.data.datascope.DataScopeUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataScopeHandler 单元测试
 */
class DataScopeHandlerTest {

    private DataScopeHandler handler;

    @BeforeEach
    void setUp() {
        var dataSource = new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:datascope_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        var jdbcTemplate = new JdbcTemplate(dataSource);
        handler = new DataScopeHandler(jdbcTemplate);
    }

    @Test
    void selfScope_generatesCorrectCondition() {
        var dataScope = createMockDataScope("create_by", DataScope.Type.SELF);
        var user = new DataScopeUser(100L, List.of(1L), List.of("admin"));

        var result = handler.generateCondition(dataScope, user);

        assertNotNull(result);
        assertEquals("create_by = ?", result.condition());
        assertEquals(List.of(100L), result.params());
    }

    @Test
    void deptScope_generatesCorrectCondition() {
        var dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
        var user = new DataScopeUser(100L, List.of(1L, 2L), List.of("admin"));

        var result = handler.generateCondition(dataScope, user);

        assertNotNull(result);
        assertEquals("dept_id IN (?,?)", result.condition());
        assertEquals(List.of(1L, 2L), result.params());
    }

    @Test
    void allScope_returnsNull() {
        var dataScope = createMockDataScope("dept_id", DataScope.Type.ALL);
        var user = new DataScopeUser(100L, List.of(1L), List.of("admin"));

        var result = handler.generateCondition(dataScope, user);

        assertNull(result);
    }

    @Test
    void nullUser_returnsNull() {
        var dataScope = createMockDataScope("create_by", DataScope.Type.SELF);

        var result = handler.generateCondition(dataScope, null);

        assertNull(result);
    }

    @Test
    void nullDataScope_returnsNull() {
        var user = new DataScopeUser(100L, List.of(1L), List.of("admin"));

        var result = handler.generateCondition(null, user);

        assertNull(result);
    }

    @Test
    void deptScope_noDeptIds_returnsFalseCondition() {
        var dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
        var user = new DataScopeUser(100L, List.of(), List.of("admin"));

        var result = handler.generateCondition(dataScope, user);

        assertNotNull(result);
        assertEquals("1 = 0", result.condition());
        assertTrue(result.params().isEmpty());
    }

    @Test
    void deptScope_nullDeptIds_returnsFalseCondition() {
        var dataScope = createMockDataScope("dept_id", DataScope.Type.DEPT);
        var user = new DataScopeUser(100L, null, List.of("admin"));

        var result = handler.generateCondition(dataScope, user);

        assertNotNull(result);
        assertEquals("1 = 0", result.condition());
    }

    /**
     * 创建模拟的 DataScope 注解
     */
    private DataScope createMockDataScope(String column, DataScope.Type type) {
        return new DataScope() {
            @Override
            public String column() {
                return column;
            }

            @Override
            public Type type() {
                return type;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return DataScope.class;
            }
        };
    }
}
