package com.zerx.spring.data.autoconfigure;

import com.zerx.spring.data.archive.ArchiveCallback;
import com.zerx.spring.data.archive.ArchiveProperties;
import com.zerx.spring.data.archive.ArchiveService;
import com.zerx.spring.data.audit.ZerxAuditorAware;
import com.zerx.spring.data.config.CamelCaseNamingStrategy;
import com.zerx.spring.data.config.SlowSqlInterceptor;
import com.zerx.spring.data.datascope.DataScopeHandler;
import com.zerx.spring.data.properties.ZerxDataProperties;
import com.zerx.spring.data.repository.ZerxRepositoryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Zerx 数据访问层自动配置。
 * <p>
 * 自动配置 Single Query Loading、慢 SQL 检测、命名策略、归档机制、审计等数据层通用能力。
 * 通过 {@code zerx.data.*} 前缀配置参数。
 * </p>
 *
 * <h3>配置项：</h3>
 * <ul>
 *     <li>{@code zerx.data.slow-sql.*} — 慢 SQL 检测</li>
 *     <li>{@code zerx.data.naming-strategy} — 命名策略（SNAKE_CASE / CAMEL_CASE）</li>
 *     <li>{@code zerx.data.single-query-loading} — 是否开启 Single Query Loading</li>
 *     <li>{@code zerx.data.archive.*} — 归档功能配置</li>
 * </ul>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnClass({JdbcTemplate.class, JdbcAggregateTemplate.class})
@EnableConfigurationProperties({ZerxDataProperties.class, ArchiveProperties.class})
@EnableJdbcAuditing
public class ZerxDataAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ZerxDataAutoConfiguration.class);

    /**
     * 配置 JdbcMappingContext，根据 {@code zerx.data.naming-strategy} 选择命名策略，
     * 并根据 {@code zerx.data.single-query-loading} 开启/关闭 Single Query Loading。
     * <p>
     * 默认使用 SNAKE_CASE（Spring Data JDBC 标准），可切换为 CAMEL_CASE 保持字段名原样。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcMappingContext jdbcMappingContext(ZerxDataProperties properties) {
        NamingStrategy namingStrategy = resolveNamingStrategy(properties.getNamingStrategy());
        JdbcMappingContext context = new JdbcMappingContext(namingStrategy);
        if (properties.isSingleQueryLoading()) {
            context.setSingleQueryLoadingEnabled(true);
            log.info("[Zerx] Single Query Loading enabled — N+1 queries will be resolved");
        }
        return context;
    }

    /**
     * 注册 SlowSqlInterceptor 作为 BeanPostProcessor，自动代理所有 JdbcTemplate 实例，
     * 在 SQL 执行前后记录耗时，超过阈值的 SQL 以 WARN 级别输出。
     */
    @Bean
    @ConditionalOnProperty(prefix = "zerx.data.slow-sql", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public static SlowSqlInterceptor slowSqlInterceptor(ZerxDataProperties properties) {
        log.info("[Zerx] Slow SQL interceptor enabled — threshold: {}ms",
                properties.getSlowSql().getThreshold().toMillis());
        return new SlowSqlInterceptor(properties);
    }

    /**
     * 慢 SQL 检测日志提示。
     */
    @Bean
    @ConditionalOnProperty(prefix = "zerx.data.slow-sql", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public ZerxSlowSqlLogger slowSqlLogger(ZerxDataProperties properties) {
        log.info("[Zerx] Slow SQL detection enabled — threshold: {}ms",
                properties.getSlowSql().getThreshold().toMillis());
        return new ZerxSlowSqlLogger(properties);
    }

    /**
     * 默认 AuditorAware（无操作），当 Web 模块不可用时使用。
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    AuditorAware<Long> zerxDefaultAuditorAware() {
        return Optional::empty;
    }

    /**
     * 注册归档回调，在实体删除前自动触发归档。
     * <p>
     * 当 {@code zerx.data.archive.enabled=true} 且存在已注册的 {@link com.zerx.spring.data.archive.Archiver} Bean 时激活。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zerx.data.archive", name = "enabled",
            havingValue = "true", matchIfMissing = false)
    public ArchiveCallback archiveCallback(ArchiveProperties properties,
                                           List<com.zerx.spring.data.archive.Archiver<?>> archivers) {
        log.info("[Zerx] Archive callback enabled — entities: {}", properties.getEntities());
        return new ArchiveCallback(properties, archivers);
    }

    /**
     * 注册归档服务门面。
     * <p>
     * 提供归档查询、恢复等便捷方法。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zerx.data.archive", name = "enabled",
            havingValue = "true", matchIfMissing = false)
    @ConditionalOnBean(JdbcTemplate.class)
    public ArchiveService archiveService(ArchiveProperties properties,
                                         List<com.zerx.spring.data.archive.Archiver<?>> archivers,
                                         JdbcTemplate jdbcTemplate) {
        return new ArchiveService(properties, archivers, jdbcTemplate);
    }

    /**
     * 注册 ZerxRepositoryHelper，提供分页查询、批量存在性检查等通用能力。
     * <p>
     * 当 DataSource 可用时自动注册。
     * 业务 Service 注入此 Bean 即可使用增强能力。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public ZerxRepositoryHelper zerxRepositoryHelper(DataSource dataSource) {
        log.info("[Zerx] ZerxRepositoryHelper registered — enhanced repository capabilities available");
        return new ZerxRepositoryHelper(dataSource);
    }

    /**
     * 根据配置枚举解析 NamingStrategy 实例。
     *
     * @param strategy 配置的命名策略
     * @return 对应的 NamingStrategy 实例
     */
    private NamingStrategy resolveNamingStrategy(ZerxDataProperties.NamingStrategy strategy) {
        return switch (strategy) {
            case SNAKE_CASE -> DefaultNamingStrategy.INSTANCE;
            case CAMEL_CASE -> new CamelCaseNamingStrategy();
        };
    }

    /**
     * Web 模块可用时，使用 ZerxAuditorAware 从 RequestContext 获取当前用户 ID。
     * <p>
     * 该内部配置通过 {@link ConditionalOnClass} 确保
     * {@code com.zerx.spring.web.context.RequestContext} 在 classpath 上时才激活。
     * 由于 {@link org.springframework.boot.autoconfigure.AutoConfiguration @AutoConfiguration}
     * 不做组件扫描，ZerxAuditorAware 必须在此显式注册。
     * </p>
     */
    @Configuration
    @ConditionalOnClass(name = "com.zerx.spring.web.context.RequestContext")
    static class RequestContextAuditingConfig {

        @Bean
        AuditorAware<Long> zerxRequestContextAuditorAware() {
            return new ZerxAuditorAware();
        }
    }

    /**
     * 慢 SQL 日志记录器（标记 Bean，实际通过 JdbcTemplate 拦截实现）
     */
    public static class ZerxSlowSqlLogger {

        private final ZerxDataProperties properties;

        public ZerxSlowSqlLogger(ZerxDataProperties properties) {
            this.properties = properties;
        }

        public boolean isSlow(long executionTimeMs) {
            return executionTimeMs >= properties.getSlowSql().getThreshold().toMillis();
        }

        public long getThresholdMs() {
            return properties.getSlowSql().getThreshold().toMillis();
        }
    }
}
