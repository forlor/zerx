package com.zerx.spring.data.repository;

import com.zerx.common.model.PageRequest;
import com.zerx.common.model.PageResult;
import com.zerx.spring.data.domain.BaseEntity;
import com.zerx.spring.data.logicaldelete.LogicalDeleteService;
import com.zerx.spring.data.util.NamingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
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
 * <h3>逻辑删除集成：</h3>
 * <p>
 * 当注入了 {@link LogicalDeleteService} 时，{@code findPage}、{@code existsByIds}、{@code countAll}
 * 会自动追加 {@code WHERE deleted = '0'} 过滤条件（仅对标注 {@code @LogicalDelete} 的实体生效）。
 * 如需查询包含已删除记录的数据，请使用对应的 {@code *IncludingDeleted} 方法。
 * </p>
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
 * @see LogicalDeleteService
 */
public class ZerxRepositoryHelper {

    private static final Logger log = LoggerFactory.getLogger(ZerxRepositoryHelper.class);

    private final JdbcTemplate jdbcTemplate;
    private final LogicalDeleteService logicalDeleteService;

    /**
     * 构造 Zerx Repository 增强组件。
     *
     * @param dataSource           数据源
     * @param logicalDeleteService 逻辑删除服务（可为 null，传入时自动过滤已删除记录）
     */
    public ZerxRepositoryHelper(DataSource dataSource,
                                @Nullable LogicalDeleteService logicalDeleteService) {
        Assert.notNull(dataSource, "DataSource must not be null");
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.logicalDeleteService = logicalDeleteService;
        if (logicalDeleteService != null) {
            log.info("[Zerx] ZerxRepositoryHelper initialized with logical delete support");
        } else {
            log.info("[Zerx] ZerxRepositoryHelper initialized (no logical delete service)");
        }
    }

    /**
     * 构造 Zerx Repository 增强组件（无逻辑删除支持）。
     *
     * @param dataSource 数据源
     */
    public ZerxRepositoryHelper(DataSource dataSource) {
        this(dataSource, null);
    }

    /**
     * 分页查询指定实体的所有数据（按 ID 倒序），返回 Map 列表。
     * <p>
     * 先通过 COUNT 查询获取总数，再执行 LIMIT/OFFSET 分页查询。
     * 返回 {@code Map<String, Object>} 列表，业务层可使用 BeanUtils 或手动转换为实体。
     * 如需完整实体映射（含关联），请配合 {@link com.zerx.spring.data.query.DynamicQuery} 使用。
     * </p>
     * <p>
     * 如果实体标注了 {@code @LogicalDelete} 且已注入 {@link LogicalDeleteService}，
     * 会自动过滤已删除记录。
     * </p>
     *
     * @param entityClass 实体类型（用于解析表名）
     * @param pageRequest 分页参数
     * @param <T>         实体类型
     * @return 分页结果（Map 形式）
     */
    public <T extends BaseEntity> PageResult<Map<String, Object>> findPage(Class<T> entityClass,
                                                                            PageRequest pageRequest) {
        return findPageInternal(entityClass, pageRequest, true);
    }

    /**
     * 分页查询指定实体的所有数据（包含已删除记录）。
     * <p>
     * 与 {@link #findPage(Class, PageRequest)} 相同，但不会自动过滤逻辑删除的记录。
     * </p>
     *
     * @param entityClass 实体类型（用于解析表名）
     * @param pageRequest 分页参数
     * @param <T>         实体类型
     * @return 分页结果（Map 形式，包含已删除记录）
     */
    public <T extends BaseEntity> PageResult<Map<String, Object>> findPageIncludingDeleted(Class<T> entityClass,
                                                                                            PageRequest pageRequest) {
        return findPageInternal(entityClass, pageRequest, false);
    }

    /**
     * 批量检查指定 ID 是否都存在。
     * <p>
     * 如果实体标注了 {@code @LogicalDelete} 且已注入 {@link LogicalDeleteService}，
     * 会自动过滤已删除记录（即仅检查未删除的记录是否存在）。
     * </p>
     *
     * @param entityClass 实体类型
     * @param ids         主键集合
     * @param <T>         实体类型
     * @return 所有 ID 都存在且未删除返回 {@code true}，空集合返回 {@code false}
     */
    public <T extends BaseEntity> boolean existsByIds(Class<T> entityClass,
                                                       Iterable<Long> ids) {
        return existsByIdsInternal(entityClass, ids, true);
    }

