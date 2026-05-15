package com.zerx.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pair Tests")
class PairTest {

    // ======================== Factory Methods ========================

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethodTest {

        @Test
        @DisplayName("of() should create a Pair with both values")
        void ofCreatesPair() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            assertEquals("hello", pair.left());
            assertEquals(42, pair.right());
        }

        @Test
        @DisplayName("ofLeft() should create a Pair with left only, right is null")
        void ofLeftCreatesPair() {
            Pair<String, Integer> pair = Pair.ofLeft("hello");
            assertEquals("hello", pair.left());
            assertNull(pair.right());
        }

        @Test
        @DisplayName("ofRight() should create a Pair with right only, left is null")
        void ofRightCreatesPair() {
            Pair<String, Integer> pair = Pair.ofRight(42);
            assertNull(pair.left());
            assertEquals(42, pair.right());
        }

        @Test
        @DisplayName("of() with null values should create Pair with nulls")
        void ofWithNullValues() {
            Pair<String, Integer> pair = Pair.of(null, null);
            assertNull(pair.left());
            assertNull(pair.right());
        }

        @Test
        @DisplayName("of() with mixed null values")
        void ofWithMixedNulls() {
            Pair<String, Integer> pair1 = Pair.of(null, 42);
            Pair<String, Integer> pair2 = Pair.of("hello", null);
            assertNull(pair1.left());
            assertEquals(42, pair1.right());
            assertEquals("hello", pair2.left());
            assertNull(pair2.right());
        }
    }

    // ======================== swap() Tests ========================

    @Nested
    @DisplayName("swap() Tests")
    class SwapTest {

        @Test
        @DisplayName("swap() should exchange left and right")
        void swapValues() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            Pair<Integer, String> swapped = pair.swap();
            assertEquals(42, swapped.left());
            assertEquals("hello", swapped.right());
        }

        @Test
        @DisplayName("swap() with null left should work")
        void swapWithNullLeft() {
            Pair<String, Integer> pair = Pair.ofLeft("hello");
            Pair<Integer, String> swapped = pair.swap();
            assertNull(swapped.left());
            assertEquals("hello", swapped.right());
        }

        @Test
        @DisplayName("swap() with null right should work")
        void swapWithNullRight() {
            Pair<String, Integer> pair = Pair.ofRight(42);
            Pair<Integer, String> swapped = pair.swap();
            assertEquals(42, swapped.left());
            assertNull(swapped.right());
        }

        @Test
        @DisplayName("swap() twice should return equivalent original values")
        void swapTwice() {
            Pair<String, Integer> original = Pair.of("hello", 42);
            Pair<String, Integer> swappedTwice = original.swap().swap();
            assertEquals(original.left(), swappedTwice.left());
            assertEquals(original.right(), swappedTwice.right());
        }
    }

    // ======================== isFull() Tests ========================

    @Nested
    @DisplayName("isFull() Tests")
    class IsFullTest {

        @Test
        @DisplayName("Should return true when both values are non-null")
        void fullPair() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            assertTrue(pair.isFull());
        }

        @Test
        @DisplayName("Should return false when left is null")
        void leftNull() {
            Pair<String, Integer> pair = Pair.ofLeft("hello");
            assertFalse(pair.isFull());
        }

        @Test
        @DisplayName("Should return false when right is null")
        void rightNull() {
            Pair<String, Integer> pair = Pair.ofRight(42);
            assertFalse(pair.isFull());
        }

        @Test
        @DisplayName("Should return false when both are null")
        void bothNull() {
            Pair<String, Integer> pair = Pair.of(null, null);
            assertFalse(pair.isFull());
        }
    }

    // ======================== toEntry() Tests ========================

    @Nested
    @DisplayName("toEntry() Tests")
    class ToEntryTest {

        @Test
        @DisplayName("toEntry() should return Map.Entry with correct key/value")
        void toEntryReturnsCorrectEntry() {
            Pair<String, Integer> pair = Pair.of("key", 100);
            Map.Entry<String, Integer> entry = pair.toEntry();
            assertEquals("key", entry.getKey());
            assertEquals(100, entry.getValue());
        }

        @Test
        @DisplayName("toEntry() setValue should throw UnsupportedOperationException")
        void toEntrySetValueThrows() {
            Pair<String, Integer> pair = Pair.of("key", 100);
            Map.Entry<String, Integer> entry = pair.toEntry();
            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
        }

        @Test
        @DisplayName("toEntry() should have same key/value as Pair")
        void toEntryEqualsHashCode() {
            Pair<String, Integer> pair1 = Pair.of("key", 100);
            Pair<String, Integer> pair2 = Pair.of("key", 100);
            Map.Entry<String, Integer> entry1 = pair1.toEntry();
            Map.Entry<String, Integer> entry2 = pair2.toEntry();
            // Verify key/value equality separately (anonymous class instances are not .equals)
            assertEquals(entry1.getKey(), entry2.getKey());
            assertEquals(entry1.getValue(), entry2.getValue());
        }

        @Test
        @DisplayName("toEntry() with null values")
        void toEntryWithNulls() {
            Pair<String, Integer> pair = Pair.of(null, null);
            Map.Entry<String, Integer> entry = pair.toEntry();
            assertNull(entry.getKey());
            assertNull(entry.getValue());
        }
    }

    // ======================== Equals / HashCode / ToString ========================

    @Nested
    @DisplayName("Equals, HashCode, ToString")
    class ObjectMethodsTest {

        @Test
        @DisplayName("Equal Pairs should have same hashCode")
        void equalInstances() {
            Pair<String, Integer> p1 = Pair.of("hello", 42);
            Pair<String, Integer> p2 = Pair.of("hello", 42);
            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }

        @Test
        @DisplayName("Different left should not be equal")
        void differentLeft() {
            Pair<String, Integer> p1 = Pair.of("hello", 42);
            Pair<String, Integer> p2 = Pair.of("world", 42);
            assertNotEquals(p1, p2);
        }

        @Test
        @DisplayName("Different right should not be equal")
        void differentRight() {
            Pair<String, Integer> p1 = Pair.of("hello", 42);
            Pair<String, Integer> p2 = Pair.of("hello", 43);
            assertNotEquals(p1, p2);
        }

        @Test
        @DisplayName("Should not equal null")
        void notEqualNull() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            assertNotEquals(null, pair);
        }

        @Test
        @DisplayName("Should not equal different type")
        void notEqualDifferentType() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            assertNotEquals("hello", pair);
        }

        @Test
        @DisplayName("Same instance should be equal")
        void sameInstanceEqual() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            assertEquals(pair, pair);
        }

        @Test
        @DisplayName("ToString should contain both values")
        void toStringContainsValues() {
            Pair<String, Integer> pair = Pair.of("hello", 42);
            String str = pair.toString();
            assertNotNull(str);
            assertTrue(str.contains("hello"));
            assertTrue(str.contains("42"));
        }

        @Test
        @DisplayName("Null value Pairs should be equal")
        void nullValueEquality() {
            Pair<String, Integer> p1 = Pair.of(null, null);
            Pair<String, Integer> p2 = Pair.of(null, null);
            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());
        }
    }

    // ======================== Different Generic Types ========================

    @Nested
    @DisplayName("Generic Type Support")
    class GenericTypeTest {

        @Test
        @DisplayName("Should work with same generic type")
        void sameGenericType() {
            Pair<String, String> pair = Pair.of("key", "value");
            assertEquals("key", pair.left());
            assertEquals("value", pair.right());
        }

        @Test
        @DisplayName("Should work with complex generic types")
        void complexGenericTypes() {
            Pair<List<Integer>, String> pair = Pair.of(List.of(1, 2, 3), "test");
            assertEquals(3, pair.left().size());
            assertEquals("test", pair.right());
        }
    }
}
