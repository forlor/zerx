package com.zerx.spring.data.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解。
 * <p>
 * 标注在 Service 方法上，声明该方法需要应用数据权限过滤。
 * 拦截器会根据当前用户的权限范围自动追加 SQL WHERE 条件，
 * 限制用户只能查询到有权限的数据。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 只看自己的数据
 * {@literal @}DataScope(column = "create_by")
 * public List<Order> listMyOrders() { ... }
 *
 * // 看本部门数据
 * {@literal @}DataScope(column = "dept_id", type = DataScope.Type.DEPT)
 * public List<User> listDeptUsers() { ... }
 *
 * // 看本部门及子部门数据
 * {@literal @}DataScope(column = "dept_id", type = DataScope.Type.DEPT_AND_CHILD)
 * public List<User> listDeptAndChildUsers() { ... }
 * }</pre>
 *
 * @author zerx
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * 数据权限控制的数据库列名
     * <p>
     * 如 "create_by"、"dept_id" 等，拦截器会追加 {@code column = ?} 条件
     * </p>
     */
    String column();

    /**
     * 数据权限类型
     */
    Type type() default Type.SELF;

    /**
     * 数据权限类型枚举
     */
    enum Type {

        /**
         * 本人数据：{@code column = currentUser.id}
         */
        SELF,

        /**
         * 本部门数据：{@code column IN (当前用户所在部门的ID)}
         */
        DEPT,

        /**
         * 本部门及子部门数据：{@code column IN (当前部门及所有子部门的ID)}
         */
        DEPT_AND_CHILD,

        /**
         * 全部数据（不追加条件）
         */
        ALL
    }
}
