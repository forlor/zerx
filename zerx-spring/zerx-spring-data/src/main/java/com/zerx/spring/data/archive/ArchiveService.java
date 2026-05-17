package com.zerx.spring.data.archive;

import com.zerx.spring.data.util.NamingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 归档服务门面，提供归档操作的便捷方法。
 * <p>
 * 封装了 {@link Archiver} 的查找和调用逻辑，业务层可直接注入此服务
 * 而不必关心具体使用哪个 Archiver 实现。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @Service
 * public class UserService {
 *     private final UserRepository userRepo;
 *     private final ArchiveService archiveService;
 *
 *     public void deleteUser(Long userId) {
 *         User user = userRepo.findById(userId).orElseThrow();
 *         userRepo.delete(user);  // 归档回调自动触发
 *     }
 *
 *     // 查看归档记录
 *     public Map<String, Object> findArchivedUser(Long userId) {
 *         return archiveService.findArchived(userId, User.class);
 *     }
 *
 *     // 恢复归档数据
 *     public void restoreUser(Long userId) {
 *         archiveService.restore(userId, User.class)
 *             .ifPresent(archivedData -> userRepo.save(convertToUser(archivedData)));
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 */
public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

    private final ArchiveProperties properties;
    private final List<Archiver<?>> archivers;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造归档服务。
     *
     * @param properties   归档配置
     * @param archivers    已注册的 Archiver 实例列表
     * @param jdbcTemplate JDBC 模板（用于直接查询归档表）
     */
    public ArchiveService(ArchiveProperties properties, List<Archiver<?>> archivers,
                          JdbcTemplate jdbcTemplate) {
        this.properties = properties;
        this.archivers = archivers;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 判断指定实体类型是否启用了归档
     *
     * @param entityClass 实体类型
     * @return 启用了归档返回 {@code true}
     */
    public boolean isArchiveEnabled(Class<?> entityClass) {
        return properties.isArchiveEnabled(entityClass);
    }

    /**
     * 手动归档指定实体（通常不需要手动调用，删除时自动触发）
     *
     * @param entity 待归档的实体
     * @param <T>    实体类型
     */
    @SuppressWarnings("unchecked")
    public <T> void archive(T entity) {
        if (entity == null) return;
        Archiver<T> archiver = findArchiver((Class<T>) entity.getClass());
        if (archiver != null) {
            archiver.archive(entity);
        }
    }

    /**
     * 恢复已归档的数据
     *
     * @param id          实体主键
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 已归档的实体数据
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> restore(Object id, Class<T> entityClass) {
        Archiver<T> archiver = findArchiver(entityClass);
        if (archiver != null) {
            return archiver.restore(id);
        }
        return Optional.empty();
    }

    /**
     * 查询归档数据（不删除归档记录）
     *
     * @param id          实体主键
     * @param entityClass 实体类型
     * @return 归档数据的 Map
     */
    public Optional<Map<String, Object>> findArchived(Object id, Class<?> entityClass) {
        if (!properties.isArchiveEnabled(entityClass)) {
            return Optional.empty();
        }
        String archiveTable = resolveArchiveTable(entityClass);
        try {
            Map<String, Object> data = jdbcTemplate.queryForMap(
                    "SELECT * FROM " + archiveTable + " WHERE id = ?", id);
            return Optional.of(data);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 查找支持指定实体类型的 Archiver
     */
    @SuppressWarnings("unchecked")
    private <T> Archiver<T> findArchiver(Class<T> entityClass) {
        for (Archiver<?> archiver : archivers) {
            if (archiver.supports(entityClass)) {
                return (Archiver<T>) archiver;
            }
        }
        return null;
    }

    /**
     * 解析归档表名
     */
    private String resolveArchiveTable(Class<?> entityClass) {
        return NamingUtils.resolveTableName(entityClass) + properties.getTableSuffix();
    }
}
