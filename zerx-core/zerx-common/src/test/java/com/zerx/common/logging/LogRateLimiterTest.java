package com.zerx.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LogRateLimiter 单元测试
 */
@DisplayName("LogRateLimiter")
class LogRateLimiterTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryTest {

        @Test
        @DisplayName("of - 正常创建")
        void of() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            assertEquals(10, limiter.getMaxPerWindow());
            assertEquals(Duration.ofSeconds(1), limiter.getWindowDuration());
        }

        @Test
        @DisplayName("of - maxPerWindow <= 0 应抛异常")
        void invalidMaxPerWindow() {
            assertThrows(IllegalArgumentException.class,
                    () -> LogRateLimiter.of(0, Duration.ofSeconds(1)));
            assertThrows(IllegalArgumentException.class,
                    () -> LogRateLimiter.of(-1, Duration.ofSeconds(1)));
        }

        @Test
        @DisplayName("of - windowDuration 为 null 应抛异常")
        void nullDuration() {
            assertThrows(NullPointerException.class,
                    () -> LogRateLimiter.of(10, null));
        }

        @Test
        @DisplayName("of - windowDuration 为零或负应抛异常")
        void invalidDuration() {
            assertThrows(IllegalArgumentException.class,
                    () -> LogRateLimiter.of(10, Duration.ZERO));
            assertThrows(IllegalArgumentException.class,
                    () -> LogRateLimiter.of(10, Duration.ofSeconds(-1)));
        }

        @Test
        @DisplayName("defaultLimiter - 每秒 10 条")
        void defaultLimiter() {
            LogRateLimiter limiter = LogRateLimiter.defaultLimiter();
            assertEquals(10, limiter.getMaxPerWindow());
            assertEquals(Duration.ofSeconds(1), limiter.getWindowDuration());
        }
    }

    @Nested
    @DisplayName("tryAcquire - 限流判断")
    class TryAcquireTest {

        @Test
        @DisplayName("key 为 null 应抛异常")
        void nullKey() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            assertThrows(NullPointerException.class, () -> limiter.tryAcquire(null));
        }

        @Test
        @DisplayName("未超阈值时应全部通过")
        void withinThreshold() {
            LogRateLimiter limiter = LogRateLimiter.of(5, Duration.ofMinutes(1));
            for (int i = 0; i < 5; i++) {
                assertTrue(limiter.tryAcquire("testKey"), "第 " + (i + 1) + " 次应通过");
            }
        }

        @Test
        @DisplayName("超出阈值后应被抑制")
        void beyondThreshold() {
            LogRateLimiter limiter = LogRateLimiter.of(3, Duration.ofMinutes(1));
            assertTrue(limiter.tryAcquire("key"));
            assertTrue(limiter.tryAcquire("key"));
            assertTrue(limiter.tryAcquire("key"));
            assertFalse(limiter.tryAcquire("key"), "第 4 次应被抑制");
            assertFalse(limiter.tryAcquire("key"), "第 5 次应被抑制");
        }

        @Test
        @DisplayName("不同 key 独立计数")
        void differentKeys() {
            LogRateLimiter limiter = LogRateLimiter.of(1, Duration.ofMinutes(1));
            assertTrue(limiter.tryAcquire("keyA"));
            assertFalse(limiter.tryAcquire("keyA"), "keyA 超阈值应被抑制");
            assertTrue(limiter.tryAcquire("keyB"), "keyB 应独立计数不受影响");
        }

        @Test
        @DisplayName("maxPerWindow=1 时只有第一次通过")
        void singlePerWindow() {
            LogRateLimiter limiter = LogRateLimiter.of(1, Duration.ofMinutes(1));
            assertTrue(limiter.tryAcquire("onlyOne"));
            assertFalse(limiter.tryAcquire("onlyOne"));
            assertFalse(limiter.tryAcquire("onlyOne"));
        }
    }

    @Nested
    @DisplayName("getSummary - 抑制摘要")
    class GetSummaryTest {

        @Test
        @DisplayName("无抑制消息时应返回 null")
        void noSuppressed() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofMinutes(1));
            limiter.tryAcquire("key");
            assertNull(limiter.getSummary("key"), "无抑制消息应返回 null");
        }

        @Test
        @DisplayName("有抑制消息时应返回摘要字符串")
        void withSuppressed() {
            LogRateLimiter limiter = LogRateLimiter.of(2, Duration.ofMinutes(1));
            limiter.tryAcquire("key");
            limiter.tryAcquire("key");
            limiter.tryAcquire("key"); // 被抑制
            limiter.tryAcquire("key"); // 被抑制

            String summary = limiter.getSummary("key");
            assertNotNull(summary, "应有摘要");
            assertTrue(summary.contains("'key'"), "应包含 key 名称");
            assertTrue(summary.contains("suppressed 2"), "应包含抑制数量");
            assertTrue(summary.contains("60000ms"), "应包含窗口时间");
        }

        @Test
        @DisplayName("不存在的 key 应返回 null")
        void nonExistentKey() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            assertNull(limiter.getSummary("nonExistent"));
        }

        @Test
        @DisplayName("key 为 null 应抛异常")
        void nullKey() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            assertThrows(NullPointerException.class, () -> limiter.getSummary(null));
        }
    }

    @Nested
    @DisplayName("getAllSummaries - 全部摘要")
    class GetAllSummariesTest {

        @Test
        @DisplayName("多个 key 各自独立摘要")
        void multipleKeys() {
            LogRateLimiter limiter = LogRateLimiter.of(1, Duration.ofMinutes(1));
            limiter.tryAcquire("A");
            limiter.tryAcquire("A"); // 抑制 1
            limiter.tryAcquire("B");
            limiter.tryAcquire("B"); // 抑制 1
            limiter.tryAcquire("C"); // 无抑制

            Map<String, String> summaries = limiter.getAllSummaries();
            assertEquals(2, summaries.size(), "只有 A 和 B 有抑制摘要");
            assertTrue(summaries.containsKey("A"));
            assertTrue(summaries.containsKey("B"));
            assertFalse(summaries.containsKey("C"));
        }

        @Test
        @DisplayName("全部通过时摘要应为空")
        void noSuppressions() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofMinutes(1));
            limiter.tryAcquire("key");
            Map<String, String> summaries = limiter.getAllSummaries();
            // key 没有被抑制，不应出现在摘要中
            assertFalse(summaries.containsKey("key"));
        }
    }

    @Nested
    @DisplayName("reset - 重置")
    class ResetTest {

        @Test
        @DisplayName("重置指定 key 后应重新计数")
        void resetKey() {
            LogRateLimiter limiter = LogRateLimiter.of(2, Duration.ofMinutes(1));
            limiter.tryAcquire("key");
            limiter.tryAcquire("key");
            assertFalse(limiter.tryAcquire("key"));

            limiter.reset("key");

            assertTrue(limiter.tryAcquire("key"), "重置后应重新计数");
            assertTrue(limiter.tryAcquire("key"));
            assertFalse(limiter.tryAcquire("key"));
        }

        @Test
        @DisplayName("resetAll 后所有 key 应重新计数")
        void resetAll() {
            LogRateLimiter limiter = LogRateLimiter.of(1, Duration.ofMinutes(1));
            limiter.tryAcquire("A");
            limiter.tryAcquire("B");
            assertFalse(limiter.tryAcquire("A"));
            assertFalse(limiter.tryAcquire("B"));

            limiter.resetAll();

            assertTrue(limiter.tryAcquire("A"));
            assertTrue(limiter.tryAcquire("B"));
        }
    }

    @Nested
    @DisplayName("size 和计数器")
    class SizeAndCountersTest {

        @Test
        @DisplayName("新创建的限流器 size 为 0")
        void initialSize() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            assertEquals(0, limiter.size());
        }

        @Test
        @DisplayName("使用后 size 应增加")
        void sizeAfterUse() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            limiter.tryAcquire("A");
            limiter.tryAcquire("B");
            assertEquals(2, limiter.size());
        }

        @Test
        @DisplayName("getPassedCount 应正确计数")
        void passedCount() {
            LogRateLimiter limiter = LogRateLimiter.of(5, Duration.ofMinutes(1));
            limiter.tryAcquire("key");
            limiter.tryAcquire("key");
            limiter.tryAcquire("key");
            assertEquals(3, limiter.getPassedCount("key"));
        }

        @Test
        @DisplayName("getSuppressedCount 应正确计数")
        void suppressedCount() {
            LogRateLimiter limiter = LogRateLimiter.of(2, Duration.ofMinutes(1));
            limiter.tryAcquire("key");
            limiter.tryAcquire("key");
            limiter.tryAcquire("key"); // 抑制 1
            limiter.tryAcquire("key"); // 抑制 2
            limiter.tryAcquire("key"); // 抑制 3
            assertEquals(3, limiter.getSuppressedCount("key"));
        }

        @Test
        @DisplayName("不存在的 key 计数器为 0")
        void nonExistentCount() {
            LogRateLimiter limiter = LogRateLimiter.of(10, Duration.ofSeconds(1));
            assertEquals(0, limiter.getPassedCount("noKey"));
            assertEquals(0, limiter.getSuppressedCount("noKey"));
        }
    }

    @Nested
    @DisplayName("并发安全")
    class ConcurrencyTest {

        @Test
        @DisplayName("多线程同时 tryAcquire 不应产生异常")
        void concurrentTryAcquire() throws Exception {
            LogRateLimiter limiter = LogRateLimiter.of(100, Duration.ofMinutes(1));
            int threadCount = 10;
            int iterations = 100;
            Thread[] threads = new Thread[threadCount];

            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < iterations; j++) {
                        limiter.tryAcquire("concurrentKey");
                    }
                });
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }

            // 总共 1000 次，但限额 100，通过数应 <= 100
            assertTrue(limiter.getPassedCount("concurrentKey") <= 100,
                    "通过数不应超过限额");
            assertEquals(threadCount * iterations - limiter.getPassedCount("concurrentKey"),
                    limiter.getSuppressedCount("concurrentKey"),
                    "通过数 + 抑制数 = 总次数");
        }
    }

    @Nested
    @DisplayName("窗口过期")
    class WindowExpiryTest {

        @Test
        @DisplayName("窗口过期后应重新计数")
        void windowExpires() throws InterruptedException {
            // 使用极短的窗口（50ms）确保测试快速完成
            LogRateLimiter limiter = LogRateLimiter.of(1, Duration.ofMillis(50));
            assertTrue(limiter.tryAcquire("key"));
            assertFalse(limiter.tryAcquire("key"));

            // 等待窗口过期
            Thread.sleep(100);

            assertTrue(limiter.tryAcquire("key"), "窗口过期后应重新计数");
        }
    }
}
