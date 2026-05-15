package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ArrayUtil} 单元测试
 */
@DisplayName("ArrayUtil 数组工具类测试")
class ArrayUtilTest {

    // ======================== 判空 ========================

    @Nested
    @DisplayName("isEmpty / isNotEmpty (Object[]) 测试")
    class IsEmptyObjectArrayTests {

        @Test
        @DisplayName("null 数组为空")
        void isEmpty_null() {
            assertTrue(ArrayUtil.isEmpty((Object[]) null));
        }

        @Test
        @DisplayName("空数组为空")
        void isEmpty_empty() {
            assertTrue(ArrayUtil.isEmpty(new Object[0]));
        }

        @Test
        @DisplayName("非空数组不为空")
        void isNotEmpty_normal() {
            assertFalse(ArrayUtil.isEmpty(new String[]{"a"}));
            assertTrue(ArrayUtil.isNotEmpty(new String[]{"a"}));
        }

        @Test
        @DisplayName("null 数组 isNotEmpty 返回 false")
        void isNotEmpty_null() {
            assertFalse(ArrayUtil.isNotEmpty((Object[]) null));
        }
    }

    @Nested
    @DisplayName("isEmpty / isNotEmpty (int[]) 测试")
    class IsEmptyIntArrayTests {

        @Test
        @DisplayName("null int 数组为空")
        void isEmpty_null() {
            assertTrue(ArrayUtil.isEmpty((int[]) null));
            assertFalse(ArrayUtil.isNotEmpty((int[]) null));
        }

        @Test
        @DisplayName("空 int 数组为空")
        void isEmpty_empty() {
            assertTrue(ArrayUtil.isEmpty(new int[0]));
        }

        @Test
        @DisplayName("非空 int 数组不为空")
        void isNotEmpty_normal() {
            assertTrue(ArrayUtil.isNotEmpty(new int[]{1}));
        }
    }

    @Nested
    @DisplayName("isEmpty / isNotEmpty (long[]) 测试")
    class IsEmptyLongArrayTests {

        @Test
        @DisplayName("null long 数组为空")
        void isEmpty_null() {
            assertTrue(ArrayUtil.isEmpty((long[]) null));
            assertFalse(ArrayUtil.isNotEmpty((long[]) null));
        }

        @Test
        @DisplayName("非空 long 数组不为空")
        void isNotEmpty_normal() {
            assertTrue(ArrayUtil.isNotEmpty(new long[]{1L}));
        }
    }

    @Nested
    @DisplayName("isEmpty / isNotEmpty (double[]) 测试")
    class IsEmptyDoubleArrayTests {

        @Test
        @DisplayName("null double 数组为空")
        void isEmpty_null() {
            assertTrue(ArrayUtil.isEmpty((double[]) null));
            assertFalse(ArrayUtil.isNotEmpty((double[]) null));
        }

        @Test
        @DisplayName("非空 double 数组不为空")
        void isNotEmpty_normal() {
            assertTrue(ArrayUtil.isNotEmpty(new double[]{1.0}));
        }
    }

    @Nested
    @DisplayName("length 测试")
    class LengthTests {

        @Test
        @DisplayName("null 返回 0")
        void length_null() {
            assertEquals(0, ArrayUtil.length(null));
        }

        @Test
        @DisplayName("空数组返回 0")
        void length_empty() {
            assertEquals(0, ArrayUtil.length(new Object[0]));
        }

        @Test
        @DisplayName("正常数组返回长度")
        void length_normal() {
            assertEquals(3, ArrayUtil.length(new String[]{"a", "b", "c"}));
        }
    }

    // ======================== 安全取值 ========================

    @Nested
    @DisplayName("first / last / get 测试")
    class AccessorTests {

        @Test
        @DisplayName("first - 空数组返回 null")
        void first_empty() {
            assertNull(ArrayUtil.first(null));
            assertNull(ArrayUtil.first(new String[0]));
        }

        @Test
        @DisplayName("first - 正常获取第一个")
        void first_normal() {
            assertEquals("a", ArrayUtil.first(new String[]{"a", "b", "c"}));
        }

        @Test
        @DisplayName("first - 单元素数组")
        void first_single() {
            assertEquals("x", ArrayUtil.first(new String[]{"x"}));
        }

        @Test
        @DisplayName("last - 空数组返回 null")
        void last_empty() {
            assertNull(ArrayUtil.last(null));
            assertNull(ArrayUtil.last(new String[0]));
        }

