package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CollectionUtil} 单元测试
 */
@DisplayName("CollectionUtil 集合工具类测试")
class CollectionUtilTest {

    // ======================== 判空 ========================

    @Nested
    @DisplayName("isEmpty(Collection) / isNotEmpty(Collection) 测试")
    class IsEmptyCollectionTests {

        @Test
        @DisplayName("null 集合为空")
        void isEmpty_null() {
            assertTrue(CollectionUtil.isEmpty((Collection<?>) null));
        }

        @Test
        @DisplayName("空集合为空")
        void isEmpty_empty() {
            assertTrue(CollectionUtil.isEmpty(List.of()));
            assertTrue(CollectionUtil.isEmpty(Set.of()));
        }

        @Test
        @DisplayName("非空集合不为空")
        void isNotEmpty_normal() {
            assertFalse(CollectionUtil.isEmpty(List.of("a")));
            assertTrue(CollectionUtil.isNotEmpty(List.of("a")));
        }

        @Test
        @DisplayName("null 集合 isNotEmpty 返回 false")
        void isNotEmpty_null() {
            assertFalse(CollectionUtil.isNotEmpty((Collection<?>) null));
        }

        @Test
        @DisplayName("空集合 isNotEmpty 返回 false")
        void isNotEmpty_empty() {
            assertFalse(CollectionUtil.isNotEmpty(Set.of()));
        }
    }

    @Nested
    @DisplayName("isEmpty(Map) / isNotEmpty(Map) 测试")
    class IsEmptyMapTests {

        @Test
        @DisplayName("null Map 为空")
        void isEmpty_null() {
            assertTrue(CollectionUtil.isEmpty((Map<?, ?>) null));
        }

        @Test
        @DisplayName("空 Map 为空")
        void isEmpty_empty() {
            assertTrue(CollectionUtil.isEmpty(Map.of()));
        }

        @Test
        @DisplayName("非空 Map 不为空")
        void isNotEmpty_normal() {
            Map<String, String> map = new HashMap<>();
            map.put("k", "v");
            assertFalse(CollectionUtil.isEmpty(map));
            assertTrue(CollectionUtil.isNotEmpty(map));
        }

        @Test
        @DisplayName("null Map isNotEmpty 返回 false")
        void isNotEmpty_null() {
            assertFalse(CollectionUtil.isNotEmpty((Map<?, ?>) null));
        }
    }

    // ======================== 安全取值 ========================

    @Nested
    @DisplayName("first / last / get 测试")
    class AccessorTests {

        @Test
        @DisplayName("first - 空集合返回 null")
        void first_empty() {
            assertNull(CollectionUtil.first(List.of()));
            assertNull(CollectionUtil.first(null));
        }

        @Test
        @DisplayName("first - List 返回第一个")
        void first_list() {
            assertEquals("a", CollectionUtil.first(List.of("a", "b", "c")));
        }

        @Test
        @DisplayName("first - Set 返回第一个（Set 无顺序保证）")
        void first_set() {
            Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
            assertEquals("a", CollectionUtil.first(set));
        }

        @Test
        @DisplayName("last - 空集合返回 null")
        void last_empty() {
            assertNull(CollectionUtil.last(List.of()));
            assertNull(CollectionUtil.last(null));
        }

        @Test
        @DisplayName("last - List 返回最后一个")
        void last_list() {
            assertEquals("c", CollectionUtil.last(List.of("a", "b", "c")));
        }

        @Test
        @DisplayName("last - Set 返回最后一个")
        void last_set() {
            Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c"));
            assertEquals("c", CollectionUtil.last(set));
        }

        @Test
        @DisplayName("get - null List 返回 null")
        void get_null() {
            assertNull(CollectionUtil.get(null, 0));
        }

        @Test
        @DisplayName("get - 负索引返回 null")
        void get_negativeIndex() {
            assertNull(CollectionUtil.get(List.of("a"), -1));
        }

        @Test
        @DisplayName("get - 超出范围返回 null")
        void get_outOfBounds() {
            assertNull(CollectionUtil.get(List.of("a"), 5));
        }

        @Test
        @DisplayName("get - 正常获取")
        void get_normal() {
            assertEquals("b", CollectionUtil.get(List.of("a", "b", "c"), 1));
        }

