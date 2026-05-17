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

        assertTrue(props.getSlowSql().isEnabled());
        assertEquals(Duration.ofMillis(1000), props.getSlowSql().getThreshold());
        assertTrue(props.getSlowSql().isLogParams());

        assertTrue(props.isSingleQueryLoading());
        assertEquals(ZerxDataProperties.NamingStrategy.SNAKE_CASE, props.getNamingStrategy());
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

    @Test
    void namingStrategy_values() {
        assertEquals(2, ZerxDataProperties.NamingStrategy.values().length);
        assertNotNull(ZerxDataProperties.NamingStrategy.valueOf("SNAKE_CASE"));
        assertNotNull(ZerxDataProperties.NamingStrategy.valueOf("CAMEL_CASE"));
    }
}