        @Test
        @DisplayName("last - 正常获取最后一个")
        void last_normal() {
            assertEquals("c", ArrayUtil.last(new String[]{"a", "b", "c"}));
        }

        @Test
        @DisplayName("get - null 数组返回 null")
        void get_null() {
            assertNull(ArrayUtil.get(null, 0));
        }

        @Test
        @DisplayName("get - 负索引返回 null")
        void get_negativeIndex() {
            assertNull(ArrayUtil.get(new String[]{"a"}, -1));
        }

        @Test
        @DisplayName("get - 越界返回 null")
        void get_outOfBounds() {
            assertNull(ArrayUtil.get(new String[]{"a"}, 1));
        }

        @Test
        @DisplayName("get - 正常获取")
        void get_normal() {
            assertEquals("b", ArrayUtil.get(new String[]{"a", "b", "c"}, 1));
        }

        @Test
        @DisplayName("get - 边界值 0 和 length-1")
        void get_boundaries() {
            String[] arr = {"a", "b", "c"};
            assertEquals("a", ArrayUtil.get(arr, 0));
            assertEquals("c", ArrayUtil.get(arr, 2));
        }
    }

    // ======================== 查找与包含 ========================

    @Nested
    @DisplayName("contains 测试")
    class ContainsTests {

        @Test
        @DisplayName("contains(Object[]) - null 数组返回 false")
        void contains_null() {
            assertFalse(ArrayUtil.contains((Object[]) null, "a"));
        }

        @Test
        @DisplayName("contains(Object[]) - 空数组返回 false")
        void contains_empty() {
            assertFalse(ArrayUtil.contains(new String[0], "a"));
        }

        @Test
        @DisplayName("contains(Object[]) - 包含返回 true")
        void contains_found() {
            assertTrue(ArrayUtil.contains(new String[]{"a", "b", "c"}, "b"));
        }

        @Test
        @DisplayName("contains(Object[]) - 不包含返回 false")
        void contains_notFound() {
            assertFalse(ArrayUtil.contains(new String[]{"a", "b", "c"}, "d"));
        }

        @Test
        @DisplayName("contains(Object[]) - null 元素")
        void contains_nullElement() {
            assertTrue(ArrayUtil.contains(new String[]{"a", null, "c"}, null));
        }

        @Test
        @DisplayName("contains(int[]) - 包含")
        void contains_int_found() {
            assertTrue(ArrayUtil.contains(new int[]{1, 2, 3}, 2));
        }

        @Test
        @DisplayName("contains(int[]) - 不包含")
        void contains_int_notFound() {
            assertFalse(ArrayUtil.contains(new int[]{1, 2, 3}, 5));
        }

        @Test
        @DisplayName("contains(int[]) - null 数组返回 false")
        void contains_int_null() {
            assertFalse(ArrayUtil.contains((int[]) null, 1));
        }

        @Test
        @DisplayName("contains(long[]) - 包含")
        void contains_long_found() {
            assertTrue(ArrayUtil.contains(new long[]{1L, 2L, 3L}, 2L));
        }

        @Test
        @DisplayName("contains(long[]) - null 数组返回 false")
        void contains_long_null() {
            assertFalse(ArrayUtil.contains((long[]) null, 1L));
        }
    }

    @Nested
    @DisplayName("indexOf / lastIndexOf 测试")
    class IndexOfTests {

        @Test
        @DisplayName("indexOf - null 数组返回 -1")
        void indexOf_null() {
            assertEquals(-1, ArrayUtil.indexOf(null, "a"));
        }

        @Test
        @DisplayName("indexOf - 找到返回索引")
        void indexOf_found() {
            assertEquals(1, ArrayUtil.indexOf(new String[]{"a", "b", "c"}, "b"));
        }

        @Test
        @DisplayName("indexOf - 未找到返回 -1")
        void indexOf_notFound() {
            assertEquals(-1, ArrayUtil.indexOf(new String[]{"a", "b", "c"}, "d"));
        }

        @Test
        @DisplayName("indexOf - 重复元素返回第一个")
        void indexOf_firstOccurrence() {
            assertEquals(0, ArrayUtil.indexOf(new String[]{"a", "b", "a"}, "a"));
        }

