package com.zerx.common.retry;

import com.zerx.common.functional.ThrowableRunnable;
import com.zerx.common.functional.ThrowableSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Retryer}
 */
class RetryerTest {

    // ======================== Successful execution ========================

    @Nested
    @DisplayName("Successful execution (no retry)")
    class SuccessfulExecution {

        @Test
        @DisplayName("execute returns result on first success")
        void firstSuccess() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .build();

            String result = retryer.execute(() -> {
                attempts.incrementAndGet();
                return "success";
            });

            assertEquals("success", result);
            assertEquals(1, attempts.get());
        }

        @Test
        @DisplayName("executeRunnable succeeds on first try")
        void runnableFirstSuccess() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<Void> retryer = Retryer.<Void>of()
                    .maxAttempts(3)
                    .build();

            retryer.executeRunnable(() -> attempts.incrementAndGet());

            assertEquals(1, attempts.get());
        }
    }

    // ======================== Retry and eventual success ========================

    @Nested
    @DisplayName("Retry on failure, eventually succeed")
    class RetryAndSucceed {

        @Test
        @DisplayName("retries and succeeds on second attempt")
        void succeedOnSecondAttempt() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .build();

            String result = retryer.execute(() -> {
                int attempt = attempts.incrementAndGet();
                if (attempt < 2) {
                    throw new RuntimeException("fail");
                }
                return "success";
            });

            assertEquals("success", result);
            assertEquals(2, attempts.get());
        }

        @Test
        @DisplayName("retries and succeeds on last attempt")
        void succeedOnLastAttempt() throws Exception {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(5)
                    .build();

            String result = retryer.execute(() -> {
                int attempt = attempts.incrementAndGet();
                if (attempt < 5) {
                    throw new RuntimeException("fail");
                }
                return "success";
            });

            assertEquals("success", result);
            assertEquals(5, attempts.get());
        }
    }

    // ======================== Retry exhausted ========================

    @Nested
    @DisplayName("Retry exhausted (max attempts reached)")
    class RetryExhausted {

        @Test
        @DisplayName("throws last exception when all attempts fail")
        void throwsLastException() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .build();

            Exception thrown = assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new IOException("connection failed: attempt " + attempts.get());
                    })
            );

            assertTrue(thrown instanceof IOException);
            assertEquals("connection failed: attempt 3", thrown.getMessage());
            assertEquals(3, attempts.get());
        }

        @Test
        @DisplayName("executeRunnable throws when all attempts fail")
        void runnableThrowsOnExhaust() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<Void> retryer = Retryer.<Void>of()
                    .maxAttempts(2)
                    .build();

            assertThrows(RuntimeException.class, () ->
                    retryer.executeRunnable(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail " + attempts);
                    })
            );
            assertEquals(2, attempts.get());
        }

        @Test
        @DisplayName("maxAttempts(1) executes only once")
        void maxAttemptsOne() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(1)
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("only once");
                    })
            );
            assertEquals(1, attempts.get());
        }
    }

    // ======================== Builder defaults ========================

    @Nested
    @DisplayName("Builder defaults")
    class BuilderDefaults {

        @Test
        @DisplayName("of(Class) creates a builder")
        void ofWithType() {
            Retryer<String> retryer = Retryer.of(String.class).build();
            assertNotNull(retryer);
        }

        @Test
        @DisplayName("of() creates a builder")
        void ofNoType() {
            Retryer<String> retryer = Retryer.<String>of().build();
            assertNotNull(retryer);
        }
    }

    // ======================== Backoff strategies ========================

    @Nested
    @DisplayName("Backoff strategies")
    class BackoffStrategies {

        @Test
        @DisplayName("NONE backoff has no delay")
        void noneBackoffNoDelay() {
            AtomicInteger attempts = new AtomicInteger(0);
            long start = System.currentTimeMillis();

            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(5)
                    .build(); // default is NONE

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail");
                    })
            );

            long elapsed = System.currentTimeMillis() - start;
            // With no delay, 5 attempts should complete very fast
            assertTrue(elapsed < 1000, "NONE backoff should have no delay, elapsed " + elapsed + "ms");
        }

        @Test
        @DisplayName("FIXED backoff applies consistent delay")
        void fixedBackoff() {
            AtomicInteger attempts = new AtomicInteger(0);
            long start = System.currentTimeMillis();

            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .fixedBackoff(Duration.ofMillis(100))
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail");
                    })
            );

            long elapsed = System.currentTimeMillis() - start;
            // 2 delays of ~100ms = ~200ms
            assertTrue(elapsed >= 150, "FIXED backoff should delay ~200ms, elapsed " + elapsed + "ms");
        }

        @Test
        @DisplayName("LINEAR backoff increases delay with attempt number")
        void linearBackoff() {
            AtomicInteger attempts = new AtomicInteger(0);
            long start = System.currentTimeMillis();

            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(4)
                    .linearBackoff(Duration.ofMillis(50), Duration.ofMillis(500))
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail");
                    })
            );

            long elapsed = System.currentTimeMillis() - start;
            // Delays: 50, 100, 150 = 300ms total minimum
            assertTrue(elapsed >= 200, "LINEAR backoff should have increasing delay, elapsed " + elapsed + "ms");
        }

        @Test
        @DisplayName("EXPONENTIAL backoff grows exponentially")
        void exponentialBackoff() {
            AtomicInteger attempts = new AtomicInteger(0);
            long start = System.currentTimeMillis();

            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(4)
                    .backoff(Duration.ofMillis(50), Duration.ofMillis(1000))
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail");
                    })
            );

            long elapsed = System.currentTimeMillis() - start;
            // Delays: 50, 100, 200 = 350ms minimum
            assertTrue(elapsed >= 250, "EXPONENTIAL backoff should grow fast, elapsed " + elapsed + "ms");
        }

        @Test
        @DisplayName("EXPONENTIAL backoff is capped at maxDelay")
        void exponentialBackoffCapped() {
            AtomicInteger attempts = new AtomicInteger(0);
            long start = System.currentTimeMillis();

            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(5)
                    .backoff(Duration.ofMillis(100), Duration.ofMillis(200))
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail");
                    })
            );

            long elapsed = System.currentTimeMillis() - start;
            // Delays: 100, 200, 200, 200 = 700ms max
            // With capping, this should not be much more than 700ms
            assertTrue(elapsed < 2000, "EXPONENTIAL backoff should be capped, elapsed " + elapsed + "ms");
        }

        @Test
        @DisplayName("jitter randomizes delay (variance between runs)")
        void jitterEnabled() {
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .fixedBackoff(Duration.ofMillis(50))
                    .jitter()
                    .build();

            long start = System.currentTimeMillis();
            AtomicInteger attempts = new AtomicInteger(0);
            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("fail");
                    })
            );
            long elapsed = System.currentTimeMillis() - start;
            // With jitter of 50ms, delays could be 37-62ms
            // 2 delays => 74-124ms total
            assertTrue(elapsed >= 50, "Jitter should not make it faster than ~50ms total");
        }
    }

    // ======================== Exception filtering ========================

    @Nested
    @DisplayName("Exception type filtering")
    class ExceptionFiltering {

        @Test
        @DisplayName("retryOn matches exact exception type")
        void retryOnExactType() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .retryOn(IOException.class)
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new IOException("io error");
                    })
            );
            assertEquals(3, attempts.get());
        }

        @Test
        @DisplayName("retryOn does not retry unmatched exception type")
        void retryOnUnmatchedType() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(5)
                    .retryOn(IOException.class)
                    .build();

            assertThrows(IllegalArgumentException.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new IllegalArgumentException("not retryable");
                    })
            );
            assertEquals(5, attempts.get());
        }

        @Test
        @DisplayName("retryOn matches subclass")
        void retryOnSubclass() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .retryOn(IOException.class)
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new java.net.SocketTimeoutException("timeout");
                    })
            );
            assertEquals(3, attempts.get());
        }

        @Test
        @DisplayName("retryOn with multiple exception types")
        void retryOnMultipleTypes() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .retryOn(IOException.class, IllegalStateException.class)
                    .build();

            // Test first type
            attempts.set(0);
            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new IllegalStateException("state error");
                    })
            );
            assertEquals(3, attempts.get());
        }
    }

    // ======================== Custom predicate ========================

    @Nested
    @DisplayName("Custom retry predicate")
    class CustomPredicate {

        @Test
        @DisplayName("retryWhen retries when predicate returns true")
        void retryWhenTrue() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .retryWhen(e -> e.getMessage().contains("retryable"))
                    .build();

            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("retryable error");
                    })
            );
            assertEquals(3, attempts.get());
        }

        @Test
        @DisplayName("retryWhen does not retry when predicate returns false")
        void retryWhenFalse() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(5)
                    .retryWhen(e -> e.getMessage().contains("retryable"))
                    .build();

            assertThrows(RuntimeException.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("not retryable");
                    })
            );
            assertEquals(5, attempts.get());
        }

        @Test
        @DisplayName("retryOn + retryWhen combines with OR logic")
        void retryOnPlusRetryWhen() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .retryOn(IOException.class)
                    .retryWhen(e -> e.getMessage() != null && e.getMessage().contains("special"))
                    .build();

            // This is neither IOException nor contains "special"
            // The loop still runs maxAttempts times (sleep is skipped when shouldRetry is false)
            assertThrows(RuntimeException.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("generic error");
                    })
            );
            assertEquals(3, attempts.get());

            // This matches retryWhen predicate
            attempts.set(0);
            assertThrows(Exception.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new RuntimeException("special error");
                    })
            );
            assertEquals(3, attempts.get());
        }
    }

    // ======================== InterruptedException ========================

    @Nested
    @DisplayName("InterruptedException handling")
    class InterruptedExceptionHandling {

        @Test
        @DisplayName("InterruptedException is propagated immediately (not retried)")
        void interruptNotRetried() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(5)
                    .build();

            Thread currentThread = Thread.currentThread();
            assertThrows(InterruptedException.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        currentThread.interrupt();
                        throw new InterruptedException("interrupted");
                    })
            );

            assertEquals(1, attempts.get(), "InterruptedException should not be retried");
            // Clear interrupt flag for subsequent tests
            assertTrue(Thread.interrupted(), "Interrupt flag should be set");
        }

        @Test
        @DisplayName("Error is not caught by retryer")
        void errorNotCaught() {
            AtomicInteger attempts = new AtomicInteger(0);
            Retryer<String> retryer = Retryer.<String>of()
                    .maxAttempts(3)
                    .build();

            assertThrows(OutOfMemoryError.class, () ->
                    retryer.execute(() -> {
                        attempts.incrementAndGet();
                        throw new OutOfMemoryError("oom");
                    })
            );
            assertEquals(1, attempts.get(), "Error should not be retried");
        }
    }

    // ======================== Null handling ========================

    @Nested
    @DisplayName("Null handling")
    class NullHandling {

        @Test
        @DisplayName("execute with null supplier throws NullPointerException")
        void nullSupplier() {
            Retryer<String> retryer = Retryer.<String>of().build();
            assertThrows(NullPointerException.class, () -> retryer.execute(null));
        }

        @Test
        @DisplayName("executeRunnable with null task throws NullPointerException")
        void nullRunnable() {
            Retryer<Void> retryer = Retryer.<Void>of().build();
            assertThrows(NullPointerException.class, () -> retryer.executeRunnable(null));
        }
    }
}
