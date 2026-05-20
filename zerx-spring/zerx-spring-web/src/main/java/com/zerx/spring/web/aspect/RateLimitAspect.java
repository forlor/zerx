package com.zerx.spring.web.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.zerx.common.exception.BusinessException;
import com.zerx.common.exception.ErrorCode;
import com.zerx.spring.web.annotation.RateLimit;
import com.zerx.spring.web.context.RequestContext;

/**
 * 限流切面 — 基于 ConcurrentHashMap + 滑动窗口实现内存级请求频率限制。
 * <p>
 * 拦截标注了 {@link RateLimit} 注解的 Controller 方法，
 * 在时间窗口内统计请求次数，超出容量时抛出 {@link BusinessException}。
 * </p>
 *
 * <h3>实现细节：</h3>
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap} 存储 key → {@link WindowCounter} 的映射</li>
 *   <li>滑动窗口：每次请求时检查窗口是否过期，过期则重置窗口和计数</li>
 *   <li>限流 key = 请求 IP + 自定义 SpEL key 或 URI + 方法签名</li>
 *   <li>惰性清理：每次访问时顺便检查并清理过期的计数器</li>
 * </ul>
 *
 * @author zerx
 */
@Aspect
public class RateLimitAspect {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitAspect.class);

    /** 限流计数器存储 */
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    /** SpEL 表达式解析器 */
    private final SpelExpressionParser parser = new SpelExpressionParser();

    /** 参数名发现器 */
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /** SpEL 表达式缓存 */
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    /** 惰性清理采样间隔（每 N 次请求触发一次过期清理） */
    private static final int CLEANUP_INTERVAL = 100;

    /** 请求计数（用于触发惰性清理） */
    private volatile int requestCount = 0;

    /**
     * 环绕通知：拦截 {@link RateLimit} 注解，执行限流检查。
     *
     * @param joinPoint 切点
     * @param rateLimit 限流注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(rateLimit)")
    public Object aroundRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String limitKey = buildLimitKey(rateLimit, joinPoint);

        if (isRateLimited(limitKey, rateLimit)) {
            LOG.warn("Rate limit exceeded: key={}, capacity={}, window={}s",
                    limitKey, rateLimit.capacity(), rateLimit.windowSeconds());
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }

        // 惰性清理过期计数器
        maybeCleanup();

        return joinPoint.proceed();
    }

    /**
     * 检查是否超过限流阈值。
     * <p>
     * 使用 CAS 风格的滑动窗口：
     * 如果窗口已过期则重置；如果窗口内计数未超限则递增并放行。
     * </p>
     *
     * @param key       限流 key
     * @param rateLimit 限流注解配置
     * @return {@code true} 表示被限流（拒绝），{@code false} 表示放行
     */
    private boolean isRateLimited(String key, RateLimit rateLimit) {
        long now = System.currentTimeMillis();
        long windowMs = rateLimit.windowSeconds() * 1000L;

        WindowCounter counter = counters.computeIfAbsent(key,
                k -> new WindowCounter(now, 0));

        synchronized (counter) {
            // 窗口已过期 → 重置
            if (now - counter.windowStart >= windowMs) {
                counter.windowStart = now;
                counter.count = 0;
            }

            // 未超限 → 递增并放行
            if (counter.count < rateLimit.capacity()) {
                counter.count++;
                return false;
            }

            // 超限
            return true;
        }
    }

    /**
     * 构建限流 key。
     * <p>
     * 格式：{@code rateLimit:{clientIp}:{resolvedKey}}
     * </p>
     *
     * @param rateLimit 限流注解
     * @param joinPoint 切点
     * @return 限流 key
     */
    private String buildLimitKey(RateLimit rateLimit, ProceedingJoinPoint joinPoint) {
        String clientIp = resolveClientIp();
        String resolvedKey;

        if (!rateLimit.key().isEmpty()) {
            resolvedKey = parseSpel(rateLimit.key(), joinPoint);
        } else {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String uri = resolveRequestUri();
            resolvedKey = uri + ":" + method.getName()
                    + ":" + Arrays.hashCode(joinPoint.getArgs());
        }

        return "rateLimit:" + clientIp + ":" + resolvedKey;
    }

    /**
     * 解析 SpEL 表达式。
     *
     * @param expression SpEL 表达式字符串
     * @param joinPoint  切点
     * @return 解析结果
     */
    private String parseSpel(String expression, ProceedingJoinPoint joinPoint) {
        try {
            EvaluationContext context = buildEvaluationContext(joinPoint);
            Expression exp = expressionCache.computeIfAbsent(expression, parser::parseExpression);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : "null";
        } catch (Exception e) {
            LOG.warn("Failed to parse SpEL expression: '{}', fallback to raw string", expression, e);
            return expression;
        }
    }

    /**
     * 构建 SpEL 求值上下文，将方法参数注册为变量。
     */
    private EvaluationContext buildEvaluationContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
            context.setVariable("a" + i, args[i]);
        }
        return context;
    }

    /**
     * 从 RequestContext 获取客户端 IP，回退到 RequestContextHolder。
     */
    private String resolveClientIp() {
        String ip = RequestContext.getRequestIp();
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        // 回退：从 Servlet 请求中获取
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                int commaIndex = xForwardedFor.indexOf(',');
                return commaIndex > 0
                        ? xForwardedFor.substring(0, commaIndex).trim()
                        : xForwardedFor.trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }

    /**
     * 获取当前请求 URI。
     */
    private String resolveRequestUri() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getRequestURI();
        }
        return "unknown";
    }

    /**
     * 惰性清理过期的计数器。
     * <p>
     * 每 {@link #CLEANUP_INTERVAL} 次请求触发一次全量扫描，
     * 移除窗口已过期超过 10 倍窗口时间的条目。
     * </p>
     */
    private void maybeCleanup() {
        if (++requestCount % CLEANUP_INTERVAL != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        counters.entrySet().removeIf(entry -> {
            WindowCounter counter = entry.getValue();
            synchronized (counter) {
                // 窗口过期超过 60 秒的条目视为可清理
                return now - counter.windowStart > 60_000L;
            }
        });
    }

    /**
     * 滑动窗口计数器。
     * <p>
     * 记录窗口起始时间和当前窗口内的请求计数。
     * </p>
     */
    static final class WindowCounter {

        /** 窗口开始时间（毫秒时间戳） */
        volatile long windowStart;

        /** 当前窗口内的请求计数 */
        volatile int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
