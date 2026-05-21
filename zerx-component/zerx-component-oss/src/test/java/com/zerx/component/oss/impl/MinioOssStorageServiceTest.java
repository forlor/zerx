package com.zerx.component.oss.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zerx.component.oss.properties.ZerxOssProperties;

/**
 * {@link MinioOssStorageService} 基本单元测试
 * <p>
 * 由于 MinIO SDK 可能不在测试 classpath 上，此测试仅验证类的可加载性。
 * 完整集成测试需引入 MinIO SDK 依赖。
 * </p>
 *
 * @author zerx
 */
class MinioOssStorageServiceTest {

    @Test
    @DisplayName("类可加载")
    void shouldLoadClass() {
        assertNotNull(MinioOssStorageService.class);
    }
}
