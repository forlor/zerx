package com.zerx.component.oss.properties;

import com.zerx.component.oss.properties.ZerxOssProperties.OssType;
import com.zerx.component.oss.properties.ZerxOssProperties.Staging;
import com.zerx.component.oss.properties.ZerxOssProperties.Staging.Strategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ZerxOssProperties 对象存储配置属性测试")
class ZerxOssPropertiesTest {

    @Nested
    @DisplayName("默认值")
    class DefaultValues {

        @Test
        @DisplayName("enabled 默认应为 true")
        void should_have_enabled_default_true() {
            var props = new ZerxOssProperties();
            assertTrue(props.isEnabled());
        }

        @Test
        @DisplayName("basePath 默认应为空字符串")
        void should_have_basePath_default_empty() {
            var props = new ZerxOssProperties();
            assertEquals("", props.getBasePath());
        }

        @Test
        @DisplayName("autoCreateBucket 默认应为 false")
        void should_have_autoCreateBucket_default_false() {
            var props = new ZerxOssProperties();
            assertFalse(props.isAutoCreateBucket());
        }

        @Test
        @DisplayName("signedUrlExpiry 默认应为 1 小时")
        void should_have_signedUrlExpiry_default_one_hour() {
            var props = new ZerxOssProperties();
            assertEquals(Duration.ofHours(1), props.getSignedUrlExpiry());
        }

