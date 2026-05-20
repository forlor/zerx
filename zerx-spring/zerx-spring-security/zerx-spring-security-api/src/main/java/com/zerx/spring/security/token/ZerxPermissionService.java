package com.zerx.spring.security.token;

import java.util.List;

/**
 * 权限加载服务接口 — 细粒度权限的 SPI。
 * <p>
 * 业务应用实现此接口，根据用户 ID 加载该用户的所有权限编码。
 * 权限编码格式建议为 {@code resource:action}（如 {@code user:create}、{@code order:export}）。
 * </p>
 *
 * <h3>使用方式：</h3>
 * <pre>{@code
 * @Service
 * public class MyPermissionService implements ZerxPermissionService {
 *     @Override
 *     public List<String> getPermissions(Long userId) {
 *         return permissionRepository.findPermissionCodesByUserId(userId);
 *     }
 * }
 * }</pre>
 *
 * <h3>权限加载策略（在 JWT Filter 中）：</h3>
 * <ol>
 *   <li>如果 {@code ZerxPermissionService} Bean 存在 → 从 SPI 实时加载权限</li>
 *   <li>否则 → 当前用户无细粒度权限（仅保留角色权限）</li>
 * </ol>
 *
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>权限在每次请求时从 SPI 实时加载（而非存储在 JWT 中），确保权限变更实时生效</li>
 *   <li>如需缓存，可在 SPI 实现中通过 {@code CacheOps} 等机制自行缓存</li>
 *   <li>与角色模型共存，权限编码会自动添加 {@code PERM_} 前缀后转换为 GrantedAuthority</li>
 * </ul>
 *
 * @author zerx
 */
public interface ZerxPermissionService {

    /**
     * 获取用户的所有权限编码列表。
     *
     * @param userId 用户 ID
     * @return 权限编码列表，无权限时返回空列表
     */
    List<String> getPermissions(Long userId);
}
