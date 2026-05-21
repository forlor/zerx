package com.zerx.component.oss.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zerx 对象存储配置属性。
 * <p>
 * 通过 {@code zerx.oss.*} 前缀在 application.yml 中配置。
 * 支持多种对象存储后端：MinIO、阿里云 OSS、腾讯云 COS、通用 S3 协议。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   oss:
 *     enabled: true
 *     type: MINIO
 *     endpoint: "https://oss.example.com"
 *     access-key: "your-access-key"
 *     secret-key: "your-secret-key"
 *     bucket: "my-bucket"
 *     custom-domain: "https://cdn.example.com"   # 可选，覆盖 endpoint 生成的 URL
 *     base-path: ""                                # 上传基础路径前缀
 *     auto-create-bucket: false                    # 是否自动创建存储桶
 *     signed-url-expiry: 1h                        # 预签名 URL 默认过期时间
 *     region: ""                                   # 区域（阿里云/腾讯 COS 必填）
 *     staging:
 *       bucket: ""                                 # 暂存桶（可选，不配则用主桶同目录暂存）
 *       prefix: "_staging/"                        # 暂存目录前缀
 *       default-ttl: 24h                           # 默认暂存有效期
 * }</pre>
 *
 * @author zerx
 * @see com.zerx.component.oss.OssStorageService
 */
@ConfigurationProperties(prefix = "zerx.oss")
public class ZerxOssProperties {

    /** 是否启用对象存储，默认 {@code true} */
    private boolean enabled = true;

    /** 存储类型 */
    private OssType type;

    /** 访问端点 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 密钥 */
    private String secretKey;

    /** 存储桶名称 */
    private String bucket;

    /** 自定义域名（覆盖 endpoint 生成的 URL），可选 */
    private String customDomain;

    /** 上传基础路径前缀，默认 "" */
    private String basePath = "";

    /** 自动创建存储桶，默认 false */
    private boolean autoCreateBucket = false;

    /** 签名 URL 默认过期时间，默认 1h */
    private Duration signedUrlExpiry = Duration.ofHours(1);

    /** 区域（阿里云/腾讯 COS 必填），可选 */
    private String region;

    /** 暂存配置 */
    private Staging staging = new Staging();

    // ======================== getter/setter ========================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public OssType getType() {
        return type;
    }

    public void setType(OssType type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean isAutoCreateBucket() {
        return autoCreateBucket;
    }

    public void setAutoCreateBucket(boolean autoCreateBucket) {
        this.autoCreateBucket = autoCreateBucket;
    }

    public Duration getSignedUrlExpiry() {
        return signedUrlExpiry;
    }

    public void setSignedUrlExpiry(Duration signedUrlExpiry) {
        this.signedUrlExpiry = signedUrlExpiry;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Staging getStaging() {
        return staging;
    }

    public void setStaging(Staging staging) {
        this.staging = staging;
    }

    // ======================== 枚举定义 ========================

    /**
     * 对象存储类型枚举。
     */
    public enum OssType {
        /** MinIO 对象存储 */
        MINIO,
        /** 阿里云 OSS */
        ALIYUN,
        /** 腾讯云 COS */
        TENCENT,
        /** 通用 S3 协议（AWS / DigitalOcean 等） */
        S3
    }

    // ======================== 暂存配置 ========================

    /**
     * 暂存配置。
     * <p>
     * 暂存机制允许客户端先上传文件到暂存区，服务端校验通过后再确认到正式路径。
     * </p>
     */
    public static class Staging {
        /** 暂存桶（可选，不配则用主桶同目录暂存） */
        private String bucket;
        /** 暂存目录前缀，默认 _staging/ */
        private String prefix = "_staging/";
        /** 默认暂存有效期，默认 24h */
        private Duration defaultTtl = Duration.ofHours(24);

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }
    }
}
