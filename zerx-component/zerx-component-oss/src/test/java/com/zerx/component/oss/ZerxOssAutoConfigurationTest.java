package com.zerx.component.oss;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zerx.component.oss.autoconfigure.ZerxOssAutoConfiguration;
import com.zerx.component.oss.properties.ZerxOssProperties;

/**
 * {@link ZerxOssAutoConfiguration} 单元测试
 *
 * @author zerx
 */
class ZerxOssAutoConfigurationTest {

    @Test
    @DisplayName("配置类可以被加载")
    void shouldLoadConfiguration() {
        assertNotNull(ZerxOssAutoConfiguration.class);
    }

    @Test
    @DisplayName("配置类注解验证")
    void shouldHaveCorrectAnnotations() {
        assertNotNull(ZerxOssAutoConfiguration.class.getAnnotation(
                org.springframework.boot.autoconfigure.AutoConfiguration.class));
        assertNotNull(ZerxOssAutoConfiguration.class.getAnnotation(
                org.springframework.boot.context.properties.EnableConfigurationProperties.class));
    }
}
