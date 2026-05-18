package com.zerx.spring.data.autoconfigure;

import com.zerx.spring.data.archive.Archiver;
import com.zerx.spring.data.archive.ArchiveProperties;
import com.zerx.spring.data.archive.JdbcArchiveRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 归档过期数据定时清理任务。
 * <p>
 * 通过 Spring {@link Scheduled @Scheduled} 定时执行，
 * 遍历所有已注册的 {@link JdbcArchiveRepository}，
 * 清理超过 {@link ArchiveProperties#getRetainDays()} 天的归档数据。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <p>
 * 此 Bean 由 {@link ZerxDataAutoConfiguration} 在
 * {@code zerx.data.archive.enabled=true} 时自动注册。
 * 需要在启动类上添加 {@code @EnableScheduling} 注解才能激活定时任务。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   data:
 *     archive:
 *       enabled: true
 *       retain-days: 90
 *       purge-cron: "0 0 3 * * ?"   # 每天凌晨 3 点清理
 * }</pre>
 *
 * @author zerx
 * @see ArchiveProperties
 * @see JdbcArchiveRepository#purgeExpired()
 */
public class ArchivePurgeTask {

    private static final Logger log = LoggerFactory.getLogger(ArchivePurgeTask.class);

    private final ArchiveProperties properties;
    private final List<Archiver<?>> archivers;

    /**
     * 构造归档清理任务。
     *
     * @param properties 归档配置
     * @param archivers  已注册的 Archiver 列表
     */
    public ArchivePurgeTask(ArchiveProperties properties, List<Archiver<?>> archivers) {
        this.properties = properties;
        this.archivers = archivers;
    }

    /**
     * 定时清理过期归档数据。
     * <p>
     * 仅对 {@link JdbcArchiveRepository} 类型的 Archiver 执行清理，
     * 其他类型（如未来的 ES 归档）跳过。
     * </p>
     */
    @Scheduled(cron = "${zerx.data.archive.purge-cron:0 0 3 * * ?}")
    public void purgeExpiredArchives() {
        if (!properties.isEnabled() || archivers.isEmpty()) {
            return;
        }

        log.info("[Zerx] Archive purge started — retainDays: {}", properties.getRetainDays());
        int totalPurged = 0;

        for (Archiver<?> archiver : archivers) {
            if (archiver instanceof JdbcArchiveRepository<?> jdbcArchiver) {
                try {
                    int purged = jdbcArchiver.purgeExpired();
                    totalPurged += purged;
                } catch (Exception e) {
                    log.error("[Zerx] Failed to purge archives for {}: {}",
                            archiver.getClass().getSimpleName(), e.getMessage());
                }
            }
        }

        log.info("[Zerx] Archive purge completed — total purged: {}", totalPurged);
    }
}
