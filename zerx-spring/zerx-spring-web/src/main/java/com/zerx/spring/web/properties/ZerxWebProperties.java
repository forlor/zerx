package com.zerx.spring.web.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Zerx Web 模块配置属性
 * <p>
 * 通过 {@code application.yml} 中的 {@code zerx.web.*} 前缀进行配置，
 * 控制统一响应封装、CORS 跨域等行为。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   web:
 *     response-wrap-enabled: true
 *     response-wrap-exclude-packages:
 *       - org.springdoc
 *       - org.springframework.boot.actuator
 *     cors:
 *       enabled: true
 *       allowed-origins: "*"
 *       allowed-methods: GET,POST,PUT,DELETE,OPTIONS
 *       max-age: 3600
 * }</pre>
 *
 * @author zerx
 */
@ConfigurationProperties(prefix = "zerx.web")
public class ZerxWebProperties {

    /**
     * 是否启用统一响应封装
     * <p>
     * 启用后，所有 {@code @RestController} 的返回值将自动包装为 {@code Result<T>}。
     * </p>
     */
    private boolean responseWrapEnabled = true;

    /**
     * 不参与统一响应封装的包路径列表
     * <p>
     * 匹配到的 Controller 将跳过自动包装逻辑。
     * </p>
     */
    private List<String> responseWrapExcludePackages =
            List.of("org.springdoc", "org.springframework.boot.actuator");

    /**
     * CORS 跨域配置
     */
    private Cors cors = new Cors();

    /** Jackson 序列化配置 */
    private Jackson jackson = new Jackson();

    /** 请求访问日志配置 */
    private AccessLog accessLog = new AccessLog();

    /** HTTP 客户端配置 */
    private HttpClient httpClient = new HttpClient();

    /** 限流配置 */
    private RateLimit rateLimit = new RateLimit();

    /** 幂等性配置 */
    private Idempotent idempotent = new Idempotent();

    /**
     * 获取是否启用统一响应封装
     *
     * @return 启用返回 {@code true}
     */
    public boolean isResponseWrapEnabled() {
        return responseWrapEnabled;
    }

    /**
     * 设置是否启用统一响应封装
     *
     * @param responseWrapEnabled 是否启用
     */
    public void setResponseWrapEnabled(boolean responseWrapEnabled) {
        this.responseWrapEnabled = responseWrapEnabled;
    }

    /**
     * 获取不参与统一响应封装的包路径列表
     *
     * @return 排除包路径列表
     */
    public List<String> getResponseWrapExcludePackages() {
        return responseWrapExcludePackages;
    }

    /**
     * 设置不参与统一响应封装的包路径列表
     *
     * @param responseWrapExcludePackages 排除包路径列表
     */
    public void setResponseWrapExcludePackages(List<String> responseWrapExcludePackages) {
        this.responseWrapExcludePackages = responseWrapExcludePackages;
    }

    /**
     * 获取 CORS 跨域配置
     *
     * @return CORS 配置对象
     */
    public Cors getCors() {
        return cors;
    }

    /**
     * 设置 CORS 跨域配置
     *
     * @param cors CORS 配置对象
     */
    public void setCors(Cors cors) {
        this.cors = cors;
    }

    /**
     * 获取 Jackson 序列化配置
     *
     * @return Jackson 配置对象
     */
    public Jackson getJackson() {
        return jackson;
    }

    /**
     * 设置 Jackson 序列化配置
     *
     * @param jackson Jackson 配置对象
     */
    public void setJackson(Jackson jackson) {
        this.jackson = jackson;
    }

    /**
     * 获取请求访问日志配置
     *
     * @return AccessLog 配置对象
     */
    public AccessLog getAccessLog() {
        return accessLog;
    }

    /**
     * 设置请求访问日志配置
     *
     * @param accessLog AccessLog 配置对象
     */
    public void setAccessLog(AccessLog accessLog) {
        this.accessLog = accessLog;
    }

    /**
     * 获取 HTTP 客户端配置
     *
     * @return HttpClient 配置对象
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * 设置 HTTP 客户端配置
     *
     * @param httpClient HttpClient 配置对象
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * 获取限流配置
     *
     * @return RateLimit 配置对象
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * 设置限流配置
     *
     * @param rateLimit RateLimit 配置对象
     */
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * 获取幂等性配置
     *
     * @return Idempotent 配置对象
     */
    public Idempotent getIdempotent() {
        return idempotent;
    }

    /**
     * 设置幂等性配置
     *
     * @param idempotent Idempotent 配置对象
     */
    public void setIdempotent(Idempotent idempotent) {
        this.idempotent = idempotent;
    }

    /**
     * CORS 跨域配置属性
     * <p>
     * 控制跨域资源共享的策略，包括允许的源、方法、头部以及缓存时间。
     * </p>
     *
     * @author zerx
     */
    public static class Cors {

