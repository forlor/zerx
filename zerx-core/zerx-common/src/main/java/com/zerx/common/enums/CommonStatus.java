package com.zerx.common.enums;

/**
 * 通用状态枚举
 * <p>
 * 适用于绝大多数业务实体的启用/禁用状态管理，
 * 如用户状态、商品状态、配置项状态等。
 * </p>
 *
 * @author zerx
 */
public enum CommonStatus implements BaseEnum<Integer> {

    /** 启用 */
    ENABLED(1, "启用"),

    /** 禁用 */
    DISABLED(0, "禁用");

    /** 状态码 */
    private final Integer code;

    /** 状态描述 */
    private final String description;

    CommonStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    // ======================== 便捷方法 ========================

    /**
     * 判断当前状态是否为启用
     *
     * @return 启用返回 true
     */
    public boolean isEnabled() {
        return this == ENABLED;
    }

    /**
     * 判断当前状态是否为禁用
     *
     * @return 禁用返回 true
     */
    public boolean isDisabled() {
        return this == DISABLED;
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 对应的枚举值，未找到返回 null
     */
    public static CommonStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (CommonStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据状态码获取枚举，未找到时返回默认值
     *
     * @param code         状态码
     * @param defaultValue 默认值
     * @return 对应的枚举值
     */
    public static CommonStatus fromCode(Integer code, CommonStatus defaultValue) {
        CommonStatus result = fromCode(code);
        return result != null ? result : defaultValue;
    }

    /**
     * 判断状态码是否为启用状态
     *
     * @param code 状态码
     * @return 启用返回 true
     */
    public static boolean isEnabled(Integer code) {
        return ENABLED.code.equals(code);
    }

    /**
     * 判断状态码是否为禁用状态
     *
     * @param code 状态码
     * @return 禁用返回 true
     */
    public static boolean isDisabled(Integer code) {
        return DISABLED.code.equals(code);
    }

    /**
     * 将布尔值转换为状态
     * <p>
     * true → ENABLED, false → DISABLED
     * </p>
     *
     * @param enabled 是否启用
     * @return 对应的状态枚举
     */
    public static CommonStatus of(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }
}
