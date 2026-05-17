package com.zerx.spring.data.repository;

import com.zerx.spring.data.domain.BaseEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Zerx 通用 Repository 接口。
 * <p>
 * 扩展 Spring Data JDBC 的 {@link CrudRepository}，为所有业务 Repository 提供统一的基础能力。
 * 业务 Repository 只需继承此接口即可获得标准 CRUD 操作。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public interface UserRepository extends ZerxRepository<User, Long> {
 *     // 自定义派生查询...
 *     List<User> findByStatus(String status);
 * }
 * }</pre>
 *
 * <h3>增强能力：</h3>
 * <p>
 * 需要分页查询、批量存在性检查等增强能力时，注入 {@link ZerxRepositoryHelper}：
 * </p>
 * <pre>{@code
 * {@literal @}Service
 * public class UserService {
 *     private final UserRepository userRepo;
 *     private final ZerxRepositoryHelper repoHelper;
 *
 *     public PageResult<User> listUsers(PageRequest pageReq) {
 *         return repoHelper.findPage(User.class, pageReq);
 *     }
 * }
 * }</pre>
 *
 * @param <T>  实体类型（必须继承 {@link BaseEntity}）
 * @param <ID> 主键类型
 * @author zerx
 * @see ZerxRepositoryHelper
 */
@NoRepositoryBean
public interface ZerxRepository<T extends BaseEntity, ID> extends CrudRepository<T, ID> {
}
