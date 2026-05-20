package com.zerx.common.model;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应结果
 * <p>
 * 封装分页查询的返回数据，包含当前页数据列表、总记录数以及分页元信息。
 * 便于前端渲染分页组件和展示数据统计信息。
 * </p>
 *
 * @param records    当前页的数据列表
 * @param total      总记录数
 * @param page       当前页码
 * @param size       每页大小
 * @param totalPages 总页数
 * @param <T>        数据记录类型
 */
public record PageResult<T>(List<T> records, long total, int page, int size, int totalPages)
        implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 紧凑构造器，确保存储不可变集合以防内部表示暴露
     */
    public PageResult {
        records = records != null ? List.copyOf(records) : List.of();
    }

    /**
     * 便捷构造：自动计算总页数
     *
     * @param records 当前页数据列表
     * @param total   总记录数
     * @param page    当前页码
     * @param size    每页大小
     * @param <T>     数据记录类型
     * @return 分页响应结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(records, total, page, size, totalPages);
    }

    /**
     * 基于 PageRequest 构建空结果
     *
     * @param pageRequest 分页请求参数
     * @param <T>        数据记录类型
     * @return 空的分页响应结果
     */
    public static <T> PageResult<T> empty(PageRequest pageRequest) {
        return new PageResult<>(List.of(), 0, pageRequest.page(), pageRequest.size(), 0);
    }

    /**
     * 判断是否为空结果
     *
     * @return 记录为空返回 true
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

    /**
     * 判断是否有下一页
     *
     * @return 有下一页返回 true
     */
    public boolean hasNext() {
        return page < totalPages;
    }

    /**
     * 判断是否有上一页
     *
     * @return 有上一页返回 true
     */
    public boolean hasPrevious() {
        return page > 1;
    }
}
