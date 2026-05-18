package com.zerx.spring.data;

import com.zerx.spring.data.config.SlowSqlInterceptor;
import com.zerx.spring.data.properties.ZerxDataProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SlowSqlInterceptor 单元测试
 */
class SlowSqlInterceptorTest {

    private SlowSqlInterceptor interceptor;
    private ZerxDataProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ZerxDataProperties();
        interceptor = new SlowSqlInterceptor(properties);
    }

    @Test
    void isBeanPostProcessor() {
        assertInstanceOf(BeanPostProcessor.class, interceptor);
    }

    @Test
    void postProcess_nonJdbcTemplate_returnsSame() {
        Object bean = new Object();
        Object result = interceptor.postProcessAfterInitialization(bean, "nonJdbcTemplate");
        assertSame(bean, result);
    }

    @Test
    void postProcess_jdbcTemplate_returnsProxy() {
        var dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:interceptor_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Object result = interceptor.postProcessAfterInitialization(jdbcTemplate, "jdbcTemplate");

        assertNotSame(jdbcTemplate, result, "Should return a proxy");
    }

    @Test
    void postProcess_jdbcTemplate_slowSqlDisabled_returnsSame() {
        properties.getSlowSql().setEnabled(false);
        SlowSqlInterceptor disabledInterceptor = new SlowSqlInterceptor(properties);

        var dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:interceptor_test2;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Object result = disabledInterceptor.postProcessAfterInitialization(jdbcTemplate, "jdbcTemplate");

        assertSame(jdbcTemplate, result, "Should NOT proxy when disabled");
    }

    @Test
    void proxyExecutesQuery() {
        var dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:proxy_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("CREATE TABLE proxy_test (id INT PRIMARY KEY, name VARCHAR(50))");

        Object proxy = interceptor.postProcessAfterInitialization(jdbcTemplate, "jdbcTemplate");

        assertInstanceOf(JdbcOperations.class, proxy, "Proxy should implement JdbcOperations");

        var proxyJdbc = (org.springframework.jdbc.core.JdbcOperations) proxy;
        proxyJdbc.update("INSERT INTO proxy_test (id, name) VALUES (1, 'test')");

        Integer count = proxyJdbc.queryForObject("SELECT COUNT(*) FROM proxy_test", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void addSensitiveParam() {
        interceptor.addSensitiveParam("custom_secret");
        // No exception means it was added successfully
    }

    @Test
    void addSensitiveParam_null_ignored() {
        interceptor.addSensitiveParam(null);
        // No exception means null was safely ignored
    }
}
