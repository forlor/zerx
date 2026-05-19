package com.zerx.spring.logging.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，AOP 切面自动拦截并发布
 * {@link com.zerx.spring.logging.event.ZerxOpLogEvent}。
 * 业务层通过实现 {@link com.zerx.spring.logging.service.ZerxOpLogService} SPI 接口来持久化操作日志。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @ZerxOpLog(value = "创建订单", type = ZerxOpLog.Type.CREATE, module = "订单管理")
 * @PostMapping("/orders")
 * public Result<Order> createOrder(@RequestBody CreateOrderReq req) {
 *     return Result.success(orderService.create(req));
 * }
 * }</pre>
 *
 * @author zerx
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ZerxOpLog {

    /**
     * 操作描述，支持 SpEL 表达式
     * <p>示例："'用户 ' + #req.username + ' 登录'"</p>
     */
    String value();

    /**
     * 操作类型
     */
    Type type() default Type.OTHER;

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 是否记录入参
     */
    boolean recordParams() default false;

    /**
     * 是否记录返回值
     */
    boolean recordResult() default false;

    /**
     * 入参中需要脱敏的字段名（精确匹配参数名）
     */
    String[] sensitiveParams() default {};

    /**
     * 操作类型枚举
     */
    enum Type {
        /** 登录 */
        LOGIN,
        /** 登出 */
        LOGOUT,
        /** 新增 */
        CREATE,
        /** 修改 */
        UPDATE,
        /** 删除 */
        DELETE,
        /** 导出 */
        EXPORT,
        /** 导入 */
        IMPORT,
        /** 查询 */
        QUERY,
        /** 其他 */
        OTHER
    }
}
