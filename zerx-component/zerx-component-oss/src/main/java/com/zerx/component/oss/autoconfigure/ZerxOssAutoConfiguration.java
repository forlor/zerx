package com.zerx.component.oss.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zerx.component.oss.OssStorageService;
import com.zerx.component.oss.impl.AliyunOssStorageService;
import com.zerx.component.oss.impl.MinioOssStorageService;
import com.zerx.component.oss.impl.S3OssStorageService;
import com.zerx.component.oss.impl.TencentCosStorageService;
import com.zerx.component.oss.properties.ZerxOssProperties;

/**
 * Zerx 对象存储自动配置。
 * <p>
 * 根据 {@code zerx.oss.type} 配置自动注册对应的 {@link OssStorageService} 实现：
 * <ul>
 *     <li>{@code MINIO} — MinIO 对象存储</li>
 *     <li>{@code ALIYUN} — 阿里云 OSS</li>
 *     <li>{@code TENCENT} — 腾讯云 COS</li>
 *     <li>{@code S3} — 通用 S3 协议（AWS / DigitalOcean 等）</li>
 * </ul>
 * </p>
 * <p>
 * 每种实现仅在对应的 SDK 类存在时才会激活，
 * 未引入相应 SDK 依赖时不会影响应用启动。
 * </p>
 *
 * @author zerx
 * @see OssStorageService
 * @see ZerxOssProperties
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zerx.oss", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZerxOssProperties.class)
public class ZerxOssAutoConfiguration {

    // ======================== MinIO 配置 ========================

    /**
     * MinIO 对象存储配置。
     * <p>
     * 当 {@code zerx.oss.type=MINIO} 且 classpath 中存在 {@code io.minio.MinioClient} 时激活。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.oss", name = "type", havingValue = "MINIO")
    @ConditionalOnClass(name = "io.minio.MinioClient")
    static class MinioOssConfiguration {

        @Bean
        @ConditionalOnMissingBean(OssStorageService.class)
        public OssStorageService ossStorageService(ZerxOssProperties properties) {
            return new MinioOssStorageService(properties);
        }
    }

    // ======================== 阿里云 OSS 配置 ========================

    /**
     * 阿里云 OSS 配置。
     * <p>
     * 当 {@code zerx.oss.type=ALIYUN} 且 classpath 中存在 {@code com.aliyun.oss.OSS} 时激活。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.oss", name = "type", havingValue = "ALIYUN")
    @ConditionalOnClass(name = "com.aliyun.oss.OSS")
    static class AliyunOssConfiguration {

        @Bean
        @ConditionalOnMissingBean(OssStorageService.class)
        public OssStorageService ossStorageService(ZerxOssProperties properties) {
            return new AliyunOssStorageService(properties);
        }
    }

    // ======================== 腾讯云 COS 配置 ========================

    /**
     * 腾讯云 COS 配置。
     * <p>
     * 当 {@code zerx.oss.type=TENCENT} 且 classpath 中存在 {@code com.qcloud.cos.COSClient} 时激活。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.oss", name = "type", havingValue = "TENCENT")
    @ConditionalOnClass(name = "com.qcloud.cos.COSClient")
    static class TencentCosConfiguration {

        @Bean
        @ConditionalOnMissingBean(OssStorageService.class)
        public OssStorageService ossStorageService(ZerxOssProperties properties) {
            return new TencentCosStorageService(properties);
        }
    }

    // ======================== 通用 S3 配置 ========================

    /**
     * 通用 S3 协议配置。
     * <p>
     * 当 {@code zerx.oss.type=S3} 且 classpath 中存在
     * {@code com.amazonaws.services.s3.AmazonS3} 时激活。
     * 适用于 AWS、DigitalOcean Spaces 等 S3 兼容对象存储服务。
     * </p>
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.oss", name = "type", havingValue = "S3")
    @ConditionalOnClass(name = "com.amazonaws.services.s3.AmazonS3")
    static class S3OssConfiguration {

        @Bean
        @ConditionalOnMissingBean(OssStorageService.class)
        public OssStorageService ossStorageService(ZerxOssProperties properties) {
            return new S3OssStorageService(properties);
        }
    }
}
