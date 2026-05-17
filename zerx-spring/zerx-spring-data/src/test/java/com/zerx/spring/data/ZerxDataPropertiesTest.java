package com.zerx.spring.data;

import com.zerx.spring.data.properties.ZerxDataProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZerxDataProperties 单元测试
 */
class ZerxDataPropertiesTest {

    @Test
    void defaultValues() {
        var props = new ZerxDataProperties();

        assertTrue(props.getLogicDelete().isEnabled());
        assertEquals("deleted", props.getLogicDelete().getField());
        assertEquals(1, props.getLogicDelete().getDeletedValue());
        assertEquals(0, props.getLogicDelete().getNotDeletedValue());

        assertTrue(props.getSlowSql().isEnabled());
        assertEquals(Duration.ofMillis(1000), props.getSlowSql().getThreshold());
        assertTrue(props.getSlowSql().isLogParams());

        assertTrue(props.isSingleQueryLoading());
        assertEquals(ZerxDataProperties.NamingStrategy.SNAKE_CASE, props.getNamingStrategy());
    }

    @Test
    void customLogicDelete() {
        var props = new ZerxDataProperties();
        props.getLogicDelete().setEnabled(false);
        props.getLogicDelete().setField("is_deleted");
        props.getLogicDelete().setDeletedValue(2);
        props.getLogicDelete().setNotDeletedValue(-1);

        assertFalse(props.getLogicDelete().isEnabled());
        assertEquals("is_deleted", props.getLogicDelete().getField());
        assertEquals(2, props.getLogicDelete().getDeletedValue());
        assertEquals(-1, props.getLogicDelete().getNotDeletedValue());
    }

    @Test
    void customSlowSql() {
        var props = new ZerxDataProperties();
        props.getSlowSql().setEnabled(false);
        props.getSlowSql().setThreshold(Duration.ofMillis(500));
        props.getSlowSql().setLogParams(false);

        assertFalse(props.getSlowSql().isEnabled());
        assertEquals(Duration.ofMillis(500), props.getSlowSql().getThreshold());
        assertFalse(props.getSlowSql().isLogParams());
    }

    @Test
    void customNamingStrategy() {
        var props = new ZerxDataProperties();
        props.setNamingStrategy(ZerxDataProperties.NamingStrategy.CAMEL_CASE);

        assertEquals(ZerxDataProperties.NamingStrategy.CAMEL_CASE, props.getNamingStrategy());
    }

    @Test
    void singleQueryLoadingToggle() {
        var props = new ZerxDataProperties();
        assertTrue(props.isSingleQueryLoading());

        props.setSingleQueryLoading(false);
        assertFalse(props.isSingleQueryLoading());
    }
}
