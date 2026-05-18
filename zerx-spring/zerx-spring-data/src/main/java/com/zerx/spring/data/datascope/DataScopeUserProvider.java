package com.zerx.spring.data.datascope;

import java.util.Optional;

/**
 * 数据权限用户信息提供者接口。
 * <p>
 * 业务系统需要实现此接口，在运行时提供当前用户的 ID、部门等信息。
 * 框架自动注册为 Spring Bean，在数据权限拦截时调用。
 * </p>
 *
 * <h3>实现示例：</h3>
 * <pre>{@code
 * {@literal @}Component
 * public class ZerxDataScopeUserProvider implements DataScopeUserProvider {
 *     {@literal @}Override
 *     public Optional<DataScopeUser> getCurrentUser() {
 *         Long userId = RequestContext.get().getUserId();
 *         Long deptId = RequestContext.get().getDeptId();
 *         if (userId == null) return Optional.empty();
 *         return Optional.of(new DataScopeUser(
 *             userId,
 *             List.of(deptId),
 *             RequestContext.get().getRoles()
 *         ));
 *     }
 * }
 * }</pre>
 *
 * @author zerx
 */
public interface DataScopeUserProvider {

    /**
     * 获取当前用户的数据权限上下文信息。
     * <p>
     * 在非 Web 请求上下文（如定时任务、消息消费）中应返回 {@link Optional#empty()}。
     * </p>
     *
     * @return 当前用户信息，不可用时返回 empty
     */
    Optional<DataScopeUser> getCurrentUser();
}