        @Test
        @DisplayName("lastIndexOf - null 数组返回 -1")
        void lastIndexOf_null() {
            assertEquals(-1, ArrayUtil.lastIndexOf(null, "a"));
        }

        @Test
        @DisplayName("lastIndexOf - 找到返回最后索引")
        void lastIndexOf_found() {
            assertEquals(2, ArrayUtil.lastIndexOf(new String[]{"a", "b", "a"}, "a"));
        }

        @Test
        @DisplayName("lastIndexOf - 未找到返回 -1")
        void lastIndexOf_notFound() {
            assertEquals(-1, ArrayUtil.lastIndexOf(new String[]{"a", "b", "c"}, "d"));
        }
    }

    // ======================== 转换 ========================

    @Nested
    @DisplayName("toList / toMutableList / toSet 测试")
    class ToListTests {

        @Test
        @DisplayName("toList - null 返回空列表")
        void toList_null() {
            assertEquals(List.of(), ArrayUtil.toList(null));
        }

        @Test
        @DisplayName("toList - 空数组返回空列表")
        void toList_empty() {
            assertEquals(List.of(), ArrayUtil.toList(new String[0]));
        }

        @Test
        @DisplayName("toList - 正常转换")
        void toList_normal() {
            assertEquals(List.of("a", "b", "c"), ArrayUtil.toList(new String[]{"a", "b", "c"}));
        }

        @Test
        @DisplayName("toList - 返回不可变列表")
        void toList_immutable() {
            List<String> list = ArrayUtil.toList(new String[]{"a"});
            assertThrows(UnsupportedOperationException.class, () -> list.add("b"));
        }

        @Test
        @DisplayName("toMutableList - null 返回空列表")
        void toMutableList_null() {
            assertEquals(List.of(), ArrayUtil.toMutableList(null));
        }

        @Test
        @DisplayName("toMutableList - 可变列表支持增删")
        void toMutableList_mutable() {
            List<String> list = ArrayUtil.toMutableList(new String[]{"a"});
            list.add("b");
            assertEquals(List.of("a", "b"), list);
        }

        @Test
        @DisplayName("toSet - null 返回空 Set")
        void toSet_null() {
            assertEquals(Set.of(), ArrayUtil.toSet(null));
        }

        @Test
        @DisplayName("toSet - 正常去重")
        void toSet_normal() {
            assertEquals(Set.of("a", "b"), ArrayUtil.toSet(new String[]{"a", "b", "a"}));
        }
    }

    @Nested
    @DisplayName("map 测试")
    class MapTests {

        @Test
        @DisplayName("map - null 返回空列表")
        void map_null() {
            assertEquals(List.of(), ArrayUtil.map(null, Object::toString));
        }

        @Test
        @DisplayName("map - 空数组返回空列表")
        void map_empty() {
            assertEquals(List.of(), ArrayUtil.map(new Integer[0], String::valueOf));
        }

        @Test
        @DisplayName("map - 正常转换")
        void map_normal() {
            List<String> result = ArrayUtil.map(new Integer[]{1, 2, 3}, String::valueOf);
            assertEquals(List.of("1", "2", "3"), result);
        }
    }

    @Nested
    @DisplayName("toArray / fromCollection 测试")
    class ToArrayTests {

