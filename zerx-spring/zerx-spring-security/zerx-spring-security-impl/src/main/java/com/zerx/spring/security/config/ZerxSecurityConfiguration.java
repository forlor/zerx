package com.zerx.spring.security.config;

import com.zerx.spring.security.filter.ZerxJwtAuthenticationFilter;
import com.zerx.spring.security.handler.ZerxAccessDeniedHandler;
import com.zerx.spring.security.handler.ZerxAuthenticationEntryPoint;
import com.zerx.spring.security.properties.ZerxSecurityProperties;
import com.zerx.spring.security.service.ZerxPasswordService;
import com.zerx.spring.security.token.ZerxRoleService;
import com.zerx.spring.security.token.ZerxTokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Spring Security 核心配置 — SecurityFilterChain、密码编码器
 * <p>
 * 配置无状态 RESTful API 安全策略，包括：
 * <ul>
 *   <li>禁用 CSRF（前后端分离架构下无需 CSRF 防护）</li>
 *   <li>CORS 委托给 Web 模块的 CorsFilter（zerx.web.cors.*）</li>
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
@EnableMethodSecurity
@ConditionalOnProperty(prefix = "zerx.security", name = "enabled", havingValue = "true", matchIfMissing = true)
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
        // 合并 permitUrls：配置值 + 默认的 actuator health/info
        List<String> permitUrls = new java.util.ArrayList<>(properties.getPermitUrls());
        permitUrls.addAll(List.of(
                "/actuator/health",
                "/actuator/info"
        ));

        // 合并角色规则：配置值 + 默认的 actuator ADMIN 规则
        List<ZerxSecurityProperties.RoleRule> roleRules = new java.util.ArrayList<>(properties.getRoleRules());
        // 如果用户未配置 /actuator/** 规则，添加默认的 ADMIN 规则
        boolean hasActuatorRule = roleRules.stream()
                .anyMatch(r -> "/actuator/**".equals(r.getPath()));
        if (!hasActuatorRule) {
            ZerxSecurityProperties.RoleRule actuatorRule = new ZerxSecurityProperties.RoleRule();
            actuatorRule.setPath("/actuator/**");
            actuatorRule.setRole("ADMIN");
            roleRules.add(actuatorRule);
        }

        // 构建授权规则
        var authConfig = http
                // 禁用 CSRF（前后端分离，使用 JWT 无状态认证）
                .csrf(AbstractHttpConfigurer::disable)
                // CORS 委托给 Web 模块的 CorsFilter（zerx.web.cors.* 配置）
                .cors(Customizer.withDefaults())
                // 无状态会话管理
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 安全响应头
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptions -> {})
                        .frameOptions(frameOptions -> frameOptions.deny())
                        .xssProtection(xss -> xss.disable())
                        .addHeaderWriter(new StaticHeadersWriter("X-XSS-Protection", "1; mode=block"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'"))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        // URL 访问权限
        authConfig.authorizeHttpRequests(auth -> {
            // 免认证 URL
            auth.requestMatchers(permitUrls.toArray(String[]::new)).permitAll();
            // 角色约束规则
            for (var rule : roleRules) {
                auth.requestMatchers(rule.getPath()).hasRole(rule.getRole());
            }
            // 其他所有请求需要认证
            auth.anyRequest().authenticated();
        });

        // 自定义异常处理
        authConfig.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        // JWT 认证过滤器（在 UsernamePasswordAuthenticationFilter 之前）
        authConfig.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 密码编码器 — BCrypt 强度 12
     * <p>
     * 使用 BCrypt 算法进行密码哈希，强度为 12（满足 OWASP 推荐最低安全标准）。
     * </p>
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * 构建 JWT 认证过滤器 Bean
     * <p>
     * 如果存在 {@link ZerxRoleService} Bean，角色将从 SPI 按需加载；
     * 否则从 JWT claims 中的 roles 字段获取（向后兼容）。
     * </p>
     *
     * @param tokenService JWT 令牌服务
     * @param properties   安全配置属性
     * @param roleService  角色加载服务（可选，由业务应用提供）
     * @return JWT 认证过滤器实例
     */
    @Bean
    public ZerxJwtAuthenticationFilter jwtAuthenticationFilter(ZerxTokenService tokenService,
                                                                ZerxSecurityProperties properties,
                                                                @Autowired(required = false) ZerxRoleService roleService) {
        return new ZerxJwtAuthenticationFilter(tokenService, properties, roleService);
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
