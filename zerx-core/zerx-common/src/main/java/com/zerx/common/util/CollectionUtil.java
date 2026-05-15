package com.zerx.common.util;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 集合工具类
 * <p>
 * 提供集合常用操作，包括判空、分组、去重、过滤、折叠、排序等。
 * 充分利用 JDK 21 Stream API 特性，提供简洁的链式调用风格。
 * </p>
 *
 * @author zerx
 */
public final class CollectionUtil {

    /** 私有构造器，防止实例化 */
    private CollectionUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 判空 ========================

    /**
     * 判断集合是否为 null 或空
     *
     * @param collection 待判断的集合
     * @param <T>        元素类型
     * @return 为 null 或空返回 true
     */
    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为 null 且不为空
     *
     * @param collection 待判断的集合
     * @param <T>        元素类型
     * @return 有元素返回 true
     */
    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为 null 或空
     *
     * @param map 待判断的 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 为 null 或空返回 true
     */
    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否不为 null 且不为空
     *
     * @param map 待判断的 Map
     * @param <K> 键类型
     * @param <V> 值类型
     * @return 有元素返回 true
     */
    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    // ======================== 安全取值 ========================

    /**
     * 获取集合第一个元素
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 第一个元素，集合为空返回 null
     */
    public static <T> T first(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List<T> list) {
            return list.getFirst();
        }
        return collection.iterator().next();
    }

    /**
     * 获取集合最后一个元素
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 最后一个元素，集合为空返回 null
     */
    public static <T> T last(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection instanceof List<T> list) {
            return list.getLast();
        }
        // 非 List 集合需遍历到最后
        Iterator<T> it = collection.iterator();
        T last = null;
        while (it.hasNext()) {
            last = it.next();
        }
        return last;
    }

    /**
     * 安全获取 List 中指定索引的元素，越界返回 null
     *
     * @param list  列表
     * @param index 索引
     * @param <T>   元素类型
     * @return 对应索引的元素，越界返回 null
     */
    public static <T> T get(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    // ======================== 过滤与转换 ========================

    /**
     * 过滤集合中满足条件的元素
     *
     * @param collection 原集合
     * @param predicate  过滤条件
     * @param <T>        元素类型
     * @return 过滤后的新 List
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return List.of();
        }
        return collection.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * 将集合中的元素转换为另一种类型
     *
     * @param collection 原集合
     * @param mapper     转换函数
     * @param <T>        源元素类型
     * @param <R>        目标元素类型
     * @return 转换后的新 List
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<T, R> mapper) {
        if (isEmpty(collection)) {
            return List.of();
        }
        return collection.stream()
                .map(mapper)
                .toList();
    }

    /**
     * 查找集合中第一个满足条件的元素
     *
     * @param collection 原集合
     * @param predicate  匹配条件
     * @param <T>        元素类型
     * @return 第一个匹配的元素，未找到返回 null
     */
    public static <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.stream()
                .filter(predicate)
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断集合中是否存在满足条件的元素
     *
     * @param collection 原集合
     * @param predicate  匹配条件
     * @param <T>        元素类型
     * @return 存在匹配元素返回 true
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return false;
        }
        return collection.stream().anyMatch(predicate);
    }

    /**
     * 判断集合中是否所有元素都满足条件
     *
     * @param collection 原集合
     * @param predicate  匹配条件
     * @param <T>        元素类型
     * @return 全部匹配返回 true
     */
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> predicate) {
        if (isEmpty(collection)) {
            return true;
        }
        return collection.stream().allMatch(predicate);
    }

    // ======================== 分组 ========================

    /**
     * 按指定键对集合进行分组
     *
     * @param collection 原集合
     * @param classifier 分组键提取函数
     * @param <T>        元素类型
     * @param <K>        分组键类型
     * @return 分组后的 Map
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> collection, Function<T, K> classifier) {
        if (isEmpty(collection)) {
            return Map.of();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(classifier));
    }

    /**
     * 按指定键分组，并将值转换为另一种类型
     *
     * @param collection   原集合
     * @param classifier   分组键提取函数
     * @param valueMapper  值转换函数
     * @param <T>          源元素类型
     * @param <K>          分组键类型
     * @param <R>          目标值类型
     * @return 分组后的 Map
     */
    public static <T, K, R> Map<K, List<R>> groupBy(Collection<T> collection,
                                                      Function<T, K> classifier,
                                                      Function<T, R> valueMapper) {
        if (isEmpty(collection)) {
            return Map.of();
        }
        return collection.stream()
                .collect(Collectors.groupingBy(
                        classifier,
                        Collectors.mapping(valueMapper, Collectors.toList())
                ));
    }

    // ======================== 去重 ========================

    /**
     * 对集合进行去重（基于 equals/hashCode）
     *
     * @param collection 原集合
     * @param <T>        元素类型
     * @return 去重后的新 List
     */
    public static <T> List<T> distinct(Collection<T> collection) {
        if (isEmpty(collection)) {
            return List.of();
        }
        return collection.stream()
                .distinct()
                .toList();
    }

    /**
     * 按指定键对集合进行去重（保留最后出现的元素）
     *
     * @param collection  原集合
     * @param keyExtractor 去重键提取函数
     * @param <T>         元素类型
     * @param <K>         键类型
     * @return 去重后的新 List
     */
    public static <T, K> List<T> distinctByKey(Collection<T> collection, Function<T, K> keyExtractor) {
        if (isEmpty(collection)) {
            return List.of();
        }
        // 使用 LinkedHashMap 保持插入顺序，键相同时后者覆盖前者
        Map<K, T> seen = new LinkedHashMap<>();
        for (T item : collection) {
            seen.put(keyExtractor.apply(item), item);
        }
        return List.copyOf(seen.values());
    }

    // ======================== 折叠与聚合 ========================

    /**
     * 将集合拼接为字符串
     *
     * @param collection 集合
     * @param separator  分隔符
     * @param <T>        元素类型
     * @return 拼接后的字符串
     */
    public static <T> String join(Collection<T> collection, String separator) {
        if (isEmpty(collection)) {
            return StringUtil.EMPTY;
        }
        return collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining(separator));
    }

    /**
     * 对集合元素求和
     *
     * @param collection 集合
     * @param mapper     值提取函数
     * @param <T>        元素类型
     * @return 求和结果，空集合返回 0
     */
    public static <T> long sum(Collection<T> collection, Function<T, ? extends Number> mapper) {
        if (isEmpty(collection)) {
            return 0;
        }
        return collection.stream()
                .mapToLong(item -> mapper.apply(item).longValue())
                .sum();
    }

    // ======================== 合并 ========================

    /**
     * 合并多个集合为一个 List
     *
     * @param collections 多个集合
     * @param <T>         元素类型
     * @return 合并后的新 List
     */
    @SafeVarargs
    public static <T> List<T> merge(Collection<T>... collections) {
        if (collections == null || collections.length == 0) {
            return List.of();
        }
        return Arrays.stream(collections)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }

    // ======================== 转换 ========================

    /**
     * 将集合转为不可变 List
     *
     * @param collection 原集合
     * @param <T>        元素类型
     * @return 不可变 List，null 返回空 List
     */
    public static <T> List<T> toImmutableList(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return List.of();
        }
        return List.copyOf(collection);
    }

    /**
     * 将集合转为不可变 Set
     *
     * @param collection 原集合
     * @param <T>        元素类型
     * @return 不可变 Set，null 返回空 Set
     */
    public static <T> Set<T> toImmutableSet(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(collection);
    }

    // ======================== 分页 ========================

    /**
     * 对集合进行分页切割
     *
     * @param collection 原集合
     * @param page       页码（从 1 开始）
     * @param size       每页大小
     * @param <T>        元素类型
     * @return 当前页的数据列表
     */
    public static <T> List<T> page(Collection<T> collection, int page, int size) {
        if (isEmpty(collection) || page < 1 || size < 1) {
            return List.of();
        }
        int fromIndex = (page - 1) * size;
        if (fromIndex >= collection.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, collection.size());
        if (collection instanceof List<T> list) {
            return List.copyOf(list.subList(fromIndex, toIndex));
        }
        return List.copyOf(new ArrayList<>(collection).subList(fromIndex, toIndex));
    }
}
