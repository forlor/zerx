package com.zerx.spring.logging.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志增强配置属性
 * <p>
 * 通过 {@code application.yml} 中 {@code zerx.logging.*} 前缀进行配置，
 * 控制操作日志记录、日志脱敏、日志限流、JSON 格式输出等行为。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>{@code
 * zerx:
 *   logging:
 *     enabled: true
 *     op-log:
 *       enabled: true
 *       log-to-logger: true
 *       record-exception-stack-trace: false
 *       max-param-length: 1024
 *       max-result-length: 2048
 *     sensitive-log:
 *       enabled: false
 *       filter-mobile: true
 *       filter-email: true
 *       filter-id-card: true
 *       filter-bank-card: true
 *       filter-password: true
 *       filter-ipv4: false
 *     rate-limit:
 *       enabled: false
 *       max-per-second: 100
 *     json-format:
 *       enabled: false
 *       include-mdc: true
 *       include-logger-name: true
 *       include-thread-name: false
 *       pretty-print: false
 * }</pre>
 *
 * @author zerx
 */
@ConfigurationProperties(prefix = "zerx.logging")
public class ZerxLoggingProperties {

    /** 是否启用日志增强模块 */
    private boolean enabled = true;

    /** 操作日志配置 */
    private OpLog opLog = new OpLog();

    /** 日志脱敏配置 */
    private SensitiveLog sensitiveLog = new SensitiveLog();

    /** 日志限流配置 */
    private RateLimit rateLimit = new RateLimit();

    /** JSON 日志格式配置 */
    private JsonFormat jsonFormat = new JsonFormat();

    // ======================== Getters / Setters ========================

    /**
     * 获取日志增强模块启用状态
     *
     * @return 是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置日志增强模块启用状态
     *
     * @param enabled 是否启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取操作日志配置
     *
     * @return 操作日志配置
     */
    public OpLog getOpLog() {
        return opLog;
    }

    /**
     * 设置操作日志配置
     *
     * @param opLog 操作日志配置
     */
    public void setOpLog(OpLog opLog) {
        this.opLog = opLog;
    }

    /**
     * 获取日志脱敏配置
     *
     * @return 日志脱敏配置
     */
    public SensitiveLog getSensitiveLog() {
        return sensitiveLog;
    }

    /**
     * 设置日志脱敏配置
     *
     * @param sensitiveLog 日志脱敏配置
     */
    public void setSensitiveLog(SensitiveLog sensitiveLog) {
        this.sensitiveLog = sensitiveLog;
    }

    /**
     * 获取日志限流配置
     *
     * @return 日志限流配置
     */
    public RateLimit getRateLimit() {
        return rateLimit;
    }

    /**
     * 设置日志限流配置
     *
     * @param rateLimit 日志限流配置
     */
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * 获取 JSON 日志格式配置
     *
     * @return JSON 日志格式配置
     */
    public JsonFormat getJsonFormat() {
        return jsonFormat;
    }

