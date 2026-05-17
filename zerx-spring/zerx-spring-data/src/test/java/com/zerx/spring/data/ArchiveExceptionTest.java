package com.zerx.spring.data;

import com.zerx.spring.data.archive.ArchiveException;
import com.zerx.spring.data.domain.BaseEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArchiveException 单元测试
 */
class ArchiveExceptionTest {

    static class TestEntity extends BaseEntity {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @Test
    void constructor_withMessage() {
        var ex = new ArchiveException(TestEntity.class, 1L, "test message");

        assertEquals(TestEntity.class, ex.getEntityClass());
        assertEquals(1L, ex.getEntityId());
        assertTrue(ex.getMessage().contains("Archive failed"));
        assertTrue(ex.getMessage().contains("TestEntity"));
        assertTrue(ex.getMessage().contains("test message"));
    }

    @Test
    void constructor_withCause() {
        var cause = new RuntimeException("DB error");
        var ex = new ArchiveException(TestEntity.class, 2L, "db failure", cause);

        assertEquals(TestEntity.class, ex.getEntityClass());
        assertEquals(2L, ex.getEntityId());
        assertSame(cause, ex.getCause());
    }

    @Test
    void isRuntimeException() {
        var ex = new ArchiveException(TestEntity.class, 1L, "test");
        assertInstanceOf(RuntimeException.class, ex);
    }
}
