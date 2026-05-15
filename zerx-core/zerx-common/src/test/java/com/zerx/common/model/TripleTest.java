package com.zerx.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Triple Tests")
class TripleTest {

    // ======================== Factory Method Tests ========================

    @Nested
    @DisplayName("of() Factory Method")
    class OfTest {

        @Test
        @DisplayName("of() should create a Triple with all three values")
        void ofCreatesTriple() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            assertEquals("hello", triple.first());
            assertEquals(42, triple.second());
            assertEquals(true, triple.third());
        }

        @Test
        @DisplayName("of() with null values should create Triple with nulls")
        void ofWithNullValues() {
            Triple<String, Integer, Boolean> triple = Triple.of(null, null, null);
            assertNull(triple.first());
            assertNull(triple.second());
            assertNull(triple.third());
        }

        @Test
        @DisplayName("of() with mixed null values")
        void ofWithMixedNulls() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", null, true);
            assertEquals("hello", triple.first());
            assertNull(triple.second());
            assertEquals(true, triple.third());
        }
    }

    // ======================== firstTwo() Tests ========================

    @Nested
    @DisplayName("firstTwo() Tests")
    class FirstTwoTest {

        @Test
        @DisplayName("firstTwo() should return Pair of first and second")
        void firstTwo() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            Pair<String, Integer> pair = triple.firstTwo();
            assertEquals("hello", pair.left());
            assertEquals(42, pair.right());
        }

        @Test
        @DisplayName("firstTwo() with null first and second")
        void firstTwoWithNulls() {
            Triple<String, Integer, Boolean> triple = Triple.of(null, null, true);
            Pair<String, Integer> pair = triple.firstTwo();
            assertNull(pair.left());
            assertNull(pair.right());
        }
    }

    // ======================== lastTwo() Tests ========================

    @Nested
    @DisplayName("lastTwo() Tests")
    class LastTwoTest {

        @Test
        @DisplayName("lastTwo() should return Pair of second and third")
        void lastTwo() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            Pair<Integer, Boolean> pair = triple.lastTwo();
            assertEquals(42, pair.left());
            assertEquals(true, pair.right());
        }

        @Test
        @DisplayName("lastTwo() with null second and third")
        void lastTwoWithNulls() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", null, null);
            Pair<Integer, Boolean> pair = triple.lastTwo();
            assertNull(pair.left());
            assertNull(pair.right());
        }
    }

    // ======================== split() Tests ========================

    @Nested
    @DisplayName("split() Tests")
    class SplitTest {

        @Test
        @DisplayName("split() should return Pair<Pair<F,S>, T>")
        void split() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            Pair<Pair<String, Integer>, Boolean> split = triple.split();
            assertEquals("hello", split.left().left());
            assertEquals(42, split.left().right());
            assertEquals(true, split.right());
        }

        @Test
        @DisplayName("split() with null values should preserve nulls")
        void splitWithNulls() {
            Triple<String, Integer, Boolean> triple = Triple.of(null, null, null);
            Pair<Pair<String, Integer>, Boolean> split = triple.split();
            assertNull(split.left().left());
            assertNull(split.left().right());
            assertNull(split.right());
        }

        @Test
        @DisplayName("split() firstTwo() should be equivalent to firstTwo()")
        void splitFirstTwoEquivalent() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            Pair<Pair<String, Integer>, Boolean> split = triple.split();
            Pair<String, Integer> firstTwo = triple.firstTwo();
            assertEquals(firstTwo, split.left());
        }
    }

    // ======================== isFull() Tests ========================

    @Nested
    @DisplayName("isFull() Tests")
    class IsFullTest {

        @Test
        @DisplayName("Should return true when all three values are non-null")
        void fullTriple() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            assertTrue(triple.isFull());
        }

        @Test
        @DisplayName("Should return false when first is null")
        void firstNull() {
            Triple<String, Integer, Boolean> triple = Triple.of(null, 42, true);
            assertFalse(triple.isFull());
        }

        @Test
        @DisplayName("Should return false when second is null")
        void secondNull() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", null, true);
            assertFalse(triple.isFull());
        }

        @Test
        @DisplayName("Should return false when third is null")
        void thirdNull() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, null);
            assertFalse(triple.isFull());
        }

        @Test
        @DisplayName("Should return false when all are null")
        void allNull() {
            Triple<String, Integer, Boolean> triple = Triple.of(null, null, null);
            assertFalse(triple.isFull());
        }
    }

    // ======================== Equals / HashCode / ToString ========================

    @Nested
    @DisplayName("Equals, HashCode, ToString")
    class ObjectMethodsTest {

        @Test
        @DisplayName("Equal Triples should have same hashCode")
        void equalInstances() {
            Triple<String, Integer, Boolean> t1 = Triple.of("hello", 42, true);
            Triple<String, Integer, Boolean> t2 = Triple.of("hello", 42, true);
            assertEquals(t1, t2);
            assertEquals(t1.hashCode(), t2.hashCode());
        }

        @Test
        @DisplayName("Different first should not be equal")
        void differentFirst() {
            Triple<String, Integer, Boolean> t1 = Triple.of("hello", 42, true);
            Triple<String, Integer, Boolean> t2 = Triple.of("world", 42, true);
            assertNotEquals(t1, t2);
        }

        @Test
        @DisplayName("Different second should not be equal")
        void differentSecond() {
            Triple<String, Integer, Boolean> t1 = Triple.of("hello", 42, true);
            Triple<String, Integer, Boolean> t2 = Triple.of("hello", 43, true);
            assertNotEquals(t1, t2);
        }

        @Test
        @DisplayName("Different third should not be equal")
        void differentThird() {
            Triple<String, Integer, Boolean> t1 = Triple.of("hello", 42, true);
            Triple<String, Integer, Boolean> t2 = Triple.of("hello", 42, false);
            assertNotEquals(t1, t2);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            assertNotEquals(null, triple);
        }

        @Test
        @DisplayName("Should not equal different type")
        void notEqualDifferentType() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            assertNotEquals("hello", triple);
        }

        @Test
        @DisplayName("Same instance should be equal")
        void sameInstanceEqual() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            assertEquals(triple, triple);
        }

        @Test
        @DisplayName("ToString should contain all values")
        void toStringContainsValues() {
            Triple<String, Integer, Boolean> triple = Triple.of("hello", 42, true);
            String str = triple.toString();
            assertNotNull(str);
            assertTrue(str.contains("hello"));
            assertTrue(str.contains("42"));
            assertTrue(str.contains("true"));
        }

        @Test
        @DisplayName("Null value Triples should be equal")
        void nullValueEquality() {
            Triple<String, Integer, Boolean> t1 = Triple.of(null, null, null);
            Triple<String, Integer, Boolean> t2 = Triple.of(null, null, null);
            assertEquals(t1, t2);
            assertEquals(t1.hashCode(), t2.hashCode());
        }
    }

    // ======================== Different Generic Types ========================

    @Nested
    @DisplayName("Generic Type Support")
    class GenericTypeTest {

        @Test
        @DisplayName("Should work with same generic type")
        void sameGenericType() {
            Triple<String, String, String> triple = Triple.of("a", "b", "c");
            assertEquals("a", triple.first());
            assertEquals("b", triple.second());
            assertEquals("c", triple.third());
        }

        @Test
        @DisplayName("Should work with numeric types")
        void numericTypes() {
            Triple<Integer, Long, Double> triple = Triple.of(1, 2L, 3.0);
            assertEquals(1, triple.first());
            assertEquals(2L, triple.second());
            assertEquals(3.0, triple.third());
        }
    }
}
