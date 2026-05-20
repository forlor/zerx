package com.zerx.spring.security.token;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.security.properties.ZerxSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * HS256 算法 JWT 令牌服务实现
 * <p>
 * 基于 HMAC-SHA256 对称加密算法实现 JWT 令牌的生成、解析、校验与黑名单管理。
 * 适用于单体应用或中小规模分布式系统。
 * </p>
 *
 * <h3>密钥轮转：</h3>
 * <p>
 * 支持通过 {@code zerx.security.jwt.previous-secret} 配置旧密钥，
 * 实现密钥轮转的无缝过渡：
 * <ul>
 *   <li>签发令牌：始终使用当前密钥，并在 JWT header 写入 {@code kid}</li>
 *   <li>验证令牌：优先使用当前密钥，失败后回退到旧密钥</li>
 *   <li>过渡期结束后，移除 {@code previous-secret} 配置即可</li>
 * </ul>
 * </p>
 *
 * @author zerx
 * @see AbstractZerxTokenService
 * @see ZerxSecurityProperties
 */
public class ZerxHs256TokenService extends AbstractZerxTokenService {

    /** HMAC-SHA256 当前签名密钥 */
    private final SecretKey key;

    /** HMAC-SHA256 旧签名密钥（密钥轮转过渡期，可选） */
    @Nullable
    private final SecretKey previousKey;

    /**
     * 构造 HS256 令牌服务
     *
     * @param props    安全配置属性
     * @param cacheOps 缓存操作工具
     */
    public ZerxHs256TokenService(@Nonnull ZerxSecurityProperties props, @Nonnull CacheOps cacheOps) {
        super(props, cacheOps);
        var jwtConfig = props.getJwt();
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));

        String previousSecret = jwtConfig.getPreviousSecret();
        if (previousSecret != null && !previousSecret.isBlank()) {
            this.previousKey = Keys.hmacShaKeyFor(previousSecret.getBytes(StandardCharsets.UTF_8));
            log.info("HS256 key rotation enabled: kid={}, previousKey present", kid);
        } else {
            this.previousKey = null;
        }

        log.info("HS256 token service initialized: kid={}", kid);
    }

    @Override
    public String generateAccessToken(Long userId, String jti, List<String> roles) {
        var jwtConfig = props.getJwt();
        var now = Instant.now();
        var expiresAt = now.plus(jwtConfig.getAccessTokenExpire());

        return Jwts.builder()
                .header().keyId(kid).and()
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_JTI, jti)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_ROLES, roles)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(Long userId, String jti) {
        var jwtConfig = props.getJwt();
        var now = Instant.now();
        var expiresAt = now.plus(jwtConfig.getRefreshTokenExpire());

        return Jwts.builder()
                .header().keyId(kid).and()
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_JTI, jti)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZerxTokenClaims parseToken(String token) {
        try {
            return doParseToken(token, key);
        } catch (JwtException ex) {
            if (previousKey != null) {
                log.debug("Token verification failed with current key, trying previous key: {}",
                        ex.getMessage());
                return doParseToken(token, previousKey);
            }
            throw ex;
        }
    }

    private ZerxTokenClaims doParseToken(String token, SecretKey verifyKey) {
        Claims claims = Jwts.parser()
                .verifyWith(verifyKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        List<String> roles = claims.get(CLAIM_ROLES, List.class);
        if (roles == null) {
            roles = List.of();
        }

        return new ZerxTokenClaims(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_JTI, String.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant(),
                claims.get(CLAIM_TOKEN_TYPE, String.class),
                roles
        );
    }
}
