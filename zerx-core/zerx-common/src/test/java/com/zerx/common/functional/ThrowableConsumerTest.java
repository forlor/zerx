package com.zerx.common.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThrowableConsumer Tests")
class ThrowableConsumerTest {

    // ======================== unchecked() Tests ========================

    @Nested
    @DisplayName("unchecked() Tests")
    class UncheckedTest {

        @Test
        @DisplayName("unchecked() should pass through normal execution")
        void uncheckedNormal() {
            ThrowableConsumer<String> consumer = s -> {};
            Consumer<String> unchecked = consumer.unchecked();
            assertDoesNotThrow(() -> unchecked.accept("hello"));
        }

        @Test
        @DisplayName("unchecked() should wrap checked exception as RuntimeException")
        void uncheckedWrapsChecked() {
            ThrowableConsumer<String> consumer = s -> {
                throw new IOException("IO error");
            };
            Consumer<String> unchecked = consumer.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> unchecked.accept("hello"));
            assertInstanceOf(IOException.class, ex.getCause());
            assertEquals("IO error", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("unchecked() should rethrow RuntimeException as-is")
        void uncheckedRethrowsRuntime() {
            IllegalArgumentException original = new IllegalArgumentException("bad arg");
            ThrowableConsumer<String> consumer = s -> {
                throw original;
            };
            Consumer<String> unchecked = consumer.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> unchecked.accept("hello"));
            assertSame(original, ex);
        }
    }

    // ======================== of() Tests ========================

    @Nested
    @DisplayName("of() Tests")
    class OfTest {

        @Test
        @DisplayName("of() should convert standard Consumer to ThrowableConsumer")
        void ofConvertsConsumer() {
            List<String> results = new ArrayList<>();
            Consumer<String> standard = results::add;
            ThrowableConsumer<String> throwable = ThrowableConsumer.of(standard);
            assertDoesNotThrow(() -> throwable.accept("hello"));
            assertEquals(1, results.size());
            assertEquals("hello", results.get(0));
        }

        @Test
        @DisplayName("of() should execute the standard Consumer correctly")
        void ofExecutesConsumer() {
            List<Integer> list = new ArrayList<>();
            ThrowableConsumer<Integer> consumer = ThrowableConsumer.of(list::add);
            assertDoesNotThrow(() -> consumer.accept(42));
            assertEquals(1, list.size());
            assertEquals(42, list.get(0));
        }
    }

    // ======================== wrap() Tests ========================

    @Nested
    @DisplayName("wrap() Tests")
    class WrapTest {

        @Test
        @DisplayName("wrap() should convert ThrowableConsumer to standard Consumer")
        void wrapConvertsThrowableConsumer() {
            List<String> results = new ArrayList<>();
            ThrowableConsumer<String> throwable = results::add;
            Consumer<String> wrapped = ThrowableConsumer.wrap(throwable);
            wrapped.accept("hello");
            assertEquals(1, results.size());
            assertEquals("hello", results.get(0));
        }

        @Test
        @DisplayName("wrap() should wrap checked exceptions as RuntimeException")
        void wrapWrapsCheckedException() {
            ThrowableConsumer<String> consumer = s -> {
                throw new IOException("error");
            };
            Consumer<String> wrapped = ThrowableConsumer.wrap(consumer);
            RuntimeException ex = assertThrows(RuntimeException.class, () -> wrapped.accept("test"));
            assertInstanceOf(IOException.class, ex.getCause());
        }

        @Test
        @DisplayName("wrap() should allow use in forEach")
        void wrapInForEach() {
            List<String> items = List.of("a", "b", "c");
            List<String> results = new ArrayList<>();
            Consumer<String> wrapped = ThrowableConsumer.wrap(results::add);
            items.forEach(wrapped);
            assertEquals(3, results.size());
        }
    }

    // ======================== andThen() Tests ========================

    @Nested
    @DisplayName("andThen() Tests")
    class AndThenTest {

        @Test
        @DisplayName("andThen() should execute both consumers in order")
        void andThenExecutesBoth() {
            List<String> results = new ArrayList<>();
            ThrowableConsumer<String> first = s -> results.add("first:" + s);
            ThrowableConsumer<String> second = s -> results.add("second:" + s);
            ThrowableConsumer<String> combined = first.andThen(second);
            assertDoesNotThrow(() -> combined.accept("hello"));
            assertEquals(2, results.size());
            assertEquals("first:hello", results.get(0));
            assertEquals("second:hello", results.get(1));
        }

        @Test
        @DisplayName("andThen() should propagate exception from first consumer")
        void andThenFirstThrows() {
            ThrowableConsumer<String> first = s -> {
                throw new IOException("first error");
            };
            ThrowableConsumer<String> second = s -> {};
            ThrowableConsumer<String> combined = first.andThen(second);
            assertThrows(IOException.class, () -> combined.accept("hello"));
        }

        @Test
        @DisplayName("andThen() should propagate exception from second consumer")
        void andThenSecondThrows() {
            List<String> results = new ArrayList<>();
            ThrowableConsumer<String> first = s -> results.add("executed");
            ThrowableConsumer<String> second = s -> {
                throw new IOException("second error");
            };
            ThrowableConsumer<String> combined = first.andThen(second);
            assertThrows(IOException.class, () -> combined.accept("hello"));
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Multiple andThen() should chain correctly")
        void multipleAndThen() {
            List<Integer> results = new ArrayList<>();
            ThrowableConsumer<Integer> c1 = i -> results.add(i * 1);
            ThrowableConsumer<Integer> c2 = i -> results.add(i * 2);
            ThrowableConsumer<Integer> c3 = i -> results.add(i * 3);
            ThrowableConsumer<Integer> chained = c1.andThen(c2).andThen(c3);
            assertDoesNotThrow(() -> chained.accept(10));
            assertEquals(List.of(10, 20, 30), results);
        }
    }
}
