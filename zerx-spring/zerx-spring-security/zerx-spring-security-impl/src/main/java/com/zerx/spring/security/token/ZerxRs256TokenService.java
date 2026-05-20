package com.zerx.spring.security.token;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.security.properties.ZerxSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * RS256 算法 JWT 令牌服务实现
 * <p>
 * 基于 RSA 非对称加密算法实现 JWT 令牌的生成、解析、校验与黑名单管理。
 * 适用于微服务架构或需要公私钥分离的大型分布式系统。
 * </p>
 *
 * <h3>密钥加载支持：</h3>
 * <ul>
 *   <li>{@code classpath:} — 类路径资源（如 classpath:keys/public.pem）</li>
 *   <li>{@code file:} — 文件路径（如 file:/etc/zerx/private.pem）</li>
 *   <li>Base64 编码的 DER 格式（直接传入 Base64 字符串）</li>
 * </ul>
 *
 * @author zerx
 * @see AbstractZerxTokenService
 * @see ZerxSecurityProperties
 */
public class ZerxRs256TokenService extends AbstractZerxTokenService {

    /** RSA 私钥（用于签名） */
    private final PrivateKey privateKey;

    /** RSA 当前公钥（用于验证） */
    private final PublicKey publicKey;

    /** RSA 旧公钥（密钥轮转过渡期，可选） */
    @Nullable
    private final PublicKey previousPublicKey;

    /**
     * 构造 RS256 令牌服务
     *
     * @param props    安全配置属性
     * @param cacheOps 缓存操作工具
     * @throws IllegalArgumentException 如果密钥加载失败
     */
    public ZerxRs256TokenService(@Nonnull ZerxSecurityProperties props, @Nonnull CacheOps cacheOps) {
        super(props, cacheOps);
        var keyConfig = props.getJwt().getRsa();
        this.privateKey = loadPrivateKey(keyConfig.getPrivateKey());
        this.publicKey = loadPublicKey(keyConfig.getPublicKey());

        String previousPubKey = keyConfig.getPreviousPublicKey();
        if (previousPubKey != null && !previousPubKey.isBlank()) {
            this.previousPublicKey = loadPublicKey(previousPubKey);
            log.info("RS256 key rotation enabled: kid={}, previousPublicKey present", kid);
        } else {
            this.previousPublicKey = null;
        }

        log.info("RS256 token service initialized: kid={}", kid);
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
                .signWith(privateKey, Jwts.SIG.RS256)
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
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZerxTokenClaims parseToken(String token) {
        try {
            return doParseToken(token, publicKey);
        } catch (JwtException ex) {
            if (previousPublicKey != null) {
                log.debug("Token verification failed with current public key, trying previous key: {}",
                        ex.getMessage());
                return doParseToken(token, previousPublicKey);
            }
            throw ex;
        }
    }

    private ZerxTokenClaims doParseToken(String token, PublicKey verifyKey) {
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

    // ======================== 密钥加载工具方法 ========================

    private PrivateKey loadPrivateKey(String keySource) {
        try {
            byte[] keyBytes = loadKeyBytes(keySource);
            var keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load RSA private key: " + e.getMessage(), e);
        }
    }

    private PublicKey loadPublicKey(String keySource) {
        try {
            byte[] keyBytes = loadKeyBytes(keySource);
            var keySpec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load RSA public key: " + e.getMessage(), e);
        }
    }

    private byte[] loadKeyBytes(String keySource) {
        if (keySource.startsWith("classpath:")) {
            String path = keySource.substring("classpath:".length());
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new IllegalArgumentException("Classpath resource not found: " + path);
                }
                return decodePem(is.readAllBytes());
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load classpath resource: " + path, e);
            }
        } else if (keySource.startsWith("file:")) {
            try {
                return decodePem(Files.readAllBytes(Paths.get(URI.create(keySource))));
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to load file: " + keySource, e);
            }
        } else {
            return Base64.getDecoder().decode(keySource);
        }
    }

    private byte[] decodePem(byte[] pemBytes) {
        String pem = new String(pemBytes, StandardCharsets.UTF_8);
        if (!pem.contains("-----BEGIN")) {
            return Base64.getDecoder().decode(pem.trim());
        }
        String[] lines = pem.split("\\R");
        StringBuilder base64 = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("-----BEGIN") && !line.startsWith("-----END") && !line.isEmpty()) {
                base64.append(line);
            }
        }
        return Base64.getDecoder().decode(base64.toString());
    }
}
