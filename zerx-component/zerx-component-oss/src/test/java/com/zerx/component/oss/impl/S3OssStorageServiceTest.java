package com.zerx.component.oss.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link S3OssStorageService} 基本单元测试
 * <p>
 * 由于 AWS S3 SDK 可能不在测试 classpath 上，此测试仅验证类的可加载性。
 * 完整集成测试需引入 AWS S3 SDK 依赖。
 * </p>
 *
 * @author zerx
 */
class S3OssStorageServiceTest {

    @Test
    @DisplayName("类可加载")
    void shouldLoadClass() {
        assertNotNull(S3OssStorageService.class);
    }
}
