package com.zerx.spring.cache.aspect;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

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

import com.zerx.spring.cache.CacheOps;
import com.zerx.spring.cache.CacheStore;
import com.zerx.spring.cache.annotation.ZerxCacheEvict;
import com.zerx.spring.cache.annotation.ZerxCachePut;
import com.zerx.spring.cache.annotation.ZerxCacheable;
import com.zerx.spring.cache.properties.ZerxCacheProperties;

/**
 * 声明式缓存注解 AOP 切面。
 * <p>
 * 拦截 {@link ZerxCacheable}、{@link ZerxCacheEvict}、{@link ZerxCachePut} 注解，
 * 通过 {@link CacheOps} 和 {@link CacheStore} 执行缓存操作。
 * </p>
 *
 * @author zerx
 */
@Aspect
public class ZerxCacheAspect {

    private static final Logger LOG = LoggerFactory.getLogger(ZerxCacheAspect.class);

    private final CacheOps cacheOps;
    private final CacheStore cacheStore;
    private final ZerxCacheProperties properties;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public ZerxCacheAspect(CacheOps cacheOps, CacheStore cacheStore,
                           ZerxCacheProperties properties) {
        this.cacheOps = cacheOps;
        this.cacheStore = cacheStore;
        this.properties = properties;
    }

    /**
     * 环绕通知：处理 {@link ZerxCacheable} 注解。
     * <p>
     * 执行流程：解析 key → 查缓存 → 命中返回 / miss 执行方法 → 写缓存 → 返回。
     * </p>
     */
    @Around("@annotation(cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint, ZerxCacheable cacheable) throws Throwable {
        String cacheKey = buildCacheKey(cacheable.name(), cacheable.key(), joinPoint);
        long ttl = resolveTtl(cacheable.ttl(), cacheable.timeUnit());

        // 使用 CacheOps 的 Cache-Aside 模式（自动防穿透/防击穿）
        return cacheOps.get(cacheKey, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable t) {
                if (t instanceof RuntimeException rt) {
                    throw rt;
                }
                throw new RuntimeException(t);
            }
        }, ttl, cacheable.timeUnit());
    }

    /**
     * 环绕通知：处理 {@link ZerxCacheEvict} 注解。
     * <p>
     * 方法执行成功后删除对应缓存。
     * </p>
     */
    @Around("@annotation(cacheEvict)")
    public Object aroundCacheEvict(ProceedingJoinPoint joinPoint, ZerxCacheEvict cacheEvict) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            String cacheKey = buildCacheKey(cacheEvict.name(), cacheEvict.key(), joinPoint);

            if (cacheEvict.prefixEvict()) {
                cacheOps.evictByPrefix(cacheKey);
                LOG.debug("Cache evicted by prefix: {}", cacheKey);
            } else {
                cacheOps.evict(cacheKey);
                LOG.debug("Cache evicted: {}", cacheKey);
            }

            return result;
        } catch (Throwable t) {
            // 方法执行失败不删除缓存，保留已有数据
            LOG.debug("Method execution failed, cache eviction skipped: {}", joinPoint.getSignature());
            throw t;
        }
    }

    /**
     * 环绕通知：处理 {@link ZerxCachePut} 注解。
     * <p>
     * 方法执行成功后，将返回值直接写入缓存（不检查是否已存在）。
     * </p>
     */
    @Around("@annotation(cachePut)")
    public Object aroundCachePut(ProceedingJoinPoint joinPoint, ZerxCachePut cachePut) throws Throwable {
        Object result = joinPoint.proceed();
        String cacheKey = buildCacheKey(cachePut.name(), cachePut.key(), joinPoint);
        long ttl = resolveTtl(cachePut.ttl(), cachePut.timeUnit());

        if (result != null) {
            cacheOps.set(cacheKey, result, ttl, cachePut.timeUnit());
            LOG.debug("Cache put: {}", cacheKey);
        } else if (properties.getNullValueTtl().toMillis() > 0) {
            // 缓存空值（防穿透）
            cacheStore.set(cacheKey, com.zerx.spring.cache.CacheConstants.NULL_MARKER,
                    properties.getNullValueTtl());
            LOG.debug("Cache put null marker: {}", cacheKey);
        }

        return result;
    }

    // ======================== Key 解析 ========================

    /**
     * 构建完整的缓存 key。
     * <p>
     * 格式：{@code {name}:{SpEL解析值}} 或 {@code {name}:{methodSignature}:{paramsHash}}（SpEL 为空时）
     * </p>
     */
    String buildCacheKey(String name, String keyExpression, ProceedingJoinPoint joinPoint) {
        String spelValue;
        if (keyExpression != null && !keyExpression.isEmpty()) {
            spelValue = parseSpel(keyExpression, joinPoint);
        } else if (joinPoint != null) {
            // 无 SpEL 表达式时，使用方法签名 + 参数 hashCode
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String paramHash = String.valueOf(java.util.Arrays.hashCode(joinPoint.getArgs()));
            spelValue = signature.getMethod().getName() + ":" + paramHash;
        } else {
            spelValue = keyExpression != null ? keyExpression : "unknown";
        }
        return name + ":" + spelValue;
    }

    /**
     * 解析 SpEL 表达式。
     */
    private String parseSpel(String expression, ProceedingJoinPoint joinPoint) {
        try {
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
            // 兼容 #p0, #p1 形式
            for (int i = 0; i < args.length; i++) {
                context.setVariable("p" + i, args[i]);
            }
            // 兼容 #a0, #a1 形式（Spring 标准）
            for (int i = 0; i < args.length; i++) {
                context.setVariable("a" + i, args[i]);
            }

            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : "null";
        } catch (Exception e) {
            LOG.warn("Failed to parse SpEL expression: '{}', fallback to raw string", expression, e);
            return expression;
        }
    }

    /**
     * 解析 TTL，{@code -1} 回退到全局默认值。
     */
    private long resolveTtl(long ttl, TimeUnit timeUnit) {
        if (ttl > 0) {
            return ttl;
        }
        // 回退到全局默认 TTL
        Duration defaultTtl = properties.getDefaultTtl();
        return switch (timeUnit) {
            case SECONDS -> defaultTtl.toSeconds();
            case MINUTES -> defaultTtl.toMinutes();
            case HOURS -> defaultTtl.toHours();
            case MILLISECONDS -> defaultTtl.toMillis();
            default -> defaultTtl.toSeconds();
        };
    }
}
