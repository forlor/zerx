package com.zerx.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageResult Tests")
class PageResultTest {

    // ======================== Factory Method Tests ========================

    @Nested
    @DisplayName("of() Factory Method")
    class OfTest {

        @Test
        @DisplayName("Should calculate totalPages correctly when total divides evenly")
        void totalPagesEven() {
            PageResult<String> result = PageResult.of(List.of("a", "b"), 20, 1, 10);
            assertEquals(2, result.totalPages());
            assertEquals(20, result.total());
            assertEquals(1, result.page());
            assertEquals(10, result.size());
        }

        @Test
        @DisplayName("Should round up totalPages when total does not divide evenly")
        void totalPagesRoundedUp() {
            PageResult<String> result = PageResult.of(List.of("a", "b", "c"), 25, 1, 10);
            assertEquals(3, result.totalPages());
        }

        @Test
        @DisplayName("Should return totalPages=0 when total is 0")
        void totalPagesZero() {
            PageResult<String> result = PageResult.of(List.of(), 0, 1, 10);
            assertEquals(0, result.totalPages());
        }

        @Test
        @DisplayName("Should return totalPages=1 when total is 1")
        void totalPagesOne() {
            PageResult<String> result = PageResult.of(List.of("a"), 1, 1, 10);
            assertEquals(1, result.totalPages());
        }

        @Test
        @DisplayName("Should handle total=1 with size=1 correctly")
        void totalOneSizeOne() {
            PageResult<String> result = PageResult.of(List.of("a"), 1, 1, 1);
            assertEquals(1, result.totalPages());
        }

        @Test
        @DisplayName("Should preserve records list")
        void preservesRecords() {
            List<String> records = List.of("a", "b", "c");
            PageResult<String> result = PageResult.of(records, 100, 1, 10);
            assertEquals(records, result.records());
        }
    }

    // ======================== empty() Tests ========================

    @Nested
    @DisplayName("empty() Factory Method")
    class EmptyTest {

        @Test
        @DisplayName("Should create empty PageResult with correct metadata")
        void createsEmptyResult() {
            PageRequest pageRequest = new PageRequest(3, 25);
            PageResult<?> result = PageResult.empty(pageRequest);

            assertTrue(result.isEmpty());
            assertEquals(0, result.total());
            assertEquals(3, result.page());
            assertEquals(25, result.size());
            assertEquals(0, result.totalPages());
        }

        @Test
        @DisplayName("Empty result should have no records")
        void emptyHasNoRecords() {
            PageResult<?> result = PageResult.empty(new PageRequest());
            assertNotNull(result.records());
            assertTrue(result.records().isEmpty());
        }
    }

    // ======================== isEmpty() Tests ========================

    @Nested
    @DisplayName("isEmpty() Tests")
    class IsEmptyTest {

        @Test
        @DisplayName("Should return true when records is empty")
        void emptyRecords() {
            PageResult<String> result = new PageResult<>(List.of(), 0, 1, 10, 0);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return false when records is not empty")
        void nonEmptyRecords() {
            PageResult<String> result = new PageResult<>(List.of("a"), 1, 1, 10, 1);
            assertFalse(result.isEmpty());
        }
    }

    // ======================== hasNext() Tests ========================

    @Nested
    @DisplayName("hasNext() Tests")
    class HasNextTest {

        @Test
        @DisplayName("Should return true when page < totalPages")
        void hasNext() {
            PageResult<String> result = new PageResult<>(List.of("a"), 20, 1, 10, 2);
            assertTrue(result.hasNext());
        }

        @Test
        @DisplayName("Should return false when page equals totalPages")
        void noNextWhenAtLast() {
            PageResult<String> result = new PageResult<>(List.of("a"), 20, 2, 10, 2);
            assertFalse(result.hasNext());
        }

        @Test
        @DisplayName("Should return false when totalPages is 0")
        void noNextWhenEmpty() {
            PageResult<String> result = new PageResult<>(List.of(), 0, 1, 10, 0);
            assertFalse(result.hasNext());
        }
    }

    // ======================== hasPrevious() Tests ========================

    @Nested
    @DisplayName("hasPrevious() Tests")
    class HasPreviousTest {

        @Test
        @DisplayName("Should return true when page > 1")
        void hasPrevious() {
            PageResult<String> result = new PageResult<>(List.of("a"), 20, 2, 10, 2);
            assertTrue(result.hasPrevious());
        }

        @Test
        @DisplayName("Should return false when page is 1")
        void noPreviousAtFirst() {
            PageResult<String> result = new PageResult<>(List.of("a"), 20, 1, 10, 2);
            assertFalse(result.hasPrevious());
        }
    }

    // ======================== Equals / HashCode / ToString ========================

    @Nested
    @DisplayName("Equals, HashCode, ToString")
    class ObjectMethodsTest {

        @Test
        @DisplayName("Equal PageResults should have same hashCode")
        void equalInstances() {
            PageResult<String> r1 = new PageResult<>(List.of("a", "b"), 2, 1, 10, 1);
            PageResult<String> r2 = new PageResult<>(List.of("a", "b"), 2, 1, 10, 1);
            assertEquals(r1, r2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test
        @DisplayName("Different records should not be equal")
        void differentRecords() {
            PageResult<String> r1 = new PageResult<>(List.of("a"), 1, 1, 10, 1);
            PageResult<String> r2 = new PageResult<>(List.of("b"), 1, 1, 10, 1);
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Different total should not be equal")
        void differentTotal() {
            PageResult<String> r1 = new PageResult<>(List.of(), 0, 1, 10, 0);
            PageResult<String> r2 = new PageResult<>(List.of(), 1, 1, 10, 1);
            assertNotEquals(r1, r2);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            PageResult<String> result = PageResult.of(List.of(), 0, 1, 10);
            assertNotEquals(null, result);
        }

        @Test
        @DisplayName("Should not equal different type")
        void notEqualDifferentType() {
            PageResult<String> result = PageResult.of(List.of(), 0, 1, 10);
            assertNotEquals("string", result);
        }

        @Test
        @DisplayName("Same instance should be equal")
        void sameInstanceEqual() {
            PageResult<String> result = PageResult.of(List.of("a"), 1, 1, 10);
            assertEquals(result, result);
        }

        @Test
        @DisplayName("ToString should contain field information")
        void toStringContainsInfo() {
            PageResult<String> result = PageResult.of(List.of("a"), 1, 1, 10);
            String str = result.toString();
            assertNotNull(str);
            assertTrue(str.contains("a"));
        }
    }

    // ======================== Generic Type Tests ========================

    @Nested
    @DisplayName("Generic Type Support")
    class GenericTypeTest {

        @Test
        @DisplayName("Should work with Integer generic type")
        void integerType() {
            PageResult<Integer> result = PageResult.of(List.of(1, 2, 3), 3, 1, 10);
            assertEquals(3, result.records().size());
            assertEquals(1, result.records().get(0));
        }

        @Test
        @DisplayName("Should work with Object generic type")
        void objectType() {
            record Item(int id, String name) {}
            PageResult<Item> result = PageResult.of(
                    List.of(new Item(1, "test")), 1, 1, 10);
            assertEquals("test", result.records().getFirst().name());
        }
    }
}
