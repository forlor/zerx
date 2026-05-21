package com.zerx.component.oss.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link AliyunOssStorageService} 基本单元测试
 * <p>
 * 由于阿里云 OSS SDK 可能不在测试 classpath 上，此测试仅验证类的可加载性。
 * 完整集成测试需引入阿里云 OSS SDK 依赖。
 * </p>
 *
 * @author zerx
 */
class AliyunOssStorageServiceTest {

    @Test
    @DisplayName("类可加载")
    void shouldLoadClass() {
        assertNotNull(AliyunOssStorageService.class);
    }
}
