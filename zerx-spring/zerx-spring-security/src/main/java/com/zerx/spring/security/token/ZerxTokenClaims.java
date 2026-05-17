package com.zerx.spring.security.token;

import java.time.Instant;
import java.util.List;

/**
 * JWT 令牌解析结果 — 从令牌中提取的声明信息
 * <p>
 * 封装了从 JWT 令牌中解析出的关键声明，包括用户标识、令牌唯一 ID、
 * 签发时间、过期时间、令牌类型和用户角色列表。
 * </p>
 *
 * @param userId    用户 ID
 * @param jti       令牌唯一标识（JWT ID）
 * @param issuedAt  令牌签发时间
 * @param expiresAt 令牌过期时间
 * @param tokenType 令牌类型（{@code access} 或 {@code refresh}）
 * @param roles     用户角色列表（RBAC），可能为空列表
 * @author zerx
 */
public record ZerxTokenClaims(Long userId, String jti, Instant issuedAt, Instant expiresAt,
                              String tokenType, List<String> roles) {
}
