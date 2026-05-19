package com.zerx.spring.security.config;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.security.props.ZerxSecurityProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link ZerxSecurityConfiguration} 集成测试
 * <p>
 * 通过 {@link SpringBootTest} 加载完整 Spring 上下文，
 * 验证 SecurityFilterChain 的配置是否正确。
 * </p>
 *
 * @author zerx
 */
@SpringBootTest(classes = ZerxSecurityConfigurationTest.TestConfig.class)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ZerxSecurityConfigurationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("免认证 URL /auth/login 无需认证即可访问")
    void permitUrl_login_accessible() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isNotFound()); // 404 because no controller mapped, but not 401/403
    }

    @Test
    @DisplayName("免认证 URL /auth/register 无需认证即可访问")
    void permitUrl_register_accessible() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("受保护 URL 未认证返回 401")
    void protectedUrl_unauthorized_returns401() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("未认证时返回 JSON 格式的 401 响应")
    void unauthorized_returnsJsonResponse() throws Exception {
        mockMvc.perform(get("/api/protected")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.message").value("未认证，请先登录"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Actuator /actuator/health 免认证")
    void actuatorHealth_accessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound()); // 404 because actuator not configured, but not 401/403
    }

    @Test
    @DisplayName("Actuator /actuator/info 免认证")
    void actuatorInfo_accessible() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isNotFound());
    }

    /**
     * 最小化测试配置
     */
    @Configuration
    @org.springframework.boot.autoconfigure.SpringBootApplication(
            scanBasePackages = "com.zerx.spring.security",
            exclude = {
                    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class,
                    com.zerx.spring.cache.autoconfigure.ZerxCacheAutoConfiguration.class,
                    com.zerx.spring.web.config.ZerxObservabilityAutoConfiguration.class
            }
    )
    @org.springframework.boot.context.properties.EnableConfigurationProperties(ZerxSecurityProperties.class)
    static class TestConfig {

        /**
         * 提供模拟的 CacheOps Bean
         */
        @Bean
        CacheOps cacheOps() {
            return new NoOpCacheOps();
        }
    }

    /**
     * 空操作 CacheOps 实现，用于测试
     */
    private static class NoOpCacheOps implements CacheOps {

        @Override
        public <T> T get(String key, Supplier<T> loader, long ttl, java.util.concurrent.TimeUnit timeUnit) {
            return loader.get();
        }

        @Override
        public <T> Optional<T> getOptional(String key, Supplier<T> loader, long ttl, java.util.concurrent.TimeUnit unit) {
            return Optional.ofNullable(get(key, loader, ttl, unit));
        }

        @Override
        public <T> T get(String key) {
            return null;
        }

        @Override
        public void set(String key, Object value, long ttl, java.util.concurrent.TimeUnit timeUnit) {
            // no-op
        }

        @Override
        public void set(String key, Object value, Duration ttl) {
            // no-op
        }

        @Override
        public void evict(String key) {
            // no-op
        }

        @Override
        public void evictByPrefix(String keyPrefix) {
            // no-op
        }

        @Override
        public boolean hasKey(String key) {
            return false;
        }

        @Override
        public CacheStore getStore() {
            return null;
        }
    }
}
