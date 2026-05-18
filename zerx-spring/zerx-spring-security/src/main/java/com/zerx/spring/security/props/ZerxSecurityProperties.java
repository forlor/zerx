package com.zerx.spring.security.props;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zerx 安全模块配置属性
 * <p>
 * 通过 {@code application.yml} 中 {@code zerx.security.*} 前缀进行配置，
 * 控制 JWT 签发策略、免认证 URL、跨域策略等安全行为。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   security:
 *     enabled: true
 *     permit-urls:
 *       - /auth/login
 *       - /auth/register
 *     jwt:
 *       algorithm: HS256
 *       secret: my-secret-key-at-least-256-bits
 *       access-token-expire: 2h
 *       refresh-token-expire: 7d
 *       rsa:
 *         public-key: classpath:keys/public.pem
 *         private-key: classpath:keys/private.pem
 *     cors:
 *       allowed-origins:
 *         - "*"
 *       allowed-methods:
 *         - GET
 *         - POST
 * }</pre>
 *
 * @author zerx
 */
@ConfigurationProperties(prefix = "zerx.security")
public class ZerxSecurityProperties {

    /** 是否启用安全模块 */
    private boolean enabled = true;

    /** JWT 相关配置 */
    private Jwt jwt = new Jwt();

    /** 免认证 URL 列表（精确匹配与 Ant 风格路径） */
    private List<String> permitUrls = List.of("/auth/login", "/auth/register", "/doc.html");

    /** CORS 跨域配置 */
    private Cors cors = new Cors();

    /**
     * 获取安全模块启用状态
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置安全模块启用状态
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取 JWT 配置
     *
     * @return JWT 配置对象
     */
    public Jwt getJwt() {
        return jwt;
    }

    /**
     * 设置 JWT 配置
     *
     * @param jwt JWT 配置对象
     */
    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    /**
     * 获取免认证 URL 列表
     *
     * @return 免认证 URL 列表
     */
    public List<String> getPermitUrls() {
        return permitUrls;
    }

    /**
     * 设置免认证 URL 列表
     *
     * @param permitUrls 免认证 URL 列表
     */
    public void setPermitUrls(List<String> permitUrls) {
        this.permitUrls = permitUrls;
    }

    /**
     * 获取 CORS 跨域配置
     *
     * @return CORS 配置对象
     */
    public Cors getCors() {
        return cors;
    }

    /**
     * 设置 CORS 跨域配置
     *
     * @param cors CORS 配置对象
     */
    public void setCors(Cors cors) {
        this.cors = cors;
    }

    // ======================== JWT 内部配置类 ========================

    /**
     * JWT 令牌配置
     * <p>
     * 控制访问令牌和刷新令牌的签发策略、有效期等参数。
     * 支持 HS256（默认）和 RS256 算法。
     * </p>
     *
     * @author zerx
     */
    public static class Jwt {

        /** 签名算法，默认 HS256 */
        private String algorithm = "HS256";

        /** 签名密钥（HS256 至少 256 位 / 32 字节） */
        private String secret = "zerx-default-secret-key-must-be-at-least-256-bits!";

        /** 访问令牌有效期，默认 2 小时 */
        private Duration accessTokenExpire = Duration.ofHours(2);

        /** 刷新令牌有效期，默认 7 天 */
        private Duration refreshTokenExpire = Duration.ofDays(7);

        /** 令牌签发者 */
        private String issuer = "zerx";

        /** 令牌请求头名称 */
        private String headerName = "Authorization";

        /** 令牌请求头前缀 */
        private String headerPrefix = "Bearer ";

        /** RSA 密钥配置（RS256 算法时使用） */
        private Rsa rsa = new Rsa();

        /**
         * 获取签名算法
         *
         * @return 签名算法（HS256 或 RS256）
         */
        public String getAlgorithm() {
            return algorithm;
        }

        /**
         * 设置签名算法
         *
         * @param algorithm 签名算法（HS256 或 RS256）
         */
        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        /**
         * 获取签名密钥
         *
         * @return 签名密钥
         */
        public String getSecret() {
            return secret;
        }

        /**
         * 设置签名密钥
         *
         * @param secret 签名密钥
         */
        public void setSecret(String secret) {
            this.secret = secret;
        }

        /**
         * 获取访问令牌有效期
         *
         * @return 访问令牌有效期
         */
        public Duration getAccessTokenExpire() {
            return accessTokenExpire;
        }

        /**
         * 设置访问令牌有效期
         *
         * @param accessTokenExpire 访问令牌有效期
         */
        public void setAccessTokenExpire(Duration accessTokenExpire) {
            this.accessTokenExpire = accessTokenExpire;
        }