        /** 是否启用 CORS */
        private boolean enabled = true;

        /** 允许的源（跨域请求来源），默认允许所有 */
        private List<String> allowedOrigins = List.of("*");

        /** 允许的 HTTP 方法 */
        private List<String> allowedMethods =
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

        /** 允许的请求头 */
        private List<String> allowedHeaders = List.of("*");

        /** 是否允许携带凭证（Cookie 等） */
        private boolean allowCredentials = false;

        /** 暴露给前端的响应头 */
        private List<String> exposedHeaders = List.of();

        /** 预检请求缓存时间（秒） */
        private long maxAge = 3600L;

        /**
         * 获取是否启用 CORS
         *
         * @return 启用返回 {@code true}
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置是否启用 CORS
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取允许的源列表
         *
         * @return 允许的源列表
         */
        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        /**
         * 设置允许的源列表
         *
         * @param allowedOrigins 允许的源列表
         */
        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        /**
         * 获取允许的 HTTP 方法列表
         *
         * @return 允许的方法列表
         */
        public List<String> getAllowedMethods() {
            return allowedMethods;
        }

        /**
         * 设置允许的 HTTP 方法列表
         *
         * @param allowedMethods 允许的方法列表
         */
        public void setAllowedMethods(List<String> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        /**
         * 获取允许的请求头列表
         *
         * @return 允许的请求头列表
         */
        public List<String> getAllowedHeaders() {
            return allowedHeaders;
        }

        /**
         * 设置允许的请求头列表
         *
         * @param allowedHeaders 允许的请求头列表
         */
        public void setAllowedHeaders(List<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        /**
         * 获取是否允许携带凭证
         *
         * @return 允许返回 {@code true}
         */
        public boolean isAllowCredentials() {
            return allowCredentials;
        }

        /**
         * 设置是否允许携带凭证
         *
         * @param allowCredentials 是否允许
         */
        public void setAllowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
        }

        /**
         * 获取暴露给前端的响应头列表
         *
         * @return 暴露的响应头列表
         */
        public List<String> getExposedHeaders() {
            return exposedHeaders;
        }

        /**
         * 设置暴露给前端的响应头列表
         *
         * @param exposedHeaders 暴露的响应头列表
         */
        public void setExposedHeaders(List<String> exposedHeaders) {
            this.exposedHeaders = exposedHeaders;
        }

        /**
         * 获取预检请求缓存时间（秒）
         *
         * @return 缓存时间（秒）
         */
        public long getMaxAge() {
            return maxAge;
        }

        /**
         * 设置预检请求缓存时间（秒）
         *
         * @param maxAge 缓存时间（秒）
         */
        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * Jackson 序列化配置属性
     * <p>
     * 控制 JSON 序列化行为：Long→String、日期格式、null 值处理等。
     * </p>
     *
     * @author zerx
     */
    public static class Jackson {
        /** 日期时间格式，如 "yyyy-MM-dd HH:mm:ss" */
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";

        /** 是否序列化 null 值 */
        private boolean includeNull = false;

        public String getDateFormat() {
            return dateFormat;
        }

        public void setDateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
        }

        public boolean isIncludeNull() {
            return includeNull;
        }

        public void setIncludeNull(boolean includeNull) {
            this.includeNull = includeNull;
        }
    }

    /**
     * 请求访问日志配置属性
     *
     * @author zerx
     */
    public static class AccessLog {
        /** 是否启用请求日志 */
        private boolean enabled = true;

        /** 慢请求阈值（毫秒），超过则 WARN */
        private long slowThresholdMs = 3000L;

        /** 排除的 URL 路径（支持通配符，如 /actuator/**） */
        private List<String> excludeUrls = List.of(
                "/actuator/**", "/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/favicon.ico"
        );

        /** 需要脱敏的参数名 */
        private List<String> sensitiveParams = List.of(
                "password", "token", "secret", "credential", "authorization", "phone", "mobile"
        );

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getSlowThresholdMs() {
            return slowThresholdMs;
        }

        public void setSlowThresholdMs(long slowThresholdMs) {
            this.slowThresholdMs = slowThresholdMs;
        }

        public List<String> getExcludeUrls() {
            return excludeUrls;
        }

        public void setExcludeUrls(List<String> excludeUrls) {
            this.excludeUrls = excludeUrls;
        }

        public List<String> getSensitiveParams() {
            return sensitiveParams;
        }

        public void setSensitiveParams(List<String> sensitiveParams) {
            this.sensitiveParams = sensitiveParams;
        }
    }

