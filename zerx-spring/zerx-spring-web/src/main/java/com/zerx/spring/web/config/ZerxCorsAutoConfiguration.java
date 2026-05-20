package com.zerx.spring.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.zerx.spring.web.properties.ZerxWebProperties;

/**
 * CORS 跨域自动配置
 * <p>
 * 根据 {@link ZerxWebProperties} 中的 CORS 配置，自动注册跨域过滤器。
 * 可通过 {@code zerx.web.cors.enabled=false} 关闭。
 * </p>
 *
 * @author zerx
 */
@Configuration
@ConditionalOnProperty(prefix = "zerx.web.cors", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ZerxCorsAutoConfiguration {

    /**
     * 注册 CORS 过滤器
     *
     * @param properties Web 模块配置属性
     * @return CORS 过滤器注册 Bean
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(ZerxWebProperties properties) {
        ZerxWebProperties.Cors corsProps = properties.getCors();

        CorsConfiguration config = new CorsConfiguration();
        corsProps.getAllowedOrigins().forEach(config::addAllowedOriginPattern);
        corsProps.getAllowedMethods().forEach(config::addAllowedMethod);
        corsProps.getAllowedHeaders().forEach(config::addAllowedHeader);
        corsProps.getExposedHeaders().forEach(config::addExposedHeader);
        config.setAllowCredentials(corsProps.isAllowCredentials());
        config.setMaxAge(corsProps.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        CorsFilter corsFilter = new CorsFilter(source);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
        registration.setOrder(-1);
        registration.setName("zerxCorsFilter");

        return registration;
    }
}