        @Test
        @DisplayName("get - 边界索引 0 和 size-1")
        void get_boundaries() {
            List<String> list = List.of("a", "b", "c");
            assertEquals("a", CollectionUtil.get(list, 0));
            assertEquals("c", CollectionUtil.get(list, 2));
        }
    }

    // ======================== 过滤与转换 ========================

    @Nested
    @DisplayName("filter 测试")
    class FilterTests {

        @Test
        @DisplayName("null 集合返回空列表")
        void filter_null() {
            assertEquals(List.of(), CollectionUtil.filter(null, x -> true));
        }

        @Test
        @DisplayName("空集合返回空列表")
        void filter_empty() {
            assertEquals(List.of(), CollectionUtil.filter(List.of(), x -> true));
        }

        @Test
        @DisplayName("正常过滤")
        void filter_normal() {
            List<Integer> list = List.of(1, 2, 3, 4, 5);
            assertEquals(List.of(2, 4), CollectionUtil.filter(list, x -> x % 2 == 0));
        }

        @Test
        @DisplayName("过滤后为空")
        void filter_noMatch() {
            assertEquals(List.of(), CollectionUtil.filter(List.of(1, 3, 5), x -> x % 2 == 0));
        }
    }

    @Nested
    @DisplayName("map 测试")
    class MapTests {

        @Test
        @DisplayName("null 集合返回空列表")
        void map_null() {
            assertEquals(List.of(), CollectionUtil.map(null, Object::toString));
        }

        @Test
        @DisplayName("空集合返回空列表")
        void map_empty() {
            assertEquals(List.of(), CollectionUtil.map(List.of(), Object::toString));
        }

        @Test
        @DisplayName("正常转换")
        void map_normal() {
            List<String> result = CollectionUtil.map(List.of(1, 2, 3), Object::toString);
            assertEquals(List.of("1", "2", "3"), result);
        }
    }

    @Nested
    @DisplayName("findFirst 测试")
    class FindFirstTests {

        @Test
        @DisplayName("null 集合返回 null")
        void findFirst_null() {
            assertNull(CollectionUtil.findFirst(null, x -> true));
        }

        @Test
        @DisplayName("未找到返回 null")
        void findFirst_notFound() {
            assertNull(CollectionUtil.findFirst(List.of(1, 2, 3), x -> x > 10));
        }

        @Test
        @DisplayName("找到第一个匹配")
        void findFirst_found() {
            assertEquals(3, CollectionUtil.findFirst(List.of(1, 2, 3, 4), x -> x >= 3));
        }
    }

    @Nested
    @DisplayName("anyMatch / allMatch 测试")
    class MatchTests {

        @Test
        @DisplayName("anyMatch - null 集合返回 false")
        void anyMatch_null() {
            assertFalse(CollectionUtil.anyMatch(null, x -> true));
        }

        @Test
        @DisplayName("anyMatch - 存在匹配返回 true")
        void anyMatch_true() {
            assertTrue(CollectionUtil.anyMatch(List.of(1, 2, 3), x -> x == 2));
        }

        @Test
        @DisplayName("anyMatch - 不存在匹配返回 false")
        void anyMatch_false() {
            assertFalse(CollectionUtil.anyMatch(List.of(1, 2, 3), x -> x > 10));
        }

        @Test
        @DisplayName("allMatch - null 集合返回 true（空真）")
        void allMatch_null() {
            assertTrue(CollectionUtil.allMatch(null, x -> true));
        }

        @Test
        @DisplayName("allMatch - 空集合返回 true（空真）")
        void allMatch_empty() {
            assertTrue(CollectionUtil.allMatch(List.of(), x -> false));
        }

        @Test
        @DisplayName("allMatch - 全部匹配返回 true")
        void allMatch_true() {
            assertTrue(CollectionUtil.allMatch(List.of(2, 4, 6), x -> x % 2 == 0));
        }

        @Test
        @DisplayName("allMatch - 部分不匹配返回 false")
        void allMatch_false() {
            assertFalse(CollectionUtil.allMatch(List.of(1, 2, 3), x -> x % 2 == 0));
        }
    }

    // ======================== 分组 ========================

    @Nested
    @DisplayName("groupBy 测试")
    class GroupByTests {

        @Test
        @DisplayName("groupBy - null 集合返回空 Map")
        void groupBy_null() {
            assertEquals(Map.of(), CollectionUtil.groupBy(null, x -> x));
        }

