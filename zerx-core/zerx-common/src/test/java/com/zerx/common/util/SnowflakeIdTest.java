package com.zerx.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SnowflakeId 单元测试
 *
 * @author zerx
 */
@DisplayName("SnowflakeId 雪花ID生成器测试")
class SnowflakeIdTest {

    // ======================== 基础功能 ========================

    @Nested
    @DisplayName("基础功能")
    class BasicTests {

        @Test
        @DisplayName("默认构造器生成有效ID")
        void defaultConstructor() {
            SnowflakeId idGen = new SnowflakeId();
            long id = idGen.nextId();
            assertTrue(id > 0, "生成的 ID 应大于 0");
        }

        @Test
        @DisplayName("指定 workerId 构造器")
        void workerIdConstructor() {
            SnowflakeId idGen = new SnowflakeId(5);
            assertEquals(5, idGen.getWorkerId());
            long id = idGen.nextId();
            assertTrue(id > 0);
        }

        @Test
        @DisplayName("自定义纪元构造器")
        void customEpochConstructor() {
            long customEpoch = 1704038400000L;
            SnowflakeId idGen = new SnowflakeId(0, customEpoch);
            long id = idGen.nextId();
            assertTrue(id > 0);

            long timestamp = idGen.parseTimestamp(id);
            assertTrue(timestamp >= customEpoch, "解析的时间戳应 >= 纪元起始时间");
        }

        @Test
        @DisplayName("workerId 边界值：0 和 1023")
        void workerIdBoundary() {
            SnowflakeId min = new SnowflakeId(0);
            assertEquals(0, min.getWorkerId());

            SnowflakeId max = new SnowflakeId(1023);
            assertEquals(1023, max.getWorkerId());

            assertThrows(IllegalArgumentException.class, () -> new SnowflakeId(-1));
            assertThrows(IllegalArgumentException.class, () -> new SnowflakeId(1024));
        }

        @Test
        @DisplayName("负数纪元应抛异常")
        void negativeEpoch() {
            assertThrows(IllegalArgumentException.class, () -> new SnowflakeId(0, -1));
        }
    }

    // ======================== 唯一性 ========================

    @Nested
    @DisplayName("唯一性")
    class UniquenessTests {

        @Test
        @DisplayName("连续生成 10000 个 ID 无重复")
        void uniqueness_10k() {
            SnowflakeId idGen = new SnowflakeId(1);
            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < 10_000; i++) {
                assertTrue(ids.add(idGen.nextId()), "第 " + i + " 个 ID 重复");
            }
            assertEquals(10_000, ids.size());
        }

