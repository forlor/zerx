package com.zerx.spring.data.audit;

import org.springframework.data.domain.AuditorAware;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Zerx 审计感知提供器
 * <p>
 * 实现 Spring Data 的 {@link AuditorAware} 接口，自动从当前请求上下文中获取用户 ID，
 * 用于填充 {@code @CreatedBy} 和 {@code @LastModifiedBy} 审计字段。
 * </p>
 *
 * <p>
 * 通过 {@code @ConditionalOnClass} 条件在
 * {@link com.zerx.spring.data.autoconfigure.ZerxDataAutoConfiguration} 中注册，
 * 仅当 {@code com.zerx.spring.web.context.RequestContext} 可用时才激活。
 * 使用反射访问 RequestContext，避免 {@code zerx-spring-web} 不在 classpath 时的编译/加载问题。
 * </p>
 *
 * @author zerx
 */
public class ZerxAuditorAware implements AuditorAware<Long> {

    /** RequestContext 类名（用于反射调用） */
    private static final String REQUEST_CONTEXT_CLASS = "com.zerx.spring.web.context.RequestContext";

    /** 反射缓存：getUserId 方法 */
    private volatile Method getUserIdMethod;

    /**
     * 获取当前审计人（用户 ID）
     * <p>
     * 通过反射从 {@code RequestContext.getUserId()} 获取当前用户 ID。
     * 如果请求上下文未初始化、用户 ID 为空或 RequestContext 不可用，
     * 返回 {@link Optional#empty()}。
     * </p>
     *
     * @return 当前用户 ID 的 Optional
     */
    @Override
    public Optional<Long> getCurrentAuditor() {
        try {
            Object ctx = getFromRequestContext("get");
            if (ctx != null) {
                Long userId = (Long) getUserIdMethod().invoke(ctx);
                return Optional.ofNullable(userId);
            }
        } catch (Exception e) {
            // 反射失败时安全降级
        }
        return Optional.empty();
    }

    /**
     * 通过反射调用 RequestContext 的静态方法
     *
     * @param methodName 静态方法名
     * @return 方法返回值，失败时返回 {@code null}
     */
    private Object getFromRequestContext(String methodName) {
        try {
            Class<?> clazz = Class.forName(REQUEST_CONTEXT_CLASS);
            Method method = clazz.getMethod(methodName);
            return method.invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取并缓存 RequestContext.getUserId() 方法
     *
     * @return Method 对象
     * @throws Exception 反射异常
     */
    private Method getUserIdMethod() throws Exception {
        if (getUserIdMethod == null) {
            synchronized (this) {
                if (getUserIdMethod == null) {
                    Class<?> clazz = Class.forName(REQUEST_CONTEXT_CLASS);
                    getUserIdMethod = clazz.getMethod("getUserId");
                }
            }
        }
        return getUserIdMethod;
    }
}