        @Test
        @DisplayName("groupBy - 空集合返回空 Map")
        void groupBy_empty() {
            assertEquals(Map.of(), CollectionUtil.groupBy(List.of(), x -> x));
        }

        @Test
        @DisplayName("groupBy - 正常分组")
        void groupBy_normal() {
            List<String> names = List.of("Alice", "Bob", "Anna", "Bill");
            Map<Character, List<String>> result = CollectionUtil.groupBy(names, s -> s.charAt(0));
            assertEquals(2, result.get('A').size());
            assertEquals(2, result.get('B').size());
        }

        @Test
        @DisplayName("groupBy 双参数 - 正常分组并转换值")
        void groupBy_twoArgs() {
            List<Integer> numbers = List.of(1, 2, 3, 4);
            Map<String, List<String>> result = CollectionUtil.groupBy(
                    numbers, i -> i % 2 == 0 ? "even" : "odd", Object::toString);
            assertEquals(List.of("1", "3"), result.get("odd"));
            assertEquals(List.of("2", "4"), result.get("even"));
        }

        @Test
        @DisplayName("groupBy 双参数 - null 集合返回空 Map")
        void groupBy_twoArgs_null() {
            assertEquals(Map.of(), CollectionUtil.groupBy(null, x -> x, Object::toString));
        }
    }

    // ======================== 去重 ========================

    @Nested
    @DisplayName("distinct / distinctByKey 测试")
    class DistinctTests {

        @Test
        @DisplayName("distinct - null 集合返回空列表")
        void distinct_null() {
            assertEquals(List.of(), CollectionUtil.distinct(null));
        }

        @Test
        @DisplayName("distinct - 正常去重")
        void distinct_normal() {
            assertEquals(List.of(1, 2, 3), CollectionUtil.distinct(List.of(1, 2, 2, 3, 1)));
        }

        @Test
        @DisplayName("distinct - 无重复保持不变")
        void distinct_noDuplicates() {
            List<Integer> list = List.of(1, 2, 3);
            assertEquals(list, CollectionUtil.distinct(list));
        }

        @Test
        @DisplayName("distinctByKey - null 集合返回空列表")
        void distinctByKey_null() {
            assertEquals(List.of(), CollectionUtil.distinctByKey(null, Object::toString));
        }

        @Test
        @DisplayName("distinctByKey - 按键去重，保留最后出现")
        void distinctByKey_normal() {
            record Item(int id, String name) {}
            List<Item> items = List.of(
                    new Item(1, "a"),
                    new Item(2, "b"),
                    new Item(1, "c")
            );
            List<Item> result = CollectionUtil.distinctByKey(items, Item::id);
            assertEquals(2, result.size());
            assertEquals("c", result.getFirst().name()); // 保留最后出现
        }

        @Test
        @DisplayName("distinctByKey - 无重复保持不变")
        void distinctByKey_noDuplicates() {
            List<Integer> list = List.of(1, 2, 3);
            assertEquals(list, CollectionUtil.distinctByKey(list, x -> x));
        }
    }

    // ======================== 折叠与聚合 ========================

    @Nested
    @DisplayName("join 测试")
    class JoinTests {

        @Test
        @DisplayName("join - null 集合返回空字符串")
        void join_null() {
            assertEquals("", CollectionUtil.join(null, ","));
        }

        @Test
        @DisplayName("join - 空集合返回空字符串")
        void join_empty() {
            assertEquals("", CollectionUtil.join(Set.of(), ","));
        }

        @Test
        @DisplayName("join - 正常连接")
        void join_normal() {
            assertEquals("1-2-3", CollectionUtil.join(List.of(1, 2, 3), "-"));
        }
    }

    @Nested
    @DisplayName("sum 测试")
    class SumTests {

        @Test
        @DisplayName("sum - null 集合返回 0")
        void sum_null() {
            assertEquals(0L, CollectionUtil.sum(null, e -> 0));
        }

        @Test
        @DisplayName("sum - 空集合返回 0")
        void sum_empty() {
            assertEquals(0L, CollectionUtil.sum(List.<Integer>of(), Number::intValue));
        }

        @Test
        @DisplayName("sum - 正常求和")
        void sum_normal() {
            assertEquals(10, CollectionUtil.sum(List.of(1, 2, 3, 4), x -> x));
        }

        @Test
        @DisplayName("sum - 提取属性求和")
        void sum_withMapper() {
            record Item(int value) {}
            List<Item> items = List.of(new Item(10), new Item(20), new Item(30));
            assertEquals(60, CollectionUtil.sum(items, item -> item.value));
        }
    }

