package com.zerx.component.oss.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zerx.component.oss.OssObject;
import com.zerx.component.oss.OssObjectMeta;
import com.zerx.component.oss.OssResult;
import com.zerx.component.oss.PresignedUrl;
import com.zerx.component.oss.properties.ZerxOssProperties;

import java.io.InputStream;

/**
 * {@link AbstractOssStorageService} 单元测试
 * <p>
 * 通过匿名子类测试模板方法中的路径生成、URL 构建、暂存键解析等公共逻辑。
 * </p>
 *
 * @author zerx
 */
class AbstractOssStorageServiceTest {

    private ZerxOssProperties props;
    private TestableAbstractService service;

    @BeforeEach
    void setUp() {
        props = new ZerxOssProperties();
        props.setEndpoint("https://oss.example.com");
        props.setBucket("my-bucket");
        props.setBasePath("uploads");
        props.setAccessKey("ak");
        props.setSecretKey("sk");
        service = new TestableAbstractService(props);
    }

    @Test
    @DisplayName("generateObjectKey 格式正确")
    void shouldGenerateCorrectObjectKey() {
        String key = service.testGenerateObjectKey("pdf");
        assertNotNull(key);
        assertTrue(key.startsWith("uploads/"));
        assertTrue(key.endsWith(".pdf"));

        // 验证日期部分
        String expectedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        assertTrue(key.contains(expectedDate));

        // 验证 UUID 部分（32 位十六进制）
        String[] parts = key.split("/");
        String uuidPart = parts[parts.length - 1].replace(".pdf", "");
        assertEquals(32, uuidPart.length());
    }

    @Test
    @DisplayName("generateObjectKey 无扩展名时使用默认值")
    void shouldUseDefaultExtensionWhenEmpty() {
        String key = service.testGenerateObjectKey("");
        assertNotNull(key);
        assertTrue(key.endsWith(".bin"));
    }

    @Test
    @DisplayName("generateObjectKey 无扩展名时使用默认值(null)")
    void shouldUseDefaultExtensionWhenNull() {
        String key = service.testGenerateObjectKey(null);
        assertNotNull(key);
        assertTrue(key.endsWith(".bin"));
    }

    @Test
    @DisplayName("generateObjectKey 自定义 basePath")
    void shouldUseCustomBasePath() {
        String key = service.testGenerateObjectKey("avatars", "jpg");
        assertTrue(key.startsWith("avatars/"));
        assertTrue(key.endsWith(".jpg"));
    }

    @Test
    @DisplayName("buildUrl 使用默认 endpoint")
    void shouldBuildUrlWithEndpoint() {
        String url = service.testBuildUrl("uploads/file.pdf");
        assertEquals("https://oss.example.com/my-bucket/uploads/file.pdf", url);
    }

    @Test
    @DisplayName("buildUrl 使用自定义域名")
    void shouldBuildUrlWithCustomDomain() {
        props.setCustomDomain("https://cdn.example.com");
        String url = service.testBuildUrl("uploads/file.pdf");
        assertEquals("https://cdn.example.com/uploads/file.pdf", url);
    }

    @Test
    @DisplayName("resolveStagingBucket DIRECTORY 模式返回主桶")
    void shouldResolveStagingBucket_to_main_in_directory_mode() {
        assertEquals("my-bucket", service.testResolveStagingBucket());
    }

    @Test
    @DisplayName("resolveStagingBucket BUCKET 模式返回暂存桶")
    void shouldResolveStagingBucket_to_staging_in_bucket_mode() {
        props.getStaging().setStrategy(ZerxOssProperties.Staging.Strategy.BUCKET);
        props.getStaging().setBucket("my-staging");
        assertEquals("my-staging", service.testResolveStagingBucket());
    }

    @Test
    @DisplayName("resolveStagingBucket BUCKET 模式自动生成暂存桶名")
    void shouldResolveStagingBucket_auto_generate() {
        props.getStaging().setStrategy(ZerxOssProperties.Staging.Strategy.BUCKET);
        assertEquals("my-bucket-staging", service.testResolveStagingBucket());
    }

    @Test
    @DisplayName("resolveStagingKey DIRECTORY 模式加前缀")
    void shouldResolveStagingKey_with_prefix() {
        assertEquals("_staging/token-123",
                props.getStaging().resolveStagingKey("token-123"));
    }

    @Test
    @DisplayName("resolveStagingKey BUCKET 模式直接返回 token")
    void shouldResolveStagingKey_without_prefix_in_bucket_mode() {
        props.getStaging().setStrategy(ZerxOssProperties.Staging.Strategy.BUCKET);
        assertEquals("token-123",
                props.getStaging().resolveStagingKey("token-123"));
    }

    @Test
    @DisplayName("getExtension 提取正确")
    void shouldExtractExtension() {
        assertEquals("pdf", service.testExtractExtension("report.pdf"));
        assertEquals("jpg", service.testExtractExtension("photo.jpg"));
        assertEquals("gz", service.testExtractExtension("archive.tar.gz"));
    }

    @Test
    @DisplayName("getExtension 无扩展名返回默认值")
    void shouldReturnDefaultWhenNoExtension() {
        assertEquals("bin", service.testExtractExtension("noextension"));
        assertEquals("bin", service.testExtractExtension(""));
        assertEquals("bin", service.testExtractExtension(null));
    }

    /**
     * 可测试的 AbstractOssStorageService 子类
     */
    private static class TestableAbstractService extends AbstractOssStorageService {

        protected TestableAbstractService(ZerxOssProperties properties) {
            super(properties);
        }

        String testGenerateObjectKey(String extension) {
            return generateObjectKey(extension);
        }

        String testGenerateObjectKey(String basePath, String extension) {
            return generateObjectKey(basePath, extension);
        }

        String testBuildUrl(String objectKey) {
            return buildUrl(objectKey);
        }

        String testResolveStagingBucket() {
            return resolveStagingBucket();
        }

        String testExtractExtension(String filename) {
            java.lang.reflect.Method method;
            try {
                method = AbstractOssStorageService.class.getDeclaredMethod(
                        "getExtension", String.class);
                method.setAccessible(true);
                return (String) method.invoke(this, filename);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected OssResult doPut(String bucket, String objectKey, InputStream input,
                                   String contentType, Map<String, String> metadata) {
            return null;
        }

        @Override
        protected OssObjectMeta doGetObjectMeta(String bucket, String objectKey) {
            return null;
        }

        @Override
        protected OssObject doGet(String bucket, String objectKey) {
            return null;
        }

        @Override
        protected boolean doExists(String bucket, String objectKey) {
            return false;
        }

        @Override
        protected void doDelete(String bucket, String objectKey) {
        }

        @Override
        protected int doDeleteBatch(String bucket, List<String> objectKeys) {
            return 0;
        }

        @Override
        protected OssResult doCopy(String sourceBucket, String sourceKey,
                                   String targetBucket, String targetKey) {
            return null;
        }

        @Override
        protected PresignedUrl doPresignPut(String objectKey, java.time.Duration expiry,
                                              Map<String, String> headers) {
            return null;
        }

        @Override
        protected PresignedUrl doPresignGet(String objectKey, java.time.Duration expiry) {
            return null;
        }

        @Override
        protected String doBuildUrl(String objectKey) {
            return buildUrl(objectKey);
        }

        @Override
        protected Map<String, String> doGetUserMetadata(String bucket, String objectKey) {
            return Map.of();
        }

        @Override
        protected int doPurgeExpired(String bucket, String prefix, Instant cutoff) {
            return 0;
        }
    }
}