        /**
         * 获取刷新令牌有效期
         *
         * @return 刷新令牌有效期
         */
        public Duration getRefreshTokenExpire() {
            return refreshTokenExpire;
        }

        /**
         * 设置刷新令牌有效期
         *
         * @param refreshTokenExpire 刷新令牌有效期
         */
        public void setRefreshTokenExpire(Duration refreshTokenExpire) {
            this.refreshTokenExpire = refreshTokenExpire;
        }

        /**
         * 获取令牌签发者
         *
         * @return 令牌签发者
         */
        public String getIssuer() {
            return issuer;
        }

        /**
         * 设置令牌签发者
         *
         * @param issuer 令牌签发者
         */
        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        /**
         * 获取令牌请求头名称
         *
         * @return 请求头名称
         */
        public String getHeaderName() {
            return headerName;
        }

        /**
         * 设置令牌请求头名称
         *
         * @param headerName 请求头名称
         */
        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        /**
         * 获取令牌请求头前缀
         *
         * @return 请求头前缀
         */
        public String getHeaderPrefix() {
            return headerPrefix;
        }

        /**
         * 设置令牌请求头前缀
         *
         * @param headerPrefix 请求头前缀
         */
        public void setHeaderPrefix(String headerPrefix) {
            this.headerPrefix = headerPrefix;
        }

        /**
         * 获取 RSA 密钥配置
         *
         * @return RSA 密钥配置
         */
        public Rsa getRsa() {
            return rsa;
        }

        /**
         * 设置 RSA 密钥配置
         *
         * @param rsa RSA 密钥配置
         */
        public void setRsa(Rsa rsa) {
            this.rsa = rsa;
        }
    }

    // ======================== RSA 内部配置类 ========================

    /**
     * RSA 密钥配置
     * <p>
     * 配置 RS256 算法所需的 RSA 公钥和私钥。
     * 支持以下格式：
     * <ul>
     *   <li>{@code classpath:} — 类路径资源（如 classpath:keys/public.pem）</li>
     *   <li>{@code file:} — 文件路径（如 file:/etc/zerx/private.pem）</li>
     *   <li>Base64 编码的 DER 格式</li>
     * </ul>
     * </p>
     *
     * @author zerx
     */
    public static class Rsa {

        /** RSA 公钥（classpath:, file:, 或 base64） */
        private String publicKey;

        /** RSA 私钥（classpath:, file:, 或 base64） */
        private String privateKey;

        /**
         * 获取 RSA 公钥
         *
         * @return RSA 公钥配置
         */
        public String getPublicKey() {
            return publicKey;
        }

        /**
         * 设置 RSA 公钥
         *
         * @param publicKey RSA 公钥配置
         */
        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        /**
         * 获取 RSA 私钥
         *
         * @return RSA 私钥配置
         */
        public String getPrivateKey() {
            return privateKey;
        }

        /**
         * 设置 RSA 私钥
         *
         * @param privateKey RSA 私钥配置
         */
        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }
    }

    // ======================== CORS 内部配置类 ========================

    /**
     * CORS 跨域配置
     * <p>
     * 控制跨域资源共享策略，包括允许的源、方法、头部等。
     * </p>
     *
     * @author zerx
     */
    public static class Cors {

        /** 允许的源，默认允许所有 */
        private List<String> allowedOrigins = List.of("*");

        /** 允许的 HTTP 方法 */
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

        /** 允许的请求头 */
        private List<String> allowedHeaders = List.of("*");

        /** 是否允许携带凭证 */
        private boolean allowCredentials = true;

        /** 预检请求缓存时间（秒） */
        private long maxAge = 3600;

        /**
         * 获取允许的源列表
         *
         * @return 允许的源列表
         */
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        /**
         * 设置允许的源列表
         *
         * @param allowedOrigins 允许的源列表
         */
        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        /**
         * 获取允许的 HTTP 方法列表
         *
         * @return 允许的 HTTP 方法列表
         */
        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        /**
         * 设置允许的 HTTP 方法列表
         *
         * @param allowedMethods 允许的 HTTP 方法列表
         */
        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        /**
         * 获取允许的请求头列表
         *
         * @return 允许的请求头列表
         */
        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        /**
         * 设置允许的请求头列表
         *
         * @param allowedHeaders 允许的请求头列表
         */
        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        /**
         * 是否允许携带凭证
         *
         * @return 是否允许携带凭证
         */
        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        /**
         * 设置是否允许携带凭证
         *
         * @param allowCredentials 是否允许携带凭证
         */
        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        /**
         * 获取预检请求缓存时间（秒）
         *
         * @return 预检请求缓存时间
         */
        public long getMaxAge() {
            return maxAge;
        }

        /**
         * 设置预检请求缓存时间（秒）
         *
         * @param maxAge 预检请求缓存时间
         */
        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }
}
