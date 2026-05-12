package com.zerx.common.util;

import com.zerx.common.enums.BaseEnum;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 枚举工具类
 * <p>
 * 提供枚举的通用操作方法，与 {@link BaseEnum} 接口配合使用。
* 支持按 code 查找枚举、获取所有枚举值列表、转为 Map 等常用操作，
 * 减少业务代码中重复的枚举处理逻辑。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 按 code 查找枚举
 * UserStatus status = EnumUtil.findByCode(UserStatus.class, 1);
 *
 * // 获取所有枚举的 code-description 列表（用于前端下拉选项）
 * List<Pair<Integer, String>> options = EnumUtil.toOptions(UserStatus.class);
 *
 * // 判断 code 是否有效
 * boolean valid = EnumUtil.isValidCode(UserStatus.class, 1);
 * }</pre>
 *
 * @author zerx
 */
public final class EnumUtil {

    /** 私有构造器，防止实例化 */
    private EnumUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== BaseEnum 操作 ========================

    /**
     * 根据 code 查找实现了 {@link BaseEnum} 的枚举
     *
     * @param enumClass 枚举类
     * @param code      枚举编码
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return 匹配的枚举值，未找到返回 null
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> E findByCode(Class<E> enumClass, C code) {
        if (enumClass == null || code == null) {
            return null;
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (E e : constants) {
            if (Objects.equals(e.getCode(), code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据 code 查找枚举，未找到时抛出异常
     *
     * @param enumClass 枚举类
     * @param code      枚举编码
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return 匹配的枚举值
     * @throws IllegalArgumentException 未找到匹配的枚举时抛出
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> E requireByCode(Class<E> enumClass, C code) {
        E result = findByCode(enumClass, code);
        if (result == null) {
            throw new IllegalArgumentException(
                    String.format("未找到编码为 [%s] 的枚举值: %s", code, enumClass.getSimpleName()));
        }
        return result;
    }

    /**
     * 根据 description 查找枚举
     *
     * @param enumClass   枚举类
     * @param description 枚举描述
     * @param <E>         枚举类型
     * @param <C>         编码类型
     * @return 匹配的枚举值，未找到返回 null
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> E findByDescription(Class<E> enumClass, String description) {
        if (enumClass == null || description == null) {
            return null;
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return null;
        }
        for (E e : constants) {
            if (Objects.equals(e.getDescription(), description)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 判断 code 是否为有效的枚举编码
     *
     * @param enumClass 枚举类
     * @param code      枚举编码
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return 有效返回 true
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> boolean isValidCode(Class<E> enumClass, C code) {
        return findByCode(enumClass, code) != null;
    }

    /**
     * 获取枚举的描述信息，未找到时返回默认值
     *
     * @param enumClass    枚举类
     * @param code         枚举编码
     * @param defaultValue 默认描述
     * @param <E>          枚举类型
     * @param <C>          编码类型
     * @return 枚举描述或默认值
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> String getDescription(Class<E> enumClass, C code, String defaultValue) {
        E e = findByCode(enumClass, code);
        return e != null ? e.getDescription() : defaultValue;
    }

    /**
     * 获取枚举的描述信息，未找到时返回 code 的字符串形式
     *
     * @param enumClass 枚举类
     * @param code      枚举编码
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return 枚举描述或 code 的字符串形式
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> String getDescription(Class<E> enumClass, C code) {
        return getDescription(enumClass, code, code == null ? "null" : code.toString());
    }

    // ======================== 转换操作 ========================

    /**
     * 将 BaseEnum 枚举转为 code → description 的 Map
     * <p>
     * 适用于前端下拉选项渲染、缓存字典等场景。
     * </p>
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return code → description 的 Map
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> Map<C, String> toMap(Class<E> enumClass) {
        if (enumClass == null) {
            return Map.of();
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return Map.of();
        }
        return Arrays.stream(constants)
                .collect(Collectors.toMap(BaseEnum::getCode, BaseEnum::getDescription, (a, b) -> a));
    }

    /**
     * 将 BaseEnum 枚举转为 code → 枚举实例的 Map
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return code → 枚举实例的 Map
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> Map<C, E> toEnumMap(Class<E> enumClass) {
        if (enumClass == null) {
            return Map.of();
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return Map.of();
        }
        return Arrays.stream(constants)
                .collect(Collectors.toMap(BaseEnum::getCode, e -> e, (a, b) -> a));
    }

    /**
     * 获取所有枚举值的列表
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @return 枚举值列表
     */
    public static <E extends Enum<E>> List<E> toList(Class<E> enumClass) {
        if (enumClass == null) {
            return List.of();
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null) {
            return List.of();
        }
        return List.of(constants);
    }

    /**
     * 获取所有实现了 BaseEnum 的枚举值列表
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return 枚举值列表
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> List<E> toBaseEnumList(Class<E> enumClass) {
        return toList(enumClass);
    }

    /**
     * 获取所有枚举的 code 列表
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return code 列表
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> List<C> codeList(Class<E> enumClass) {
        if (enumClass == null) {
            return List.of();
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return List.of();
        }
        return Arrays.stream(constants)
                .map(BaseEnum::getCode)
                .toList();
    }

    /**
     * 获取所有枚举的 description 列表
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return description 列表
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> List<String> descriptionList(Class<E> enumClass) {
        if (enumClass == null) {
            return List.of();
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return List.of();
        }
        return Arrays.stream(constants)
                .map(BaseEnum::getDescription)
                .toList();
    }

    /**
     * 将 BaseEnum 枚举转为 Pair 列表（用于前端下拉选项）
     * <p>
     * 每个 Pair 的 left 为 code，right 为 description。
     * </p>
     *
     * @param enumClass 枚举类
     * @param <E>       枚举类型
     * @param <C>       编码类型
     * @return Pair(code, description) 列表
     */
    public static <E extends Enum<E> & BaseEnum<C>, C extends Serializable> List<com.zerx.common.model.Pair<C, String>> toOptions(Class<E> enumClass) {
        if (enumClass == null) {
            return List.of();
        }
        E[] constants = enumClass.getEnumConstants();
        if (constants == null || constants.length == 0) {
            return List.of();
        }
        return Arrays.stream(constants)
                .map(e -> com.zerx.common.model.Pair.of(e.getCode(), e.getDescription()))
                .toList();
    }

    // ======================== 普通枚举操作 ========================

    /**
     * 根据枚举名称获取枚举值（忽略大小写）
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 匹配的枚举值，未找到返回 null
     */
    public static <E extends Enum<E>> E findByName(Class<E> enumClass, String name) {
        if (enumClass == null || name == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 判断枚举名称是否有效
     *
     * @param enumClass 枚举类
     * @param name      枚举名称
     * @param <E>       枚举类型
     * @return 有效返回 true
     */
    public static <E extends Enum<E>> boolean isValidName(Class<E> enumClass, String name) {
        return findByName(enumClass, name) != null;
    }

    /**
     * 按条件过滤枚举值
     *
     * @param enumClass 枚举类
     * @param predicate 过滤条件
     * @param <E>       枚举类型
     * @return 符合条件的枚举列表
     */
    public static <E extends Enum<E>> List<E> filter(Class<E> enumClass, Predicate<E> predicate) {
        if (enumClass == null || predicate == null) {
            return List.of();
        }
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(predicate)
                .toList();
    }

    /**
     * 判断枚举类是否包含指定值
     *
     * @param value 枚举值
     * @param <E>   枚举类型
     * @return 包含返回 true
     */
    public static <E extends Enum<E>> boolean contains(E value) {
        return value != null;
    }
}
