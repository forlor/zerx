package com.zerx.common.model;

import java.io.Serializable;

import com.zerx.common.exception.ErrorCode;

/**
 * 统一响应结构体
 * <p>
 * 所有 API 接口的返回值统一包装为该类型，确保前端以一致的方式处理响应。
 * 支持泛型，可携带任意类型的业务数据。
 * </p>
 *
 * @param success 操作是否成功
 * @param code    响应码
 * @param message 响应描述信息
 * @param data    业务数据
 * @param <T>     业务数据类型
 */
public record Result<T>(boolean success, String code, String message, T data) implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    /**
     * 成功响应（携带数据）
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(true, ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data);
    }

    /**
     * 成功响应（无数据）
     *
     * @param <T> 业务数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 成功响应（自定义消息）
     *
     * @param message 自定义描述信息
     * @param data    业务数据
     * @param <T>     业务数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(true, ErrorCode.SUCCESS.code(), message, data);
    }

    /**
     * 失败响应（指定错误码）
     *
     * @param errorCode 错误码枚举
     * @param <T>       业务数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(false, errorCode.code(), errorCode.message(), null);
    }

    /**
     * 失败响应（自定义码和消息）
     *
     * @param code    响应码
     * @param message 响应描述信息
     * @param <T>     业务数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> fail(String code, String message) {
        return new Result<>(false, code, message, null);
    }

    /**
     * 失败响应（指定错误码和自定义消息）
     *
     * @param errorCode 错误码枚举
     * @param message   自定义描述信息
     * @param <T>       业务数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(false, errorCode.code(), message, null);
    }

    /**
     * 判断当前响应是否成功
     *
     * @return 成功返回 true，否则返回 false
     */
    public boolean isSuccess() {
        return success;
    }
}
