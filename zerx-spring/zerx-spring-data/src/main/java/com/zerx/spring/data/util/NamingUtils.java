package com.zerx.spring.data.util;

import org.springframework.data.relational.core.mapping.Table;

/**
 * 命名工具类。
 * <p>
 * 提供实体类到数据库表名的解析、驼峰转下划线等通用命名转换方法，
 * 供模块内多个组件共享使用，避免重复实现。
 * </p>
 *
 * @author zerx
 */
public final class NamingUtils {

    private NamingUtils() {
    }

    /**
     * 解析实体类对应的数据库表名。
     * <p>
     * 优先使用 {@link Table @Table} 注解指定的表名，
     * 否则使用类名的 snake_case 转换。
     * </p>
     *
     * @param entityClass 实体类型
     * @return 表名
     */
    public static String resolveTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.value().isBlank()) {
            return tableAnnotation.value();
        }
        return camelToSnake(entityClass.getSimpleName());
    }

    /**
     * 驼峰命名转下划线命名。
     * <p>
     * 例如：{@code "sysUser" → "sys_user"}，{@code "UserOrder" → "user_order"}。
     * 连续大写字母视为一个单词（如 {@code "XMLParser" → "xml_parser"}）。
     * </p>
     *
     * @param str 驼峰命名字符串
     * @return 下划线命名字符串
     */
    public static String camelToSnake(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    char prev = str.charAt(i - 1);
                    // 连续大写时不插入下划线（如 XML 中的 X, M, L），除非下一个字符是小写
                    boolean nextIsLower = (i + 1 < str.length())
                            && Character.isLowerCase(str.charAt(i + 1));
                    if (!Character.isUpperCase(prev) || nextIsLower) {
                        result.append('_');
                    }
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