    /**
     * HTTP 客户端配置属性
     * <p>
     * 控制出站 HTTP 调用的超时策略、连接池、重试、日志和异常转换行为。
     * 基于 Spring Boot 3.x {@link org.springframework.web.client.RestClient} 实现。
     * </p>
     *
     * @author zerx
     */
    public static class HttpClient {

        /** 是否启用 HTTP 客户端自动配置 */
        private boolean enabled = true;

        /** 连接超时（秒） */
        private int connectTimeout = 5;

        /** 读取超时（秒） */
        private int readTimeout = 30;

        /** 写入超时（秒） */
        private int writeTimeout = 30;

        /** 连接池最大连接数 */
        private int maxConnections = 100;

        /** 每个路由的最大连接数 */
        private int maxConnectionsPerRoute = 20;

        /** 空闲连接存活时间（秒） */
        private int connectionIdleTimeout = 30;

        /** 是否启用出站请求日志 */
        private boolean accessLogEnabled = true;

        /** 出站请求日志中响应体的最大记录长度（超过则截断），0 表示不记录 */
        private int maxResponseBodyLogLength = 1024;

        /** 最大重试次数（0 = 不重试） */
        private int maxRetries = 2;

        /** 重试初始退避间隔（毫秒） */
        private long retryInitialDelayMs = 100;

        /** 重试最大退避间隔（毫秒） */
        private long retryMaxDelayMs = 3000;

        /** 是否启用重试抖动（随机化延迟避免惊群） */
        private boolean retryJitterEnabled = true;

        /** 是否启用链路追踪透传（自动注入 X-Trace-Id） */
        private boolean tracePropagationEnabled = true;

        /** 是否启用非 2xx 响应自动转 ExternalServiceException */
        private boolean errorHandlingEnabled = true;

        /** 是否启用请求日志中敏感 Header 脱敏（Authorization 等） */
        private boolean sensitiveHeaderMaskingEnabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public void setWriteTimeout(int writeTimeout) {
            this.writeTimeout = writeTimeout;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getMaxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }

        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }

        public int getConnectionIdleTimeout() {
            return connectionIdleTimeout;
        }

        public void setConnectionIdleTimeout(int connectionIdleTimeout) {
            this.connectionIdleTimeout = connectionIdleTimeout;
        }

        public boolean isAccessLogEnabled() {
            return accessLogEnabled;
        }

        public void setAccessLogEnabled(boolean accessLogEnabled) {
            this.accessLogEnabled = accessLogEnabled;
        }

        public int getMaxResponseBodyLogLength() {
            return maxResponseBodyLogLength;
        }

        public void setMaxResponseBodyLogLength(int maxResponseBodyLogLength) {
            this.maxResponseBodyLogLength = maxResponseBodyLogLength;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getRetryInitialDelayMs() {
            return retryInitialDelayMs;
        }

        public void setRetryInitialDelayMs(long retryInitialDelayMs) {
            this.retryInitialDelayMs = retryInitialDelayMs;
        }

        public long getRetryMaxDelayMs() {
            return retryMaxDelayMs;
        }

        public void setRetryMaxDelayMs(long retryMaxDelayMs) {
            this.retryMaxDelayMs = retryMaxDelayMs;
        }

        public boolean isRetryJitterEnabled() {
            return retryJitterEnabled;
        }

        public void setRetryJitterEnabled(boolean retryJitterEnabled) {
            this.retryJitterEnabled = retryJitterEnabled;
        }

        public boolean isTracePropagationEnabled() {
            return tracePropagationEnabled;
        }

        public void setTracePropagationEnabled(boolean tracePropagationEnabled) {
            this.tracePropagationEnabled = tracePropagationEnabled;
        }

        public boolean isErrorHandlingEnabled() {
            return errorHandlingEnabled;
        }

        public void setErrorHandlingEnabled(boolean errorHandlingEnabled) {
            this.errorHandlingEnabled = errorHandlingEnabled;
        }

        public boolean isSensitiveHeaderMaskingEnabled() {
            return sensitiveHeaderMaskingEnabled;
        }

        public void setSensitiveHeaderMaskingEnabled(boolean sensitiveHeaderMaskingEnabled) {
            this.sensitiveHeaderMaskingEnabled = sensitiveHeaderMaskingEnabled;
        }
    }

    /**
     * 限流配置属性
     * <p>
     * 控制基于 {@link com.zerx.spring.web.annotation.RateLimit} 注解的限流行为。
     * </p>
     *
     * @author zerx
     */
    public static class RateLimit {

        /** 是否启用限流切面（关闭后 @RateLimit 注解不生效） */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 幂等性配置属性
     * <p>
     * 控制基于 {@link com.zerx.spring.web.annotation.Idempotent} 注解的幂等性行为。
     * </p>
     *
     * @author zerx
     */
    public static class Idempotent {

        /** 是否启用幂等性切面（关闭后 @Idempotent 注解不生效） */
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