        @Test
        @DisplayName("toArray - null 返回空数组")
        void toArray_null() {
            String[] result = ArrayUtil.toArray(null, String[]::new);
            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("toArray - 正常复制")
        void toArray_normal() {
            String[] original = {"a", "b"};
            String[] result = ArrayUtil.toArray(original, String[]::new);
            assertArrayEquals(original, result);
            assertNotSame(original, result);
        }

        @Test
        @DisplayName("fromCollection - null 集合返回空数组")
        void fromCollection_null() {
            Object[] result = ArrayUtil.fromCollection(null);
            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("fromCollection - 空集合返回空数组")
        void fromCollection_empty() {
            Object[] result = ArrayUtil.fromCollection(List.of());
            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("fromCollection - 正常转换")
        void fromCollection_normal() {
            String[] result = ArrayUtil.fromCollection(List.of("a", "b"));
            assertArrayEquals(new String[]{"a", "b"}, result);
        }
    }

    @Nested
    @DisplayName("toWrapper / toPrimitive 测试")
    class WrapperPrimitiveTests {

        @Test
        @DisplayName("toWrapper(int[]) - null 返回空数组")
        void toWrapper_int_null() {
            assertEquals(0, ArrayUtil.toWrapper((int[]) null).length);
        }

        @Test
        @DisplayName("toWrapper(int[]) - 正常转换")
        void toWrapper_int_normal() {
            Integer[] result = ArrayUtil.toWrapper(new int[]{1, 2, 3});
            assertArrayEquals(new Integer[]{1, 2, 3}, result);
        }

        @Test
        @DisplayName("toWrapper(long[]) - null 返回空数组")
        void toWrapper_long_null() {
            assertEquals(0, ArrayUtil.toWrapper((long[]) null).length);
        }

        @Test
        @DisplayName("toWrapper(long[]) - 正常转换")
        void toWrapper_long_normal() {
            Long[] result = ArrayUtil.toWrapper(new long[]{1L, 2L});
            assertArrayEquals(new Long[]{1L, 2L}, result);
        }

        @Test
        @DisplayName("toWrapper(double[]) - null 返回空数组")
        void toWrapper_double_null() {
            assertEquals(0, ArrayUtil.toWrapper((double[]) null).length);
        }

        @Test
        @DisplayName("toWrapper(double[]) - 正常转换")
        void toWrapper_double_normal() {
            Double[] result = ArrayUtil.toWrapper(new double[]{1.0, 2.0});
            assertArrayEquals(new Double[]{1.0, 2.0}, result);
        }

        @Test
        @DisplayName("toPrimitive(Integer[]) - null 返回空数组")
        void toPrimitive_Integer_null() {
            assertEquals(0, ArrayUtil.toPrimitive((Integer[]) null).length);
        }

        @Test
        @DisplayName("toPrimitive(Integer[]) - 正常转换")
        void toPrimitive_Integer_normal() {
            int[] result = ArrayUtil.toPrimitive(new Integer[]{1, 2, 3});
            assertArrayEquals(new int[]{1, 2, 3}, result);
        }

        @Test
        @DisplayName("toPrimitive(Long[]) - null 返回空数组")
        void toPrimitive_Long_null() {
            assertEquals(0, ArrayUtil.toPrimitive((Long[]) null).length);
        }

        @Test
        @DisplayName("toPrimitive(Long[]) - 正常转换")
        void toPrimitive_Long_normal() {
            long[] result = ArrayUtil.toPrimitive(new Long[]{1L, 2L});
            assertArrayEquals(new long[]{1L, 2L}, result);
        }

        @Test
        @DisplayName("toPrimitive(Double[]) - null 返回空数组")
        void toPrimitive_Double_null() {
            assertEquals(0, ArrayUtil.toPrimitive((Double[]) null).length);
        }

        @Test
        @DisplayName("toPrimitive(Double[]) - 正常转换")
        void toPrimitive_Double_normal() {
            double[] result = ArrayUtil.toPrimitive(new Double[]{1.0, 2.0});
            assertArrayEquals(new double[]{1.0, 2.0}, result);
        }
    }

    // ======================== 拼接与合并 ========================

    @Nested
    @DisplayName("join 测试")
    class JoinTests {

        @Test
        @DisplayName("join - null 数组返回空字符串")
        void join_null() {
            assertEquals("", ArrayUtil.join(",", null));
        }

        @Test
        @DisplayName("join - 空数组返回空字符串")
        void join_empty() {
            assertEquals("", ArrayUtil.join(",", new String[0]));
        }

        @Test
        @DisplayName("join - 正常连接")
        void join_normal() {
            assertEquals("a,b,c", ArrayUtil.join(",", new String[]{"a", "b", "c"}));
        }
    }

    @Nested
    @DisplayName("concat 测试")
    class ConcatTests {

        @Test
        @DisplayName("concat(Object[]) - 全 null 返回空数组")
        void concat_allNull() {
            assertEquals(0, ArrayUtil.concat((Object[]) null).length);
        }

        @Test
        @DisplayName("concat(Object[]) - 正常合并")
        void concat_normal() {
            String[] result = ArrayUtil.concat(new String[]{"a"}, new String[]{"b"}, new String[]{"c"});
            assertArrayEquals(new String[]{"a", "b", "c"}, result);
        }

        @Test
        @DisplayName("concat(Object[]) - 跳过 null 数组")
        void concat_skipNull() {
            String[] result = ArrayUtil.concat(new String[]{"a"}, null, new String[]{"c"});
            assertArrayEquals(new String[]{"a", "c"}, result);
        }

        @Test
        @DisplayName("concat(Object[]) - 空数组参与合并")
        void concat_emptyArray() {
            String[] result = ArrayUtil.concat(new String[]{"a"}, new String[0], new String[]{"b"});
            assertArrayEquals(new String[]{"a", "b"}, result);
        }

        @Test
        @DisplayName("concat(int[]) - 正常合并")
        void concat_int_normal() {
            int[] result = ArrayUtil.concat(new int[]{1, 2}, new int[]{3});
            assertArrayEquals(new int[]{1, 2, 3}, result);
        }

        @Test
        @DisplayName("concat(int[]) - first 为 null")
        void concat_int_nullFirst() {
            int[] result = ArrayUtil.concat(null, new int[]{1, 2});
            assertArrayEquals(new int[]{1, 2}, result);
        }
    }

    // ======================== 操作 ========================

    @Nested
    @DisplayName("reverse 测试")
    class ReverseTests {

        @Test
        @DisplayName("reverse(Object[]) - null 返回 null")
        void reverse_null() {
            assertNull(ArrayUtil.reverse((Object[]) null));
        }

        @Test
        @DisplayName("reverse(Object[]) - 空数组返回空数组")
        void reverse_empty() {
            String[] arr = new String[0];
            assertSame(arr, ArrayUtil.reverse(arr));
        }

        @Test
        @DisplayName("reverse(Object[]) - 正常反转（原位修改）")
        void reverse_normal() {
            String[] arr = {"a", "b", "c"};
            String[] result = ArrayUtil.reverse(arr);
            assertSame(arr, result);
            assertArrayEquals(new String[]{"c", "b", "a"}, arr);
        }

        @Test
        @DisplayName("reverse(int[]) - 正常反转")
        void reverse_int_normal() {
            int[] arr = {1, 2, 3};
            ArrayUtil.reverse(arr);
            assertArrayEquals(new int[]{3, 2, 1}, arr);
        }

        @Test
        @DisplayName("reverse(int[]) - null 返回 null")
        void reverse_int_null() {
            assertNull(ArrayUtil.reverse((int[]) null));
        }

        @Test
        @DisplayName("reverse(long[]) - 正常反转")
        void reverse_long_normal() {
            long[] arr = {1L, 2L, 3L};
            ArrayUtil.reverse(arr);
            assertArrayEquals(new long[]{3L, 2L, 1L}, arr);
        }

        @Test
        @DisplayName("reverse(long[]) - null 返回 null")
        void reverse_long_null() {
            assertNull(ArrayUtil.reverse((long[]) null));
        }
    }

    @Nested
    @DisplayName("swap 测试")
    class SwapTests {

        @Test
        @DisplayName("swap - null 数组不做操作")
        void swap_null() {
            assertDoesNotThrow(() -> ArrayUtil.swap(null, 0, 1));
        }

        @Test
        @DisplayName("swap - 相同索引不做操作")
        void swap_sameIndex() {
            String[] arr = {"a", "b"};
            ArrayUtil.swap(arr, 0, 0);
            assertArrayEquals(new String[]{"a", "b"}, arr);
        }

        @Test
        @DisplayName("swap - 负索引不做操作")
        void swap_negativeIndex() {
            String[] arr = {"a", "b"};
            ArrayUtil.swap(arr, -1, 0);
            assertArrayEquals(new String[]{"a", "b"}, arr);
        }

        @Test
        @DisplayName("swap - 越界索引不做操作")
        void swap_outOfBounds() {
            String[] arr = {"a", "b"};
            ArrayUtil.swap(arr, 0, 5);
            assertArrayEquals(new String[]{"a", "b"}, arr);
        }

        @Test
        @DisplayName("swap - 正常交换")
        void swap_normal() {
            String[] arr = {"a", "b", "c"};
            ArrayUtil.swap(arr, 0, 2);
            assertArrayEquals(new String[]{"c", "b", "a"}, arr);
        }
    }

    @Nested
    @DisplayName("insert 测试")
    class InsertTests {

        @Test
        @DisplayName("insert - null 数组返回包含单个元素的 Object 数组")
        void insert_null() {
            Object[] result = ArrayUtil.insert(null, 0, "a");
            assertEquals(1, result.length);
            assertEquals("a", result[0]);
        }

        @Test
        @DisplayName("insert - 在头部插入")
        void insert_atHead() {
            String[] result = ArrayUtil.insert(new String[]{"b", "c"}, 0, "a");
            assertArrayEquals(new String[]{"a", "b", "c"}, result);
        }

        @Test
        @DisplayName("insert - 在尾部插入")
        void insert_atTail() {
            String[] result = ArrayUtil.insert(new String[]{"a", "b"}, 2, "c");
            assertArrayEquals(new String[]{"a", "b", "c"}, result);
        }

        @Test
        @DisplayName("insert - 在中间插入")
        void insert_inMiddle() {
            String[] result = ArrayUtil.insert(new String[]{"a", "c"}, 1, "b");
            assertArrayEquals(new String[]{"a", "b", "c"}, result);
        }

        @Test
        @DisplayName("insert - 负索引抛出异常")
        void insert_negativeIndex() {
            assertThrows(IndexOutOfBoundsException.class, () -> ArrayUtil.insert(new String[]{"a"}, -1, "b"));
        }

        @Test
        @DisplayName("insert - 越界索引抛出异常")
        void insert_outOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> ArrayUtil.insert(new String[]{"a"}, 5, "b"));
        }
    }

