package com.zerx.common.enums;

import java.io.Serializable;

/**
 * 业务枚举统一接口
 * <p>
 * 所有业务枚举均应实现此接口，提供统一的 {@link #getCode()} 和 {@link #getDescription()} 方法，
 * 便于前端通用下拉选项渲染、字典表管理等场景。
 * 通过该接口可以统一获取枚举的编码和描述信息，避免每个枚举都定义自己的方法签名。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * public enum UserStatus implements BaseEnum<Integer> {
 *     ACTIVE(1, "正常"),
 *     DISABLED(2, "禁用"),
 *     DELETED(3, "已删除");
 *
 *     private final Integer code;
 *     private final String description;
 *
 *     UserStatus(Integer code, String description) {
 *         this.code = code;
 *         this.description = description;
 *     }
 *
 *     public Integer getCode() { return code; }
 *     public String getDescription() { return description; }
 * }
 * }</pre>
 *
 * @param <C> 枚举编码类型（通常为 Integer 或 String）
 * @author zerx
 */
public interface BaseEnum<C extends Serializable> {

    /**
     * 获取枚举编码
     * <p>
     * 编码通常对应数据库中存储的值，也可以用于前后端数据交互。
     * </p>
     *
     * @return 枚举编码值
     */
    C getCode();

    /**
     * 获取枚举描述信息
     * <p>
     * 描述信息通常用于前端展示、日志输出等场景。
     * </p>
     *
     * @return 枚举的中文描述
     */
    String getDescription();
}
