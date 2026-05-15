package com.zerx.common.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ThreadUtil}
 */
class ThreadUtilTest {

    // ======================== Constructor ========================

    @Test
    @DisplayName("private constructor throws UnsupportedOperationException")
    void privateConstructor() throws Exception {
        var constructor = ThreadUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var thrown = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, thrown.getCause());
    }

    // ======================== sleep ========================

    @Test
    @DisplayName("sleep(long millis) sleeps for approximately the given duration")
    void sleepMillis() {
        long start = System.currentTimeMillis();
        ThreadUtil.sleep(100);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 80, "Should sleep at least 80ms, was " + elapsed);
        assertTrue(elapsed < 500, "Should not sleep more than 500ms, was " + elapsed);
    }

    @Test
    @DisplayName("sleep(Duration) sleeps for approximately the given duration")
    void sleepDuration() {
        long start = System.currentTimeMillis();
        ThreadUtil.sleep(Duration.ofMillis(100));
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 80, "Should sleep at least 80ms, was " + elapsed);
        assertTrue(elapsed < 500, "Should not sleep more than 500ms, was " + elapsed);
    }

    @Test
    @DisplayName("sleep with zero duration returns quickly")
    void sleepZero() {
        long start = System.currentTimeMillis();
        ThreadUtil.sleep(0);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 100, "Zero sleep should return quickly, was " + elapsed);
    }

    @Test
    @DisplayName("sleep handles InterruptedException by restoring interrupt flag")
    void sleepInterruptedException() throws InterruptedException {
        Thread thread = new Thread(() -> ThreadUtil.sleep(10_000));
        thread.start();
        // Give the thread a moment to start sleeping
        Thread.sleep(50);
        thread.interrupt();
        thread.join(1000);
        assertFalse(thread.isAlive(), "Thread should have terminated");
    }

    // ======================== parallelism ========================

    @Test
    @DisplayName("parallelism() returns a positive integer")
    void parallelism() {
        int p = ThreadUtil.parallelism();
        assertTrue(p > 0, "Parallelism should be positive, was " + p);
    }

    // ======================== availableProcessors ========================

    @Test
    @DisplayName("availableProcessors() returns a positive integer")
    void availableProcessors() {
        int processors = ThreadUtil.availableProcessors();
        assertTrue(processors > 0, "Available processors should be positive, was " + processors);
    }

    @Test
    @DisplayName("availableProcessors() matches Runtime value")
    void availableProcessorsMatchesRuntime() {
        assertEquals(Runtime.getRuntime().availableProcessors(), ThreadUtil.availableProcessors());
    }

    // ======================== startVirtual ========================

    @Test
    @DisplayName("startVirtual(name, task) starts a virtual thread")
    void startVirtualNamed() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<String> threadName = new AtomicReference<>();

        Thread vt = ThreadUtil.startVirtual("test-vt", () -> {
            threadName.set(Thread.currentThread().getName());
            executed.set(true);
        });

        vt.join(5000);
        assertTrue(executed.get(), "Virtual thread should have executed");
        assertTrue(threadName.get().startsWith("test-vt"),
                "Thread name should start with 'test-vt', was " + threadName.get());
        assertTrue(vt.isVirtual(), "Thread should be virtual");
    }

    @Test
    @DisplayName("startVirtual(task) starts a virtual thread with default prefix")
    void startVirtualDefaultPrefix() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicReference<String> threadName = new AtomicReference<>();

        Thread vt = ThreadUtil.startVirtual(() -> {
            threadName.set(Thread.currentThread().getName());
            executed.set(true);
        });

        vt.join(5000);
        assertTrue(executed.get(), "Virtual thread should have executed");
        assertTrue(threadName.get().startsWith("zerx-vt-"),
                "Thread name should start with 'zerx-vt-', was " + threadName.get());
        assertTrue(vt.isVirtual(), "Thread should be virtual");
    }

    @Test
    @DisplayName("startVirtual with null task throws NullPointerException")
    void startVirtualNullTask() {
        assertThrows(NullPointerException.class, () -> ThreadUtil.startVirtual(null));
    }

    // ======================== startVirtualWithPrefix ========================

    @Test
    @DisplayName("startVirtualWithPrefix uses the specified prefix")
    void startVirtualWithPrefix() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        Thread vt = ThreadUtil.startVirtualWithPrefix("myprefix-", () -> {
            threadName.set(Thread.currentThread().getName());
        });

        vt.join(5000);
        assertTrue(threadName.get().startsWith("myprefix-"),
                "Thread name should start with 'myprefix-', was " + threadName.get());
    }

    // ======================== virtualThreadBuilder ========================

    @Test
    @DisplayName("virtualThreadBuilder() returns a non-null builder")
    void virtualThreadBuilder() {
        var builder = ThreadUtil.virtualThreadBuilder();
        assertNotNull(builder);
    }

    // ======================== virtualThreadFactory ========================

    @Test
    @DisplayName("virtualThreadFactory(prefix) creates virtual threads with correct prefix")
    void virtualThreadFactoryWithPrefix() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        var factory = ThreadUtil.virtualThreadFactory("vtf-");
        Thread t = factory.newThread(() -> threadName.set(Thread.currentThread().getName()));
        t.start();
        t.join(5000);
        assertTrue(threadName.get().startsWith("vtf-"), "Was: " + threadName.get());
        assertTrue(t.isVirtual());
    }

    @Test
    @DisplayName("virtualThreadFactory() creates virtual threads with default prefix")
    void virtualThreadFactoryDefault() throws InterruptedException {
        AtomicReference<String> threadName = new AtomicReference<>();
        var factory = ThreadUtil.virtualThreadFactory();
        Thread t = factory.newThread(() -> threadName.set(Thread.currentThread().getName()));
        t.start();
        t.join(5000);
        assertTrue(threadName.get().startsWith("zerx-vt-"), "Was: " + threadName.get());
    }

    // ======================== newVirtualExecutor ========================

    @Test
    @DisplayName("newVirtualExecutor() executes tasks in virtual threads")
    void newVirtualExecutor() throws InterruptedException {
        ExecutorService executor = ThreadUtil.newVirtualExecutor();
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean isVirtual = new AtomicBoolean(false);

        executor.submit(() -> {
            executed.set(true);
            isVirtual.set(Thread.currentThread().isVirtual());
        });

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(executed.get());
        assertTrue(isVirtual.get());
    }

    @Test
    @DisplayName("newVirtualExecutor(prefix) executes tasks with named threads")
    void newVirtualExecutorWithPrefix() throws InterruptedException {
        ExecutorService executor = ThreadUtil.newVirtualExecutor("custom-vt-");
        AtomicReference<String> threadName = new AtomicReference<>();

        executor.submit(() -> threadName.set(Thread.currentThread().getName()));

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(threadName.get().startsWith("custom-vt-"), "Was: " + threadName.get());
    }

    // ======================== namedThreadFactory ========================

    @Test
    @DisplayName("namedThreadFactory(prefix) creates platform threads with correct naming")
    void namedThreadFactory() throws InterruptedException {
        AtomicReference<String> name1 = new AtomicReference<>();
        AtomicReference<String> name2 = new AtomicReference<>();
        var factory = ThreadUtil.namedThreadFactory("pool-");

        Thread t1 = factory.newThread(() -> name1.set(Thread.currentThread().getName()));
        Thread t2 = factory.newThread(() -> name2.set(Thread.currentThread().getName()));

        t1.start();
        t2.start();
        t1.join(5000);
        t2.join(5000);

        assertTrue(name1.get().startsWith("pool-"), "Was: " + name1.get());
        assertTrue(name2.get().startsWith("pool-"), "Was: " + name2.get());
        assertNotEquals(name1.get(), name2.get(), "Each thread should have unique name");
    }

    @Test
    @DisplayName("namedThreadFactory(prefix, daemon) sets daemon flag")
    void namedThreadFactoryDaemon() throws InterruptedException {
        var factory = ThreadUtil.namedThreadFactory("daemon-", true);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isDaemon = new AtomicBoolean(false);

        Thread t = factory.newThread(() -> {
            isDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        });

        t.start();
        latch.await(5, TimeUnit.SECONDS);
        assertTrue(isDaemon.get());
    }

    // ======================== newFixedPool ========================

    @Test
    @DisplayName("newFixedPool creates a working fixed thread pool")
    void newFixedPool() throws InterruptedException {
        ExecutorService pool = ThreadUtil.newFixedPool(2, "fixed-");
        CountDownLatch latch = new CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            pool.submit(latch::countDown);
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(0, latch.getCount());
    }

    // ======================== newCachedPool ========================

    @Test
    @DisplayName("newCachedPool creates a working cached thread pool")
    void newCachedPool() throws InterruptedException {
        ExecutorService pool = ThreadUtil.newCachedPool("cached-");
        CountDownLatch latch = new CountDownLatch(4);

        for (int i = 0; i < 4; i++) {
            pool.submit(latch::countDown);
        }

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(0, latch.getCount());
    }

    // ======================== newSinglePool ========================

    @Test
    @DisplayName("newSinglePool executes tasks sequentially")
    void newSinglePool() throws InterruptedException {
        ExecutorService pool = ThreadUtil.newSinglePool("single-");
        AtomicReference<String> executingThread = new AtomicReference<>();

        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch block = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        // First task holds the thread
        pool.submit(() -> {
            executingThread.set(Thread.currentThread().getName());
            latch1.countDown();
            // Wait until released
            try {
                block.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            done.countDown();
        });

        latch1.await(5, TimeUnit.SECONDS);
        String firstThread = executingThread.get();

        // Second task should queue and then execute on same thread
        pool.submit(() -> {
            executingThread.set(Thread.currentThread().getName());
            done.countDown();
        });

        // Release first task
        block.countDown();

        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        // Both should run on the same single thread
        assertEquals(firstThread, executingThread.get());
    }

    // ======================== newScheduledPool ========================

    @Test
    @DisplayName("newScheduledPool creates a working scheduled pool")
    void newScheduledPool() throws InterruptedException {
        var pool = ThreadUtil.newScheduledPool("sched-");
        AtomicBoolean executed = new AtomicBoolean(false);

        pool.schedule(() -> executed.set(true), 50, TimeUnit.MILLISECONDS);

        Thread.sleep(200);
        pool.shutdown();
        assertTrue(executed.get(), "Scheduled task should have executed");
    }
}
