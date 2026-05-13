package com.zerx.common.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 简易计时器
 * <p>
 * 用于测量代码执行耗时，支持分段计时和多任务汇总。
 * 典型用法：
 * </p>
 * <pre>{@code
 * StopWatch watch = StopWatch.start("查询任务");
 * // ... 业务逻辑 ...
 * watch.split("数据查询");
 * // ... 更多逻辑 ...
 * watch.stop();
 * System.out.println(watch); // 输出分段耗时和总耗时
 * }</pre>
 *
 * @author zerx
 */
public final class StopWatch {

    /** 任务名称 */
    private final String taskName;

    /** 分段记录列表 */
    private final List<Segment> segments;

    /** 计时器状态 */
    private State state;

    /** 最近一次 start/split 的时间戳（纳秒） */
    private long startNano;

    /** 总耗时（纳秒） */
    private long totalNano;

    /**
     * 计时器状态
     */
    private enum State {
        /** 未开始 */
        IDLE,
        /** 计时中 */
        RUNNING,
        /** 已停止 */
        STOPPED
    }

    /**
     * 分段记录
     *
     * @param name  分段名称
     * @param nano  耗时（纳秒）
     */
    private record Segment(String name, long nano) {
    }

    // ======================== 构造与启动 ========================

    /**
     * 创建命名计时器并立即启动
     *
     * @param taskName 任务名称
     * @return 已启动的计时器
     */
    public static StopWatch start(String taskName) {
        StopWatch watch = new StopWatch(taskName);
        watch.start0();
        return watch;
    }

    /**
     * 创建匿名计时器并立即启动
     *
     * @return 已启动的计时器
     */
    public static StopWatch start() {
        return start("");
    }

    /**
     * 创建命名计时器（未启动状态）
     *
     * @param taskName 任务名称
     */
    private StopWatch(String taskName) {
        this.taskName = taskName;
        this.segments = new ArrayList<>();
        this.state = State.IDLE;
        this.totalNano = 0;
    }

    private void start0() {
        this.startNano = System.nanoTime();
        this.state = State.RUNNING;
    }

    // ======================== 分段与停止 ========================

    /**
     * 记录一个分段耗时并重新开始计时
     * <p>
     * 用于将整个任务拆分为多个阶段分别计时。
     * 必须在计时中状态下调用。
     * </p>
     *
     * @param name 分段名称
     * @return 当前计时器（支持链式调用）
     * @throws IllegalStateException 计时器未运行时抛出
     */
    public StopWatch split(String name) {
        ensureRunning();
        long now = System.nanoTime();
        long elapsed = now - startNano;
        segments.add(new Segment(name, elapsed));
        totalNano += elapsed;
        startNano = now;
        return this;
    }

    /**
     * 停止计时
     *
     * @return 当前计时器（支持链式调用）
     * @throws IllegalStateException 计时器未运行时抛出
     */
    public StopWatch stop() {
        ensureRunning();
        long now = System.nanoTime();
        totalNano += now - startNano;
        state = State.STOPPED;
        return this;
    }

    // ======================== 耗时获取 ========================

    /**
     * 获取总耗时（毫秒）
     *
     * @return 总耗时毫秒数
     */
    public long getTotalMillis() {
        return TimeUnit.NANOSECONDS.toMillis(totalNano);
    }

    /**
     * 获取总耗时（秒，保留 3 位小数）
     *
     * @return 总耗时秒数
     */
    public double getTotalSeconds() {
        return totalNano / 1_000_000_000.0;
    }

    /**
     * 获取总耗时（纳秒）
     *
     * @return 总耗时纳秒数
     */
    public long getTotalNanos() {
        return totalNano;
    }

    /**
     * 获取分段数量
     *
     * @return 分段数
     */
    public int getSegmentCount() {
        return segments.size();
    }

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 判断计时器是否正在运行
     *
     * @return 正在运行返回 true
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    // ======================== 输出 ========================

    /**
     * 格式化输出计时结果
     * <p>
     * 输出格式：
     * </p>
     * <pre>
     * StopWatch '任务名称': 总耗时 1234ms
     *   - 分段1: 500ms (40.5%)
     *   - 分段2: 734ms (59.5%)
     * </pre>
     *
     * @return 格式化字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!taskName.isEmpty()) {
            sb.append("StopWatch '").append(taskName).append("': ");
        } else {
            sb.append("StopWatch: ");
        }
        sb.append("总耗时 ").append(getTotalMillis()).append("ms");

        if (!segments.isEmpty()) {
            sb.append('\n');
            for (int i = 0; i < segments.size(); i++) {
                Segment seg = segments.get(i);
                long millis = TimeUnit.NANOSECONDS.toMillis(seg.nano());
                double percent = totalNano > 0 ? (seg.nano() * 100.0 / totalNano) : 0;
                sb.append("  - ").append(seg.name())
                        .append(": ").append(millis).append("ms")
                        .append(String.format(" (%.1f%%)", percent));
                if (i < segments.size() - 1) {
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    /**
     * 简短格式：仅输出总耗时毫秒数
     *
     * @return 简短字符串
     */
    public String shortSummary() {
        if (taskName.isEmpty()) {
            return getTotalMillis() + "ms";
        }
        return taskName + ": " + getTotalMillis() + "ms";
    }

    // ======================== 内部方法 ========================

    private void ensureRunning() {
        if (state != State.RUNNING) {
            throw new IllegalStateException("计时器未运行，当前状态: " + state);
        }
    }
}
