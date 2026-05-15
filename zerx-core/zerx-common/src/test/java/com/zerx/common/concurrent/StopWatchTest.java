package com.zerx.common.concurrent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link StopWatch}
 */
class StopWatchTest {

    // ======================== Start / Stop ========================

    @Test
    @DisplayName("start() creates a running watch")
    void startCreatesRunningWatch() {
        StopWatch watch = StopWatch.start();
        assertTrue(watch.isRunning());
    }

    @Test
    @DisplayName("start(name) creates a running named watch")
    void startNamedWatch() {
        StopWatch watch = StopWatch.start("my-task");
        assertTrue(watch.isRunning());
        assertEquals("my-task", watch.getTaskName());
    }

    @Test
    @DisplayName("stop() stops the watch and returns it")
    void stopReturnsWatch() {
        StopWatch watch = StopWatch.start();
        StopWatch stopped = watch.stop();
        assertFalse(watch.isRunning());
        assertSame(watch, stopped);
    }

    // ======================== Timing ========================

    @Test
    @DisplayName("getTotalMillis returns non-negative after stop")
    void totalMillisAfterStop() throws InterruptedException {
        StopWatch watch = StopWatch.start();
        Thread.sleep(50);
        watch.stop();
        assertTrue(watch.getTotalMillis() >= 50);
    }

    @Test
    @DisplayName("getTotalSeconds returns non-negative after stop")
    void totalSecondsAfterStop() throws InterruptedException {
        StopWatch watch = StopWatch.start();
        Thread.sleep(50);
        watch.stop();
        double seconds = watch.getTotalSeconds();
        assertTrue(seconds >= 0.05);
    }

    @Test
    @DisplayName("getTotalNanos returns positive after stop")
    void totalNanosAfterStop() throws InterruptedException {
        StopWatch watch = StopWatch.start();
        Thread.sleep(50);
        watch.stop();
        assertTrue(watch.getTotalNanos() >= 50_000_000L);
    }

    @Test
    @DisplayName("getTotalMillis returns 0 immediately after start")
    void totalMillisImmediatelyAfterStart() {
        StopWatch watch = StopWatch.start();
        // The watch is running, but totalNano is still 0 until stop or split
        assertEquals(0, watch.getTotalMillis());
    }

    // ======================== Splits ========================

    @Test
    @DisplayName("split() records a segment and returns watch")
    void splitRecordsSegment() throws InterruptedException {
        StopWatch watch = StopWatch.start("test");
        Thread.sleep(30);
        StopWatch returned = watch.split("phase1");
        assertSame(watch, returned);
        assertTrue(watch.isRunning());
        assertEquals(1, watch.getSegmentCount());
    }

    @Test
    @DisplayName("multiple splits accumulate")
    void multipleSplits() throws InterruptedException {
        StopWatch watch = StopWatch.start("test");
        Thread.sleep(20);
        watch.split("phase1");
        Thread.sleep(20);
        watch.split("phase2");
        Thread.sleep(20);
        watch.split("phase3");
        watch.stop();

        assertEquals(3, watch.getSegmentCount());
        // Total time should be >= 60ms
        assertTrue(watch.getTotalMillis() >= 60, "Total should be >= 60ms but was " + watch.getTotalMillis());
    }

    // ======================== IllegalStateException ========================

    @Test
    @DisplayName("stop() on already stopped watch throws IllegalStateException")
    void stopWhenAlreadyStopped() {
        StopWatch watch = StopWatch.start();
        watch.stop();
        assertThrows(IllegalStateException.class, watch::stop);
    }

    @Test
    @DisplayName("split() on stopped watch throws IllegalStateException")
    void splitWhenStopped() {
        StopWatch watch = StopWatch.start();
        watch.stop();
        assertThrows(IllegalStateException.class, () -> watch.split("bad"));
    }

    // ======================== toString ========================

    @Test
    @DisplayName("toString includes task name and total time")
    void toStringFormat() throws InterruptedException {
        StopWatch watch = StopWatch.start("my-task");
        Thread.sleep(30);
        watch.split("phase1");
        watch.stop();

        String output = watch.toString();
        assertTrue(output.contains("my-task"), "Should contain task name");
        assertTrue(output.contains("总耗时"), "Should contain 总耗时");
        assertTrue(output.contains("phase1"), "Should contain segment name");
        assertTrue(output.contains("ms"), "Should contain ms unit");
    }

    @Test
    @DisplayName("toString without task name shows 'StopWatch:'")
    void toStringNoTaskName() throws InterruptedException {
        StopWatch watch = StopWatch.start();
        Thread.sleep(10);
        watch.stop();

        String output = watch.toString();
        assertTrue(output.startsWith("StopWatch:"));
    }

    @Test
    @DisplayName("toString shows segment percentages")
    void toStringSegmentsPercent() throws InterruptedException {
        StopWatch watch = StopWatch.start("test");
        Thread.sleep(30);
        watch.split("seg1");
        Thread.sleep(30);
        watch.split("seg2");
        watch.stop();

        String output = watch.toString();
        assertTrue(output.contains("%"), "Should contain percentage");
    }

    // ======================== shortSummary ========================

    @Test
    @DisplayName("shortSummary with name returns 'name: Xms'")
    void shortSummaryWithName() throws InterruptedException {
        StopWatch watch = StopWatch.start("task");
        Thread.sleep(10);
        watch.stop();

        String summary = watch.shortSummary();
        assertTrue(summary.startsWith("task:"));
        assertTrue(summary.endsWith("ms"));
    }

    @Test
    @DisplayName("shortSummary without name returns 'Xms'")
    void shortSummaryNoName() throws InterruptedException {
        StopWatch watch = StopWatch.start();
        Thread.sleep(10);
        watch.stop();

        String summary = watch.shortSummary();
        assertTrue(summary.endsWith("ms"));
        assertFalse(summary.contains(":"));
    }

    // ======================== Task name ========================

    @Test
    @DisplayName("getTaskName returns the name")
    void getTaskName() {
        StopWatch watch = StopWatch.start("test-name");
        assertEquals("test-name", watch.getTaskName());
    }

    @Test
    @DisplayName("anonymous watch has empty task name")
    void anonymousTaskName() {
        StopWatch watch = StopWatch.start();
        assertEquals("", watch.getTaskName());
    }

    // ======================== Segment count ========================

    @Test
    @DisplayName("getSegmentCount returns 0 before any split")
    void segmentCountZero() {
        StopWatch watch = StopWatch.start();
        assertEquals(0, watch.getSegmentCount());
    }

    // ======================== Chain calls ========================

    @Test
    @DisplayName("split and stop support chaining")
    void chaining() {
        StopWatch watch = StopWatch.start("chain-test")
                .split("s1")
                .split("s2")
                .stop();
        assertFalse(watch.isRunning());
        assertEquals(2, watch.getSegmentCount());
    }
}
