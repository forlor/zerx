package com.zerx.common.enums;

/**
 * 是否枚举
 * <p>
 * 适用于表示"是/否"二元选择的业务场景，
 * 如是否删除、是否默认、是否必填等。
 * 与 {@link CommonStatus} 不同，YesNo 语义上更偏向逻辑判断，
 * 而 CommonStatus 语义上更偏向业务实体状态管理。
 * </p>
 *
 * @author zerx
 */
public enum YesNo implements BaseEnum<String> {

    /** 是 */
    YES("Y", "是"),

    /** 否 */
    NO("N", "否");

    /** 编码 */
    private final String code;

    /** 描述 */
    private final String description;

    YesNo(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return description;
    }

    // ======================== 便捷方法 ========================

    /**
     * 判断当前值是否为"是"
     *
     * @return 是返回 true
     */
    public boolean isYes() {
        return this == YES;
    }

    /**
     * 判断当前值是否为"否"
     *
     * @return 否返回 true
     */
    public boolean isNo() {
        return this == NO;
    }

    /**
     * 根据编码获取枚举
     *
     * @param code 编码（"Y" 或 "N"）
     * @return 对应的枚举值，未找到返回 null
     */
    public static YesNo fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (YesNo yesNo : values()) {
            if (yesNo.code.equalsIgnoreCase(code)) {
                return yesNo;
            }
        }
        return null;
    }

    /**
     * 根据编码获取枚举，未找到时返回默认值
     *
     * @param code         编码
     * @param defaultValue 默认值
     * @return 对应的枚举值
     */
    public static YesNo fromCode(String code, YesNo defaultValue) {
        YesNo result = fromCode(code);
        return result != null ? result : defaultValue;
    }

    /**
     * 将布尔值转换为 YesNo
     *
     * @param value 布尔值
     * @return true → YES, false → NO
     */
    public static YesNo of(boolean value) {
        return value ? YES : NO;
    }

    /**
     * 判断编码是否为"是"
     * <p>
     * 支持不区分大小写匹配 "Y"。
     * </p>
     *
     * @param code 编码
     * @return 是返回 true
     */
    public static boolean isYes(String code) {
        return YES.code.equalsIgnoreCase(code);
    }

    /**
     * 判断编码是否为"否"
     *
     * @param code 编码
     * @return 否返回 true
     */
    public static boolean isNo(String code) {
        return NO.code.equalsIgnoreCase(code);
    }
}
