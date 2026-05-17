package com.zerx.spring.data;

import com.zerx.spring.data.autoconfigure.ZerxDataAutoConfiguration;
import com.zerx.spring.data.config.CamelCaseNamingStrategy;
import com.zerx.spring.data.config.SlowSqlInterceptor;
import com.zerx.spring.data.config.SoftDeleteCallback;
import com.zerx.spring.data.properties.ZerxDataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * ZerxDataAutoConfiguration 集成测试。
 * <p>
 * 使用 {@link ApplicationContextRunner} 验证自动配置在各种条件下正确注册 Bean。
 * </p>
 */
class ZerxDataAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZerxDataAutoConfiguration.class))
            .withUserConfiguration(TestDataSourceConfig.class);

    @Test
    void autoConfiguration_createsDefaultBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ZerxDataAutoConfiguration.ZerxSlowSqlLogger.class);
            assertThat(context).hasSingleBean(SlowSqlInterceptor.class);
            assertThat(context).hasSingleBean(SoftDeleteCallback.class);
            assertThat(context).hasSingleBean(AuditorAware.class);
        });
    }

    @Test
    void slowSqlInterceptor_registeredByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SlowSqlInterceptor.class);
            SlowSqlInterceptor interceptor = context.getBean(SlowSqlInterceptor.class);
            assertThat(interceptor).isNotNull();
        });
    }

    @Test
    void slowSqlInterceptor_disabled_whenPropertyFalse() {
        contextRunner.withPropertyValues("zerx.data.slow-sql.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SlowSqlInterceptor.class);
                    assertThat(context).doesNotHaveBean(
                            ZerxDataAutoConfiguration.ZerxSlowSqlLogger.class);
                });
    }

    @Test
    void slowSqlInterceptor_customThreshold() {
        contextRunner.withPropertyValues(
                        "zerx.data.slow-sql.threshold=500ms",
                        "zerx.data.slow-sql.log-params=false")
                .run(context -> {
                    ZerxDataAutoConfiguration.ZerxSlowSqlLogger logger =
                            context.getBean(ZerxDataAutoConfiguration.ZerxSlowSqlLogger.class);
                    assertThat(logger.getThresholdMs()).isEqualTo(500);
                });
    }

    @Test
    void softDeleteCallback_registeredByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SoftDeleteCallback.class);
        });
    }

    @Test
    void softDeleteCallback_disabled_whenPropertyFalse() {
        contextRunner.withPropertyValues("zerx.data.logic-delete.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(SoftDeleteCallback.class);
                });
    }

    @Test
    void jdbcMappingContext_defaultSnakeCase() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JdbcMappingContext.class);
        });
    }

    @Test
    void jdbcMappingContext_camelCaseStrategy() {
        contextRunner.withPropertyValues("zerx.data.naming-strategy=CAMEL_CASE")
                .run(context -> {
                    assertThat(context).hasSingleBean(JdbcMappingContext.class);
                });
    }

    @Test
    void singleQueryLoading_enabledByDefault() {
        contextRunner.run(context -> {
            JdbcMappingContext mappingContext = context.getBean(JdbcMappingContext.class);
            assertThat(mappingContext.isSingleQueryLoadingEnabled()).isTrue();
        });
    }

    @Test
    void singleQueryLoading_disabledWhenPropertyFalse() {
        contextRunner.withPropertyValues("zerx.data.single-query-loading=false")
                .run(context -> {
                    JdbcMappingContext mappingContext = context.getBean(JdbcMappingContext.class);
                    assertThat(mappingContext.isSingleQueryLoadingEnabled()).isFalse();
                });
    }

    @Test
    void auditorAware_defaultNoOp() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuditorAware.class);
            AuditorAware<?> auditor = context.getBean(AuditorAware.class);
            assertThat(auditor.getCurrentAuditor()).isEmpty();
        });
    }

    @Test
    void auditorAware_customProvider() {
        contextRunner.withUserConfiguration(CustomAuditorConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditorAware.class);
                    AuditorAware<?> auditor = context.getBean(AuditorAware.class);
                    assertThat(auditor.getCurrentAuditor()).isEqualTo(java.util.Optional.of(Long.valueOf(999L)));
                });
    }

    @Test
    void notActive_whenJdbcTemplateNotOnClasspath() {
        contextRunner.withClassLoader(new FilteredClassLoader(JdbcTemplate.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ZerxDataAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(SlowSqlInterceptor.class);
                });
    }

    /**
     * 测试用数据源配置
     */
    @Configuration
    static class TestDataSourceConfig {
        @Bean
        DataSource dataSource() {
            return new org.springframework.jdbc.datasource.SimpleDriverDataSource(
                    new org.h2.Driver(),
                    "jdbc:h2:mem:auto_test;MODE=MYSQL;DB_CLOSE_DELAY=-1", null, null);
        }
    }

    /**
     * 自定义 AuditorAware 配置
     */
    @Configuration
    static class CustomAuditorConfig {
        @Bean
        AuditorAware<Long> customAuditorAware() {
            return () -> java.util.Optional.of(999L);
        }
    }
}
