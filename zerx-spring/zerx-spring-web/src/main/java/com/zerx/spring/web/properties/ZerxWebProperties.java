package com.zerx.spring.web.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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

        public String getDateFormat() { return dateFormat; }
        public void setDateFormat(String dateFormat) { this.dateFormat = dateFormat; }
        public boolean isIncludeNull() { return includeNull; }
        public void setIncludeNull(boolean includeNull) { this.includeNull = includeNull; }
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

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getSlowThresholdMs() { return slowThresholdMs; }
        public void setSlowThresholdMs(long slowThresholdMs) { this.slowThresholdMs = slowThresholdMs; }
        public List<String> getExcludeUrls() { return excludeUrls; }
        public void setExcludeUrls(List<String> excludeUrls) { this.excludeUrls = excludeUrls; }
        public List<String> getSensitiveParams() { return sensitiveParams; }
        public void setSensitiveParams(List<String> sensitiveParams) { this.sensitiveParams = sensitiveParams; }
    }
}
