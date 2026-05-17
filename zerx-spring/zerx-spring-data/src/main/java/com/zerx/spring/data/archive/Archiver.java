package com.zerx.spring.data.archive;

import java.util.Optional;

/**
 * 归档策略接口。
 * <p>
 * 定义数据删除前的归档行为，采用策略模式设计，支持灵活切换归档实现。
 * 默认实现 {@link JdbcArchiveRepository} 将数据归档到数据库归档表；
 * 可扩展实现归档到 Elasticsearch、对象存储等其他介质。
 * </p>
 *
 * <h3>设计约束：</h3>
 * <ul>
 *     <li>不是每张表都需要归档，通过 {@link ArchiveProperties} 配置哪些实体启用归档</li>
 *     <li>归档操作在删除之前执行，保证归档失败时原始数据不受影响</li>
 *     <li>实现类必须是线程安全的</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 默认实现：归档到数据库归档表
 * @Bean
 * Archiver<User> userArchiver(JdbcTemplate jdbcTemplate, ArchiveProperties props) {
 *     return new JdbcArchiveRepository<>(jdbcTemplate, props, User.class);
 * }
 *
 * // 自定义实现：归档到 Elasticsearch
 * public class EsArchiveRepository<T> implements Archiver<T> {
 *     // ...
 * }
 * }</pre>
 *
 * @param <T> 实体类型
 * @author zerx
 * @see JdbcArchiveRepository
 * @see ArchiveProperties
 */
public interface Archiver<T> {

    /**
     * 归档实体数据。
     * <p>
     * 在物理删除之前调用，将实体数据持久化到归档存储。
     * 如果归档失败，应抛出异常以阻止后续的物理删除操作。
     * </p>
     *
     * @param entity 待归档的实体（必须包含 ID）
     * @throws IllegalArgumentException 当实体 ID 为空时
     * @throws ArchiveException         归档操作失败时
     */
    void archive(T entity);

    /**
     * 根据主键恢复已归档的数据。
     * <p>
     * 从归档存储中恢复指定 ID 的数据，并将归档存储中的对应记录删除。
     * 恢复的数据需要业务层重新插入到主表中。
     * </p>
     *
     * @param id 实体主键
     * @return 已归档的实体数据，不存在时返回 {@link Optional#empty()}
     * @throws ArchiveException 恢复操作失败时
     */
    Optional<T> restore(Object id);

    /**
     * 检查指定实体类型是否支持归档。
     * <p>
     * 用于在删除操作前快速判断是否需要执行归档流程。
     * </p>
     *
     * @param entityClass 实体类型
     * @return 支持归档返回 {@code true}
     */
    boolean supports(Class<?> entityClass);
}
