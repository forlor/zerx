package com.zerx.common.enums;

import java.io.Serializable;

/**
 * 统一响应码枚举
 * <p>
 * 定义系统中所有 API 响应的状态码，采用分段编码规则：
 * <ul>
 *   <li>1xxxx — 系统级异常（网络、IO、运行时等）</li>
 *   <li>2xxxx — 业务逻辑异常（业务规则校验失败等）</li>
 *   <li>3xxxx — 参数校验异常（格式错误、必填缺失等）</li>
 *   <li>4xxxx — 权限相关异常（未认证、无权限等）</li>
 *   <li>5xxxx — 外部服务异常（第三方接口调用失败等）</li>
 * </ul>
 * </p>
 *
 * @param code       响应码
 * @param message    响应描述信息
 * @param httpStatus 对应的 HTTP 状态码
 */
public enum ResponseCode implements BaseEnum<String> {

    // ======================== 成功 ========================
    SUCCESS("00000", "操作成功", 200),

    // ======================== 系统级异常 (1xxxx) ========================
    SYSTEM_ERROR("10001", "系统内部错误", 500),
    NETWORK_ERROR("10002", "网络连接异常", 503),
    SERVICE_UNAVAILABLE("10003", "服务暂不可用", 503),
    TOO_MANY_REQUESTS("10004", "请求过于频繁", 429),

    // ======================== 业务逻辑异常 (2xxxx) ========================
    BUSINESS_ERROR("20001", "业务处理失败", 400),
    DATA_NOT_FOUND("20002", "数据不存在", 404),
    DATA_ALREADY_EXISTS("20003", "数据已存在", 409),
    OPERATION_FAILED("20004", "操作失败", 400),
    STATE_CONFLICT("20005", "当前状态不允许此操作", 409),

    // ======================== 参数校验异常 (3xxxx) ========================
    PARAM_REQUIRED("30001", "必填参数不能为空", 400),
    PARAM_FORMAT_ERROR("30002", "参数格式错误", 400),
    PARAM_OUT_OF_RANGE("30003", "参数值超出允许范围", 400),
    PARAM_TYPE_ERROR("30004", "参数类型错误", 400),

    // ======================== 权限相关异常 (4xxxx) ========================
    UNAUTHORIZED("40001", "未登录或认证已过期", 401),
    FORBIDDEN("40002", "无访问权限", 403),
    TOKEN_EXPIRED("40003", "认证令牌已过期", 401),
    TOKEN_INVALID("40004", "认证令牌无效", 401),

    // ======================== 外部服务异常 (5xxxx) ========================
    EXTERNAL_SERVICE_ERROR("50001", "外部服务调用失败", 502),
    EXTERNAL_SERVICE_TIMEOUT("50002", "外部服务调用超时", 504),
    EXTERNAL_SERVICE_RATE_LIMIT("50003", "外部服务触发限流", 429);

    /** 响应码 */
    private final String code;

    /** 响应描述信息 */
    private final String message;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    ResponseCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDescription() {
        return message;
    }
}
