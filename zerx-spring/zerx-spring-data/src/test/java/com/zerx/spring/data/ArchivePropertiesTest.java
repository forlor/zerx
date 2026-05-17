package com.zerx.spring.data;

import com.zerx.spring.data.archive.ArchiveProperties;
import com.zerx.spring.data.domain.BaseEntity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArchiveProperties 单元测试
 */
class ArchivePropertiesTest {

    @Test
    void defaultValues() {
        var props = new ArchiveProperties();

        assertFalse(props.isEnabled());
        assertEquals("_archive", props.getTableSuffix());
        assertTrue(props.getEntities().isEmpty());
        assertEquals(90, props.getRetainDays());
        assertEquals(java.time.Duration.ofSeconds(10), props.getTimeout());
    }

    @Test
    void customValues() {
        var props = new ArchiveProperties();
        props.setEnabled(true);
        props.setTableSuffix("_history");
        Set<String> entities = new HashSet<>();
        entities.add("com.example.User");
        entities.add("com.example.Order");
        props.setEntities(entities);
        props.setRetainDays(180);
        props.setTimeout(java.time.Duration.ofSeconds(30));

        assertTrue(props.isEnabled());
        assertEquals("_history", props.getTableSuffix());
        assertEquals(2, props.getEntities().size());
        assertTrue(props.getEntities().contains("com.example.User"));
        assertEquals(180, props.getRetainDays());
        assertEquals(java.time.Duration.ofSeconds(30), props.getTimeout());
    }

    @Test
    void isArchiveEnabled_matchingEntity() {
        var props = new ArchiveProperties();
        props.setEnabled(true);
        props.getEntities().add(TestEntity.class.getName());

        assertTrue(props.isArchiveEnabled(TestEntity.class));
    }

    @Test
    void isArchiveEnabled_noMatchEntity() {
        var props = new ArchiveProperties();
        props.setEnabled(true);
        props.getEntities().add("com.example.User");

        assertFalse(props.isArchiveEnabled(TestEntity.class));
    }

    @Test
    void isArchiveEnabled_disabled() {
        var props = new ArchiveProperties();
        props.setEnabled(false);
        props.getEntities().add(TestEntity.class.getName());

        assertFalse(props.isArchiveEnabled(TestEntity.class));
    }

    /**
     * 测试用实体
     */
    static class TestEntity extends BaseEntity {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
