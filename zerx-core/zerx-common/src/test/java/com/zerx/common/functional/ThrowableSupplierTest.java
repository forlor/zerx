package com.zerx.common.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThrowableSupplier Tests")
class ThrowableSupplierTest {

    // ======================== unchecked() Tests ========================

    @Nested
    @DisplayName("unchecked() Tests")
    class UncheckedTest {

        @Test
        @DisplayName("unchecked() should pass through normal execution")
        void uncheckedNormal() {
            ThrowableSupplier<String> supplier = () -> "hello";
            Supplier<String> unchecked = supplier.unchecked();
            assertEquals("hello", unchecked.get());
        }

        @Test
        @DisplayName("unchecked() should wrap checked exception as RuntimeException")
        void uncheckedWrapsChecked() {
            ThrowableSupplier<String> supplier = () -> {
                throw new IOException("IO error");
            };
            Supplier<String> unchecked = supplier.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, unchecked::get);
            assertInstanceOf(IOException.class, ex.getCause());
            assertEquals("IO error", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("unchecked() should rethrow RuntimeException as-is")
        void uncheckedRethrowsRuntime() {
            IllegalArgumentException original = new IllegalArgumentException("bad arg");
            ThrowableSupplier<String> supplier = () -> {
                throw original;
            };
            Supplier<String> unchecked = supplier.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, unchecked::get);
            assertSame(original, ex);
        }

        @Test
        @DisplayName("unchecked() should handle null return value")
        void uncheckedNullReturn() {
            ThrowableSupplier<String> supplier = () -> null;
            Supplier<String> unchecked = supplier.unchecked();
            assertNull(unchecked.get());
        }
    }

    // ======================== of() Tests ========================

    @Nested
    @DisplayName("of() Tests")
    class OfTest {

        @Test
        @DisplayName("of() should convert standard Supplier to ThrowableSupplier")
        void ofConvertsSupplier() throws Throwable {
            Supplier<String> standard = () -> "hello";
            ThrowableSupplier<String> throwable = ThrowableSupplier.of(standard);
            assertEquals("hello", throwable.get());
        }

        @Test
        @DisplayName("of() should execute the standard Supplier correctly")
        void ofExecutesSupplier() {
            AtomicBoolean called = new AtomicBoolean(false);
            Supplier<String> standard = () -> {
                called.set(true);
                return "result";
            };
            ThrowableSupplier<String> throwable = ThrowableSupplier.of(standard);
            assertDoesNotThrow(() -> assertEquals("result", throwable.get()));
            assertTrue(called.get());
        }
    }

    // ======================== wrap() Tests ========================

    @Nested
    @DisplayName("wrap() Tests")
    class WrapTest {

        @Test
        @DisplayName("wrap() should convert ThrowableSupplier to standard Supplier")
        void wrapConvertsThrowableSupplier() {
            ThrowableSupplier<String> throwable = () -> "hello";
            Supplier<String> wrapped = ThrowableSupplier.wrap(throwable);
            assertEquals("hello", wrapped.get());
        }

        @Test
        @DisplayName("wrap() should wrap checked exceptions as RuntimeException")
        void wrapWrapsCheckedException() {
            ThrowableSupplier<String> supplier = () -> {
                throw new IOException("error");
            };
            Supplier<String> wrapped = ThrowableSupplier.wrap(supplier);
            RuntimeException ex = assertThrows(RuntimeException.class, wrapped::get);
            assertInstanceOf(IOException.class, ex.getCause());
        }

        @Test
        @DisplayName("wrap() should handle complex return types")
        void wrapComplexReturnTypes() {
            ThrowableSupplier<int[]> supplier = () -> new int[]{1, 2, 3};
            Supplier<int[]> wrapped = ThrowableSupplier.wrap(supplier);
            int[] result = wrapped.get();
            assertArrayEquals(new int[]{1, 2, 3}, result);
        }
    }

    // ======================== orElse() Tests ========================

    @Nested
    @DisplayName("orElse() Tests")
    class OrElseTest {

        @Test
        @DisplayName("orElse() should return value when successful")
        void orElseSuccess() {
            ThrowableSupplier<String> supplier = () -> "hello";
            assertEquals("hello", supplier.orElse("default"));
        }

        @Test
        @DisplayName("orElse() should return default when exception occurs")
        void orElseDefault() {
            ThrowableSupplier<String> supplier = () -> {
                throw new IOException("error");
            };
            assertEquals("default", supplier.orElse("default"));
        }

