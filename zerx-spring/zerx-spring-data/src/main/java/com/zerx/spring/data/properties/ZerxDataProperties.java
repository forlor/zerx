package com.zerx.spring.data.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Zerx 数据访问层配置属性。
 * <p>
 * 统一管理慢 SQL 检测、Single Query Loading、命名策略等数据层通用配置。
 * 通过 {@code zerx.data.*} 前缀在 application.yml 中配置。
 * </p>
 *
 * <p>
 * 归档相关配置见 {@link com.zerx.spring.data.archive.ArchiveProperties}，
 * 通过 {@code zerx.data.archive.*} 前缀配置。
 * </p>
 *
 * @author zerx
 */
@ConfigurationProperties(prefix = "zerx.data")
public class ZerxDataProperties {

    private SlowSql slowSql = new SlowSql();

    /**
     * 是否开启 Single Query Loading（解决 Spring Data JDBC N+1 查询问题）
     */
    private boolean singleQueryLoading = true;

    /**
     * 列名命名策略
     */
    private NamingStrategy namingStrategy = NamingStrategy.SNAKE_CASE;

    // --- getter/setter ---

    public SlowSql getSlowSql() {
        return slowSql;
    }

    public void setSlowSql(SlowSql slowSql) {
        this.slowSql = slowSql;
    }

    public boolean isSingleQueryLoading() {
        return singleQueryLoading;
    }

    public void setSingleQueryLoading(boolean singleQueryLoading) {
        this.singleQueryLoading = singleQueryLoading;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    // ======================== 慢 SQL 检测配置 ========================

    /**
     * 慢 SQL 检测配置
     */
    public static class SlowSql {

        /**
         * 是否启用慢 SQL 检测
         */
        private boolean enabled = true;

        /**
         * 慢 SQL 阈值（毫秒）
         */
        private Duration threshold = Duration.ofMillis(1000);

        /**
         * 是否记录 SQL 参数值
         */
        private boolean logParams = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getThreshold() {
            return threshold;
        }

        public void setThreshold(Duration threshold) {
            this.threshold = threshold;
        }

        public boolean isLogParams() {
            return logParams;
        }

        public void setLogParams(boolean logParams) {
            this.logParams = logParams;
        }
    }

    // ======================== 命名策略枚举 ========================

    /**
     * 列名命名策略
     */
    public enum NamingStrategy {

        /** 下划线命名（user_name） */
        SNAKE_CASE,

        /** 驼峰命名（userName） */
        CAMEL_CASE
    }
}
