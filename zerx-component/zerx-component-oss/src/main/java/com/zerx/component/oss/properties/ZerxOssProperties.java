package com.zerx.component.oss.properties;

import java.time.Duration;

import com.zerx.common.util.StringUtil;

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
 *       strategy: DIRECTORY                        # 暂存策略：DIRECTORY（同桶目录）| BUCKET（独立桶）
 *       bucket: "my-bucket-staging"                # BUCKET 模式的暂存桶名（可选，默认 {主桶}-staging）
 *       prefix: "_staging/"                        # DIRECTORY 模式的暂存目录前缀
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
     * 支持两种隔离策略：
     * <ul>
     *   <li>{@link Staging.Strategy#DIRECTORY}（默认）：同桶的指定前缀目录下隔离暂存文件</li>
     *   <li>{@link Staging.Strategy#BUCKET}：使用独立存储桶隔离暂存文件，物理隔离更彻底</li>
     * </ul>
     * </p>
     */
    public static class Staging {

        /**
         * 暂存隔离策略枚举。
         */
        public enum Strategy {
            /**
             * 目录级别隔离（默认）。
             * <p>
             * 暂存文件存储在主桶的指定前缀目录下（如 {@code _staging/{uuid}}），
             * 与正式文件通过目录前缀区分。配置简单，不需要额外创建存储桶。
             * </p>
             */
            DIRECTORY,
            /**
             * 桶级别隔离。
             * <p>
             * 暂存文件存储在独立的暂存桶中（如 {@code my-bucket-staging/{uuid}}），
             * 与正式文件通过存储桶物理隔离。可独立管理生命周期策略，隔离更彻底。
             * </p>
             */
            BUCKET
        }

        /** 暂存隔离策略，默认 DIRECTORY */
        private Strategy strategy = Strategy.DIRECTORY;

        /**
         * BUCKET 模式的暂存桶名，可选。
         * <p>
         * 仅在 {@link #strategy} 为 {@link Strategy#BUCKET} 时生效。
         * 如果未配置，默认使用 "{@code 主桶名}-staging"。
         * </p>
         */
        private String bucket;

        /**
         * DIRECTORY 模式的暂存目录前缀，默认 "_staging/"。
         * <p>
         * 仅在 {@link #strategy} 为 {@link Strategy#DIRECTORY} 时生效。
         * 暂存对象的完整路径为 {@code {prefix}{stageToken}}。
         * </p>
         */
        private String prefix = "_staging/";

        /** 默认暂存有效期，默认 24h */
        private Duration defaultTtl = Duration.ofHours(24);

        public Strategy getStrategy() {
            return strategy;
        }

        public void setStrategy(Strategy strategy) {
            this.strategy = strategy;
        }

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

        /**
         * 获取暂存使用的存储桶名称。
         * <p>
         * 根据策略返回：
         * <ul>
         *   <li>{@link Strategy#DIRECTORY}：返回主存储桶名称</li>
         *   <li>{@link Strategy#BUCKET}：返回配置的暂存桶名，
         *       未配置时自动生成 "{@code 主桶名}-staging"</li>
         * </ul>
         * </p>
         *
         * @param mainBucket 主存储桶名称
         * @return 暂存存储桶名称
         */
        public String resolveBucket(String mainBucket) {
            if (strategy == Strategy.BUCKET) {
                return StringUtil.isNotBlank(this.bucket) ? this.bucket : mainBucket + "-staging";
            }
            return mainBucket;
        }

        /**
         * 根据暂存令牌生成暂存对象键。
         * <p>
         * 根据策略返回：
         * <ul>
         *   <li>{@link Strategy#DIRECTORY}：返回 {@code {prefix}{stageToken}}
         *       （如 {@code _staging/550e8400-e29b-41d4}）</li>
         *   <li>{@link Strategy#BUCKET}：直接返回 {@code stageToken}
         *       （独立桶中不需要目录前缀）</li>
         * </ul>
         * </p>
         *
         * @param stageToken 暂存令牌（UUID）
         * @return 暂存对象键
         */
        public String resolveStagingKey(String stageToken) {
            if (strategy == Strategy.BUCKET) {
                return stageToken;
            }
            return prefix + stageToken;
        }

        /**
         * 判断对象键是否为暂存对象。
         * <p>
         * 用于子类根据对象键路由到正确的存储桶。
         * </p>
         *
         * @param objectKey 对象键
         * @return 如果对象键属于暂存范围则返回 {@code true}
         */
        public boolean isStagingKey(String objectKey) {
            if (strategy == Strategy.BUCKET) {
                return true; // 暂存桶中的所有对象都是暂存对象
            }
            return objectKey != null && objectKey.startsWith(prefix);
        }

        /**
         * 获取暂存扫描前缀。
         * <p>
         * 用于 {@code purgeExpiredStages} 列举暂存对象：
         * <ul>
         *   <li>{@link Strategy#DIRECTORY}：返回 {@code prefix}（如 {@code _staging/}）</li>
         *   <li>{@link Strategy#BUCKET}：返回空字符串（列举暂存桶全部对象）</li>
         * </ul>
         * </p>
         *
         * @return 扫描前缀，不会返回 {@code null}
         */
        public String resolveScanPrefix() {
            if (strategy == Strategy.BUCKET) {
                return "";
            }
            return prefix != null ? prefix : "";
        }
    }
}