        @Test
        @DisplayName("orElse() should return null default when exception occurs")
        void orElseNullDefault() {
            ThrowableSupplier<String> supplier = () -> {
                throw new IOException("error");
            };
            assertNull(supplier.orElse(null));
        }

        @Test
        @DisplayName("orElse() should rethrow Error")
        void orElseRethrowsError() {
            ThrowableSupplier<String> supplier = () -> {
                throw new OutOfMemoryError("fatal");
            };
            assertThrows(OutOfMemoryError.class, () -> supplier.orElse("default"));
        }

        @Test
        @DisplayName("orElse() should return value when null is intentionally returned")
        void orElseNullValue() {
            ThrowableSupplier<String> supplier = () -> null;
            assertNull(supplier.orElse("default")); // null is a valid return, not an exception
        }
    }

    // ======================== orElseGet() Tests ========================

    @Nested
    @DisplayName("orElseGet() Tests")
    class OrElseGetTest {

        @Test
        @DisplayName("orElseGet() should return value when successful")
        void orElseGetSuccess() {
            ThrowableSupplier<String> supplier = () -> "hello";
            AtomicBoolean fallbackCalled = new AtomicBoolean(false);
            assertEquals("hello", supplier.orElseGet(() -> {
                fallbackCalled.set(true);
                return "fallback";
            }));
            assertFalse(fallbackCalled.get());
        }

        @Test
        @DisplayName("orElseGet() should call fallback supplier when exception occurs")
        void orElseGetFallback() {
            ThrowableSupplier<String> supplier = () -> {
                throw new IOException("error");
            };
            assertEquals("fallback", supplier.orElseGet(() -> "fallback"));
        }

        @Test
        @DisplayName("orElseGet() fallback should be lazily evaluated")
        void orElseGetLazyEvaluation() {
            ThrowableSupplier<String> supplier = () -> "hello";
            AtomicBoolean fallbackCalled = new AtomicBoolean(false);
            supplier.orElseGet(() -> {
                fallbackCalled.set(true);
                return "fallback";
            });
            assertFalse(fallbackCalled.get());
        }

        @Test
        @DisplayName("orElseGet() should rethrow Error")
        void orElseGetRethrowsError() {
            ThrowableSupplier<String> supplier = () -> {
                throw new OutOfMemoryError("fatal");
            };
            assertThrows(OutOfMemoryError.class, () -> supplier.orElseGet(() -> "fallback"));
        }

        @Test
        @DisplayName("orElseGet() should use fallback for RuntimeException too")
        void orElseGetRuntimeException() {
            ThrowableSupplier<String> supplier = () -> {
                throw new RuntimeException("runtime error");
            };
            assertEquals("fallback", supplier.orElseGet(() -> "fallback"));
        }
    }

    // ======================== orElseThrow() Tests ========================

    @Nested
    @DisplayName("orElseThrow() Tests")
    class OrElseThrowTest {

        @Test
        @DisplayName("orElseThrow() should return value when successful")
        void orElseThrowSuccess() {
            ThrowableSupplier<String> supplier = () -> "hello";
            assertEquals("hello", supplier.orElseThrow(() -> new IllegalStateException("not called")));
        }

        @Test
        @DisplayName("orElseThrow() should throw custom exception when error occurs")
        void orElseThrowCustom() {
            ThrowableSupplier<String> supplier = () -> {
                throw new IOException("error");
            };
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> supplier.orElseThrow(() -> new IllegalStateException("custom error")));
            assertEquals("custom error", ex.getMessage());
        }

        @Test
        @DisplayName("orElseThrow() should use exception supplier lazily")
        void orElseThrowLazy() {
            ThrowableSupplier<String> supplier = () -> "hello";
            AtomicBoolean exceptionCreated = new AtomicBoolean(false);
            supplier.orElseThrow(() -> {
                exceptionCreated.set(true);
                return new IllegalStateException("should not be created");
            });
            assertFalse(exceptionCreated.get());
        }

        @Test
        @DisplayName("orElseThrow() should rethrow Error even with custom exception supplier")
        void orElseThrowRethrowsError() {
            ThrowableSupplier<String> supplier = () -> {
                throw new OutOfMemoryError("fatal");
            };
            assertThrows(OutOfMemoryError.class,
                    () -> supplier.orElseThrow(() -> new IllegalStateException("custom")));
        }

        @Test
        @DisplayName("orElseThrow() should work with different exception types")
        void orElseThrowDifferentExceptionType() {
            ThrowableSupplier<Integer> supplier = () -> {
                throw new IOException("error");
            };
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> supplier.orElseThrow(() -> new IllegalArgumentException("invalid")));
            assertEquals("invalid", ex.getMessage());
        }
    }
}
