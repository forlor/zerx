package com.zerx.spring.data.datascope;

import com.zerx.spring.data.datascope.DataScopeHandler.DataScopeSql;

/**
 * 数据权限线程上下文。
 * <p>
 * 基于 ThreadLocal 存储当前线程的数据权限 SQL 条件，
 * 由 {@link DataScopeInterceptor} 在方法执行前设置，
 * 方法执行后清理。
 * </p>
 *
 * <p>DynamicQuery 通过 {@link #current()} 获取条件并追加到 SQL 中。</p>
 *
 * @author zerx
 * @see DataScopeInterceptor
 * @see DataScopeHandler
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeSql> HOLDER = new ThreadLocal<>();

    private DataScopeContext() {
    }

    /**
     * 设置当前线程的数据权限 SQL 条件
     *
     * @param sqlCondition SQL 条件片段
     */
    public static void set(DataScopeSql sqlCondition) {
        HOLDER.set(sqlCondition);
    }

    /**
     * 获取当前线程的数据权限 SQL 条件
     *
     * @return SQL 条件片段，无权限条件时返回 {@code null}
     */
    public static DataScopeSql current() {
        return HOLDER.get();
    }

    /**
     * 清理当前线程的数据权限上下文。
     * <p>
     * 必须在 finally 块中调用，防止线程池复用导致的权限泄漏。
     * </p>
     */
    public static void clear() {
        HOLDER.remove();
    }
}