        @Test
        @DisplayName("type 默认应为 null")
        void should_have_type_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getType());
        }

        @Test
        @DisplayName("endpoint 默认应为 null")
        void should_have_endpoint_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getEndpoint());
        }

        @Test
        @DisplayName("accessKey 默认应为 null")
        void should_have_accessKey_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getAccessKey());
        }

        @Test
        @DisplayName("secretKey 默认应为 null")
        void should_have_secretKey_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getSecretKey());
        }

        @Test
        @DisplayName("bucket 默认应为 null")
        void should_have_bucket_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getBucket());
        }

        @Test
        @DisplayName("customDomain 默认应为 null")
        void should_have_customDomain_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getCustomDomain());
        }

        @Test
        @DisplayName("region 默认应为 null")
        void should_have_region_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getRegion());
        }

        @Test
        @DisplayName("staging 默认不应为 null")
        void should_have_staging_default_not_null() {
            var props = new ZerxOssProperties();
            assertNotNull(props.getStaging());
        }

        @Test
        @DisplayName("staging.strategy 默认应为 DIRECTORY")
        void should_have_staging_strategy_default_directory() {
            var props = new ZerxOssProperties();
            assertEquals(Strategy.DIRECTORY, props.getStaging().getStrategy());
        }
    }

    @Nested
    @DisplayName("Getter / Setter")
    class GetterSetter {

        @Test
        @DisplayName("应正确设置和获取 enabled")
        void should_set_and_get_enabled() {
            var props = new ZerxOssProperties();
            props.setEnabled(false);
            assertFalse(props.isEnabled());

            props.setEnabled(true);
            assertTrue(props.isEnabled());
        }

        @Test
        @DisplayName("应正确设置和获取 type")
        void should_set_and_get_type() {
            var props = new ZerxOssProperties();
            props.setType(OssType.MINIO);
            assertEquals(OssType.MINIO, props.getType());

            props.setType(OssType.ALIYUN);
            assertEquals(OssType.ALIYUN, props.getType());

            props.setType(OssType.TENCENT);
            assertEquals(OssType.TENCENT, props.getType());

            props.setType(OssType.S3);
            assertEquals(OssType.S3, props.getType());
        }

        @Test
        @DisplayName("应正确设置和获取 endpoint")
        void should_set_and_get_endpoint() {
            var props = new ZerxOssProperties();
            props.setEndpoint("https://oss.example.com");
            assertEquals("https://oss.example.com", props.getEndpoint());
        }

        @Test
        @DisplayName("应正确设置和获取 accessKey")
        void should_set_and_get_accessKey() {
            var props = new ZerxOssProperties();
            props.setAccessKey("ak-test");
            assertEquals("ak-test", props.getAccessKey());
        }

        @Test
        @DisplayName("应正确设置和获取 secretKey")
        void should_set_and_get_secretKey() {
            var props = new ZerxOssProperties();
            props.setSecretKey("sk-test");
            assertEquals("sk-test", props.getSecretKey());
        }

        @Test
        @DisplayName("应正确设置和获取 bucket")
        void should_set_and_get_bucket() {
            var props = new ZerxOssProperties();
            props.setBucket("my-bucket");
            assertEquals("my-bucket", props.getBucket());
        }

        @Test
        @DisplayName("应正确设置和获取 customDomain")
        void should_set_and_get_customDomain() {
            var props = new ZerxOssProperties();
            props.setCustomDomain("https://cdn.example.com");
            assertEquals("https://cdn.example.com", props.getCustomDomain());
        }

        @Test
        @DisplayName("应正确设置和获取 basePath")
        void should_set_and_get_basePath() {
            var props = new ZerxOssProperties();
            props.setBasePath("uploads/");
            assertEquals("uploads/", props.getBasePath());
        }

        @Test
        @DisplayName("应正确设置和获取 autoCreateBucket")
        void should_set_and_get_autoCreateBucket() {
            var props = new ZerxOssProperties();
            props.setAutoCreateBucket(true);
            assertTrue(props.isAutoCreateBucket());
        }

        @Test
        @DisplayName("应正确设置和获取 signedUrlExpiry")
        void should_set_and_get_signedUrlExpiry() {
            var props = new ZerxOssProperties();
            props.setSignedUrlExpiry(Duration.ofMinutes(30));
            assertEquals(Duration.ofMinutes(30), props.getSignedUrlExpiry());
        }

        @Test
        @DisplayName("应正确设置和获取 region")
        void should_set_and_get_region() {
            var props = new ZerxOssProperties();
            props.setRegion("cn-hangzhou");
            assertEquals("cn-hangzhou", props.getRegion());
        }

        @Test
        @DisplayName("应正确设置和获取 staging")
        void should_set_and_get_staging() {
            var props = new ZerxOssProperties();
            var newStaging = new Staging();
            newStaging.setBucket("staging-bucket");
            props.setStaging(newStaging);

            assertEquals("staging-bucket", props.getStaging().getBucket());
        }
    }

    @Nested
    @DisplayName("Staging 暂存配置")
    class StagingConfig {

        @Test
        @DisplayName("prefix 默认应为 _staging/")
        void should_have_prefix_default_staging() {
            var props = new ZerxOssProperties();
            assertEquals("_staging/", props.getStaging().getPrefix());
        }

        @Test
        @DisplayName("defaultTtl 默认应为 24 小时")
        void should_have_defaultTtl_default_24h() {
            var props = new ZerxOssProperties();
            assertEquals(Duration.ofHours(24), props.getStaging().getDefaultTtl());
        }

        @Test
        @DisplayName("bucket 默认应为 null")
        void should_have_bucket_default_null() {
            var props = new ZerxOssProperties();
            assertNull(props.getStaging().getBucket());
        }

        @Test
        @DisplayName("应正确设置和获取 staging.bucket")
        void should_set_and_get_staging_bucket() {
            var props = new ZerxOssProperties();
            props.getStaging().setBucket("staging-bucket");
            assertEquals("staging-bucket", props.getStaging().getBucket());
        }

        @Test
        @DisplayName("应正确设置和获取 staging.prefix")
        void should_set_and_get_staging_prefix() {
            var props = new ZerxOssProperties();
            props.getStaging().setPrefix("_tmp/");
            assertEquals("_tmp/", props.getStaging().getPrefix());
        }

        @Test
        @DisplayName("应正确设置和获取 staging.defaultTtl")
        void should_set_and_get_staging_defaultTtl() {
            var props = new ZerxOssProperties();
            props.getStaging().setDefaultTtl(Duration.ofHours(48));
            assertEquals(Duration.ofHours(48), props.getStaging().getDefaultTtl());
        }

        @Test
        @DisplayName("应支持整体替换 staging 对象")
        void should_support_replace_entire_staging() {
            var props = new ZerxOssProperties();
            var newStaging = new Staging();
            newStaging.setBucket("new-staging-bucket");
            newStaging.setPrefix("_new/");
            newStaging.setDefaultTtl(Duration.ofHours(12));
            newStaging.setStrategy(Strategy.BUCKET);
            props.setStaging(newStaging);

            assertEquals("new-staging-bucket", props.getStaging().getBucket());
            assertEquals("_new/", props.getStaging().getPrefix());
            assertEquals(Duration.ofHours(12), props.getStaging().getDefaultTtl());
            assertEquals(Strategy.BUCKET, props.getStaging().getStrategy());
        }
    }

    @Nested
    @DisplayName("Staging 策略路由方法")
    class StagingStrategyRouting {

        @Test
        @DisplayName("DIRECTORY 模式：resolveBucket 返回主桶")
        void should_resolveBucket_to_main_in_directory_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.DIRECTORY);
            assertEquals("my-bucket", staging.resolveBucket("my-bucket"));
        }

        @Test
        @DisplayName("BUCKET 模式：resolveBucket 返回配置的暂存桶")
        void should_resolveBucket_to_configured_in_bucket_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.BUCKET);
            staging.setBucket("my-staging-bucket");
            assertEquals("my-staging-bucket", staging.resolveBucket("my-bucket"));
        }

        @Test
        @DisplayName("BUCKET 模式：未配置桶名时自动生成 -staging 后缀")
        void should_auto_generate_staging_bucket_name() {
            var staging = new Staging();
            staging.setStrategy(Strategy.BUCKET);
            assertEquals("my-bucket-staging", staging.resolveBucket("my-bucket"));
        }

        @Test
        @DisplayName("DIRECTORY 模式：resolveStagingKey 返回前缀+token")
        void should_resolveStagingKey_with_prefix_in_directory_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.DIRECTORY);
            staging.setPrefix("_staging/");
            assertEquals("_staging/abc-123", staging.resolveStagingKey("abc-123"));
        }

        @Test
        @DisplayName("BUCKET 模式：resolveStagingKey 直接返回 token")
        void should_resolveStagingKey_as_token_in_bucket_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.BUCKET);
            assertEquals("abc-123", staging.resolveStagingKey("abc-123"));
        }

        @Test
        @DisplayName("DIRECTORY 模式：isStagingKey 根据前缀判断")
        void should_isStagingKey_by_prefix_in_directory_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.DIRECTORY);
            staging.setPrefix("_staging/");
            assertTrue(staging.isStagingKey("_staging/abc"));
            assertFalse(staging.isStagingKey("uploads/file.pdf"));
            assertFalse(staging.isStagingKey(null));
        }

        @Test
        @DisplayName("BUCKET 模式：isStagingKey 始终返回 true")
        void should_isStagingKey_always_true_in_bucket_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.BUCKET);
            assertTrue(staging.isStagingKey("anything"));
            assertTrue(staging.isStagingKey("abc-123"));
        }

        @Test
        @DisplayName("DIRECTORY 模式：resolveScanPrefix 返回前缀")
        void should_resolveScanPrefix_as_prefix_in_directory_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.DIRECTORY);
            staging.setPrefix("_staging/");
            assertEquals("_staging/", staging.resolveScanPrefix());
        }

        @Test
        @DisplayName("BUCKET 模式：resolveScanPrefix 返回空字符串")
        void should_resolveScanPrefix_as_empty_in_bucket_mode() {
            var staging = new Staging();
            staging.setStrategy(Strategy.BUCKET);
            assertEquals("", staging.resolveScanPrefix());
        }

        @Test
        @DisplayName("resolveScanPrefix prefix 为 null 时返回空字符串")
        void should_resolveScanPrefix_empty_when_prefix_null() {
            var staging = new Staging();
            staging.setStrategy(Strategy.DIRECTORY);
            staging.setPrefix(null);
            assertEquals("", staging.resolveScanPrefix());
        }
    }

    @Nested
    @DisplayName("OssType 枚举")
    class OssTypeEnum {

        @Test
        @DisplayName("应包含 MINIO 枚举值")
        void should_have_minio_value() {
            assertNotNull(OssType.MINIO);
            assertEquals("MINIO", OssType.MINIO.name());
        }

        @Test
        @DisplayName("应包含 ALIYUN 枚举值")
        void should_have_aliyun_value() {
            assertNotNull(OssType.ALIYUN);
            assertEquals("ALIYUN", OssType.ALIYUN.name());
        }

        @Test
        @DisplayName("应包含 TENCENT 枚举值")
        void should_have_tencent_value() {
            assertNotNull(OssType.TENCENT);
            assertEquals("TENCENT", OssType.TENCENT.name());
        }

        @Test
        @DisplayName("应包含 S3 枚举值")
        void should_have_s3_value() {
            assertNotNull(OssType.S3);
            assertEquals("S3", OssType.S3.name());
        }

        @Test
        @DisplayName("枚举值数量应为 4 个")
        void should_have_four_values() {
            assertEquals(4, OssType.values().length);
        }

        @Test
        @DisplayName("valueOf 应能正确反查枚举值")
        void should_lookup_by_valueOf() {
            assertEquals(OssType.MINIO, OssType.valueOf("MINIO"));
            assertEquals(OssType.ALIYUN, OssType.valueOf("ALIYUN"));
            assertEquals(OssType.TENCENT, OssType.valueOf("TENCENT"));
            assertEquals(OssType.S3, OssType.valueOf("S3"));
        }
    }

    @Nested
    @DisplayName("Strategy 枚举")
    class StrategyEnum {

        @Test
        @DisplayName("应包含 DIRECTORY 枚举值")
        void should_have_directory_value() {
            assertNotNull(Strategy.DIRECTORY);
            assertEquals("DIRECTORY", Strategy.DIRECTORY.name());
        }

        @Test
        @DisplayName("应包含 BUCKET 枚举值")
        void should_have_bucket_value() {
            assertNotNull(Strategy.BUCKET);
            assertEquals("BUCKET", Strategy.BUCKET.name());
        }

        @Test
        @DisplayName("枚举值数量应为 2 个")
        void should_have_two_values() {
            assertEquals(2, Strategy.values().length);
        }
    }
}
