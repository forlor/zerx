package com.zerx.common.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThrowableFunction Tests")
class ThrowableFunctionTest {

    // ======================== unchecked() Tests ========================

    @Nested
    @DisplayName("unchecked() Tests")
    class UncheckedTest {

        @Test
        @DisplayName("unchecked() should pass through normal execution")
        void uncheckedNormal() {
            ThrowableFunction<String, Integer> fn = Integer::parseInt;
            Function<String, Integer> unchecked = fn.unchecked();
            assertEquals(42, unchecked.apply("42"));
        }

        @Test
        @DisplayName("unchecked() should wrap checked exception as RuntimeException")
        void uncheckedWrapsChecked() {
            ThrowableFunction<String, String> fn = s -> {
                throw new IOException("IO error");
            };
            Function<String, String> unchecked = fn.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> unchecked.apply("hello"));
            assertInstanceOf(IOException.class, ex.getCause());
            assertEquals("IO error", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("unchecked() should rethrow RuntimeException as-is")
        void uncheckedRethrowsRuntime() {
            IllegalArgumentException original = new IllegalArgumentException("bad arg");
            ThrowableFunction<String, String> fn = s -> {
                throw original;
            };
            Function<String, String> unchecked = fn.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> unchecked.apply("hello"));
            assertSame(original, ex);
        }

        @Test
        @DisplayName("unchecked() should handle null return value")
        void uncheckedNullReturn() {
            ThrowableFunction<String, String> fn = s -> null;
            Function<String, String> unchecked = fn.unchecked();
            assertNull(unchecked.apply("hello"));
        }
    }

    // ======================== of() Tests ========================

    @Nested
    @DisplayName("of() Tests")
    class OfTest {

        @Test
        @DisplayName("of() should convert standard Function to ThrowableFunction")
        void ofConvertsFunction() throws Throwable {
            Function<String, Integer> standard = String::length;
            ThrowableFunction<String, Integer> throwable = ThrowableFunction.of(standard);
            assertEquals(5, throwable.apply("hello"));
        }

        @Test
        @DisplayName("of() should execute the standard Function correctly")
        void ofExecutesFunction() throws Throwable {
            Function<Integer, String> standard = i -> "Number: " + i;
            ThrowableFunction<Integer, String> throwable = ThrowableFunction.of(standard);
            assertEquals("Number: 42", throwable.apply(42));
        }
    }

    // ======================== wrap() Tests ========================

    @Nested
    @DisplayName("wrap() Tests")
    class WrapTest {

        @Test
        @DisplayName("wrap() should convert ThrowableFunction to standard Function")
        void wrapConvertsThrowableFunction() {
            ThrowableFunction<String, Integer> throwable = String::length;
            Function<String, Integer> wrapped = ThrowableFunction.wrap(throwable);
            assertEquals(5, wrapped.apply("hello"));
        }

        @Test
        @DisplayName("wrap() should wrap checked exceptions as RuntimeException")
        void wrapWrapsCheckedException() {
            ThrowableFunction<String, String> fn = s -> {
                throw new IOException("error");
            };
            Function<String, String> wrapped = ThrowableFunction.wrap(fn);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> wrapped.apply("test"));
            assertInstanceOf(IOException.class, ex.getCause());
        }

        @Test
        @DisplayName("wrap() should allow use in Stream.map()")
        void wrapInStream() {
            java.util.List<String> items = java.util.List.of("1", "2", "3");
            java.util.List<Integer> results = items.stream()
                    .map(ThrowableFunction.wrap(Integer::parseInt))
                    .toList();
            assertEquals(java.util.List.of(1, 2, 3), results);
        }
    }

    // ======================== andThen() Tests ========================

    @Nested
    @DisplayName("andThen() Tests")
    class AndThenTest {

        @Test
        @DisplayName("andThen() should compose two functions")
        void andThenComposes() throws Throwable {
            ThrowableFunction<String, Integer> parseInt = Integer::parseInt;
            ThrowableFunction<Integer, String> toString = i -> "Result: " + i;
            ThrowableFunction<String, String> composed = parseInt.andThen(toString);
            assertEquals("Result: 42", composed.apply("42"));
        }

        @Test
        @DisplayName("andThen() should propagate exception from first function")
        void andThenFirstThrows() {
            ThrowableFunction<String, String> first = s -> {
                throw new IOException("first error");
            };
            ThrowableFunction<String, String> second = String::toUpperCase;
            ThrowableFunction<String, String> composed = first.andThen(second);
            assertThrows(IOException.class, () -> composed.apply("hello"));
        }

        @Test
        @DisplayName("andThen() should propagate exception from second function")
        void andThenSecondThrows() {
            ThrowableFunction<String, String> first = String::toUpperCase;
            ThrowableFunction<String, String> second = s -> {
                throw new IOException("second error");
            };
            ThrowableFunction<String, String> composed = first.andThen(second);
            assertThrows(IOException.class, () -> composed.apply("hello"));
        }

        @Test
        @DisplayName("Multiple andThen() should chain correctly")
        void multipleAndThen() throws Throwable {
            ThrowableFunction<Integer, Integer> doubleIt = i -> i * 2;
            ThrowableFunction<Integer, Integer> addTen = i -> i + 10;
            ThrowableFunction<Integer, String> toString = i -> "Value: " + i;
            ThrowableFunction<Integer, String> chained = doubleIt.andThen(addTen).andThen(toString);
            assertEquals("Value: 30", chained.apply(10)); // (10 * 2) + 10 = 30
        }

        @Test
        @DisplayName("andThen() should handle different type parameters")
        void andThenDifferentTypes() throws Throwable {
            ThrowableFunction<String, Integer> parse = Integer::parseInt;
            ThrowableFunction<Integer, Double> toDouble = i -> i.doubleValue();
            ThrowableFunction<String, Double> composed = parse.andThen(toDouble);
            assertEquals(42.0, composed.apply("42"));
        }
    }
}
