package com.zerx.common.concurrent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程工具类
 * <p>
 * 提供线程创建、线程池构建、虚拟线程启动等能力。
 * 充分利用 JDK 21 虚拟线程特性，简化并发编程。
 * </p>
 *
 * @author zerx
 */
public final class ThreadUtil {

    /** 默认虚拟线程名称前缀 */
    private static final String VIRTUAL_THREAD_PREFIX = "zerx-vt-";

    /** 默认调度线程池大小 */
    private static final int SCHEDULED_POOL_SIZE = 2;

    /** 私有构造器，防止实例化 */
    private ThreadUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ======================== 虚拟线程 ========================

    /**
     * 创建一个无名称的虚拟线程 Builder
     * <p>
     * JDK 21 虚拟线程是轻量级线程，由 JVM 调度，
     * 适用于 I/O 密集型任务和高并发场景。
     * </p>
     *
     * @return 虚拟线程 Builder
     */
    public static Thread.Builder.OfVirtual virtualThreadBuilder() {
        return Thread.ofVirtual();
    }

    /**
     * 创建指定名称的虚拟线程并启动
     *
     * @param name  线程名称
     * @param task  要执行的任务
     * @return 已启动的虚拟线程
     */
    public static Thread startVirtual(String name, Runnable task) {
        Objects.requireNonNull(task, "任务不能为 null");
        return Thread.ofVirtual().name(name).start(task);
    }

    /**
     * 创建指定名称前缀的虚拟线程并启动
     *
     * @param namePrefix 线程名称前缀
     * @param task       要执行的任务
     * @return 已启动的虚拟线程
     */
    public static Thread startVirtualWithPrefix(String namePrefix, Runnable task) {
        Objects.requireNonNull(task, "任务不能为 null");
        return Thread.ofVirtual().name(namePrefix).start(task);
    }

    /**
     * 创建无名称的虚拟线程并启动（默认前缀 zerx-vt-）
     *
     * @param task 要执行的任务
     * @return 已启动的虚拟线程
     */
    public static Thread startVirtual(Runnable task) {
        return startVirtualWithPrefix(VIRTUAL_THREAD_PREFIX, task);
    }

    /**
     * 创建指定名称前缀的虚拟线程 Factory
     * <p>
     * 线程名称格式为 "{prefix}{counter}"，counter 从 0 开始自动递增。
     * 适用于 ExecutorService 等需要 ThreadFactory 的场景。
     * </p>
     *
     * @param prefix 名称前缀
     * @return 虚拟线程工厂
     */
    public static ThreadFactory virtualThreadFactory(String prefix) {
        return Thread.ofVirtual().name(prefix, 0).factory();
    }

    /**
     * 创建默认命名的虚拟线程 Factory
     *
     * @return 虚拟线程工厂
     */
    public static ThreadFactory virtualThreadFactory() {
        return virtualThreadFactory(VIRTUAL_THREAD_PREFIX);
    }

    /**
     * 创建一个不限容量的虚拟线程执行器
     * <p>
     * 适用于大量 I/O 密集型任务的并发执行。
     * 每个任务会分配一个虚拟线程，不消耗平台线程资源。
     * </p>
     *
     * @return 虚拟线程执行器
     */
    public static ExecutorService newVirtualExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 创建指定名称前缀的虚拟线程执行器
     *
     * @param namePrefix 线程名称前缀
     * @return 虚拟线程执行器
     */
    public static ExecutorService newVirtualExecutor(String namePrefix) {
        return Executors.newThreadPerTaskExecutor(virtualThreadFactory(namePrefix));
    }

    // ======================== 平台线程 ========================

    /**
     * 创建指定名称前缀的平台线程 Factory
     * <p>
     * 线程名称格式为 "{prefix}{counter}"，counter 从 1 开始自动递增。
     * 创建的线程为非守护线程（daemon=false）。
     * </p>
     *
     * @param prefix 名称前缀
     * @return 平台线程工厂
     */
    public static ThreadFactory namedThreadFactory(String prefix) {
        return namedThreadFactory(prefix, false);
    }

    /**
     * 创建指定名称前缀和守护属性的平台线程 Factory
     *
     * @param prefix    名称前缀
     * @param isDaemon  是否为守护线程
     * @return 平台线程工厂
     */
    public static ThreadFactory namedThreadFactory(String prefix, boolean isDaemon) {
        return new NamedThreadFactory(prefix, isDaemon);
    }