    @Nested
    @DisplayName("remove / removeAll 测试")
    class RemoveTests {

        @Test
        @DisplayName("remove - null 数组返回 null")
        void remove_null() {
            assertNull(ArrayUtil.remove(null, 0));
        }

        @Test
        @DisplayName("remove - 空数组返回空数组")
        void remove_empty() {
            String[] arr = new String[0];
            assertSame(arr, ArrayUtil.remove(arr, 0));
        }

        @Test
        @DisplayName("remove - 负索引返回原数组")
        void remove_negativeIndex() {
            String[] arr = {"a"};
            assertSame(arr, ArrayUtil.remove(arr, -1));
        }

        @Test
        @DisplayName("remove - 越界索引返回原数组")
        void remove_outOfBounds() {
            String[] arr = {"a"};
            assertSame(arr, ArrayUtil.remove(arr, 5));
        }

        @Test
        @DisplayName("remove - 正常移除")
        void remove_normal() {
            String[] result = ArrayUtil.remove(new String[]{"a", "b", "c"}, 1);
            assertArrayEquals(new String[]{"a", "c"}, result);
        }

        @Test
        @DisplayName("remove - 移除第一个元素")
        void remove_first() {
            String[] result = ArrayUtil.remove(new String[]{"a", "b", "c"}, 0);
            assertArrayEquals(new String[]{"b", "c"}, result);
        }

