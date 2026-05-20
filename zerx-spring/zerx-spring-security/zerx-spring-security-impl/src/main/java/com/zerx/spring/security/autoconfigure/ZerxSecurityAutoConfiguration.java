package com.zerx.spring.security.autoconfigure;

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.security.config.ZerxSecurityConfiguration;
import com.zerx.spring.security.properties.ZerxSecurityProperties;
import com.zerx.spring.security.service.DefaultLoginAttemptService;
import com.zerx.spring.security.service.DefaultPasswordValidator;
import com.zerx.spring.security.service.ZerxLoginAttemptService;
import com.zerx.spring.security.service.ZerxPasswordValidator;
import com.zerx.spring.security.token.ZerxHs256TokenService;
import com.zerx.spring.security.token.ZerxRs256TokenService;
import com.zerx.spring.security.token.ZerxTokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Zerx 安全模块自动配置
 * <p>
 * 通过 Spring Boot 自动装配机制注册安全模块的核心组件。
 * 当 classpath 中存在 {@link SecurityFilterChain} 时自动激活。
 * </p>
 *
 * <h3>启用条件：</h3>
 * <ul>
 *   <li>classpath 中存在 {@code SecurityFilterChain} 类</li>
 *   <li>{@code zerx.security.enabled} 配置为 {@code true}（默认值）</li>
 * </ul>
 *
 * <h3>自动注册的 Bean：</h3>
 * <ul>
 *   <li>{@link ZerxTokenService} — 根据 {@code zerx.security.jwt.algorithm} 选择 HS256 或 RS256 实现</li>
 * </ul>
 *
 * <h3>自动配置行为：</h3>
 * <ul>
 *   <li>默认使用 Spring Security 的 {@code MODE_THREADLOCAL} 策略（安全、无上下文泄漏风险）</li>
 *   <li>如需在 {@code @Async} 方法中访问认证信息，请配置 {@code DelegatingSecurityContextExecutor}</li>
 * </ul>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(prefix = "zerx.security", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(ZerxSecurityConfiguration.class)
@EnableConfigurationProperties(ZerxSecurityProperties.class)
public class ZerxSecurityAutoConfiguration {

    /**
     * 注册 JWT 令牌服务（根据配置自动选择算法）
     * <p>
     * 当用户未提供自定义 {@link ZerxTokenService} Bean 时，
     * 根据 {@code zerx.security.jwt.algorithm} 配置自动选择实现：
     * <ul>
     *   <li>{@code RS256} — 使用 {@link ZerxRs256TokenService}</li>
     *   <li>{@code HS256}（默认）— 使用 {@link ZerxHs256TokenService}</li>
     * </ul>
     * </p>
     *
     * @param props    安全配置属性
     * @param cacheOps 缓存操作工具
     * @return 令牌服务实例
     */
    @Bean
    @ConditionalOnMissingBean(ZerxTokenService.class)
    public ZerxTokenService zerxTokenService(ZerxSecurityProperties props, CacheOps cacheOps) {
        return switch (props.getJwt().getAlgorithm().toUpperCase()) {
            case "RS256" -> new ZerxRs256TokenService(props, cacheOps);
            default -> new ZerxHs256TokenService(props, cacheOps);
        };
    }

    /**
     * 注册默认登录失败锁定服务（基于内存，5 次锁定 30 分钟）。
     * <p>
     * 业务方可提供自定义 {@link ZerxLoginAttemptService} Bean 覆盖此默认实现。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ZerxLoginAttemptService.class)
    public ZerxLoginAttemptService zerxLoginAttemptService() {
        return new DefaultLoginAttemptService();
    }

    /**
     * 注册默认密码强度校验器（OWASP 基线：8 位 + 大小写 + 数字）。
     * <p>
     * 业务方可提供自定义 {@link ZerxPasswordValidator} Bean 覆盖此默认实现。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(ZerxPasswordValidator.class)
    public ZerxPasswordValidator zerxPasswordValidator() {
        return new DefaultPasswordValidator();
    }
}