    /**
     * 创建指定名称前缀、守护属性和优先级的平台线程 Factory
     *
     * @param prefix    名称前缀
     * @param isDaemon  是否为守护线程
     * @param priority  线程优先级（1-10）
     * @return 平台线程工厂
     */
    public static ThreadFactory namedThreadFactory(String prefix, boolean isDaemon, int priority) {
        return new NamedThreadFactory(prefix, isDaemon, priority);
    }

    // ======================== 线程池构建 ========================

    /**
     * 创建固定大小的线程池（带命名）
     * <p>
     * 核心线程数和最大线程数相同，使用无界队列。
     * 适用于 CPU 密集型任务或需要控制并发度的场景。
     * </p>
     *
     * @param nThreads 线程数量
     * @param namePrefix 线程名称前缀
     * @return 固定大小线程池
     */
    public static ExecutorService newFixedPool(int nThreads, String namePrefix) {
        return new ThreadPoolExecutor(
                nThreads, nThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                namedThreadFactory(namePrefix)
        );
    }

    /**
     * 创建可缓存的线程池（带命名）
     * <p>
     * 核心线程数为 0，最大线程数为 Integer.MAX_VALUE，
     * 空闲线程 60 秒后回收。适用于短时异步任务。
     * </p>
     *
     * @param namePrefix 线程名称前缀
     * @return 可缓存线程池
     */
    public static ExecutorService newCachedPool(String namePrefix) {
        return new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                namedThreadFactory(namePrefix)
        );
    }

    /**
     * 创建单线程执行器（带命名）
     * <p>
     * 保证任务按提交顺序串行执行。
     * </p>
     *
     * @param namePrefix 线程名称前缀
     * @return 单线程执行器
     */
    public static ExecutorService newSinglePool(String namePrefix) {
        return Executors.newSingleThreadExecutor(namedThreadFactory(namePrefix));
    }

    /**
     * 创建定时任务线程池（带命名）
     * <p>
     * 支持定时执行和周期执行任务。
     * 默认 2 个线程，适用于少量定时任务场景。
     * </p>
     *
     * @param namePrefix 线程名称前缀
     * @return 定时任务线程池
     */
    public static ScheduledExecutorService newScheduledPool(String namePrefix) {
        return Executors.newScheduledThreadPool(SCHEDULED_POOL_SIZE,
                namedThreadFactory(namePrefix));
    }

    /**
     * 创建指定大小的定时任务线程池（带命名）
     *
     * @param corePoolSize 核心线程数
     * @param namePrefix   线程名称前缀
     * @return 定时任务线程池
     */
    public static ScheduledExecutorService newScheduledPool(int corePoolSize, String namePrefix) {
        return Executors.newScheduledThreadPool(corePoolSize,
                namedThreadFactory(namePrefix));
    }

    // ======================== 线程休眠 ========================

    /**
     * 当前线程休眠指定毫秒数，不抛出受检异常
     *
     * @param millis 休眠毫秒数
     */
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 当前线程休眠指定时长，不抛出受检异常
     *
     * @param duration 休眠时长
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ======================== 并行处理 ========================

    /**
     * 获取当前 ForkJoinPool 的并行度
     * <p>
     * 即 JVM 可用的处理器核心数。
     * </p>
     *
     * @return 并行度
     */
    public static int parallelism() {
        return ForkJoinPool.commonPool().getParallelism();
    }

    /**
     * 获取当前平台可用的处理器核心数
     *
     * @return 处理器核心数
     */
    public static int availableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    // ======================== 内部实现 ========================

    /**
     * 命名线程工厂
     * <p>
     * 支持自定义前缀、守护属性和优先级。
     * 线程名称格式为 "{prefix}{counter}"，counter 从 1 开始递增。
     * </p>
     */
    private static class NamedThreadFactory implements ThreadFactory {

        private final ThreadFactory defaultFactory;
        private final String prefix;
        private final boolean isDaemon;
        private final int priority;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix, boolean isDaemon) {
            this(prefix, isDaemon, Thread.NORM_PRIORITY);
        }

        NamedThreadFactory(String prefix, boolean isDaemon, int priority) {
            this.defaultFactory = Thread.ofPlatform().factory();
            this.prefix = prefix;
            this.isDaemon = isDaemon;
            this.priority = Math.max(Thread.MIN_PRIORITY, Math.min(Thread.MAX_PRIORITY, priority));
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = defaultFactory.newThread(r);
            thread.setName(prefix + counter.getAndIncrement());
            thread.setDaemon(isDaemon);
            if (thread.getPriority() != priority) {
                thread.setPriority(priority);
            }
            return thread;
        }
    }
}
