package com.zerx.spring.security.token;

/**
 * JWT 令牌对 — 访问令牌和刷新令牌的组合
 * <p>
 * 登录成功后返回的令牌对，包含访问令牌、刷新令牌及其有效期信息，
 * 用于客户端在访问令牌过期后通过刷新令牌获取新的访问令牌。
 * </p>
 *
 * @param accessToken      访问令牌
 * @param refreshToken     刷新令牌
 * @param accessExpiresIn  访问令牌有效期（秒）
 * @param refreshExpiresIn 刷新令牌有效期（秒）
 * @param jti              令牌对唯一标识
 * @author zerx
 */
public record ZerxTokenPair(String accessToken, String refreshToken,
                            long accessExpiresIn, long refreshExpiresIn, String jti) {
}
