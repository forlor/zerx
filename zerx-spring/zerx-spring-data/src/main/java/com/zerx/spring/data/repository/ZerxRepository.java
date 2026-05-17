package com.zerx.spring.data.repository;

import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * Zerx 通用 Repository Fragment 接口。
 * <p>
 * 提供逻辑删除感知的通用查询方法，业务 Repository 同时继承
 * {@code CrudRepository} 和 {@code ZerxRepository} 即可获得这些能力。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public interface UserRepository extends CrudRepository<User, Long>,
 *     ZerxRepository<User, Long> {
 *     // 自定义派生查询...
 * }
 * }</pre>
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 * @author zerx
 */
@NoRepositoryBean
public interface ZerxRepository<T, ID> {

    /**
     * 根据 ID 查询实体（自动过滤已删除记录）
     *
     * @param id 主键
     * @return 未删除的实体，不存在或已删除返回 Optional.empty()
     */
    Optional<T> findByIdAndDeletedFalse(ID id);

    /**
     * 查询所有未删除的记录
     *
     * @return 未删除的实体列表
     */
    List<T> findAllByDeletedFalse();

    /**
     * 统计未删除的记录数
     *
     * @return 未删除记录的数量
     */
    long countByDeletedFalse();

    /**
     * 判断指定 ID 的未删除记录是否存在
     *
     * @param id 主键
     * @return 存在且未删除返回 true
     */
    boolean existsByIdAndDeletedFalse(ID id);
}