        @Test
        @DisplayName("remove - 移除最后一个元素")
        void remove_last() {
            String[] result = ArrayUtil.remove(new String[]{"a", "b", "c"}, 2);
            assertArrayEquals(new String[]{"a", "b"}, result);
        }

        @Test
        @DisplayName("removeAll - null 数组返回 null")
        void removeAll_null() {
            assertNull(ArrayUtil.removeAll(null, "a"));
        }

        @Test
        @DisplayName("removeAll - 空数组返回空数组")
        void removeAll_empty() {
            String[] arr = new String[0];
            assertSame(arr, ArrayUtil.removeAll(arr, "a"));
        }

        @Test
        @DisplayName("removeAll - 移除所有匹配元素")
        void removeAll_normal() {
            String[] result = ArrayUtil.removeAll(new String[]{"a", "b", "a", "c"}, "a");
            assertArrayEquals(new String[]{"b", "c"}, result);
        }

        @Test
        @DisplayName("removeAll - 无匹配返回原数组（但重新构造）")
        void removeAll_noMatch() {
            String[] result = ArrayUtil.removeAll(new String[]{"a", "b"}, "c");
            assertArrayEquals(new String[]{"a", "b"}, result);
        }
    }

    // ======================== 去重 ========================

    @Nested
    @DisplayName("distinct 测试")
    class DistinctTests {

        @Test
        @DisplayName("distinct - null 返回 null")
        void distinct_null() {
            assertNull(ArrayUtil.distinct(null));
        }

        @Test
        @DisplayName("distinct - 空数组返回空数组")
        void distinct_empty() {
            assertEquals(0, ArrayUtil.distinct(new String[0]).length);
        }

        @Test
        @DisplayName("distinct - 正常去重，保持顺序")
        void distinct_normal() {
            String[] result = ArrayUtil.distinct(new String[]{"a", "b", "a", "c", "b"});
            assertArrayEquals(new String[]{"a", "b", "c"}, result);
        }
    }

