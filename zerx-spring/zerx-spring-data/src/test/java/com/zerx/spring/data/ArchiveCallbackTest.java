package com.zerx.spring.data;

import com.zerx.spring.data.archive.ArchiveCallback;
import com.zerx.spring.data.archive.ArchiveException;
import com.zerx.spring.data.archive.ArchiveProperties;
import com.zerx.spring.data.archive.Archiver;
import com.zerx.spring.data.domain.BaseEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.relational.core.conversion.MutableAggregateChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArchiveCallback 单元测试
 */
class ArchiveCallbackTest {

    private ArchiveProperties properties;
    private List<Archiver<?>> archivers;

    /**
     * 测试用实体
     */
    static class TestEntity extends BaseEntity {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @BeforeEach
    void setUp() {
        properties = new ArchiveProperties();
        archivers = new ArrayList<>();
    }

    @Test
    void archiveDisabled_noArchiveAttempt() {
        properties.setEnabled(false);
        var mockArchiver = new MockArchiver();
        archivers.add(mockArchiver);
        var callback = new ArchiveCallback(properties, archivers);

        var entity = new TestEntity();
        entity.setId(1L);

        callback.onBeforeDelete(entity, null);

        assertFalse(mockArchiver.archiveCalled);
    }

    @Test
    void noArchiverRegistered_noArchiveAttempt() {
        properties.setEnabled(true);
        // Don't register any archiver
        var callback = new ArchiveCallback(properties, archivers);

        var entity = new TestEntity();
        entity.setId(1L);

        callback.onBeforeDelete(entity, null);

        // No archiver registered, should be skipped
        assertEquals(0, archivers.size());
    }

    @Test
    void archiverRegistered_archiveTriggered() {
        properties.setEnabled(true);
        var mockArchiver = new MockArchiver();
        archivers.add(mockArchiver);
        var callback = new ArchiveCallback(properties, archivers);

        var entity = new TestEntity();
        entity.setId(1L);

        callback.onBeforeDelete(entity, null);

        assertTrue(mockArchiver.archiveCalled);
        assertEquals(1L, mockArchiver.archivedEntity.getId());
    }

    @Test
    void nullEntity_skipped() {
        properties.setEnabled(true);
        var mockArchiver = new MockArchiver();
        archivers.add(mockArchiver);
        var callback = new ArchiveCallback(properties, archivers);

        var result = callback.onBeforeDelete(null, null);

        assertFalse(mockArchiver.archiveCalled);
        assertNull(result);
    }

    @Test
    void entityWithNullId_skipped() {
        properties.setEnabled(true);
        var mockArchiver = new MockArchiver();
        archivers.add(mockArchiver);
        var callback = new ArchiveCallback(properties, archivers);

        var entity = new TestEntity();
        // id is null

        callback.onBeforeDelete(entity, null);

        assertFalse(mockArchiver.archiveCalled);
    }

    @Test
    void noArchiversRegistered_noError() {
        properties.setEnabled(true);
        var callback = new ArchiveCallback(properties, archivers);

        var entity = new TestEntity();
        entity.setId(1L);

        // Should not throw
        assertDoesNotThrow(() -> callback.onBeforeDelete(entity, null));
    }

    /**
     * Mock Archiver for testing
     */
    static class MockArchiver implements Archiver<BaseEntity> {
        boolean archiveCalled = false;
        BaseEntity archivedEntity;
        boolean restoreCalled = false;

        @Override
        public void archive(BaseEntity entity) {
            archiveCalled = true;
            archivedEntity = entity;
        }

        @Override
        public Optional<BaseEntity> restore(Object id) {
            restoreCalled = true;
            return Optional.empty();
        }

        @Override
        public boolean supports(Class<?> entityClass) {
            return BaseEntity.class.isAssignableFrom(entityClass);
        }
    }
}
