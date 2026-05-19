package com.zerx.spring.security.token;

import java.util.List;

/**
 * 用户角色加载服务接口 — RBAC 角色按需加载的 SPI
 * <p>
 * 业务应用实现此接口，为安全模块提供用户角色数据。
 * 实现类注册为 Spring Bean 后，{@code ZerxJwtAuthenticationFilter} 将自动使用。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * @Service
 * public class MyRoleService implements ZerxRoleService {
 *     @Override
 *     public List<String> getRoles(Long userId) {
 *         return roleRepository.findRoleCodesByUserId(userId);
 *     }
 * }
 * }</pre>
 *
 * <h3>角色加载优先级（在 JWT Filter 中）：</h3>
 * <ol>
 *   <li>如果 {@code ZerxRoleService} Bean 存在 → 从 SPI 加载角色</li>
 *   <li>否则 → 从 JWT claims 中的 roles 字段获取（向后兼容）</li>
 * </ol>
 *
 * @author zerx
 */
public interface ZerxRoleService {

    /**
     * 根据用户 ID 加载角色列表
     * <p>
     * 返回的角色编码会自动添加 {@code ROLE_} 前缀后转换为
     * Spring Security 的 {@code GrantedAuthority}。
     * </p>
     *
     * @param userId 用户 ID
     * @return 角色编码列表（如 {@code ["admin", "editor"]}），不应包含 {@code ROLE_} 前缀
     */
    List<String> getRoles(Long userId);
}