    // ======================== 过滤 ========================

    @Nested
    @DisplayName("filter / findFirst 测试")
    class FilterTests {

        @Test
        @DisplayName("filter - null 返回空数组")
        void filter_null() {
            assertEquals(0, ArrayUtil.filter(null, x -> true).length);
        }

        @Test
        @DisplayName("filter - 正常过滤")
        void filter_normal() {
            Integer[] result = ArrayUtil.filter(new Integer[]{1, 2, 3, 4, 5}, x -> x % 2 == 0);
            assertArrayEquals(new Integer[]{2, 4}, result);
        }

        @Test
        @DisplayName("filter - 无匹配返回空数组")
        void filter_noMatch() {
            Integer[] result = ArrayUtil.filter(new Integer[]{1, 3, 5}, x -> x % 2 == 0);
            assertEquals(0, result.length);
        }

        @Test
        @DisplayName("findFirst - null 返回 null")
        void findFirst_null() {
            assertNull(ArrayUtil.findFirst(null, x -> true));
        }

        @Test
        @DisplayName("findFirst - 找到返回第一个匹配")
        void findFirst_found() {
            assertEquals(2, ArrayUtil.findFirst(new Integer[]{1, 2, 3, 2}, x -> x == 2));
        }

        @Test
        @DisplayName("findFirst - 未找到返回 null")
        void findFirst_notFound() {
            assertNull(ArrayUtil.findFirst(new Integer[]{1, 2, 3}, x -> x > 10));
        }
    }

    // ======================== 数组复制与调整 ========================

    @Nested
    @DisplayName("subArray 测试")
    class SubArrayTests {

        @Test
        @DisplayName("subArray - null 返回 null")
        void subArray_null() {
            assertNull(ArrayUtil.subArray(null, 0, 1));
        }

        @Test
        @DisplayName("subArray - 正常截取")
        void subArray_normal() {
            String[] result = ArrayUtil.subArray(new String[]{"a", "b", "c", "d"}, 1, 3);
            assertArrayEquals(new String[]{"b", "c"}, result);
        }

        @Test
        @DisplayName("subArray - start < 0 当作 0")
        void subArray_negativeStart() {
            String[] result = ArrayUtil.subArray(new String[]{"a", "b"}, -1, 1);
            assertArrayEquals(new String[]{"a"}, result);
        }

        @Test
        @DisplayName("subArray - end > length 当作 length")
        void subArray_endTooBig() {
            String[] result = ArrayUtil.subArray(new String[]{"a", "b"}, 0, 10);
            assertArrayEquals(new String[]{"a", "b"}, result);
        }

        @Test
        @DisplayName("subArray - start >= end 返回空数组")
        void subArray_invalidRange() {
            assertEquals(0, ArrayUtil.subArray(new String[]{"a", "b"}, 1, 1).length);
            assertEquals(0, ArrayUtil.subArray(new String[]{"a", "b"}, 2, 1).length);
        }
    }

    @Nested
    @DisplayName("resize 测试")
    class ResizeTests {

        @Test
        @DisplayName("resize - null 抛出 NullPointerException")
        void resize_null() {
            assertThrows(NullPointerException.class, () -> ArrayUtil.resize(null, 5));
        }

        @Test
        @DisplayName("resize - 扩大数组（新位置为 null）")
        void resize_grow() {
            String[] result = ArrayUtil.resize(new String[]{"a"}, 3);
            assertEquals(3, result.length);
            assertEquals("a", result[0]);
            assertNull(result[1]);
            assertNull(result[2]);
        }

        @Test
        @DisplayName("resize - 缩小数组（截断）")
        void resize_shrink() {
            String[] result = ArrayUtil.resize(new String[]{"a", "b", "c"}, 1);
            assertEquals(1, result.length);
            assertEquals("a", result[0]);
        }

        @Test
        @DisplayName("resize - 大小不变")
        void resize_same() {
            String[] result = ArrayUtil.resize(new String[]{"a", "b"}, 2);
            assertArrayEquals(new String[]{"a", "b"}, result);
        }

        @Test
        @DisplayName("resize - 缩小到 0")
        void resize_zero() {
            String[] result = ArrayUtil.resize(new String[]{"a", "b"}, 0);
            assertEquals(0, result.length);
        }
    }
}
