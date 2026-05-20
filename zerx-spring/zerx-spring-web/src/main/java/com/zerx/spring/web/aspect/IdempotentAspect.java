package com.zerx.spring.web.aspect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

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
import com.zerx.spring.web.annotation.Idempotent;
import com.zerx.spring.web.context.RequestContext;

/**
 * 幂等性切面 — 基于 ConcurrentHashMap 实现请求去重。
 * <p>
 * 拦截标注了 {@link Idempotent} 注解的 Controller 方法，
 * 在指定时间窗口内，相同请求指纹只会被执行一次。
 * </p>
 *
 * <h3>实现细节：</h3>
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap} 存储 key → 过期时间戳的映射</li>
 *   <li>幂等 key = "idempotent:" + userId + ":" + URI + ":" + (SpEL key 或参数 hash)</li>
 *   <li>第一次请求：记录 key + 过期时间戳，执行方法</li>
 *   <li>重复请求：发现 key 未过期，抛出 {@link BusinessException}</li>
 *   <li>惰性清理：每次访问时顺便检查并清理过期条目</li>
 * </ul>
 *
 * @author zerx
 */
@Aspect
public class IdempotentAspect {

    private static final Logger LOG = LoggerFactory.getLogger(IdempotentAspect.class);

    /** 幂等性记录存储：key → 过期时间戳（毫秒） */
    private final ConcurrentHashMap<String, Long> idempotentKeys = new ConcurrentHashMap<>();

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
     * 环绕通知：拦截 {@link Idempotent} 注解，执行幂等性检查。
     *
     * @param joinPoint  切点
     * @param idempotent 幂等性注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(idempotent)")
    public Object aroundIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String key = buildIdempotentKey(idempotent, joinPoint);
        long ttlMs = idempotent.timeUnit().toMillis(idempotent.ttl());
        long now = System.currentTimeMillis();
        long expireAt = now + ttlMs;

        // CAS 风格的 putIfAbsent：如果 key 不存在或已过期则写入新值
        Long existingExpireAt = idempotentKeys.putIfAbsent(key, expireAt);

        if (existingExpireAt != null) {
            // key 已存在，检查是否过期
            if (existingExpireAt > now) {
                // 未过期 → 重复请求
                LOG.warn("Idempotent check failed (duplicate request): key={}, expiresAt={}", key, existingExpireAt);
                throw new BusinessException(ErrorCode.STATE_CONFLICT, "请勿重复提交");
            }

            // 已过期 → 替换为新的过期时间
            idempotentKeys.put(key, expireAt);
        }

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            // 方法执行失败时移除幂等记录，允许重试
            idempotentKeys.remove(key, expireAt);
            LOG.debug("Method execution failed, idempotent key removed: key={}", key);
            throw t;
        } finally {
            // 惰性清理过期条目
            maybeCleanup(now);
        }
    }

    /**
     * 构建幂等性 key。
     * <p>
     * 格式：{@code idempotent:{userId}:{uri}:{resolvedKey}}
     * </p>
     *
     * @param idempotent 幂等性注解
     * @param joinPoint  切点
     * @return 幂等性 key
     */
    private String buildIdempotentKey(Idempotent idempotent, ProceedingJoinPoint joinPoint) {
        String userId = resolveUserId();
        String uri = resolveRequestUri();
        String resolvedKey;

        if (!idempotent.key().isEmpty()) {
            resolvedKey = parseSpel(idempotent.key(), joinPoint);
        } else {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            resolvedKey = method.getName() + ":" + Arrays.hashCode(joinPoint.getArgs());
        }

        return "idempotent:" + userId + ":" + uri + ":" + resolvedKey;
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
     * 获取当前用户 ID，未登录时使用请求 IP 作为回退。
     */
    private String resolveUserId() {
        Long userId = RequestContext.getUserId();
        if (userId != null) {
            return String.valueOf(userId);
        }
        // 回退：使用客户端 IP
        String ip = RequestContext.getRequestIp();
        return ip != null ? ip : "anonymous";
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
     * 惰性清理过期的幂等性记录。
     * <p>
     * 每 {@link #CLEANUP_INTERVAL} 次请求触发一次全量扫描，
     * 移除已过期的条目。
     * </p>
     *
     * @param now 当前时间戳
     */
    private void maybeCleanup(long now) {
        if (++requestCount % CLEANUP_INTERVAL != 0) {
            return;
        }

        idempotentKeys.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
