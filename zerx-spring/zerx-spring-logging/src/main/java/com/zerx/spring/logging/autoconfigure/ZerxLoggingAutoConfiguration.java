package com.zerx.spring.logging.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.logging.LogRateLimiter;
import com.zerx.common.logging.OpLogContextExtractor;
import com.zerx.spring.logging.aspect.ZerxOpLogAspect;
import com.zerx.spring.logging.properties.ZerxLoggingProperties;
import com.zerx.spring.logging.service.ZerxOpLogService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Zerx 日志增强模块自动配置
 * <p>
 * 通过 Spring Boot 自动装配机制注册日志增强模块的核心组件。
 * 当 {@code zerx.logging.enabled} 配置为 {@code true}（默认值）时自动激活。
 * </p>
 *
 * <h3>自动注册的 Bean：</h3>
 * <ul>
 *   <li>{@link ZerxOpLogAspect} — 操作日志 AOP 切面</li>
 *   <li>{@link LogRateLimiter} — 日志限流器（可选）</li>
 * </ul>
 *
 * <h3>Logback 集成（可选）：</h3>
 * <p>
 * 当 classpath 中存在 {@code ch.qos.logback.classic.LoggerContext} 时，
 * 自动注册 {@link com.zerx.spring.logging.converter.SensitiveMessageConverter}
 * 作为 Spring Bean，便于程序化管理 Logback 配置。
 * </p>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zerx.logging", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZerxLoggingProperties.class)
public class ZerxLoggingAutoConfiguration {

    /**
     * 注册操作日志 AOP 切面
     * <p>
     * {@link ZerxOpLogService} 通过 {@link ObjectProvider} 注入，允许业务层不提供实现。
     * 如果未提供实现，操作日志仅输出到日志（不持久化）。
     * </p>
     *
     * @param properties       日志增强配置
     * @param opLogService     操作日志持久化服务（可选）
     * @param objectMapper     Jackson ObjectMapper
     * @param rateLimiter      日志限流器（可选）
     * @param contextExtractor 操作日志上下文提取器（可选，由 Web 等模块提供）
     * @return 操作日志切面
     */
    @Bean
    @ConditionalOnMissingBean(ZerxOpLogAspect.class)
    public ZerxOpLogAspect zerxOpLogAspect(ZerxLoggingProperties properties,
                                           ObjectProvider<ZerxOpLogService> opLogService,
                                           ObjectMapper objectMapper,
                                           ObjectProvider<LogRateLimiter> rateLimiter,
                                           ObjectProvider<OpLogContextExtractor> contextExtractor) {
        return new ZerxOpLogAspect(
                properties,
                opLogService.getIfAvailable(),
                objectMapper,
                rateLimiter.getIfAvailable(),
                contextExtractor.getIfAvailable());
    }

    /**
     * 注册日志限流器（可选）
     * <p>
     * 仅在 {@code zerx.logging.rate-limit.enabled} 为 {@code true} 时注册。
     * 用户可自行提供 {@link LogRateLimiter} Bean 覆盖默认配置。
     * </p>
     *
     * @param properties 日志增强配置
     * @return 日志限流器
     */
    @Configuration
    @ConditionalOnProperty(prefix = "zerx.logging.rate-limit", name = "enabled", havingValue = "true")
    static class RateLimitConfiguration {

        @Bean
        @ConditionalOnMissingBean(LogRateLimiter.class)
        public LogRateLimiter zerxLogRateLimiter(ZerxLoggingProperties properties) {
            int maxPerSecond = properties.getRateLimit().getMaxPerSecond();
            return LogRateLimiter.of(maxPerSecond, Duration.ofSeconds(1));
        }
    }

    /**
     * Logback 集成配置（可选）
     * <p>
     * 仅当 classpath 中存在 Logback 时激活。
     * </p>
     */
    @Configuration
    @ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
    static class LogbackConfiguration {

        /**
         * 注册脱敏消息转换器作为 Spring Bean
         * <p>
         * 便于在代码中获取或通过 {@code LogbackConfigurator} 编程式配置 Logback。
         * </p>
         *
         * @return 脱敏消息转换器实例
         */
        @Bean
        @ConditionalOnMissingBean(com.zerx.spring.logging.converter.SensitiveMessageConverter.class)
        public com.zerx.spring.logging.converter.SensitiveMessageConverter sensitiveMessageConverter() {
            return new com.zerx.spring.logging.converter.SensitiveMessageConverter();
        }
    }
}
