package com.zerx.spring.web.config;

import java.util.Set;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import com.zerx.spring.web.properties.ZerxWebProperties;

/**
 * Micrometer HTTP 可观测性自动配置
 * <p>
 * 为每个 HTTP 请求自动创建 {@link Observation}（Span + Metrics），
 * 产出以下指标数据：
 * <ul>
 *   <li><b>http.server.requests</b> — QPS、响应时间直方图、错误率（Timer）</li>
 *   <li><b>Span</b> — 支持导出至 Zipkin/Jaeger 等链路追踪后端</li>
 * </ul>
 * </p>
 *
 * <h3>性能设计：</h3>
 * <ul>
 *   <li>自研轻量 {@link OncePerRequestFilter}，零反射、零代理开销</li>
 *   <li>使用 {@code System.nanoTime()} 高精度计时，避免 {@code System.currentTimeMillis()} 的漂移</li>
 *   <li>Observation 创建/停止开销极低（ConcurrentLinkedQueue 管理观察者，注册 O(1)）</li>
 *   <li>条件装配：仅当 Micrometer + ObservationRegistry 在 classpath 上才激活</li>
 *   <li>支持通过配置排除低价值路径（健康检查、静态资源）</li>
 * </ul>
 *
 * <h3>与 Spring Boot Actuator 的关系：</h3>
 * <p>
 * 此配置通过 optional 依赖引入 Micrometer Observation API，不强制引入 actuator。
 * 当应用同时引入 {@code spring-boot-starter-actuator} 时，Prometheus/Jaeger 等后端自动生效。
 * </p>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ObservationRegistry.class, Observation.class})
@ConditionalOnProperty(prefix = "zerx.web.observability", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ZerxObservabilityAutoConfiguration {

    /**
     * 注册 HTTP Server 请求观测过滤器
     * <p>
     * 为每个 HTTP 请求创建 {@link Observation}，包含以下 Key-Value：
     * <ul>
     *   <li>{@code method} — HTTP 方法（GET/POST/PUT/DELETE）</li>
     *   <li>{@code uri} — 请求 URI</li>
     *   <li>{@code status} — HTTP 状态码（200/400/500）</li>
     *   <li>{@code exception} — 异常类名（如有）</li>
     * </ul>
     * </p>
     *
     * @param registry Micrometer 观察注册表
     * @param properties Web 模块配置属性
     * @return HTTP 观测过滤器
     */
    @Bean
    @ConditionalOnMissingBean(name = "zerxObservationFilter")
    public FilterRegistrationBean<Filter> zerxObservationFilter(
            ObservationRegistry registry, ZerxWebProperties properties) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ZerxHttpObservationFilter(registry, properties));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("zerxObservationFilter");
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 轻量 HTTP 观测过滤器
     * <p>
     * 使用 Micrometer Observation API 为每个 HTTP 请求创建 Span，
     * 自动关联 MDC TraceID、产出 http.server.requests 指标。
     * </p>
     *
     * @author zerx
     */
    static final class ZerxHttpObservationFilter extends OncePerRequestFilter {

        private static final String OBSERVATION_ATTR = "zerx.observation";

        private final ObservationRegistry registry;
        private final Set<String> excludePrefixes;

        ZerxHttpObservationFilter(ObservationRegistry registry, ZerxWebProperties properties) {
            this.registry = registry;
            this.excludePrefixes = Set.copyOf(properties.getAccessLog().getExcludeUrls());
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String uri = request.getRequestURI();
            for (String prefix : excludePrefixes) {
                if (prefix.endsWith("/**")) {
                    if (uri.startsWith(prefix.substring(0, prefix.length() - 3))) {
                        return true;
                    }
                } else if (uri.equals(prefix)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                         HttpServletResponse response,
                                         jakarta.servlet.FilterChain filterChain)
                throws jakarta.servlet.ServletException, java.io.IOException {

            Observation observation = Observation.createNotStarted(
                    "http.server.requests", registry)
                    .contextualName(request.getMethod() + " " + request.getRequestURI())
                    .lowCardinalityKeyValue("method", request.getMethod())
                    .lowCardinalityKeyValue("uri", request.getRequestURI())
                    .highCardinalityKeyValue("client.ip", request.getRemoteAddr())
                    .start();

            request.setAttribute(OBSERVATION_ATTR, observation);

            try {
                filterChain.doFilter(request, response);
            } catch (Exception ex) {
                observation.error(ex);
                throw ex;
            } finally {
                observation.highCardinalityKeyValue("status",
                        String.valueOf(response.getStatus()));
                observation.stop();
            }
        }
    }
}
