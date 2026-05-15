package com.zerx.common.enums;

/**
 * 逻辑删除标记枚举
 * <p>
 * 适用于业务实体的逻辑删除状态管理。
 * 使用枚举代替原始 Integer 常量，提供类型安全和语义清晰的表达。
 * </p>
 *
 * @author zerx
 */
public enum DeleteFlag implements BaseEnum<Integer> {

    /** 已删除 */
    DELETED(1, "已删除"),

    /** 未删除 */
    NOT_DELETED(0, "未删除");

    /** 删除标记值 */
    private final Integer code;

    /** 描述 */
    private final String description;

    DeleteFlag(Integer code, String description) {
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
     * 判断当前标记是否为已删除
     *
     * @return 已删除返回 true
     */
    public boolean isDeleted() {
        return this == DELETED;
    }

    /**
     * 判断当前标记是否为未删除
     *
     * @return 未删除返回 true
     */
    public boolean isNotDeleted() {
        return this == NOT_DELETED;
    }

    /**
     * 根据标记值获取枚举
     *
     * @param code 删除标记值
     * @return 对应的枚举值，未找到返回 null
     */
    public static DeleteFlag fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeleteFlag flag : values()) {
            if (flag.code.equals(code)) {
                return flag;
            }
        }
        return null;
    }

    /**
     * 判断标记值是否为已删除
     *
     * @param code 删除标记值
     * @return 已删除返回 true
     */
    public static boolean isDeleted(Integer code) {
        return DELETED.code.equals(code);
    }

    /**
     * 判断标记值是否为未删除
     *
     * @param code 删除标记值
     * @return 未删除返回 true
     */
    public static boolean isNotDeleted(Integer code) {
        return NOT_DELETED.code.equals(code);
    }
}
