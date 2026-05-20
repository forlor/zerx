package com.zerx.spring.security.token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.zerx.common.util.UuidUtil;
import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.security.properties.ZerxSecurityProperties;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT 令牌服务抽象基类 — 提取 HS256/RS256 共享逻辑。
 * <p>
 * 包含：黑名单管理、令牌校验、令牌对生成等通用逻辑。
 *
 * @author zerx
 */
abstract class AbstractZerxTokenService implements ZerxTokenService {

    final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String BLACKLIST_PREFIX = "zerx:token:blacklist:";
    protected static final String TOKEN_TYPE_ACCESS = "access";
    protected static final String TOKEN_TYPE_REFRESH = "refresh";
    protected static final String CLAIM_JTI = "jti";
    protected static final String CLAIM_TOKEN_TYPE = "tokenType";
    protected static final String CLAIM_ROLES = "roles";

    protected final ZerxSecurityProperties props;
    protected final CacheOps cacheOps;
    protected final String kid;

    protected AbstractZerxTokenService(ZerxSecurityProperties props, CacheOps cacheOps) {
        this.props = props;
        this.cacheOps = cacheOps;
        this.kid = props.getJwt().getKid();
    }

    @Override
    public String generateAccessToken(Long userId, String jti) {
        return generateAccessToken(userId, jti, List.of());
    }

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

    @Override
    public void blacklistToken(String jti, Instant expiresAt) {
        var now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            log.debug("Token already expired, skip blacklisting: jti={}", jti);
            return;
        }
        var ttl = Duration.between(now, expiresAt);
        cacheOps.set(BLACKLIST_PREFIX + jti, "1", ttl);
        log.debug("Token blacklisted: jti={}, ttl={}", jti, ttl);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return cacheOps.hasKey(BLACKLIST_PREFIX + jti);
    }

    public ZerxTokenPair generateTokenPair(Long userId) {
        return generateTokenPair(userId, List.of());
    }

    public ZerxTokenPair generateTokenPair(Long userId, List<String> roles) {
        var jti = UuidUtil.uuidv7Hex();
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
