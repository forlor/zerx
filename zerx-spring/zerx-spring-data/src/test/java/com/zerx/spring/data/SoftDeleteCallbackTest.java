package com.zerx.spring.data;

import com.zerx.spring.data.config.SoftDeleteCallback;
import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.properties.ZerxDataProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SoftDeleteCallback 单元测试
 */
class SoftDeleteCallbackTest {

    private SoftDeleteCallback callback;

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
        ZerxDataProperties properties = new ZerxDataProperties();
        callback = new SoftDeleteCallback(properties);
    }

    @Test
    void newEntity_withNullDeleted_setsFalse() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getId());
        // Explicitly set to null to test the callback's default-setting behavior
        entity.setDeleted(null);

        BaseEntity result = callback.onBeforeConvert(entity);

        assertFalse(result.isDeleted());
        assertEquals(false, result.getDeleted());
    }

    @Test
    void newEntity_withDeletedFalse_keepsFalse() {
        TestEntity entity = new TestEntity();
        entity.setDeleted(false);

        BaseEntity result = callback.onBeforeConvert(entity);

        assertFalse(result.isDeleted());
    }

    @Test
    void newEntity_defaultDeletedState_unchanged() {
        // BaseEntity initializes deleted = false by default
        TestEntity entity = new TestEntity();
        assertEquals(false, entity.getDeleted());

        BaseEntity result = callback.onBeforeConvert(entity);

        assertFalse(result.isDeleted());
        assertEquals(false, result.getDeleted());
    }

    @Test
    void existingEntity_withNullDeleted_notModified() {
        TestEntity entity = new TestEntity();
        entity.setId(42L);
        entity.setDeleted(null);

        BaseEntity result = callback.onBeforeConvert(entity);

        assertNull(result.getDeleted());
    }

    @Test
    void newEntity_withDeletedTrue_keepsTrue() {
        TestEntity entity = new TestEntity();
        entity.setDeleted(true);

        BaseEntity result = callback.onBeforeConvert(entity);

        assertTrue(result.isDeleted());
    }

    @Test
    void logicDeleteDisabled_withNullDeleted_doesNotSetDefault() {
        ZerxDataProperties properties = new ZerxDataProperties();
        properties.getLogicDelete().setEnabled(false);
        SoftDeleteCallback disabledCallback = new SoftDeleteCallback(properties);

        TestEntity entity = new TestEntity();
        entity.setDeleted(null);

        BaseEntity result = disabledCallback.onBeforeConvert(entity);

        assertNull(result.getDeleted());
    }

    @Test
    void logicDeleteDisabled_keepsExistingState() {
        ZerxDataProperties properties = new ZerxDataProperties();
        properties.getLogicDelete().setEnabled(false);
        SoftDeleteCallback disabledCallback = new SoftDeleteCallback(properties);

        TestEntity entity = new TestEntity();
        // default is false, should remain false when logic delete disabled
        assertEquals(false, entity.getDeleted());

        BaseEntity result = disabledCallback.onBeforeConvert(entity);

        assertFalse(result.isDeleted());
    }

    @Test
    void markDeletedBeforeCallback_preservesState() {
        TestEntity entity = new TestEntity();
        entity.markDeleted();

        BaseEntity result = callback.onBeforeConvert(entity);

        assertTrue(result.isDeleted());
        assertEquals(true, result.getDeleted());
    }
}
