package com.zerx.common.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ThrowableRunnable Tests")
class ThrowableRunnableTest {

    // ======================== unchecked() Tests ========================

    @Nested
    @DisplayName("unchecked() Tests")
    class UncheckedTest {

        @Test
        @DisplayName("unchecked() should pass through normal execution")
        void uncheckedNormal() {
            AtomicBoolean executed = new AtomicBoolean(false);
            ThrowableRunnable runnable = () -> executed.set(true);
            Runnable unchecked = runnable.unchecked();
            assertDoesNotThrow(unchecked::run);
            assertTrue(executed.get());
        }

        @Test
        @DisplayName("unchecked() should wrap checked exception as RuntimeException")
        void uncheckedWrapsChecked() {
            ThrowableRunnable runnable = () -> {
                throw new IOException("IO error");
            };
            Runnable unchecked = runnable.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, unchecked::run);
            assertInstanceOf(IOException.class, ex.getCause());
            assertEquals("IO error", ex.getCause().getMessage());
        }

        @Test
        @DisplayName("unchecked() should rethrow RuntimeException as-is")
        void uncheckedRethrowsRuntime() {
            IllegalArgumentException original = new IllegalArgumentException("bad arg");
            ThrowableRunnable runnable = () -> {
                throw original;
            };
            Runnable unchecked = runnable.unchecked();
            RuntimeException ex = assertThrows(RuntimeException.class, unchecked::run);
            assertSame(original, ex);
        }
    }

    // ======================== of() Tests ========================

    @Nested
    @DisplayName("of() Tests")
    class OfTest {

        @Test
        @DisplayName("of() should convert standard Runnable to ThrowableRunnable")
        void ofConvertsRunnable() {
            AtomicBoolean executed = new AtomicBoolean(false);
            Runnable standard = () -> executed.set(true);
            ThrowableRunnable throwable = ThrowableRunnable.of(standard);
            assertDoesNotThrow(throwable::run);
            assertTrue(executed.get());
        }

        @Test
        @DisplayName("of() should execute the standard Runnable correctly")
        void ofExecutesRunnable() {
            AtomicReference<String> ref = new AtomicReference<>();
            ThrowableRunnable throwable = ThrowableRunnable.of(() -> ref.set("executed"));
            assertDoesNotThrow(throwable::run);
            assertEquals("executed", ref.get());
        }
    }

    // ======================== wrap() Tests ========================

    @Nested
    @DisplayName("wrap() Tests")
    class WrapTest {

        @Test
        @DisplayName("wrap() should convert ThrowableRunnable to standard Runnable")
        void wrapConvertsThrowableRunnable() {
            AtomicBoolean executed = new AtomicBoolean(false);
            ThrowableRunnable throwable = () -> executed.set(true);
            Runnable wrapped = ThrowableRunnable.wrap(throwable);
            wrapped.run();
            assertTrue(executed.get());
        }

        @Test
        @DisplayName("wrap() should wrap checked exceptions as RuntimeException")
        void wrapWrapsCheckedException() {
            ThrowableRunnable runnable = () -> {
                throw new IOException("error");
            };
            Runnable wrapped = ThrowableRunnable.wrap(runnable);
            RuntimeException ex = assertThrows(RuntimeException.class, wrapped::run);
            assertInstanceOf(IOException.class, ex.getCause());
        }

        @Test
        @DisplayName("wrap() should execute successfully for normal case")
        void wrapNormalExecution() {
            AtomicReference<String> ref = new AtomicReference<>();
            ThrowableRunnable throwable = () -> ref.set("done");
            Runnable wrapped = ThrowableRunnable.wrap(throwable);
            assertDoesNotThrow(wrapped::run);
            assertEquals("done", ref.get());
        }
    }

    // ======================== silent() Tests ========================

    @Nested
    @DisplayName("silent() Tests")
    class SilentTest {

        @Test
        @DisplayName("silent() should execute normally without throwing")
        void silentNormal() {
            AtomicBoolean executed = new AtomicBoolean(false);
            assertDoesNotThrow(() -> ThrowableRunnable.silent(() -> executed.set(true)));
            assertTrue(executed.get());
        }

        @Test
        @DisplayName("silent() should silently ignore checked exceptions")
        void silentIgnoresChecked() {
            assertDoesNotThrow(() ->
                    ThrowableRunnable.silent(() -> {
                        throw new IOException("should be ignored");
                    })
            );
        }

        @Test
        @DisplayName("silent() should silently ignore RuntimeException")
        void silentIgnoresRuntime() {
            assertDoesNotThrow(() ->
                    ThrowableRunnable.silent(() -> {
                        throw new RuntimeException("should be ignored");
                    })
            );
        }

        @Test
        @DisplayName("silent() should rethrow Error (e.g. OutOfMemoryError)")
        void silentRethrowsError() {
            assertThrows(OutOfMemoryError.class, () ->
                    ThrowableRunnable.silent(() -> {
                        throw new OutOfMemoryError("should not be swallowed");
                    })
            );
        }

        @Test
        @DisplayName("silent() should rethrow StackOverflowError")
        void silentRethrowsStackOverflowError() {
            assertThrows(StackOverflowError.class, () ->
                    ThrowableRunnable.silent(() -> {
                        throw new StackOverflowError("should not be swallowed");
                    })
            );
        }
    }

    // ======================== quiet() Tests ========================

    @Nested
    @DisplayName("quiet() Tests")
    class QuietTest {

        @Test
        @DisplayName("quiet() should execute normally without calling logger")
        void quietNormal() {
            AtomicBoolean loggerCalled = new AtomicBoolean(false);
            AtomicBoolean executed = new AtomicBoolean(false);
            assertDoesNotThrow(() ->
                    ThrowableRunnable.quiet(() -> executed.set(true), e -> loggerCalled.set(true))
            );
            assertTrue(executed.get());
            assertFalse(loggerCalled.get());
        }

        @Test
        @DisplayName("quiet() should pass exception to logger when exception occurs")
        void quietPassesExceptionToLogger() {
            AtomicReference<Throwable> captured = new AtomicReference<>();
            assertDoesNotThrow(() ->
                    ThrowableRunnable.quiet(
                            () -> { throw new IOException("test error"); },
                            captured::set
                    )
            );
            assertNotNull(captured.get());
            assertInstanceOf(IOException.class, captured.get());
            assertEquals("test error", captured.get().getMessage());
        }

        @Test
        @DisplayName("quiet() with null logger should not throw")
        void quietNullLogger() {
            assertDoesNotThrow(() ->
                    ThrowableRunnable.quiet(
                            () -> { throw new IOException("test error"); },
                            null
                    )
            );
        }
    }

    // ======================== withFallback() Tests ========================

    @Nested
    @DisplayName("withFallback() Tests")
    class WithFallbackTest {

        @Test
        @DisplayName("withFallback() should execute main runnable when successful")
        void withFallbackMainSuccess() {
            AtomicBoolean mainExecuted = new AtomicBoolean(false);
            AtomicBoolean fallbackExecuted = new AtomicBoolean(false);
            assertDoesNotThrow(() ->
                    ThrowableRunnable.withFallback(
                            () -> mainExecuted.set(true),
                            () -> fallbackExecuted.set(true)
                    )
            );
            assertTrue(mainExecuted.get());
            assertFalse(fallbackExecuted.get());
        }

        @Test
        @DisplayName("withFallback() should execute fallback when main throws")
        void withFallbackExecutesFallback() {
            AtomicBoolean mainExecuted = new AtomicBoolean(false);
            AtomicBoolean fallbackExecuted = new AtomicBoolean(false);
            assertDoesNotThrow(() ->
                    ThrowableRunnable.withFallback(
                            () -> {
                                mainExecuted.set(true);
                                throw new IOException("main failed");
                            },
                            () -> fallbackExecuted.set(true)
                    )
            );
            assertTrue(mainExecuted.get());
            assertTrue(fallbackExecuted.get());
        }

        @Test
        @DisplayName("withFallback() should silently ignore when both main and fallback throw")
        void withFallbackBothThrow() {
            assertDoesNotThrow(() ->
                    ThrowableRunnable.withFallback(
                            () -> { throw new IOException("main failed"); },
                            () -> { throw new RuntimeException("fallback failed"); }
                    )
            );
        }

        @Test
        @DisplayName("withFallback() should rethrow Error from main")
        void withFallbackRethrowsErrorFromMain() {
            assertThrows(OutOfMemoryError.class, () ->
                    ThrowableRunnable.withFallback(
                            () -> { throw new OutOfMemoryError("fatal"); },
                            () -> {}
                    )
            );
        }

        @Test
        @DisplayName("withFallback() should rethrow Error from fallback")
        void withFallbackRethrowsErrorFromFallback() {
            assertThrows(OutOfMemoryError.class, () ->
                    ThrowableRunnable.withFallback(
                            () -> { throw new IOException("main failed"); },
                            () -> { throw new OutOfMemoryError("fallback fatal"); }
                    )
            );
        }
    }
}
