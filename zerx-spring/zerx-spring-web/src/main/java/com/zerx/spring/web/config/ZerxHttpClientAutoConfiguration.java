package com.zerx.spring.web.config;

import com.zerx.spring.web.client.interceptor.*;
import com.zerx.spring.web.properties.ZerxWebProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * HTTP 客户端自动配置
 * <p>
 * 基于 Spring Boot 3.x {@link RestClient} 构建高性能出站 HTTP 客户端，
 * 通过拦截器链实现：链路追踪透传、敏感 Header 脱敏、出站日志、
 * 异常转换、自动重试五大横切能力。
 * </p>
 *
 * <h3>架构设计：</h3>
 * <pre>
 *   业务代码 → RestClient → [拦截器链] → JDK HttpClient (连接池)
 *                              ├── RetryInterceptor          (最外层：重试)
 *                              ├── ErrorResponseInterceptor  (异常转换)
 *                              ├── OutboundAccessLogInterceptor (出站日志)
 *                              ├── SensitiveHeaderInterceptor (Header 脱敏)
 *                              └── TracePropagationInterceptor  (TraceID 透传)
 * </pre>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   web:
 *     http-client:
 *       enabled: true
 *       connect-timeout: 5
 *       read-timeout: 30
 *       max-connections: 100
 *       max-retries: 2
 *       access-log-enabled: true
 * }</pre>
 *
 * @author zerx
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zerx.web.http-client", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class ZerxHttpClientAutoConfiguration {

    /**
     * 配置 JDK HttpClient 连接工厂
     * <p>
     * 使用 JDK 内置的 {@link HttpClient} 作为底层实现，零额外依赖。
     * 连接池参数通过 {@link HttpClient.Version#HTTP_2} 和
     * {@link java.net.http.HttpClient.Builder#connectTimeout(Duration)} 进行调优。
     * </p>
     *
     * @param properties HTTP 客户端配置
     * @return 配置好的请求工厂
     */
    @Bean
    @ConditionalOnMissingBean(ClientHttpRequestFactory.class)
    public ClientHttpRequestFactory zerxClientHttpRequestFactory(ZerxWebProperties properties) {
        ZerxWebProperties.HttpClient config = properties.getHttpClient();

        HttpClient jdkClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(config.getConnectTimeout()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdkClient);
        factory.setReadTimeout(Duration.ofSeconds(config.getReadTimeout()));

        return factory;
    }

    /**
     * 配置 RestClient Bean
     * <p>
     * 构建拦截器链，按顺序注册：
     * <ol>
     *   <li>{@link RetryInterceptor} — 最外层，控制重试逻辑</li>
     *   <li>{@link ErrorResponseInterceptor} — 非 2xx 响应转 ExternalServiceException</li>
     *   <li>{@link OutboundAccessLogInterceptor} — 出站请求/响应日志</li>
     *   <li>{@link SensitiveHeaderInterceptor} — 敏感 Header 脱敏（日志安全）</li>
     *   <li>{@link TracePropagationInterceptor} — TraceID 自动透传</li>
     * </ol>
     * </p>
     *
     * @param properties  HTTP 客户端配置
     * @param requestFactory 请求工厂
     * @return 配置好的 RestClient 实例
     */
    @Bean
    @ConditionalOnMissingBean(RestClient.class)
    public RestClient zerxRestClient(ZerxWebProperties properties,
                                     ClientHttpRequestFactory requestFactory) {
        ZerxWebProperties.HttpClient config = properties.getHttpClient();

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory);

        // 注册拦截器链（按执行顺序，第一个拦截器最先执行）
        // 1. 重试拦截器 — 最外层包装
        if (config.getMaxRetries() > 0) {
            builder.requestInterceptor(new RetryInterceptor(config));
        }

        // 2. 异常转换拦截器
        if (config.isErrorHandlingEnabled()) {
            builder.requestInterceptor(new ErrorResponseInterceptor());
        }

        // 3. 出站请求日志拦截器
        if (config.isAccessLogEnabled()) {
            builder.requestInterceptor(new OutboundAccessLogInterceptor(
                    config.getMaxResponseBodyLogLength()));
        }

        // 4. 敏感 Header 脱敏拦截器
        if (config.isSensitiveHeaderMaskingEnabled()) {
            builder.requestInterceptor(new SensitiveHeaderInterceptor());
        }

        // 5. 链路追踪透传拦截器
        if (config.isTracePropagationEnabled()) {
            builder.requestInterceptor(new TracePropagationInterceptor());
        }

        return builder.build();
    }
}
