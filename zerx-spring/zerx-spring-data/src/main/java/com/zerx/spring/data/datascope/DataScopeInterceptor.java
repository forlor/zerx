package com.zerx.spring.data.datascope;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * 数据权限 AOP 拦截器。
 * <p>
 * 拦截标注了 {@link DataScope @DataScope} 注解的 Service 方法，
 * 通过 {@link DataScopeHandler} 生成 SQL 条件片段，
 * 并存入 {@link DataScopeContext} 线程变量，
 * 供 {@link com.zerx.spring.data.query.DynamicQuery} 在构建 SQL 时自动追加。
 * </p>
 *
 * <h3>工作流程：</h3>
 * <ol>
 *     <li>Service 方法标注 {@code @DataScope(column = "dept_id", type = Type.DEPT)}</li>
 *     <li>AOP 拦截器在方法执行前读取注解 + 当前用户上下文</li>
 *     <li>调用 {@link DataScopeHandler#generateCondition} 生成 SQL 条件</li>
 *     <li>将条件存入 {@link DataScopeContext} ThreadLocal</li>
 *     <li>Service 内的 DynamicQuery 通过 {@link DataScopeContext#current()} 获取条件并追加</li>
 *     <li>方法执行完毕后清理 ThreadLocal，防止内存泄漏</li>
 * </ol>
 *
 * <h3>DynamicQuery 集成：</h3>
 * <pre>{@code
 * @DataScope(column = "dept_id", type = DataScope.Type.DEPT)
 * public List<User> listUsers(String keyword) {
 *     return DynamicQuery.from(jdbcTemplate, "sys_user")
 *         .eq("status", "ACTIVE")
 *         .like("username", keyword)
 *         .applyDataScope()   // 自动追加 dept_id IN (?,?,?)
 *         .list(rowMapper);
 * }
 * }</pre>
 *
 * @author zerx
 * @see DataScope
 * @see DataScopeHandler
 * @see DataScopeContext
 */
@Aspect
public class DataScopeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    private final DataScopeHandler dataScopeHandler;
    private final DataScopeUserProvider userProvider;

    /**
     * 构造数据权限拦截器。
     *
     * @param dataScopeHandler 数据权限 SQL 条件生成器
     * @param userProvider     当前用户信息提供者
     */
    public DataScopeInterceptor(DataScopeHandler dataScopeHandler,
                                DataScopeUserProvider userProvider) {
        this.dataScopeHandler = dataScopeHandler;
        this.userProvider = userProvider;
    }

    /**
     * 环绕通知：拦截标注 {@link DataScope} 的方法，自动应用数据权限过滤。
     * <p>
     * 在方法执行前解析注解和用户上下文，生成 SQL 条件并存入 ThreadLocal；
     * 方法执行后清理 ThreadLocal，防止线程池复用导致的权限泄漏。
     * </p>
     *
     * @param joinPoint AOP 连接点
     * @param dataScope 数据权限注解
     * @return 方法原始返回值
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        Optional<DataScopeUser> userOpt = userProvider.getCurrentUser();

        if (userOpt.isEmpty()) {
            // 非用户上下文（如定时任务、消息消费），跳过权限过滤
            log.debug("[Zerx] DataScope skipped — no user context for method: {}",
                    joinPoint.getSignature() != null
                            ? joinPoint.getSignature().toShortString()
                            : "unknown");
            return joinPoint.proceed();
        }

        DataScopeUser user = userOpt.get();
        DataScopeHandler.DataScopeSql sqlCondition =
                dataScopeHandler.generateCondition(dataScope, user);

        if (sqlCondition == null) {
            // ALL 权限或无权限约束，跳过
            return joinPoint.proceed();
        }

        try {
            DataScopeContext.set(sqlCondition);
            log.debug("[Zerx] DataScope applied: type={}, condition={}, method={}",
                    dataScope.type(), sqlCondition.condition(),
                    joinPoint.getSignature() != null
                            ? joinPoint.getSignature().toShortString()
                            : "unknown");
            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