    /**
     * 批量检查指定 ID 是否都存在（包含已删除记录）。
     * <p>
     * 与 {@link #existsByIds(Class, Iterable)} 相同，但不会自动过滤逻辑删除的记录。
     * </p>
     *
     * @param entityClass 实体类型
     * @param ids         主键集合
     * @param <T>         实体类型
     * @return 所有 ID 都存在返回 {@code true}，空集合返回 {@code false}
     */
    public <T extends BaseEntity> boolean existsByIdsIncludingDeleted(Class<T> entityClass,
                                                                       Iterable<Long> ids) {
        return existsByIdsInternal(entityClass, ids, false);
    }

    /**
     * 统计指定实体的总记录数。
     * <p>
     * 如果实体标注了 {@code @LogicalDelete} 且已注入 {@link LogicalDeleteService}，
     * 会自动过滤已删除记录。
     * </p>
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 总数（仅统计未删除记录）
     */
    public <T extends BaseEntity> long countAll(Class<T> entityClass) {
        return countAllInternal(entityClass, true);
    }

    /**
     * 统计指定实体的总记录数（包含已删除记录）。
     * <p>
     * 与 {@link #countAll(Class)} 相同，但不会自动过滤逻辑删除的记录。
     * </p>
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 总数（包含所有记录）
     */
    public <T extends BaseEntity> long countAllIncludingDeleted(Class<T> entityClass) {
        return countAllInternal(entityClass, false);
    }

    // ======================== 内部实现 ========================

    /**
     * 分页查询内部实现。
     */
    private <T extends BaseEntity> PageResult<Map<String, Object>> findPageInternal(Class<T> entityClass,
                                                                                     PageRequest pageRequest,
                                                                                     boolean filterDeleted) {
        String tableName = resolveTableName(entityClass);

        // 构建 SQL，自动追加逻辑删除过滤
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        String querySql = "SELECT * FROM " + tableName + " ORDER BY id DESC LIMIT ? OFFSET ?";

        if (filterDeleted && logicalDeleteService != null && logicalDeleteService.isLogicalDelete(entityClass)) {
            countSql = logicalDeleteService.appendNotDeletedFilter(countSql, entityClass);
            querySql = logicalDeleteService.appendNotDeletedFilter(querySql, entityClass);
        }

        // 查询总数
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);
        long totalCount = total != null ? total : 0L;

        if (totalCount == 0) {
            return PageResult.of(List.of(), 0, pageRequest.page(), pageRequest.size());
        }

        // 查询分页数据
        List<Map<String, Object>> records = jdbcTemplate.queryForList(querySql,
                pageRequest.size(), pageRequest.offset());

        return PageResult.of(records, totalCount, pageRequest.page(), pageRequest.size());
    }

    /**
     * 批量存在性检查内部实现。
     */
    private <T extends BaseEntity> boolean existsByIdsInternal(Class<T> entityClass,
                                                                Iterable<Long> ids,
                                                                boolean filterDeleted) {
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

        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE id IN (" + placeholders + ")";

        if (filterDeleted && logicalDeleteService != null && logicalDeleteService.isLogicalDelete(entityClass)) {
            sql = logicalDeleteService.appendNotDeletedFilter(sql, entityClass);
        }

        Long count = jdbcTemplate.queryForObject(sql, Long.class, idCollection.toArray());

        return count != null && count == idCollection.size();
    }

    /**
     * 统计总数内部实现。
     */
    private <T extends BaseEntity> long countAllInternal(Class<T> entityClass,
                                                         boolean filterDeleted) {
        String tableName = resolveTableName(entityClass);
        String sql = "SELECT COUNT(*) FROM " + tableName;

        if (filterDeleted && logicalDeleteService != null && logicalDeleteService.isLogicalDelete(entityClass)) {
            sql = logicalDeleteService.appendNotDeletedFilter(sql, entityClass);
        }

        Long count = jdbcTemplate.queryForObject(sql, Long.class);
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
