package com.zerx.spring.data.repository;

import com.zerx.common.model.PageRequest;
import com.zerx.common.model.PageResult;
import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.util.NamingUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Zerx Repository 增强能力组件。
 * <p>
 * 为 Spring Data JDBC 实体提供分页查询、批量存在性检查、总数统计等通用增强能力。
 * 作为 Spring Bean 注入到业务 Service 中使用，与业务 Repository 配合。
 * </p>
 *
 * <h3>设计说明：</h3>
 * <p>
 * 此类不作为 Spring Data JDBC 的 Fragment 实现，而是作为独立的 Spring Bean 存在。
 * 这样做的好处：
 * </p>
 * <ul>
 *     <li>不依赖 Spring Data 的 Fragment 代理机制，避免泛型擦除问题</li>
 *     <li>可在 Service 层直接注入使用，使用方式更直观</li>
 *     <li>与 {@link com.zerx.spring.data.query.DynamicQuery} 形成互补：DynamicQuery 负责 SQL 构建，此类负责 Map 结果返回</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * {@literal @}Service
 * public class UserService {
 *     private final UserRepository userRepo;
 *     private final ZerxRepositoryHelper repoHelper;
 *
 *     public PageResult<Map<String, Object>> listUsers(PageRequest pageReq) {
 *         return repoHelper.findPage(User.class, pageReq);
 *     }
 *
 *     public boolean allExist(List<Long> userIds) {
 *         return repoHelper.existsByIds(User.class, userIds);
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 * @see ZerxRepository
 * @see com.zerx.spring.data.query.DynamicQuery
 */
public class ZerxRepositoryHelper {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造 Zerx Repository 增强组件。
     *
     * @param dataSource 数据源
     */
    public ZerxRepositoryHelper(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource must not be null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * 分页查询指定实体的所有数据（按 ID 倒序），返回 Map 列表。
     * <p>
     * 先通过 COUNT 查询获取总数，再执行 LIMIT/OFFSET 分页查询。
     * 返回 {@code Map<String, Object>} 列表，业务层可使用 BeanUtils 或手动转换为实体。
     * 如需完整实体映射（含关联），请配合 {@link com.zerx.spring.data.query.DynamicQuery} 使用。
     * </p>
     *
     * @param entityClass 实体类型（用于解析表名）
     * @param pageRequest 分页参数
     * @param <T>         实体类型
     * @return 分页结果（Map 形式）
     */
    public <T extends BaseEntity> PageResult<Map<String, Object>> findPage(Class<T> entityClass,
                                                                            PageRequest pageRequest) {
        String tableName = resolveTableName(entityClass);

        // 查询总数
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Long.class);
        long totalCount = total != null ? total : 0L;

        if (totalCount == 0) {
            return PageResult.of(List.of(), 0, pageRequest.page(), pageRequest.size());
        }

        // 查询分页数据
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM " + tableName + " ORDER BY id DESC LIMIT ? OFFSET ?",
                pageRequest.size(), pageRequest.offset());

        return PageResult.of(records, totalCount, pageRequest.page(), pageRequest.size());
    }

    /**
     * 批量检查指定 ID 是否都存在。
     *
     * @param entityClass 实体类型
     * @param ids         主键集合
     * @param <T>         实体类型
     * @return 所有 ID 都存在返回 {@code true}，空集合返回 {@code false}
     */
    public <T extends BaseEntity> boolean existsByIds(Class<T> entityClass,
                                                       Iterable<Long> ids) {
        Collection<Long> idCollection = (ids instanceof Collection<?> c)
                ? (Collection<Long>) c
                : new ArrayList<>();

        if (idCollection.isEmpty()) {
            return false;
        }

        String tableName = resolveTableName(entityClass);
        String placeholders = idCollection.stream()
                .map(v -> "?")
                .collect(java.util.stream.Collectors.joining(","));

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE id IN (" + placeholders + ")",
                Long.class, idCollection.toArray());

        return count != null && count == idCollection.size();
    }

    /**
     * 统计指定实体的总记录数。
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 总数
     */
    public <T extends BaseEntity> long countAll(Class<T> entityClass) {
        String tableName = resolveTableName(entityClass);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * 解析实体类对应的数据库表名。
     * <p>
     * 优先使用 {@code @Table} 注解指定的表名，
     * 否则使用类名的 snake_case 转换。
     * </p>
     *
     * @param entityClass 实体类型
     * @return 表名
     */
    private String resolveTableName(Class<?> entityClass) {
        return NamingUtils.resolveTableName(entityClass);
    }
}
