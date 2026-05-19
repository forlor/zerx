package com.zerx.spring.security.token;

import java.util.List;
import java.util.Optional;

/**
 * Refresh Token 旋转服务接口 — 令牌续签的 SPI
 * <p>
 * 业务应用实现此接口，提供 Refresh Token 旋转、重放检测和设备限制能力。
 * 框架层仅定义契约，不提供默认实现（因为涉及业务数据模型）。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>每次使用 Refresh Token 续签时，旧 Token 立即失效（旋转）</li>
 *   <li>已使用的 Refresh Token 不能重复使用（重放检测）</li>
 *   <li>同一用户允许同时存在的设备数量受限（设备限制）</li>
 * </ul>
 *
 * <h3>典型实现方式：</h3>
 * <ul>
 *   <li>数据库表：{@code sys_refresh_token(user_id, jti, device_info, expires_at, revoked)}</li>
 *   <li>旋转：续签时标记旧 Token 为 revoked，生成新 jti 并插入</li>
 *   <li>重放：检查 Token 是否已 revoked</li>
 *   <li>设备限制：续签前检查该用户的活跃 Token 数量</li>
 * </ul>
 *
 * @author zerx
 */
public interface ZerxRefreshTokenService {

    /**
     * 旋转 Refresh Token
     * <p>
     * 验证当前 Refresh Token 的有效性，使其失效，并生成新的令牌对。
     * 如果当前 Token 已被使用过（重放攻击），返回 empty。
     * </p>
     *
     * @param refreshToken 当前的刷新令牌
     * @param roles        用户角色列表（用于生成新的访问令牌）
     * @return 新的令牌对，如果旋转失败（重放/过期/设备限制）返回 empty
     */
    Optional<ZerxTokenPair> rotate(String refreshToken, List<String> roles);

    /**
     * 撤销指定 JTI 的 Refresh Token
     *
     * @param jti 令牌唯一标识
     */
    void revoke(String jti);

    /**
     * 撤销用户的所有 Refresh Token（登出所有设备）
     *
     * @param userId 用户 ID
     * @return 被撤销的令牌数量
     */
    int revokeAll(Long userId);
}
