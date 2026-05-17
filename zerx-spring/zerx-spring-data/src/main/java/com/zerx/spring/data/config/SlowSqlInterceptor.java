package com.zerx.spring.data.config;

import com.zerx.spring.data.properties.ZerxDataProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 慢 SQL 检测拦截器
 * <p>
 * 通过 {@link BeanPostProcessor} 代理 {@code JdbcTemplate}，在 SQL 执行前后记录耗时。
 * 当执行时间超过配置的阈值时，以 WARN 级别日志记录慢 SQL。
 * 敏感参数（如密码、token 等）自动脱敏处理。
 * </p>
 *
 * <h3>功能特性：</h3>
 * <ul>
 *   <li>自动拦截所有 JdbcTemplate 查询、更新、执行方法</li>
 *   <li>超过阈值的 SQL 以 WARN 级别记录（含参数）</li>
 *   <li>正常 SQL 以 DEBUG 级别记录</li>
 *   <li>敏感参数自动脱敏（密码、密钥、token 等）</li>
 * </ul>
 *
 * @author zerx
 */
public class SlowSqlInterceptor implements BeanPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);

    /** 敏感参数关键词（用于自动脱敏） */
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "passwd", "pwd",
            "secret", "token", "credential",
            "api_key", "apikey", "access_key"
    );

    /** 数据库字符串字面量上下文匹配的正则表达式 */
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("'[^']*'");

    /** SQL 中注释匹配 */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/|--[^\\n]*");

    private final ZerxDataProperties properties;
    private final Set<String> sensitiveParams;

    /**
     * 构造函数
     *
     * @param properties 数据模块配置属性
     */
    public SlowSqlInterceptor(ZerxDataProperties properties) {
        this.properties = properties;
        this.sensitiveParams = new HashSet<>();
    }

    /**
     * 添加自定义敏感参数名称
     *
     * @param paramName 参数名
     */
    public void addSensitiveParam(String paramName) {
        if (paramName != null) {
            this.sensitiveParams.add(paramName.toLowerCase());
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (isJdbcTemplate(bean) && properties.getSlowSql().isEnabled()) {
            return createProxy(bean);
        }
        return bean;
    }

    /**
     * 判断是否为 JdbcTemplate 实例
     *
     * @param bean Bean 实例
     * @return 是 JdbcTemplate 返回 {@code true}
     */
    private boolean isJdbcTemplate(Object bean) {
        return ClassUtils.isAssignable(org.springframework.jdbc.core.JdbcTemplate.class, bean.getClass());
    }

    /**
     * 创建 JdbcTemplate 代理
     *
     * @param jdbcTemplate 原始 JdbcTemplate
     * @return 代理后的 JdbcTemplate
     */
    private Object createProxy(Object jdbcTemplate) {
        ProxyFactory proxyFactory = new ProxyFactory(jdbcTemplate);
        proxyFactory.addInterface(org.springframework.jdbc.core.JdbcOperations.class);
        proxyFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) invocation -> {
            String methodName = invocation.getMethod().getName();
            Object[] arguments = invocation.getArguments();

            // 只拦截查询、更新、执行相关方法
            if (shouldIntercept(methodName)) {
                return interceptExecution(methodName, arguments, invocation);
            }
            return invocation.proceed();
        });
        return proxyFactory.getProxy();
    }

    /**
     * 判断方法是否需要拦截
     *
     * @param methodName 方法名
     * @return 需要拦截返回 {@code true}
     */
    private boolean shouldIntercept(String methodName) {
        return methodName.startsWith("query")
                || methodName.startsWith("update")
                || methodName.startsWith("execute")
                || methodName.startsWith("batch");
    }

    /**
     * 拦截执行并记录耗时
     *
     * @param methodName 方法名
     * @param arguments  方法参数
     * @param invocation 方法调用对象
     * @return 执行结果
     * @throws Throwable 执行异常
     */
    private Object interceptExecution(String methodName, Object[] arguments,
                                      org.aopalliance.intercept.MethodInvocation invocation) throws Throwable {
        String sql = extractSql(arguments);
        if (sql == null) {
            return invocation.proceed();
        }

        long startTime = System.nanoTime();
        try {
            Object result = invocation.proceed();
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;

            if (elapsed >= properties.getSlowSql().getThreshold().toMillis()) {
                logSlowSql(sql, arguments, elapsed, methodName);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("SQL executed in {}ms | method={} | sql={}", elapsed, methodName, sql);
                }
            }
            return result;
        } catch (Throwable ex) {
            long elapsed = (System.nanoTime() - startTime) / 1_000_000;
            log.error("SQL failed in {}ms | method={} | sql={} | error={}",
                    elapsed, methodName, sql, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 从方法参数中提取 SQL 语句
     *
     * @param arguments 方法参数
     * @return SQL 语句，无法提取时返回 {@code null}
     */
    private String extractSql(Object[] arguments) {
        if (arguments != null && arguments.length > 0) {
            Object firstArg = arguments[0];
            if (firstArg instanceof String sql) {
                return sql;
            }
        }
        return null;
    }

    /**
     * 记录慢 SQL 日志
     *
     * @param sql        SQL 语句
     * @param arguments  方法参数
     * @param elapsedMs  执行耗时（毫秒）
     * @param methodName 方法名
     */
    private void logSlowSql(String sql, Object[] arguments, long elapsedMs, String methodName) {
        String maskedSql = maskSensitiveData(sql);

        if (properties.getSlowSql().isLogParams() && arguments != null && arguments.length > 1) {
            Object[] maskedParams = Arrays.copyOfRange(arguments, 1, arguments.length);
            for (int i = 0; i < maskedParams.length; i++) {
                maskedParams[i] = maskParam(maskedParams[i]);
            }
            log.warn("Slow SQL detected! elapsed={}ms | method={} | sql={} | params={}",
                    elapsedMs, methodName, maskedSql, Arrays.toString(maskedParams));
        } else {
            log.warn("Slow SQL detected! elapsed={}ms | method={} | sql={}",
                    elapsedMs, methodName, maskedSql);
        }
    }

    /**
     * 脱敏 SQL 中的敏感数据
     *
     * @param sql 原始 SQL
     * @return 脱敏后的 SQL
     */
    private String maskSensitiveData(String sql) {
        String masked = sql;
        for (String keyword : SENSITIVE_KEYWORDS) {
            // 匹配 password = 'xxx' 或 password = :password 等模式
            masked = masked.replaceAll(
                    "(?i)\\b" + Pattern.quote(keyword) + "\\b\\s*=\\s*'[^']*'",
                    keyword + " = '***MASKED***'");
        }
        return masked;
    }

    /**
     * 脱敏参数值
     *
     * @param param 参数值
     * @return 脱敏后的值
     */
    private Object maskParam(Object param) {
        if (param instanceof String str) {
            String lower = str.toLowerCase();
            for (String keyword : SENSITIVE_KEYWORDS) {
                if (lower.contains(keyword)) {
                    return "***MASKED***";
                }
            }
        }
        return param;
    }
}