    /**
     * 设置 JSON 日志格式配置
     *
     * @param jsonFormat JSON 日志格式配置
     */
    public void setJsonFormat(JsonFormat jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    // ======================== 操作日志配置 ========================

    /**
     * 操作日志配置
     *
     * @author zerx
     */
    public static class OpLog {

        /** 是否启用操作日志记录 */
        private boolean enabled = true;

        /** 是否同时输出到日志 */
        private boolean logToLogger = true;

        /** 是否记录异常堆栈 */
        private boolean recordExceptionStackTrace = false;

        /** 参数序列化最大长度 */
        private int maxParamLength = 1024;

        /** 返回值序列化最大长度 */
        private int maxResultLength = 2048;

        /**
         * 获取操作日志启用状态
         *
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置操作日志启用状态
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取是否同时输出到日志
         *
         * @return 是否输出到日志
         */
        public boolean isLogToLogger() {
            return logToLogger;
        }

        /**
         * 设置是否同时输出到日志
         *
         * @param logToLogger 是否输出到日志
         */
        public void setLogToLogger(boolean logToLogger) {
            this.logToLogger = logToLogger;
        }

        /**
         * 获取是否记录异常堆栈
         *
         * @return 是否记录异常堆栈
         */
        public boolean isRecordExceptionStackTrace() {
            return recordExceptionStackTrace;
        }

        /**
         * 设置是否记录异常堆栈
         *
         * @param recordExceptionStackTrace 是否记录异常堆栈
         */
        public void setRecordExceptionStackTrace(boolean recordExceptionStackTrace) {
            this.recordExceptionStackTrace = recordExceptionStackTrace;
        }

        /**
         * 获取参数序列化最大长度
         *
         * @return 最大长度
         */
        public int getMaxParamLength() {
            return maxParamLength;
        }

        /**
         * 设置参数序列化最大长度
         *
         * @param maxParamLength 最大长度
         */
        public void setMaxParamLength(int maxParamLength) {
            this.maxParamLength = maxParamLength;
        }

        /**
         * 获取返回值序列化最大长度
         *
         * @return 最大长度
         */
        public int getMaxResultLength() {
            return maxResultLength;
        }

        /**
         * 设置返回值序列化最大长度
         *
         * @param maxResultLength 最大长度
         */
        public void setMaxResultLength(int maxResultLength) {
            this.maxResultLength = maxResultLength;
        }
    }

    // ======================== 日志脱敏配置 ========================

    /**
     * 日志脱敏配置
     * <p>
     * 控制 Logback Appender 是否对日志消息进行敏感数据脱敏。
     * 脱敏规则由 {@link com.zerx.common.logging.SensitiveLogFilter} 提供。
     * </p>
     *
     * @author zerx
     */
    public static class SensitiveLog {

        /** 是否启用日志脱敏 */
        private boolean enabled = false;

        /** 是否脱敏手机号 */
        private boolean filterMobile = true;

        /** 是否脱敏邮箱 */
        private boolean filterEmail = true;

        /** 是否脱敏身份证号 */
        private boolean filterIdCard = true;

        /** 是否脱敏银行卡号 */
        private boolean filterBankCard = true;

        /** 是否脱敏密码 */
        private boolean filterPassword = true;

        /** 是否脱敏 IPv4 地址 */
        private boolean filterIpv4 = false;

        /**
         * 获取日志脱敏启用状态
         *
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置日志脱敏启用状态
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取是否脱敏手机号
         *
         * @return 是否脱敏手机号
         */
        public boolean isFilterMobile() {
            return filterMobile;
        }

        /**
         * 设置是否脱敏手机号
         *
         * @param filterMobile 是否脱敏手机号
         */
        public void setFilterMobile(boolean filterMobile) {
            this.filterMobile = filterMobile;
        }

        /**
         * 获取是否脱敏邮箱
         *
         * @return 是否脱敏邮箱
         */
        public boolean isFilterEmail() {
            return filterEmail;
        }

        /**
         * 设置是否脱敏邮箱
         *
         * @param filterEmail 是否脱敏邮箱
         */
        public void setFilterEmail(boolean filterEmail) {
            this.filterEmail = filterEmail;
        }

        /**
         * 获取是否脱敏身份证号
         *
         * @return 是否脱敏身份证号
         */
        public boolean isFilterIdCard() {
            return filterIdCard;
        }

        /**
         * 设置是否脱敏身份证号
         *
         * @param filterIdCard 是否脱敏身份证号
         */
        public void setFilterIdCard(boolean filterIdCard) {
            this.filterIdCard = filterIdCard;
        }

        /**
         * 获取是否脱敏银行卡号
         *
         * @return 是否脱敏银行卡号
         */
        public boolean isFilterBankCard() {
            return filterBankCard;
        }

        /**
         * 设置是否脱敏银行卡号
         *
         * @param filterBankCard 是否脱敏银行卡号
         */
        public void setFilterBankCard(boolean filterBankCard) {
            this.filterBankCard = filterBankCard;
        }

        /**
         * 获取是否脱敏密码
         *
         * @return 是否脱敏密码
         */
        public boolean isFilterPassword() {
            return filterPassword;
        }

        /**
         * 设置是否脱敏密码
         *
         * @param filterPassword 是否脱敏密码
         */
        public void setFilterPassword(boolean filterPassword) {
            this.filterPassword = filterPassword;
        }

        /**
         * 获取是否脱敏 IPv4 地址
         *
         * @return 是否脱敏 IPv4 地址
         */
        public boolean isFilterIpv4() {
            return filterIpv4;
        }

        /**
         * 设置是否脱敏 IPv4 地址
         *
         * @param filterIpv4 是否脱敏 IPv4 地址
         */
        public void setFilterIpv4(boolean filterIpv4) {
            this.filterIpv4 = filterIpv4;
        }
    }

    // ======================== 日志限流配置 ========================

    /**
     * 日志限流配置
     * <p>
     * 基于 {@link com.zerx.common.logging.LogRateLimiter} 实现，
     * 防止同类日志在短时间内大量重复输出。
     * </p>
     *
     * @author zerx
     */
    public static class RateLimit {

        /** 是否启用日志限流 */
        private boolean enabled = false;

        /** 每秒允许通过的最大日志数 */
        private int maxPerSecond = 100;

        /**
         * 获取日志限流启用状态
         *
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置日志限流启用状态
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取每秒允许通过的最大日志数
         *
         * @return 最大日志数
         */
        public int getMaxPerSecond() {
            return maxPerSecond;
        }

        /**
         * 设置每秒允许通过的最大日志数
         *
         * @param maxPerSecond 最大日志数
         */
        public void setMaxPerSecond(int maxPerSecond) {
            this.maxPerSecond = maxPerSecond;
        }
    }

    // ======================== JSON 日志格式配置 ========================

    /**
     * JSON 日志格式配置
     * <p>
     * 将日志以 JSON 格式输出，便于日志采集系统（如 ELK）解析。
     * </p>
     *
     * @author zerx
     */
    public static class JsonFormat {

        /** 是否启用 JSON 格式 */
        private boolean enabled = false;

        /** 是否包含 MDC 上下文 */
        private boolean includeMdc = true;

        /** 是否包含 Logger 名称 */
        private boolean includeLoggerName = true;

        /** 是否包含线程名 */
        private boolean includeThreadName = false;

        /** 是否美化输出（开发环境推荐） */
        private boolean prettyPrint = false;

        /**
         * 获取 JSON 格式启用状态
         *
         * @return 是否启用
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * 设置 JSON 格式启用状态
         *
         * @param enabled 是否启用
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * 获取是否包含 MDC 上下文
         *
         * @return 是否包含 MDC
         */
        public boolean isIncludeMdc() {
            return includeMdc;
        }

        /**
         * 设置是否包含 MDC 上下文
         *
         * @param includeMdc 是否包含 MDC
         */
        public void setIncludeMdc(boolean includeMdc) {
            this.includeMdc = includeMdc;
        }

        /**
         * 获取是否包含 Logger 名称
         *
         * @return 是否包含 Logger 名称
         */
        public boolean isIncludeLoggerName() {
            return includeLoggerName;
        }

        /**
         * 设置是否包含 Logger 名称
         *
         * @param includeLoggerName 是否包含 Logger 名称
         */
        public void setIncludeLoggerName(boolean includeLoggerName) {
            this.includeLoggerName = includeLoggerName;
        }

        /**
         * 获取是否包含线程名
         *
         * @return 是否包含线程名
         */
        public boolean isIncludeThreadName() {
            return includeThreadName;
        }

        /**
         * 设置是否包含线程名
         *
         * @param includeThreadName 是否包含线程名
         */
        public void setIncludeThreadName(boolean includeThreadName) {
            this.includeThreadName = includeThreadName;
        }

        /**
         * 获取是否美化输出
         *
         * @return 是否美化输出
         */
        public boolean isPrettyPrint() {
            return prettyPrint;
        }

        /**
         * 设置是否美化输出
         *
         * @param prettyPrint 是否美化输出
         */
        public void setPrettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
        }
    }
}
