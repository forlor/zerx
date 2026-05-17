package com.zerx.spring.data;

import com.zerx.spring.data.domain.BaseEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BaseEntity 单元测试
 */
class BaseEntityTest {

    /**
     * 测试用实体
     */
    static class TestEntity extends BaseEntity {
        private String name;
        private String email;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    @Test
    void defaultValues() {
        var entity = new TestEntity();

        assertNull(entity.getId());
        assertNull(entity.getCreateTime());
        assertNull(entity.getUpdateTime());
        assertNull(entity.getCreateBy());
        assertNull(entity.getUpdateBy());
        assertFalse(entity.isDeleted());
        assertEquals(false, entity.getDeleted());
    }

    @Test
    void settersAndGetters() {
        var entity = new TestEntity();
        var now = LocalDateTime.now();

        entity.setId(1L);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        entity.setCreateBy(100L);
        entity.setUpdateBy(200L);
        entity.setName("test");
        entity.setEmail("test@example.com");

        assertEquals(1L, entity.getId());
        assertEquals(now, entity.getCreateTime());
        assertEquals(now, entity.getUpdateTime());
        assertEquals(100L, entity.getCreateBy());
        assertEquals(200L, entity.getUpdateBy());
        assertEquals("test", entity.getName());
        assertEquals("test@example.com", entity.getEmail());
    }

    @Test
    void markDeleted() {
        var entity = new TestEntity();
        assertFalse(entity.isDeleted());

        entity.markDeleted();
        assertTrue(entity.isDeleted());
        assertTrue(entity.getDeleted());
    }

    @Test
    void isDeleted_handlesNull() {
        var entity = new TestEntity();
        entity.setDeleted(null);

        assertFalse(entity.isDeleted()); // Boolean.TRUE.equals(null) = false
    }
}
