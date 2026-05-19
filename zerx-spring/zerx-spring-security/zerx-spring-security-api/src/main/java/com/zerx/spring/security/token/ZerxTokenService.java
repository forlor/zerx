package com.zerx.spring.security.token;

import java.time.Instant;
import java.util.List;

/**
 * JWT 令牌服务接口 — 令牌生成、解析、校验与黑名单管理
 * <p>
 * 定义了 JWT 令牌的完整生命周期操作，包括访问令牌和刷新令牌的生成、
 * 令牌解析、有效性校验以及令牌黑名单的维护。
 * </p>
 *
 * <h3>职责边界：</h3>
 * <ul>
 *   <li>令牌生成：使用配置的密钥和有效期签发 JWT</li>
 *   <li>令牌解析：从 JWT 字符串中提取声明信息</li>
 *   <li>令牌校验：验证签名、过期时间和黑名单状态</li>
 *   <li>黑名单管理：将已注销的令牌加入缓存黑名单</li>
 * </ul>
 *
 * @author zerx
 * @see ZerxTokenClaims
 * @see ZerxTokenPair
 */
public interface ZerxTokenService {

    /**
     * 生成访问令牌（带角色信息）
     * <p>
     * 创建一个短期有效的访问令牌，用于 API 请求认证，并携带 RBAC 角色信息。
     * </p>
     *
     * @param userId 用户 ID
     * @param jti    令牌唯一标识
     * @param roles  用户角色列表
     * @return JWT 访问令牌字符串
     */
    String generateAccessToken(Long userId, String jti, List<String> roles);

    /**
     * 生成访问令牌（无角色信息，向后兼容）
     * <p>
     * 创建一个短期有效的访问令牌，用于 API 请求认证。
     * 等效于 {@code generateAccessToken(userId, jti, List.of())}。
     * </p>
     *
     * @param userId 用户 ID
     * @param jti    令牌唯一标识
     * @return JWT 访问令牌字符串
     */
    String generateAccessToken(Long userId, String jti);

    /**
     * 生成刷新令牌
     * <p>
     * 创建一个长期有效的刷新令牌，用于在访问令牌过期后获取新的访问令牌。
     * </p>
     *
     * @param userId 用户 ID
     * @param jti    令牌唯一标识
     * @return JWT 刷新令牌字符串
     */
    String generateRefreshToken(Long userId, String jti);

    /**
     * 解析令牌，提取声明信息
     * <p>
     * 从 JWT 字符串中解析出用户 ID、令牌 ID、签发时间、过期时间等信息。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 令牌声明信息
     * @throws RuntimeException 令牌格式错误、签名无效或已过期
     */
    ZerxTokenClaims parseToken(String token);

    /**
     * 校验令牌有效性
     * <p>
     * 综合检查令牌签名、过期时间和黑名单状态。
     * </p>
     *
     * @param token JWT 令牌字符串
     * @return 令牌有效返回 {@code true}，否则返回 {@code false}
     */
    boolean validateToken(String token);

    /**
     * 将令牌加入黑名单
     * <p>
     * 将指定 JTI 的令牌标记为已失效，缓存 TTL 等于令牌剩余有效期。
     * </p>
     *
     * @param jti       令牌唯一标识
     * @param expiresAt 令牌过期时间
     */
    void blacklistToken(String jti, Instant expiresAt);

    /**
     * 检查令牌是否已被加入黑名单
     *
     * @param jti 令牌唯一标识
     * @return 在黑名单中返回 {@code true}，否则返回 {@code false}
     */
    boolean isBlacklisted(String jti);
}
