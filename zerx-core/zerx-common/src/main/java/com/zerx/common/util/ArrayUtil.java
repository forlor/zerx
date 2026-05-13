package com.zerx.common.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

/**
 * 数组工具类
 * <p>
 * 提供数组常用操作，包括判空、查找、转换、拼接、反转、去重等。
 * 支持对象数组和基本类型数组（int、long、double）。
 * 所有方法均为静态方法，线程安全。
 * </p>
 *
 * @author zerx
 */
public final class ArrayUtil {

    /** 私有构造器，防止实例化 */
    private ArrayUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 判空 ========================

    /**
     * 判断数组是否为 null 或长度为 0
     *
     * @param array 待判断的数组
     * @return 为 null 或空返回 true
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否为 null 或长度为 0（int 数组）
     *
     * @param array 待判断的数组
     * @return 为 null 或空返回 true
     */
    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否为 null 或长度为 0（long 数组）
     *
     * @param array 待判断的数组
     * @return 为 null 或空返回 true
     */
    public static boolean isEmpty(long[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否为 null 或长度为 0（double 数组）
     *
     * @param array 待判断的数组
     * @return 为 null 或空返回 true
     */
    public static boolean isEmpty(double[] array) {
        return array == null || array.length == 0;
    }

    /**
     * 判断数组是否不为 null 且长度大于 0
     *
     * @param array 待判断的数组
     * @return 有元素返回 true
     */
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /**
     * 判断数组是否不为 null 且长度大于 0（int 数组）
     *
     * @param array 待判断的数组
     * @return 有元素返回 true
     */
    public static boolean isNotEmpty(int[] array) {
        return !isEmpty(array);
    }

    /**
     * 判断数组是否不为 null 且长度大于 0（long 数组）
     *
     * @param array 待判断的数组
     * @return 有元素返回 true
     */
    public static boolean isNotEmpty(long[] array) {
        return !isEmpty(array);
    }

    /**
     * 判断数组是否不为 null 且长度大于 0（double 数组）
     *
     * @param array 待判断的数组
     * @return 有元素返回 true
     */
    public static boolean isNotEmpty(double[] array) {
        return !isEmpty(array);
    }

    /**
     * 获取数组长度，null 返回 0
     *
     * @param array 数组
     * @return 数组长度，null 返回 0
     */
    public static int length(Object[] array) {
        return array == null ? 0 : array.length;
    }

    // ======================== 安全取值 ========================

    /**
     * 获取数组第一个元素
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 第一个元素，数组为空返回 null
     */
    public static <T> T first(T[] array) {
        return isEmpty(array) ? null : array[0];
    }

    /**
     * 获取数组最后一个元素
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 最后一个元素，数组为空返回 null
     */
    public static <T> T last(T[] array) {
        return isEmpty(array) ? null : array[array.length - 1];
    }

    /**
     * 安全获取数组指定索引的元素，越界返回 null
     *
     * @param array 数组
     * @param index 索引
     * @param <T>   元素类型
     * @return 对应索引的元素，越界或 null 数组返回 null
     */
    public static <T> T get(T[] array, int index) {
        if (array == null || index < 0 || index >= array.length) {
            return null;
        }
        return array[index];
    }

    // ======================== 查找与包含 ========================

    /**
     * 判断数组中是否包含指定元素
     *
     * @param array   数组
     * @param element 待查找的元素
     * @param <T>     元素类型
     * @return 包含返回 true
     */
    public static <T> boolean contains(T[] array, T element) {
        if (isEmpty(array)) {
            return false;
        }
        for (T item : array) {
            if (Objects.equals(item, element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 int 数组中是否包含指定值
     *
     * @param array int 数组
     * @param value 待查找的值
     * @return 包含返回 true
     */
    public static boolean contains(int[] array, int value) {
        if (isEmpty(array)) {
            return false;
        }
        for (int item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 long 数组中是否包含指定值
     *
     * @param array long 数组
     * @param value 待查找的值
     * @return 包含返回 true
     */
    public static boolean contains(long[] array, long value) {
        if (isEmpty(array)) {
            return false;
        }
        for (long item : array) {
            if (item == value) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找元素在数组中首次出现的索引
     *
     * @param array   数组
     * @param element 待查找的元素
     * @param <T>     元素类型
     * @return 索引位置，未找到返回 -1
     */
    public static <T> int indexOf(T[] array, T element) {
        if (isEmpty(array)) {
            return -1;
        }
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], element)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 查找元素在数组中最后出现的索引
     *
     * @param array   数组
     * @param element 待查找的元素
     * @param <T>     元素类型
     * @return 索引位置，未找到返回 -1
     */
    public static <T> int lastIndexOf(T[] array, T element) {
        if (isEmpty(array)) {
            return -1;
        }
        for (int i = array.length - 1; i >= 0; i--) {
            if (Objects.equals(array[i], element)) {
                return i;
            }
        }
        return -1;
    }

    // ======================== 转换 ========================

    /**
     * 将数组转换为 List
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 不可变 List，null 返回空 List
     */
    public static <T> List<T> toList(T[] array) {
        if (isEmpty(array)) {
            return List.of();
        }
        return List.of(array);
    }

    /**
     * 将数组转换为可变的 ArrayList（支持后续增删操作）
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 可变的 ArrayList，null 返回空 ArrayList
     */
    public static <T> List<T> toMutableList(T[] array) {
        if (isEmpty(array)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(array));
    }

    /**
     * 将数组转换为 Set（基于 equals/hashCode 去重）
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 不可变 Set，null 返回空 Set
     */
    public static <T> Set<T> toSet(T[] array) {
        if (isEmpty(array)) {
            return Set.of();
        }
        return Set.copyOf(Arrays.asList(array));
    }

    /**
     * 将数组中的每个元素转换为另一种类型
     *
     * @param array  原始数组
     * @param mapper 转换函数
     * @param <T>    源元素类型
     * @param <R>    目标元素类型
     * @return 转换后的不可变 List
     */
    public static <T, R> List<R> map(T[] array, Function<T, R> mapper) {
        if (isEmpty(array)) {
            return List.of();
        }
        return Arrays.stream(array)
                .map(mapper)
                .toList();
    }

    /**
     * 将数组转换为指定类型的数组
     * <p>
     * 示例：toArray(stringArray, String[]::new)
     * </p>
     *
     * @param array    原始数组
     * @param generator 数组工厂方法
     * @param <T>      元素类型
     * @return 新数组
     */
    public static <T> T[] toArray(T[] array, IntFunction<T[]> generator) {
        if (array == null) {
            return generator.apply(0);
        }
        return Arrays.copyOf(array, array.length);
    }

    /**
     * 将集合转换为数组
     *
     * @param collection 集合
     * @param <T>        元素类型
     * @return 数组，null 集合返回空数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] fromCollection(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return (T[]) new Object[0];
        }
        return collection.toArray((T[]) Array.newInstance(collection.iterator().next().getClass(), 0));
    }

    /**
     * 将 int 数组转换为 Integer 数组
     *
     * @param array int 数组
     * @return Integer 数组，null 返回空数组
     */
    public static Integer[] toWrapper(int[] array) {
        if (isEmpty(array)) {
            return new Integer[0];
        }
        return Arrays.stream(array).boxed().toArray(Integer[]::new);
    }

    /**
     * 将 long 数组转换为 Long 数组
     *
     * @param array long 数组
     * @return Long 数组，null 返回空数组
     */
    public static Long[] toWrapper(long[] array) {
        if (isEmpty(array)) {
            return new Long[0];
        }
        return Arrays.stream(array).boxed().toArray(Long[]::new);
    }

    /**
     * 将 double 数组转换为 Double 数组
     *
     * @param array double 数组
     * @return Double 数组，null 返回空数组
     */
    public static Double[] toWrapper(double[] array) {
        if (isEmpty(array)) {
            return new Double[0];
        }
        return Arrays.stream(array).boxed().toArray(Double[]::new);
    }

    /**
     * 将 Integer 数组转换为 int 数组
     *
     * @param array Integer 数组
     * @return int 数组，null 返回空数组
     */
    public static int[] toPrimitive(Integer[] array) {
        if (isEmpty(array)) {
            return new int[0];
        }
        return Arrays.stream(array).mapToInt(Integer::intValue).toArray();
    }

    /**
     * 将 Long 数组转换为 long 数组
     *
     * @param array Long 数组
     * @return long 数组，null 返回空数组
     */
    public static long[] toPrimitive(Long[] array) {
        if (isEmpty(array)) {
            return new long[0];
        }
        return Arrays.stream(array).mapToLong(Long::longValue).toArray();
    }

    /**
     * 将 Double 数组转换为 double 数组
     *
     * @param array Double 数组
     * @return double 数组，null 返回空数组
     */
    public static double[] toPrimitive(Double[] array) {
        if (isEmpty(array)) {
            return new double[0];
        }
        return Arrays.stream(array).mapToDouble(Double::doubleValue).toArray();
    }

    // ======================== 拼接与合并 ========================

    /**
     * 使用分隔符将数组元素拼接为字符串
     *
     * @param separator 分隔符
     * @param array     数组
     * @param <T>       元素类型
     * @return 拼接后的字符串，null 数组返回空字符串
     */
    public static <T> String join(String separator, T[] array) {
        if (isEmpty(array)) {
            return StringUtil.EMPTY;
        }
        return Arrays.stream(array)
                .map(Object::toString)
                .collect(Collectors.joining(separator));
    }

    /**
     * 将多个数组合并为一个新数组
     *
     * @param first 第一个数组
     * @param rest  其余数组
     * @param <T>   元素类型
     * @return 合并后的新数组
     */
    @SafeVarargs
    public static <T> T[] concat(T[] first, T[]... rest) {
        if (isEmpty(first) && (rest == null || rest.length == 0)) {
            @SuppressWarnings("unchecked")
            T[] empty = (T[]) new Object[0];
            return empty;
        }

        int totalLength = isEmpty(first) ? 0 : first.length;
        if (rest != null) {
            for (T[] arr : rest) {
                totalLength += isEmpty(arr) ? 0 : arr.length;
            }
        }

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(
                first.getClass().getComponentType(), totalLength);
        int destPos = 0;
        if (!isEmpty(first)) {
            System.arraycopy(first, 0, result, destPos, first.length);
            destPos += first.length;
        }
        if (rest != null) {
            for (T[] arr : rest) {
                if (!isEmpty(arr)) {
                    System.arraycopy(arr, 0, result, destPos, arr.length);
                    destPos += arr.length;
                }
            }
        }
        return result;
    }

    /**
     * 将 int 数组拼接为一个新数组
     *
     * @param first 第一个数组
     * @param rest  其余数组
     * @return 拼接后的新数组
     */
    public static int[] concat(int[] first, int[]... rest) {
        int totalLength = isEmpty(first) ? 0 : first.length;
        if (rest != null) {
            for (int[] arr : rest) {
                totalLength += isEmpty(arr) ? 0 : arr.length;
            }
        }
        int[] result = new int[totalLength];
        int destPos = 0;
        if (!isEmpty(first)) {
            System.arraycopy(first, 0, result, destPos, first.length);
            destPos += first.length;
        }
        if (rest != null) {
            for (int[] arr : rest) {
                if (!isEmpty(arr)) {
                    System.arraycopy(arr, 0, result, destPos, arr.length);
                    destPos += arr.length;
                }
            }
        }
        return result;
    }

    // ======================== 操作 ========================

    /**
     * 反转数组（原位反转，会修改原数组）
     *
     * @param array 待反转的数组
     * @param <T>   元素类型
     * @return 反转后的数组（与原数组是同一个引用）
     */
    public static <T> T[] reverse(T[] array) {
        if (isEmpty(array)) {
            return array;
        }
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            T temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    /**
     * 反转 int 数组（原位反转）
     *
     * @param array 待反转的数组
     * @return 反转后的数组
     */
    public static int[] reverse(int[] array) {
        if (isEmpty(array)) {
            return array;
        }
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            int temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    /**
     * 反转 long 数组（原位反转）
     *
     * @param array 待反转的数组
     * @return 反转后的数组
     */
    public static long[] reverse(long[] array) {
        if (isEmpty(array)) {
            return array;
        }
        int left = 0;
        int right = array.length - 1;
        while (left < right) {
            long temp = array[left];
            array[left] = array[right];
            array[right] = temp;
            left++;
            right--;
        }
        return array;
    }

    /**
     * 交换数组中两个位置的元素
     *
     * @param array 数组
     * @param i     第一个索引
     * @param j     第二个索引
     * @param <T>   元素类型
     */
    public static <T> void swap(T[] array, int i, int j) {
        if (array == null || i < 0 || j < 0 || i >= array.length || j >= array.length || i == j) {
            return;
        }
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * 将元素插入数组指定位置，返回新数组（不修改原数组）
     *
     * @param array   原数组
     * @param index   插入位置
     * @param element 待插入的元素
     * @param <T>     元素类型
     * @return 新数组
     * @throws IndexOutOfBoundsException 索引超出范围时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] insert(T[] array, int index, T element) {
        if (array == null) {
            return (T[]) new Object[]{element};
        }
        if (index < 0 || index > array.length) {
            throw new IndexOutOfBoundsException("索引超出范围: " + index);
        }
        T[] result = Arrays.copyOf(array, array.length + 1);
        System.arraycopy(result, index, result, index + 1, array.length - index);
        result[index] = element;
        return result;
    }

    /**
     * 移除数组中指定位置的元素，返回新数组（不修改原数组）
     *
     * @param array 数组
     * @param index 要移除的索引
     * @param <T>   元素类型
     * @return 新数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] remove(T[] array, int index) {
        if (isEmpty(array) || index < 0 || index >= array.length) {
            return array;
        }
        T[] result = Arrays.copyOf(array, array.length - 1);
        System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        return result;
    }

    /**
     * 移除数组中所有等于指定元素的项目，返回新数组
     *
     * @param array   数组
     * @param element 待移除的元素
     * @param <T>     元素类型
     * @return 新数组
     */
    public static <T> T[] removeAll(T[] array, T element) {
        if (isEmpty(array)) {
            return array;
        }
        List<T> list = new ArrayList<>(Arrays.asList(array));
        list.removeAll(List.of(element));
        return list.toArray(Arrays.copyOf(array, 0));
    }

    // ======================== 去重 ========================

    /**
     * 对数组进行去重（基于 equals/hashCode，保持原有顺序）
     *
     * @param array 数组
     * @param <T>   元素类型
     * @return 去重后的新数组
     */
    public static <T> T[] distinct(T[] array) {
        if (isEmpty(array)) {
            return array;
        }
        LinkedHashSet<T> set = new LinkedHashSet<>(Arrays.asList(array));
        return set.toArray(Arrays.copyOf(array, 0));
    }

    // ======================== 过滤 ========================

    /**
     * 过滤数组中满足条件的元素
     *
     * @param array     数组
     * @param predicate 过滤条件
     * @param <T>       元素类型
     * @return 过滤后的新数组
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] filter(T[] array, java.util.function.Predicate<T> predicate) {
        if (isEmpty(array)) {
            return (T[]) new Object[0];
        }
        List<T> result = new ArrayList<>();
        for (T item : array) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result.toArray(Arrays.copyOf(array, 0));
    }

    /**
     * 查找数组中第一个满足条件的元素
     *
     * @param array     数组
     * @param predicate 匹配条件
     * @param <T>       元素类型
     * @return 第一个匹配的元素，未找到返回 null
     */
    public static <T> T findFirst(T[] array, java.util.function.Predicate<T> predicate) {
        if (isEmpty(array)) {
            return null;
        }
        for (T item : array) {
            if (predicate.test(item)) {
                return item;
            }
        }
        return null;
    }

    // ======================== 数组复制与调整 ========================

    /**
     * 截取数组指定范围的元素，返回新数组
     *
     * @param array 原数组
     * @param start 起始索引（包含）
     * @param end   结束索引（不包含）
     * @param <T>   元素类型
     * @return 截取后的新数组
     */
    public static <T> T[] subArray(T[] array, int start, int end) {
        if (isEmpty(array)) {
            return array;
        }
        if (start < 0) {
            start = 0;
        }
        if (end > array.length) {
            end = array.length;
        }
        if (start >= end) {
            return Arrays.copyOf(array, 0);
        }
        return Arrays.copyOfRange(array, start, end);
    }

    /**
     * 调整数组大小（不足用 null 填充，超出截断）
     *
     * @param array 原数组
     * @param newSize 新长度
     * @param <T>   元素类型
     * @return 调整大小后的新数组
     */
    public static <T> T[] resize(T[] array, int newSize) {
        if (array == null) {
            return Arrays.copyOf(array, 0);
        }
        return Arrays.copyOf(array, newSize);
    }
}
