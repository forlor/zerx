package com.zerx.spring.logging.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.logging.LogRateLimiter;
import com.zerx.common.logging.OpLogContextExtractor;
import com.zerx.spring.logging.annotation.ZerxOpLog;
import com.zerx.spring.logging.event.ZerxOpLogEvent;
import com.zerx.spring.logging.properties.ZerxLoggingProperties;
import com.zerx.spring.logging.service.ZerxOpLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 操作日志 AOP 切面
 * <p>
 * 拦截标注了 {@link ZerxOpLog} 的方法，记录操作信息并发布 {@link ZerxOpLogEvent}。
 * 操作上下文信息从 {@link OpLogContextExtractor} 和 {@link MDC} 中获取。
 * </p>
 *
 * <h3>执行流程：</h3>
 * <ol>
 *   <li>提取方法签名、参数名、参数值</li>
 *   <li>从 OpLogContextExtractor 获取 userId、username、clientIp</li>
 *   <li>执行目标方法</li>
 *   <li>构建 {@link ZerxOpLogEvent}（成功/失败）</li>
 *   <li>通过 {@link ZerxOpLogService#save} 持久化（可选）</li>
 *   <li>输出日志（可选，受 {@code zerx.logging.op-log.log-to-logger} 控制）</li>
 * </ol>
 *
 * @author zerx
 */
@Aspect
public class ZerxOpLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ZerxOpLogAspect.class);

    private final ZerxLoggingProperties properties;
    private final ZerxOpLogService opLogService;
    private final ObjectMapper objectMapper;
    private final LogRateLimiter rateLimiter;
    private final OpLogContextExtractor contextExtractor;
    private final SpelExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 创建操作日志切面
     *
     * @param properties       日志增强配置
     * @param opLogService     操作日志持久化服务（可为 null）
     * @param objectMapper     Jackson ObjectMapper
     * @param rateLimiter      日志限流器（可为 null）
     * @param contextExtractor 操作日志上下文提取器（可为 null）
     */
    public ZerxOpLogAspect(ZerxLoggingProperties properties,
                           ZerxOpLogService opLogService,
                           ObjectMapper objectMapper,
                           LogRateLimiter rateLimiter,
                           OpLogContextExtractor contextExtractor) {
        this.properties = properties;
        this.opLogService = opLogService;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
        this.contextExtractor = contextExtractor;
    }

    /**
     * 环绕通知：拦截标注了 {@link ZerxOpLog} 的方法
     *
     * @param joinPoint 连接点
     * @param opLog     操作日志注解
     * @return 方法返回值
     * @throws Throwable 目标方法抛出的异常
     */
    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, ZerxOpLog opLog) throws Throwable {
        if (!properties.isEnabled() || !properties.getOpLog().isEnabled()) {
            return joinPoint.proceed();
        }

        long startNanos = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        Method method = signature.getMethod();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames == null) {
            paramNames = signature.getParameterNames();
        }
        Object[] paramValues = joinPoint.getArgs();

        String traceId = MDC.get("traceId");
        Long userId = null;
        String username = null;
        String clientIp = null;
        if (contextExtractor != null) {
            OpLogContextExtractor.OpLogContext ctx = contextExtractor.extract();
            if (ctx != null) {
                userId = ctx.userId();
                username = ctx.username();
                clientIp = ctx.clientIp();
            }
        }

        // 解析 SpEL 操作描述
        String description = resolveDescription(opLog.value(), method, paramNames, paramValues);

        Map<String, Object> extra = new LinkedHashMap<>();

        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            if (opLog.recordParams() && paramNames != null && paramNames.length > 0) {
                extra.put("params", truncateParams(paramNames, paramValues, opLog.sensitiveParams()));
            }
            if (opLog.recordResult() && result != null) {
                extra.put("result", truncate(result, properties.getOpLog().getMaxResultLength()));
            }

            ZerxOpLogEvent event = ZerxOpLogEvent.success(
                    traceId, userId, username, opLog.module(), opLog.type(), description,
                    className, methodName, paramNames, paramValues, result, durationMs,
                    clientIp, extra);

            publishEvent(event);
            return result;

        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

            ZerxOpLogEvent event = ZerxOpLogEvent.failure(
                    traceId, userId, username, opLog.module(), opLog.type(), description,
                    className, methodName, paramNames, paramValues, ex, durationMs,
                    clientIp, extra);

            publishEvent(event);
            throw ex;
        }
    }

    /**
     * 发布操作日志事件
     * <p>
     * 先尝试持久化，再输出到日志。如果持久化失败，仅记录错误日志，不影响主流程。
     * </p>
     *
     * @param event 操作日志事件
     */
    private void publishEvent(ZerxOpLogEvent event) {
        // 限流检查
        if (rateLimiter != null && !rateLimiter.tryAcquire(
                event.className() + "." + event.methodName())) {
            return;
        }

        // 通过 SPI 持久化
        if (opLogService != null) {
            try {
                opLogService.save(event);
            } catch (Exception ex) {
                log.error("操作日志持久化失败: {}", ex.getMessage());
            }
        }

        // 输出到日志
        if (properties.getOpLog().isLogToLogger()) {
            logToLogger(event);
        }
    }

    /**
     * 将操作日志输出到 SLF4J Logger
     */
    private void logToLogger(ZerxOpLogEvent event) {
        if (event.exception() != null) {
            log.warn("[OpLog] {} | {} | {} | {}ms | error: {}",
                    event.type(), event.description(),
                    event.className() + "." + event.methodName(),
                    event.durationMs(), event.exception().getMessage());
            if (properties.getOpLog().isRecordExceptionStackTrace()) {
                log.debug("[OpLog] Exception detail:", event.exception());
            }
        } else {
            log.info("[OpLog] {} | {} | {} | {}ms",
                    event.type(), event.description(),
                    event.className() + "." + event.methodName(),
                    event.durationMs());
        }
    }

    /**
     * 解析 SpEL 操作描述
     * <p>
     * 如果描述不以 {@code '} 开头（非纯字符串字面量），尝试作为 SpEL 表达式解析。
     * 解析失败时回退到原始字符串。
     * </p>
     */
    String resolveDescription(String expression, Method method, String[] paramNames, Object[] paramValues) {
        if (expression == null || expression.isEmpty()) {
            return "";
        }
        // 纯静态文本（不包含 #变量引用 或 T(类引用)）直接返回，避免不必要的 SpEL 解析
        if (!expression.contains("#") && !expression.contains("T(")) {
            return expression;
        }
        try {
            EvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null && paramValues != null) {
                for (int i = 0; i < paramNames.length && i < paramValues.length; i++) {
                    context.setVariable(paramNames[i], paramValues[i]);
                }
            }
            // 兼容 #p0, #p1 形式
            if (paramValues != null) {
                for (int i = 0; i < paramValues.length; i++) {
                    context.setVariable("p" + i, paramValues[i]);
                }
            }
            Expression exp = spelParser.parseExpression(expression);
            Object value = exp.getValue(context);
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            log.debug("操作描述 SpEL 解析失败，使用原始值: '{}'", expression);
            return expression;
        }
    }

    /**
     * 截断参数列表，对敏感参数进行脱敏处理
     *
     * @param paramNames      参数名数组
     * @param paramValues     参数值数组
     * @param sensitiveParams 需要脱敏的参数名
     * @return 截断后的参数 Map
     */
    Object truncateParams(String[] paramNames, Object[] paramValues, String[] sensitiveParams) {
        if (paramNames == null || paramNames.length == 0) {
            return null;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        Set<String> sensitiveSet = Set.of(sensitiveParams != null ? sensitiveParams : new String[0]);
        int maxLen = properties.getOpLog().getMaxParamLength();
        for (int i = 0; i < paramNames.length && i < paramValues.length; i++) {
            Object value = paramValues[i];
            if (sensitiveSet.contains(paramNames[i])) {
                params.put(paramNames[i], "******");
            } else {
                params.put(paramNames[i], truncate(value, maxLen));
            }
        }
        return params;
    }

    /**
     * 将对象序列化为 JSON 并截断
     *
     * @param obj    待序列化对象
     * @param maxLen 最大长度
     * @return 截断后的 JSON 字符串
     */
    Object truncate(Object obj, int maxLen) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            if (json.length() > maxLen) {
                return json.substring(0, maxLen) + "...(truncated)";
            }
            return json;
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
