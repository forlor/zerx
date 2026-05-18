package com.zerx.spring.security.config;

import com.zerx.spring.security.filter.ZerxJwtAuthenticationFilter;
import com.zerx.spring.security.handler.ZerxAccessDeniedHandler;
import com.zerx.spring.security.handler.ZerxAuthenticationEntryPoint;
import com.zerx.spring.security.props.ZerxSecurityProperties;
import com.zerx.spring.security.token.ZerxTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import com.zerx.spring.security.service.ZerxPasswordService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 核心配置 — SecurityFilterChain、密码编码器、CORS 策略
 * <p>
 * 配置无状态 RESTful API 安全策略，包括：
 * <ul>
 *   <li>禁用 CSRF（前后端分离架构下无需 CSRF 防护）</li>
 *   <li>配置 CORS 跨域策略</li>
 *   <li>禁用 Session（使用 JWT 无状态认证）</li>
 *   <li>配置安全响应头（XSS 防护、内容类型嗅探防护、Frame 防护）</li>
 *   <li>配置 URL 访问权限（免认证 URL + Actuator 端点）</li>
 *   <li>注册 JWT 认证过滤器和自定义异常处理器</li>
 * </ul>
 * </p>
 *
 * @author zerx
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(ZerxSecurityProperties.class)
public class ZerxSecurityConfiguration {

    /**
     * 构建 Spring Security 过滤器链
     * <p>
     * 核心安全策略配置，定义请求认证和授权规则。
     * </p>
     *
     * @param http               HTTP 安全配置构建器
     * @param jwtAuthFilter      JWT 认证过滤器
     * @param authEntryPoint     未认证处理器
     * @param accessDeniedHandler 访问拒绝处理器
     * @param properties         安全配置属性
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ZerxJwtAuthenticationFilter jwtAuthFilter,
                                                   ZerxAuthenticationEntryPoint authEntryPoint,
                                                   ZerxAccessDeniedHandler accessDeniedHandler,
                                                   ZerxSecurityProperties properties) throws Exception {
        List<String> permitUrls = new java.util.ArrayList<>(properties.getPermitUrls());
        permitUrls.addAll(List.of(
                "/actuator/health",
                "/actuator/info"
        ));

        http
                // 禁用 CSRF（前后端分离，使用 JWT 无状态认证）
                .csrf(AbstractHttpConfigurer::disable)
                // 配置 CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
                // 无状态会话管理
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 安全响应头
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> {})
                        .xssProtection(xss -> xss.disable()))
                // URL 访问权限
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitUrls.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                // 自定义异常处理
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                // JWT 认证过滤器（在 UsernamePasswordAuthenticationFilter 之前）
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 构建 CORS 配置源
     * <p>
     * 根据配置属性中的 CORS 设置创建跨域配置。
     * </p>
     *
     * @param properties 安全配置属性
     * @return CORS 配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(ZerxSecurityProperties properties) {
        var corsConfig = properties.getCors();
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsConfig.getAllowedOrigins());
        configuration.setAllowedMethods(corsConfig.getAllowedMethods());
        configuration.setAllowedHeaders(corsConfig.getAllowedHeaders());
        configuration.setAllowCredentials(corsConfig.isAllowCredentials());
        configuration.setMaxAge(corsConfig.getMaxAge());

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 密码编码器 — BCrypt 强度 10
     * <p>
     * 使用 BCrypt 算法进行密码哈希，强度为 10（默认值，兼顾安全性和性能）。
     * </p>
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * 构建 JWT 认证过滤器 Bean
     *
     * @param tokenService JWT 令牌服务
     * @param properties   安全配置属性
     * @return JWT 认证过滤器实例
     */
    @Bean
    public ZerxJwtAuthenticationFilter jwtAuthenticationFilter(ZerxTokenService tokenService,
                                                                ZerxSecurityProperties properties) {
        return new ZerxJwtAuthenticationFilter(tokenService, properties);
    }

    /**
     * 构建未认证处理器 Bean
     *
     * @return 未认证处理器实例
     */
    @Bean
    public ZerxAuthenticationEntryPoint zerxAuthenticationEntryPoint() {
        return new ZerxAuthenticationEntryPoint();
    }

    /**
     * 构建访问拒绝处理器 Bean
     *
     * @return 访问拒绝处理器实例
     */
    @Bean
    public ZerxAccessDeniedHandler zerxAccessDeniedHandler() {
        return new ZerxAccessDeniedHandler();
    }

    /**
     * 密码工具服务
     * <p>
     * 提供 BCrypt 密码哈希和验证功能，封装 PasswordEncoder 为业务层提供简洁 API。
     * </p>
     *
     * @param passwordEncoder 密码编码器
     * @return 密码服务实例
     */
    @Bean
    @ConditionalOnMissingBean(ZerxPasswordService.class)
    public ZerxPasswordService zerxPasswordService(PasswordEncoder passwordEncoder) {
        return new ZerxPasswordService(passwordEncoder);
    }
}
