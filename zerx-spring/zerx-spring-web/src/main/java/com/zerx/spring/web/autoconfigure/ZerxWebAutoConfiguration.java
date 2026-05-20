package com.zerx.spring.web.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.logging.OpLogContextExtractor;
import com.zerx.spring.web.advise.GlobalExceptionHandler;
import com.zerx.spring.web.advise.ZerxResponseBodyAdvice;
import com.zerx.spring.web.aspect.IdempotentAspect;
import com.zerx.spring.web.aspect.RateLimitAspect;
import com.zerx.spring.web.config.ZerxCorsAutoConfiguration;
import com.zerx.spring.web.filter.AccessLogFilter;
import com.zerx.spring.web.filter.TraceFilter;
import com.zerx.spring.web.interceptor.RequestContextInterceptor;
import com.zerx.spring.web.properties.ZerxWebProperties;

/**
 * Zerx Web 模块自动配置
 * <p>
 * 作为 Spring Boot Starter 的核心入口，自动注册以下组件：
 * <ul>
 *   <li>{@link ZerxResponseBodyAdvice} — 统一响应体封装</li>
 *   <li>{@link GlobalExceptionHandler} — 全局异常处理</li>
 *   <li>{@link TraceFilter} — 链路追踪过滤器</li>
 *   <li>{@link RequestContextInterceptor} — 请求上下文拦截器</li>
 *   <li>{@link ZerxCorsAutoConfiguration} — CORS 跨域配置</li>
 *   <li>{@link OpLogContextExtractor} — 操作日志上下文提取器</li>
 * </ul>
 * </p>
 *
 * <p>
 * 仅在 Servlet Web 应用环境下激活（{@code @ConditionalOnWebApplication}）。
 * </p>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(ZerxWebProperties.class)
@Import(ZerxCorsAutoConfiguration.class)
public class ZerxWebAutoConfiguration {

    /**
     * 注册统一响应体增强
     *
     * @param properties Web 模块配置属性
     * @return ZerxResponseBodyAdvice 实例
     */
    @Bean
    public ZerxResponseBodyAdvice zerxResponseBodyAdvice(ZerxWebProperties properties,
                                                          ObjectMapper objectMapper) {
        return new ZerxResponseBodyAdvice(properties, objectMapper);
    }

    /**
     * 注册链路追踪过滤器
     *
     * @return TraceFilter 的 FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration() {
        FilterRegistrationBean<TraceFilter> registration = new FilterRegistrationBean<>(new TraceFilter());
        registration.setOrder(Integer.MIN_VALUE);
        registration.setName("zerxTraceFilter");
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册请求访问日志过滤器
     *
     * @param properties Web 模块配置属性
     * @return AccessLogFilter 的 FilterRegistrationBean
     */
    @Bean
    @ConditionalOnProperty(prefix = "zerx.web.access-log", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration(ZerxWebProperties properties) {
        FilterRegistrationBean<AccessLogFilter> registration =
                new FilterRegistrationBean<>(new AccessLogFilter(properties));
        registration.setOrder(Integer.MIN_VALUE + 2);
        registration.setName("zerxAccessLogFilter");
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册 WebMvc 配置 — 添加 RequestContextInterceptor
     *
     * @return WebMvcConfigurer 实例
     */
    @Bean
    public WebMvcConfigurer zerxWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new RequestContextInterceptor())
                        .addPathPatterns("/**")
                        .order(Integer.MIN_VALUE);
            }
        };
    }

    /**
     * 注册操作日志上下文提取器
     * <p>
     * 将 {@link RequestContext} 桥接为 {@link OpLogContextExtractor}，
     * 使得 {@code zerx-spring-logging} 模块在 Web 环境下能自动获取
     * 当前请求的用户身份信息（userId、username、clientIp）。
     * </p>
     *
     * @return 操作日志上下文提取器
     */
    @Bean
    @ConditionalOnMissingBean(OpLogContextExtractor.class)
    public OpLogContextExtractor opLogContextExtractor() {
        return new RequestContextOpLogContextExtractor();
    }

    /**
     * 注册限流切面
     * <p>
     * 当 {@code zerx.web.rate-limit.enabled=true}（默认）时激活，
     * 拦截标注了 {@link com.zerx.spring.web.annotation.RateLimit} 的方法。
     * </p>
     *
     * @return RateLimitAspect 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "zerx.web.rate-limit", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect() {
        return new RateLimitAspect();
    }

    /**
     * 注册幂等性切面
     * <p>
     * 当 {@code zerx.web.idempotent.enabled=true}（默认）时激活，
     * 拦截标注了 {@link com.zerx.spring.web.annotation.Idempotent} 的方法。
     * </p>
     *
     * @return IdempotentAspect 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "zerx.web.idempotent", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    public IdempotentAspect idempotentAspect() {
        return new IdempotentAspect();
    }
}
