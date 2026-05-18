package com.zerx.spring.data.logicaldelete;

import java.lang.annotation.*;

/**
 * 标注在实体类上，表示该实体支持逻辑删除。
 * <p>
 * 标注后，通过 {@link LogicalDeleteService} 进行的删除操作将转为 UPDATE（设置删除标记），
 * 而非物理 DELETE。业务代码应使用 LogicalDeleteService 而非 Repository.delete()。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * @LogicalDelete
 * public class User extends BaseEntity {
 *     // ...
 * }
 * }</pre>
 *
 * <p>
 * 配合 {@link LogicalDeleteService} 使用：
 * </p>
 * <pre>{@code
 * logicalDeleteService.deleteById(userId, User.class);
 * logicalDeleteService.restoreById(userId, User.class);
 * }</pre>
 *
 * @author zerx
 * @see LogicalDeleteService
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogicalDelete {

    /**
     * 删除标记列名
     */
    String column() default "deleted";

    /**
     * 已删除值
     */
    String deletedValue() default "1";

    /**
     * 未删除值
     */
    String notDeletedValue() default "0";
}