    // ======================== 合并 ========================

    @Nested
    @DisplayName("merge 测试")
    class MergeTests {

        @Test
        @DisplayName("merge - null 参数返回空列表")
        void merge_null() {
            assertEquals(List.of(), CollectionUtil.merge(null));
        }

        @Test
        @DisplayName("merge - 空参数返回空列表")
        void merge_empty() {
            assertEquals(List.of(), CollectionUtil.merge());
        }

        @Test
        @DisplayName("merge - 合并多个集合")
        void merge_normal() {
            List<String> result = CollectionUtil.merge(List.of("a"), List.of("b"), List.of("c"));
            assertEquals(List.of("a", "b", "c"), result);
        }

        @Test
        @DisplayName("merge - 自动跳过 null 集合")
        void merge_skipNull() {
            List<String> result = CollectionUtil.merge(List.of("a"), null, List.of("c"));
            assertEquals(List.of("a", "c"), result);
        }
    }

    // ======================== 转换 ========================

    @Nested
    @DisplayName("toImmutableList / toImmutableSet 测试")
    class ToImmutableTests {

        @Test
        @DisplayName("toImmutableList - null 返回空列表")
        void toImmutableList_null() {
            assertEquals(List.of(), CollectionUtil.toImmutableList(null));
        }

        @Test
        @DisplayName("toImmutableList - 空集合返回空列表")
        void toImmutableList_empty() {
            assertEquals(List.of(), CollectionUtil.toImmutableList(Set.of()));
        }

        @Test
        @DisplayName("toImmutableList - 正常转换")
        void toImmutableList_normal() {
            List<String> result = CollectionUtil.toImmutableList(new ArrayList<>(List.of("a", "b")));
            assertEquals(List.of("a", "b"), result);
            assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
        }

        @Test
        @DisplayName("toImmutableSet - null 返回空 Set")
        void toImmutableSet_null() {
            assertEquals(Set.of(), CollectionUtil.toImmutableSet(null));
        }

        @Test
        @DisplayName("toImmutableSet - 正常转换")
        void toImmutableSet_normal() {
            Set<String> result = CollectionUtil.toImmutableSet(List.of("a", "b", "a"));
            assertEquals(Set.of("a", "b"), result);
            assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
        }
    }

    // ======================== 分页 ========================

    @Nested
    @DisplayName("page 测试")
    class PageTests {

        @Test
        @DisplayName("page - null 集合返回空列表")
        void page_null() {
            assertEquals(List.of(), CollectionUtil.page(null, 1, 10));
        }

        @Test
        @DisplayName("page - 空集合返回空列表")
        void page_empty() {
            assertEquals(List.of(), CollectionUtil.page(Set.of(), 1, 10));
        }

        @Test
        @DisplayName("page - 页码为 0 返回空列表")
        void page_zeroPage() {
            assertEquals(List.of(), CollectionUtil.page(List.of(1, 2, 3), 0, 10));
        }

        @Test
        @DisplayName("page - 页大小为 0 返回空列表")
        void page_zeroSize() {
            assertEquals(List.of(), CollectionUtil.page(List.of(1, 2, 3), 1, 0));
        }

        @Test
        @DisplayName("page - 正常分页第一页")
        void page_firstPage() {
            List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            assertEquals(List.of(1, 2, 3), CollectionUtil.page(list, 1, 3));
        }

        @Test
        @DisplayName("page - 正常分页中间页")
        void page_middlePage() {
            List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            assertEquals(List.of(4, 5, 6), CollectionUtil.page(list, 2, 3));
        }

        @Test
        @DisplayName("page - 最后一页不满")
        void page_lastPage() {
            List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            assertEquals(List.of(10), CollectionUtil.page(list, 4, 3));
        }

        @Test
        @DisplayName("page - 超出范围返回空列表")
        void page_outOfRange() {
            List<Integer> list = List.of(1, 2, 3);
            assertEquals(List.of(), CollectionUtil.page(list, 5, 10));
        }

        @Test
        @DisplayName("page - 对 Set 分页")
        void page_set() {
            Set<Integer> set = new LinkedHashSet<>(List.of(1, 2, 3, 4, 5));
            assertEquals(List.of(1, 2), CollectionUtil.page(set, 1, 2));
        }
    }
}
