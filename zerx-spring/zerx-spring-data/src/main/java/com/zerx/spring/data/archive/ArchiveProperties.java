package com.zerx.spring.data.archive;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * 归档功能配置属性。
 * <p>
 * 通过 {@code zerx.data.archive.*} 前缀在 application.yml 中配置。
 * 控制归档功能的启停、归档表命名规则、需要归档的实体列表等。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   data:
 *     archive:
 *       enabled: true
 *       table-suffix: "_archive"
 *       entities:
 *         - com.zerx.business.user.entity.User
 *         - com.zerx.business.order.entity.Order
 *       retain-days: 90
 * }</pre>
 *
 * @author zerx
 */
@ConfigurationProperties(prefix = "zerx.data.archive")
public class ArchiveProperties {

    /**
     * 是否启用归档功能
     */
    private boolean enabled = false;

    /**
     * 归档表后缀（默认为 _archive）
     * <p>
     * 归档表名 = 原表名 + 后缀，例如 sys_user → sys_user_archive
     * </p>
     */
    private String tableSuffix = "_archive";

    /**
     * 需要启用归档的实体全限定类名集合。
     * <p>
     * 只有在此集合中的实体类，删除时才会触发归档流程。
     * 未配置的实体直接物理删除，不会创建归档表。
     * </p>
     */
    private Set<String> entities = new HashSet<>();

    /**
     * 归档数据保留天数（用于后续清理归档表数据）
     */
    private long retainDays = 90;

    /**
     * 归档操作超时时间
     */
    private Duration timeout = Duration.ofSeconds(10);

    /**
     * 过期归档清理 cron 表达式。
     * <p>
     * 定时清理超过 {@link #retainDays} 天的归档数据。
     * 需要在启动类上添加 {@code @EnableScheduling} 才能生效。
     * 默认每天凌晨 3 点执行。
     * </p>
     */
    private String purgeCron = "0 0 3 * * ?";

    // ======================== getter/setter ========================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTableSuffix() {
        return tableSuffix;
    }

    public void setTableSuffix(String tableSuffix) {
        this.tableSuffix = tableSuffix;
    }

    public Set<String> getEntities() {
        return entities;
    }

    public void setEntities(Set<String> entities) {
        this.entities = entities;
    }

    public long getRetainDays() {
        return retainDays;
    }

    public void setRetainDays(long retainDays) {
        this.retainDays = retainDays;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public String getPurgeCron() {
        return purgeCron;
    }

    public void setPurgeCron(String purgeCron) {
        this.purgeCron = purgeCron;
    }

    /**
     * 判断指定实体类是否需要归档
     *
     * @param entityClass 实体类
     * @return 需要归档返回 {@code true}
     */
    public boolean isArchiveEnabled(Class<?> entityClass) {
        return enabled && entities.contains(entityClass.getName());
    }
}
