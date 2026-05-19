package com.zerx.spring.security.token;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.security.properties.ZerxSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
 * <h3>令牌策略：</h3>
 * <ul>
 *   <li>访问令牌：有效期较短（默认 2h），用于 API 认证，可携带 RBAC 角色信息</li>
 *   <li>刷新令牌：有效期较长（默认 7d），用于续签访问令牌</li>
 *   <li>黑名单：通过缓存实现，TTL 等于令牌剩余有效期</li>
 * </ul>
 *
 * @author zerx
 * @see ZerxTokenService
 * @see ZerxSecurityProperties
 */
public class ZerxHs256TokenService implements ZerxTokenService {

    private static final Logger log = LoggerFactory.getLogger(ZerxHs256TokenService.class);

    /** 黑名单缓存键前缀 */
    private static final String BLACKLIST_PREFIX = "zerx:token:blacklist:";

    /** 令牌类型声明：访问令牌 */
    private static final String TOKEN_TYPE_ACCESS = "access";

    /** 令牌类型声明：刷新令牌 */
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    /** JWT ID 声明键 */
    private static final String CLAIM_JTI = "jti";

    /** 令牌类型声明键 */
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    /** 角色声明键 */
    private static final String CLAIM_ROLES = "roles";

    /** HMAC-SHA256 当前签名密钥 */
    private final SecretKey key;

    /** HMAC-SHA256 旧签名密钥（密钥轮转过渡期，可选） */
    @Nullable
    private final SecretKey previousKey;

    /** 当前密钥 ID */
    private final String kid;

    /** 安全配置属性 */
    private final ZerxSecurityProperties props;

    /** 缓存操作工具 */
    private final CacheOps cacheOps;

    /**
     * 构造 HS256 令牌服务
     * <p>
     * 从配置属性中加载当前密钥，并可选加载旧密钥用于密钥轮转过渡。
     * </p>
     *
     * @param props    安全配置属性
     * @param cacheOps 缓存操作工具
     */
    public ZerxHs256TokenService(@Nonnull ZerxSecurityProperties props, @Nonnull CacheOps cacheOps) {
        this.props = props;
        this.cacheOps = cacheOps;
        var jwtConfig = props.getJwt();
        this.key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        this.kid = jwtConfig.getKid();

        // 加载旧密钥（密钥轮转过渡期）
        String previousSecret = jwtConfig.getPreviousSecret();
        if (previousSecret != null && !previousSecret.isBlank()) {
            this.previousKey = Keys.hmacShaKeyFor(previousSecret.getBytes(StandardCharsets.UTF_8));
            log.info("HS256 key rotation enabled: kid={}, previousKey present", kid);
        } else {
            this.previousKey = null;
        }

        log.info("HS256 token service initialized: kid={}", kid);
    }

    /**
     * 生成访问令牌（带角色信息）
     * <p>
     * 生成访问令牌：subject = userId, claims = {jti, tokenType=access, roles=[...]},
     * 过期时间 = 当前时间 + accessTokenExpire，header 写入 kid。
     * </p>
     */
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

    /**
     * 生成访问令牌（无角色信息，向后兼容）
     * <p>
     * 等同于 {@code generateAccessToken(userId, jti, List.of())}。
     * </p>
     */
    @Override
    public String generateAccessToken(Long userId, String jti) {
        return generateAccessToken(userId, jti, List.of());
    }

    /**
     * {@inheritDoc}
     * <p>
     * 生成刷新令牌：subject = userId, claims = {jti, tokenType=refresh},
     * 过期时间 = 当前时间 + refreshTokenExpire，header 写入 kid。
     * </p>
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 解析 JWT 字符串，支持密钥轮转：优先使用当前密钥验证，失败后回退到旧密钥。
     * 如果令牌中没有 roles 声明（旧令牌），默认返回空列表。
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public ZerxTokenClaims parseToken(String token) {
        // 优先使用当前密钥验证
        try {
            return doParseToken(token, key);
        } catch (JwtException ex) {
            // 当前密钥验证失败，尝试旧密钥
            if (previousKey != null) {
                log.debug("Token verification failed with current key, trying previous key: {}",
                        ex.getMessage());
                return doParseToken(token, previousKey);
            }
            throw ex;
        }
    }

    /**
     * 使用指定密钥解析令牌
     *
     * @param token   JWT 令牌字符串
     * @param verifyKey 验证密钥
     * @return 令牌声明信息
     * @throws JwtException 令牌验证失败
     */
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

    /**
     * {@inheritDoc}
     * <p>
     * 校验流程：
     * <ol>
     *   <li>解析令牌（验证签名和格式，支持密钥轮转回退）</li>
     *   <li>检查是否过期（jjwt 自动处理）</li>
     *   <li>检查黑名单状态</li>
     * </ol>
     * </p>
     */
    @Override
    public boolean validateToken(String token) {
        try {
            var claims = parseToken(token);
            if (isBlacklisted(claims.jti())) {
                log.debug("Token is blacklisted: jti={}", claims.jti());
                return false;
            }
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Token validation failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * 将 JTI 写入缓存，TTL 等于令牌剩余有效期（expiresAt - now）。
     * 如果令牌已过期，则无需加入黑名单。
     * </p>
     */
    @Override
    public void blacklistToken(String jti, Instant expiresAt) {
        var now = Instant.now();
        if (expiresAt.isBefore(now) || expiresAt.equals(now)) {
            log.debug("Token already expired, skip blacklisting: jti={}", jti);
            return;
        }
        var ttl = Duration.between(now, expiresAt);
        cacheOps.set(BLACKLIST_PREFIX + jti, "1", ttl);
        log.debug("Token blacklisted: jti={}, ttl={}", jti, ttl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBlacklisted(String jti) {
        return cacheOps.hasKey(BLACKLIST_PREFIX + jti);
    }

    /**
     * 生成新的令牌对（访问令牌 + 刷新令牌），不携带角色
     * <p>
     * 便捷方法，使用同一个 JTI 同时生成访问令牌和刷新令牌，
     * 方便登录时一次性返回令牌对。访问令牌不携带角色信息。
     * </p>
     *
     * @param userId 用户 ID
     * @return 令牌对
     */
    public ZerxTokenPair generateTokenPair(Long userId) {
        return generateTokenPair(userId, List.of());
    }

    /**
     * 生成新的令牌对（访问令牌 + 刷新令牌），带角色信息
     * <p>
     * 便捷方法，使用同一个 JTI 同时生成访问令牌和刷新令牌，
     * 方便登录时一次性返回令牌对。访问令牌携带指定的角色信息。
     * </p>
     *
     * @param userId 用户 ID
     * @param roles  用户角色列表
     * @return 令牌对
     */
    public ZerxTokenPair generateTokenPair(Long userId, List<String> roles) {
        var jti = UUID.randomUUID().toString().replace("-", "");
        var accessToken = generateAccessToken(userId, jti, roles);
        var refreshToken = generateRefreshToken(userId, jti);

        var jwtConfig = props.getJwt();
        return new ZerxTokenPair(
                accessToken,
                refreshToken,
                jwtConfig.getAccessTokenExpire().getSeconds(),
                jwtConfig.getRefreshTokenExpire().getSeconds(),
                jti
        );
    }
}
