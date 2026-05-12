package com.zerx.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页请求参数
 * <p>
 * 封装前端传入的分页查询参数，包含页码、每页大小和排序字段。
 * 通过 compact constructor 对参数进行合法性校验，确保边界值安全。
 * </p>
 *
 * @param page   当前页码（从 1 开始，最小值 1）
 * @param size   每页大小（最小值 1，最大值 200）
 * @param orders 排序字段列表
 */
public record PageRequest(int page, int size, List<OrderItem> orders) implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;

    /** 默认每页大小 */
    public static final int DEFAULT_SIZE = 10;

    /** 每页最大记录数 */
    public static final int MAX_SIZE = 200;

    /**
     * 紧凑构造器，对分页参数进行边界值校验
     */
    public PageRequest {
        if (page < 1) {
            page = DEFAULT_PAGE;
        }
        if (size < 1) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        if (orders == null) {
            orders = new ArrayList<>();
        } else {
            orders = new ArrayList<>(orders); // 防御性拷贝，确保不可变
        }
    }

    /**
     * 便捷构造：仅指定页码和每页大小，不排序
     *
     * @param page 页码
     * @param size 每页大小
     */
    public PageRequest(int page, int size) {
        this(page, size, null);
    }

    /**
     * 便捷构造：使用默认分页参数（第 1 页，每页 10 条）
     */
    public PageRequest() {
        this(DEFAULT_PAGE, DEFAULT_SIZE, null);
    }

    /**
     * 计算当前页的偏移量（用于 SQL 的 OFFSET）
     *
     * @return 偏移量（从 0 开始）
     */
    public int offset() {
        return (page - 1) * size;
    }

    /**
     * 排序项，描述单个排序规则
     *
     * @param field     排序字段名
     * @param direction 排序方向（ASC / DESC）
     */
    public record OrderItem(String field, Direction direction) implements Serializable {

        /** 序列化版本号 */
        private static final long serialVersionUID = 1L;

        /**
         * 便捷构造：默认升序
         *
         * @param field 排序字段名
         */
        public OrderItem(String field) {
            this(field, Direction.ASC);
        }

        /**
         * 紧凑构造器，校验字段非空
         */
        public OrderItem {
            if (field == null || field.isBlank()) {
                throw new IllegalArgumentException("排序字段不能为空");
            }
            if (direction == null) {
                direction = Direction.ASC;
            }
        }
    }

    /**
     * 排序方向枚举
     */
    public enum Direction {
        /** 升序 */
        ASC,
        /** 降序 */
        DESC
    }
}
