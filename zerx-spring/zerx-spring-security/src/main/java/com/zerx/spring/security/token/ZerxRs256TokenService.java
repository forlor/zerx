package com.zerx.spring.security.token;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.security.props.ZerxSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
public class ZerxRs256TokenService implements ZerxTokenService {

    private static final Logger log = LoggerFactory.getLogger(ZerxRs256TokenService.class);

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

    /** RSA 私钥（用于签名） */
    private final PrivateKey privateKey;

    /** RSA 公钥（用于验证） */
    private final PublicKey publicKey;

    /** 安全配置属性 */
    private final ZerxSecurityProperties props;

    /** 缓存操作工具 */
    private final CacheOps cacheOps;

    /**
     * 构造 RS256 令牌服务
     * <p>
     * 从配置属性中加载 RSA 密钥对，支持 classpath、file 和 Base64 格式。
     * </p>
     *
     * @param props    安全配置属性
     * @param cacheOps 缓存操作工具
     * @throws IllegalArgumentException 如果密钥加载失败
     */
    public ZerxRs256TokenService(@Nonnull ZerxSecurityProperties props, @Nonnull CacheOps cacheOps) {
        this.props = props;
        this.cacheOps = cacheOps;
        var keyConfig = props.getJwt().getRsa();
        this.privateKey = loadPrivateKey(keyConfig.getPrivateKey());
        this.publicKey = loadPublicKey(keyConfig.getPublicKey());
        log.info("RS256 token service initialized successfully");
    }

    /**
     * 生成访问令牌（带角色信息）
     * <p>
     * 使用 RSA 私钥签名，subject = userId, claims = {jti, tokenType=access, roles=[...]},
     * 过期时间 = 当前时间 + accessTokenExpire
     * </p>
     */
    @Override
    public String generateAccessToken(Long userId, String jti, List<String> roles) {
        var jwtConfig = props.getJwt();
        var now = Instant.now();
        var expiresAt = now.plus(jwtConfig.getAccessTokenExpire());

        return Jwts.builder()
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
     * 使用 RSA 私钥签名刷新令牌。
     * </p>
     */
    @Override
    public String generateRefreshToken(Long userId, String jti) {
        var jwtConfig = props.getJwt();
        var now = Instant.now();
        var expiresAt = now.plus(jwtConfig.getRefreshTokenExpire());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_JTI, jti)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 RSA 公钥验证签名，解析 JWT 字符串。
     * 如果令牌中没有 roles 声明（旧令牌），默认返回空列表。
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public ZerxTokenClaims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
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
     *   <li>使用 RSA 公钥验证签名</li>
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
     *
     * @param userId 用户 ID
     * @return 令牌对
     */
    public ZerxTokenPair generateTokenPair(Long userId) {
        return generateTokenPair(userId, List.of());
    }

    /**
     * 生成新的令牌对（访问令牌 + 刷新令牌），带角色信息
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

    // ======================== 密钥加载工具方法 ========================

    /**
     * 加载 RSA 私钥
     * <p>
     * 支持三种格式：classpath 资源、文件路径、Base64 编码的 DER。
     * </p>
     *
     * @param keySource 密钥来源
     * @return RSA 私钥
     * @throws IllegalArgumentException 如果密钥加载或解析失败
     */
    private PrivateKey loadPrivateKey(String keySource) {
        try {
            byte[] keyBytes = loadKeyBytes(keySource);
            var keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load RSA private key: " + e.getMessage(), e);
        }
    }

    /**
     * 加载 RSA 公钥
     * <p>
     * 支持三种格式：classpath 资源、文件路径、Base64 编码的 DER。
     * </p>
     *
     * @param keySource 密钥来源
     * @return RSA 公钥
     * @throws IllegalArgumentException 如果密钥加载或解析失败
     */
    private PublicKey loadPublicKey(String keySource) {
        try {
            byte[] keyBytes = loadKeyBytes(keySource);
            var keySpec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load RSA public key: " + e.getMessage(), e);
        }
    }

    /**
     * 根据来源加载密钥字节
     * <p>
     * 支持：
     * <ul>
     *   <li>{@code classpath:} — 从类路径加载 PEM 或 DER 文件</li>
     *   <li>{@code file:} — 从文件系统加载 PEM 或 DER 文件</li>
     *   <li>其他 — 直接作为 Base64 编码的 DER 内容</li>
     * </ul>
     * </p>
     *
     * @param keySource 密钥来源
     * @return DER 编码的密钥字节
     * @throws IllegalArgumentException 如果加载失败
     */
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
            // Base64 编码的 DER
            return Base64.getDecoder().decode(keySource);
        }
    }

    /**
     * 解码 PEM 格式的密钥为 DER 字节
     * <p>
     * 自动剥离 PEM 头尾标记，仅保留 Base64 编码部分。
     * 如果输入不是 PEM 格式（无 BEGIN/END 标记），直接 Base64 解码。
     * </p>
     *
     * @param pemBytes PEM 格式的密钥字节
     * @return DER 编码的密钥字节
     */
    private byte[] decodePem(byte[] pemBytes) {
        String pem = new String(pemBytes, StandardCharsets.UTF_8);
        // 检查是否为 PEM 格式
        if (!pem.contains("-----BEGIN")) {
            // 不是 PEM 格式，直接 Base64 解码
            return Base64.getDecoder().decode(pem.trim());
        }
        // 剥离 PEM 头尾标记
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