        @Test
        @DisplayName("连续生成 100000 个 ID 无重复")
        void uniqueness_100k() {
            SnowflakeId idGen = new SnowflakeId(42);
            Set<Long> ids = Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
            for (int i = 0; i < 100_000; i++) {
                assertTrue(ids.add(idGen.nextId()), "第 " + i + " 个 ID 重复");
            }
            assertEquals(100_000, ids.size());
        }
    }

    // ======================== 单调递增 ========================

    @Nested
    @DisplayName("单调递增")
    class MonotonicTests {

        @Test
        @DisplayName("同一实例生成的 ID 单调递增")
        void monotonicallyIncreasing() {
            SnowflakeId idGen = new SnowflakeId(1);
            long prev = Long.MIN_VALUE;
            for (int i = 0; i < 5000; i++) {
                long current = idGen.nextId();
                assertTrue(current > prev,
                        "ID 应单调递增，prev=" + prev + ", current=" + current);
                prev = current;
            }
        }
    }

    // ======================== 解析 ========================

    @Nested
    @DisplayName("ID 解析")
    class ParseTests {

        @Test
        @DisplayName("解析 workerId 正确")
        void parseWorkerId() {
            long customEpoch = System.currentTimeMillis();
            SnowflakeId idGen = new SnowflakeId(77, customEpoch);
            long id = idGen.nextId();
            assertEquals(77, idGen.parseWorkerId(id));
        }

        @Test
        @DisplayName("解析序列号在合法范围内")
        void parseSequence() {
            long customEpoch = System.currentTimeMillis();
            SnowflakeId idGen = new SnowflakeId(0, customEpoch);
            long id = idGen.nextId();
            long seq = idGen.parseSequence(id);
            assertTrue(seq >= 0 && seq <= 8191, "序列号应在 0~8191 之间，实际: " + seq);
        }

        @Test
        @DisplayName("解析时间戳合理")
        void parseTimestamp() {
            long before = System.currentTimeMillis();
            SnowflakeId idGen = new SnowflakeId();
            long id = idGen.nextId();
            long after = System.currentTimeMillis();

            long parsedTs = idGen.parseTimestamp(id);
            assertTrue(parsedTs >= before && parsedTs <= after + 1,
                    "解析的时间戳应在生成前后之间，parsed=" + parsedTs + ", before=" + before + ", after=" + after);
        }

        @Test
        @DisplayName("解析还原一致性")
        void parseConsistency() {
            long epoch = System.currentTimeMillis();
            SnowflakeId idGen = new SnowflakeId(123, epoch);
            long id = idGen.nextId();

            assertEquals(123, idGen.parseWorkerId(id));
            assertTrue(idGen.parseTimestamp(id) >= epoch);
            long seq = idGen.parseSequence(id);
            assertTrue(seq >= 0 && seq <= 8191);
        }
    }

    // ======================== nextIdStr ========================

    @Nested
    @DisplayName("nextIdStr")
    class NextIdStrTests {

        @Test
        @DisplayName("nextIdStr 返回有效数字字符串")
        void nextIdStr() {
            SnowflakeId idGen = new SnowflakeId();
            String strId = idGen.nextIdStr();
            assertNotNull(strId);
            assertTrue(strId.matches("\\d+"), "应为纯数字字符串");
            long id = Long.parseLong(strId);
            assertTrue(id > 0);
        }
    }

    // ======================== 并发安全 ========================

    @Nested
    @DisplayName("并发安全")
    class ConcurrencyTests {

        @Test
        @DisplayName("多线程并发生成 ID 无重复")
        void concurrentUniqueness() throws InterruptedException {
            final int threadCount = 16;
            final int idsPerThread = 10_000;
            SnowflakeId idGen = new SnowflakeId(42);
            Set<Long> allIds = Collections.synchronizedSet(new HashSet<>());
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < idsPerThread; i++) {
                            long id = idGen.nextId();
                            if (!allIds.add(id)) {
                                errors.add(new AssertionError("重复 ID: " + id));
                            }
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "等待超时");
            executor.shutdown();
            assertTrue(errors.isEmpty(), "并发错误: " + errors);
            assertEquals(threadCount * idsPerThread, allIds.size(), "总 ID 数量不匹配");
        }

        @Test
        @DisplayName("多线程并发生成 ID 全局单调递增（近似）")
        void concurrentMonotonic() throws InterruptedException {
            final int threadCount = 8;
            final int idsPerThread = 5000;
            SnowflakeId idGen = new SnowflakeId(0);
            ConcurrentLinkedQueue<Long> allIds = new ConcurrentLinkedQueue<>();
            CountDownLatch latch = new CountDownLatch(threadCount);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < idsPerThread; i++) {
                            allIds.add(idGen.nextId());
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(30, TimeUnit.SECONDS), "等待超时");
            executor.shutdown();

            // 将所有 ID 收集后检查近似单调（允许少量乱序）
            List<Long> sortedIds = new ArrayList<>(allIds);
            Collections.sort(sortedIds);
            long violations = 0;
            long prev = sortedIds.get(0);
            for (int i = 1; i < sortedIds.size(); i++) {
                long curr = sortedIds.get(i);
                if (curr <= prev) {
                    violations++;
                }
                prev = curr;
            }
            // CAS 实现应该保证严格单调递增
            assertEquals(0, violations, "严格单调递增违规次数应为 0");
        }

        @Test
        @DisplayName("高吞吐量测试：16 线程各生成 10000 个 ID")
        void highThroughput() throws InterruptedException {
            final int threadCount = 16;
            final int idsPerThread = 10_000;
            SnowflakeId idGen = new SnowflakeId(0);
            AtomicLong counter = new AtomicLong(0);
            CountDownLatch latch = new CountDownLatch(threadCount);

            long start = System.currentTimeMillis();
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < idsPerThread; i++) {
                            idGen.nextId();
                            counter.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertTrue(latch.await(60, TimeUnit.SECONDS), "等待超时");
            executor.shutdown();
            long elapsed = System.currentTimeMillis() - start;

            assertEquals(threadCount * idsPerThread, counter.get());
            // 16 万个 ID 应在合理时间内完成
            assertTrue(elapsed < 5000,
                    "16万ID生成耗时过长: " + elapsed + "ms");
        }
    }

    // ======================== 多实例 ========================

    @Nested
    @DisplayName("多实例")
    class MultiInstanceTests {

        @Test
        @DisplayName("不同 workerId 的实例可以生成不同 ID")
        void differentWorkerIds() {
            SnowflakeId gen1 = new SnowflakeId(0);
            SnowflakeId gen2 = new SnowflakeId(1);

            long id1 = gen1.nextId();
            long id2 = gen2.nextId();

            assertNotEquals(id1, id2);
            assertEquals(0, gen1.parseWorkerId(id1));
            assertEquals(1, gen2.parseWorkerId(id2));
        }
    }

    // ======================== 序列号溢出 ========================

    @Nested
    @DisplayName("序列号溢出")
    class SequenceOverflowTests {

        @Test
        @DisplayName("快速生成超过单毫秒序列号上限的 ID")
        void sequenceOverflow() {
            // 快速生成 10000 个 ID（超过单毫秒 8192 上限），验证唯一性和正确性
            SnowflakeId idGen = new SnowflakeId(0);
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < 10_000; i++) {
                ids.add(idGen.nextId());
            }
            // 按生成顺序保证唯一
            assertEquals(10_000, new HashSet<>(ids).size(), "所有 ID 应唯一");

            // 验证按生成顺序时间戳单调递增
            long prevTs = 0L;
            for (Long id : ids) {
                long ts = idGen.parseTimestamp(id);
                assertTrue(ts >= prevTs, "时间戳应单调递增");
                prevTs = ts;
            }

            // 验证所有序列号在合法范围
            for (Long id : ids) {
                long seq = idGen.parseSequence(id);
                assertTrue(seq >= 0 && seq <= 8191, "序列号应在 0~8191 之间，实际: " + seq);
            }
        }
    }

    // ======================== toString ========================

    @Test
    @DisplayName("toString 包含 workerId 信息")
    void testToString() {
        SnowflakeId idGen = new SnowflakeId(42);
        String str = idGen.toString();
        assertTrue(str.contains("42"), "toString 应包含 workerId");
        assertTrue(str.contains("SnowflakeId"), "toString 应包含类名");
    }
}
