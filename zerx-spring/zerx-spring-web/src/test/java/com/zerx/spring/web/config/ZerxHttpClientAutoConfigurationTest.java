package com.zerx.spring.web.config;

import com.zerx.spring.web.properties.ZerxWebProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ZerxWebProperties.HttpClient 配置属性测试
 */
class ZerxHttpClientAutoConfigurationTest {

    @Nested
    class DefaultValues {

        private final ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();

        @Test
        void defaultEnabledIsTrue() { assertTrue(config.isEnabled()); }

        @Test
        void defaultConnectTimeoutIs5() { assertEquals(5, config.getConnectTimeout()); }

        @Test
        void defaultReadTimeoutIs30() { assertEquals(30, config.getReadTimeout()); }

        @Test
        void defaultWriteTimeoutIs30() { assertEquals(30, config.getWriteTimeout()); }

        @Test
        void defaultMaxConnectionsIs100() { assertEquals(100, config.getMaxConnections()); }

        @Test
        void defaultMaxConnectionsPerRouteIs20() { assertEquals(20, config.getMaxConnectionsPerRoute()); }

        @Test
        void defaultConnectionIdleTimeoutIs30() { assertEquals(30, config.getConnectionIdleTimeout()); }

        @Test
        void defaultAccessLogEnabledIsTrue() { assertTrue(config.isAccessLogEnabled()); }

        @Test
        void defaultMaxResponseBodyLogLengthIs1024() { assertEquals(1024, config.getMaxResponseBodyLogLength()); }

        @Test
        void defaultMaxRetriesIs2() { assertEquals(2, config.getMaxRetries()); }

        @Test
        void defaultRetryInitialDelayMsIs100() { assertEquals(100, config.getRetryInitialDelayMs()); }

        @Test
        void defaultRetryMaxDelayMsIs3000() { assertEquals(3000, config.getRetryMaxDelayMs()); }

        @Test
        void defaultRetryJitterEnabledIsTrue() { assertTrue(config.isRetryJitterEnabled()); }

        @Test
        void defaultTracePropagationEnabledIsTrue() { assertTrue(config.isTracePropagationEnabled()); }

        @Test
        void defaultErrorHandlingEnabledIsTrue() { assertTrue(config.isErrorHandlingEnabled()); }

        @Test
        void defaultSensitiveHeaderMaskingEnabledIsTrue() { assertTrue(config.isSensitiveHeaderMaskingEnabled()); }
    }

    @Nested
    class CustomValues {

        @Test
        void canSetAllProperties() {
            ZerxWebProperties.HttpClient config = new ZerxWebProperties.HttpClient();
            config.setEnabled(false);
            config.setConnectTimeout(10);
            config.setReadTimeout(60);
            config.setWriteTimeout(45);
            config.setMaxConnections(200);
            config.setMaxConnectionsPerRoute(50);
            config.setConnectionIdleTimeout(60);
            config.setAccessLogEnabled(false);
            config.setMaxResponseBodyLogLength(2048);
            config.setMaxRetries(5);
            config.setRetryInitialDelayMs(200);
            config.setRetryMaxDelayMs(10000);
            config.setRetryJitterEnabled(false);
            config.setTracePropagationEnabled(false);
            config.setErrorHandlingEnabled(false);
            config.setSensitiveHeaderMaskingEnabled(false);

            assertFalse(config.isEnabled());
            assertEquals(10, config.getConnectTimeout());
            assertEquals(60, config.getReadTimeout());
            assertEquals(45, config.getWriteTimeout());
            assertEquals(200, config.getMaxConnections());
            assertEquals(50, config.getMaxConnectionsPerRoute());
            assertEquals(60, config.getConnectionIdleTimeout());
            assertFalse(config.isAccessLogEnabled());
            assertEquals(2048, config.getMaxResponseBodyLogLength());
            assertEquals(5, config.getMaxRetries());
            assertEquals(200, config.getRetryInitialDelayMs());
            assertEquals(10000, config.getRetryMaxDelayMs());
            assertFalse(config.isRetryJitterEnabled());
            assertFalse(config.isTracePropagationEnabled());
            assertFalse(config.isErrorHandlingEnabled());
            assertFalse(config.isSensitiveHeaderMaskingEnabled());
        }
    }

    @Nested
    class PropertiesIntegration {

        @Test
        void httpClientIsAccessibleFromZerxWebProperties() {
            ZerxWebProperties properties = new ZerxWebProperties();
            assertNotNull(properties.getHttpClient());
            assertTrue(properties.getHttpClient().isEnabled());
        }

        @Test
        void canOverrideHttpClientProperty() {
            ZerxWebProperties properties = new ZerxWebProperties();
            ZerxWebProperties.HttpClient custom = new ZerxWebProperties.HttpClient();
            custom.setMaxRetries(10);
            properties.setHttpClient(custom);

            assertEquals(10, properties.getHttpClient().getMaxRetries());
        }
    }
}
