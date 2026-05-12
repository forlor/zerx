package com.zerx.common.exception;

import java.io.Serializable;

/**
 * 错误码定义
 * <p>
 * 采用分段编码规则，统一管理系统中所有异常的编码、描述和 HTTP 状态码映射。
 * 与 {@link com.zerx.common.enums.ResponseCode} 不同，ErrorCode 主要用于异常场景，
 * 而 ResponseCode 用于 API 正常响应的状态码定义。
 * </p>
 *
 * <h3>分段编码规则：</h3>
 * <table>
 *   <tr><th>段位</th><th>范围</th><th>说明</th></tr>
 *   <tr><td>1xxxx</td><td>10000-19999</td><td>系统级异常（网络、IO、运行时等）</td></tr>
 *   <tr><td>2xxxx</td><td>20000-29999</td><td>业务逻辑异常（业务规则校验失败等）</td></tr>
 *   <tr><td>3xxxx</td><td>30000-39999</td><td>参数校验异常（格式错误、必填缺失等）</td></tr>
 *   <tr><td>4xxxx</td><td>40000-49999</td><td>权限相关异常（未认证、无权限等）</td></tr>
 *   <tr><td>5xxxx</td><td>50000-59999</td><td>外部服务异常（第三方接口调用失败等）</td></tr>
 * </table>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用预定义错误码
 * throw new BusinessException(ErrorCode.SYSTEM_ERROR);
 *
 * // 自定义错误码
 * throw new BusinessException(new ErrorCode("20010", "库存不足", 400));
 * }</pre>
 *
 * @param code       错误码（5 位数字字符串）
 * @param message    错误描述信息
 * @param httpStatus 对应的 HTTP 状态码
 * @author zerx
 */
public record ErrorCode(String code, String message, int httpStatus) implements Serializable {

    /** 序列化版本号 */
    private static final long serialVersionUID = 1L;

    // ======================== 系统级异常 (1xxxx) ========================

    /** 系统内部错误 */
    public static final ErrorCode SYSTEM_ERROR =
            new ErrorCode("10001", "系统内部错误", 500);

    /** 网络连接异常 */
    public static final ErrorCode NETWORK_ERROR =
            new ErrorCode("10002", "网络连接异常", 503);

    /** 服务暂不可用 */
    public static final ErrorCode SERVICE_UNAVAILABLE =
            new ErrorCode("10003", "服务暂不可用", 503);

    /** 请求过于频繁 */
    public static final ErrorCode TOO_MANY_REQUESTS =
            new ErrorCode("10004", "请求过于频繁", 429);

    /** 序列化/反序列化失败 */
    public static final ErrorCode SERIALIZATION_ERROR =
            new ErrorCode("10005", "数据序列化失败", 500);

    /** 数据库操作异常 */
    public static final ErrorCode DATABASE_ERROR =
            new ErrorCode("10006", "数据库操作异常", 500);

    // ======================== 业务逻辑异常 (2xxxx) ========================

    /** 通用业务异常 */
    public static final ErrorCode BUSINESS_ERROR =
            new ErrorCode("20001", "业务处理失败", 400);

    /** 数据不存在 */
    public static final ErrorCode DATA_NOT_FOUND =
            new ErrorCode("20002", "数据不存在", 404);

    /** 数据已存在 */
    public static final ErrorCode DATA_ALREADY_EXISTS =
            new ErrorCode("20003", "数据已存在", 409);

    /** 操作失败 */
    public static final ErrorCode OPERATION_FAILED =
            new ErrorCode("20004", "操作失败", 400);

    /** 状态冲突（当前状态不允许此操作） */
    public static final ErrorCode STATE_CONFLICT =
            new ErrorCode("20005", "当前状态不允许此操作", 409);

    /** 数据版本冲突（乐观锁） */
    public static final ErrorCode VERSION_CONFLICT =
            new ErrorCode("20006", "数据版本冲突，请刷新后重试", 409);

    /** 余额不足 */
    public static final ErrorCode BALANCE_NOT_ENOUGH =
            new ErrorCode("20007", "余额不足", 400);

    /** 库存不足 */
    public static final ErrorCode STOCK_NOT_ENOUGH =
            new ErrorCode("20008", "库存不足", 400);

    // ======================== 参数校验异常 (3xxxx) ========================

    /** 必填参数不能为空 */
    public static final ErrorCode PARAM_REQUIRED =
            new ErrorCode("30001", "必填参数不能为空", 400);

    /** 参数格式错误 */
    public static final ErrorCode PARAM_FORMAT_ERROR =
            new ErrorCode("30002", "参数格式错误", 400);

    /** 参数值超出允许范围 */
    public static final ErrorCode PARAM_OUT_OF_RANGE =
            new ErrorCode("30003", "参数值超出允许范围", 400);

    /** 参数类型错误 */
    public static final ErrorCode PARAM_TYPE_ERROR =
            new ErrorCode("30004", "参数类型错误", 400);

    /** 参数值非法 */
    public static final ErrorCode PARAM_INVALID =
            new ErrorCode("30005", "参数值不合法", 400);

    /** 请求体不能为空 */
    public static final ErrorCode BODY_REQUIRED =
            new ErrorCode("30006", "请求体不能为空", 400);

    // ======================== 权限相关异常 (4xxxx) ========================

    /** 未登录或认证已过期 */
    public static final ErrorCode UNAUTHORIZED =
            new ErrorCode("40001", "未登录或认证已过期", 401);

    /** 无访问权限 */
    public static final ErrorCode FORBIDDEN =
            new ErrorCode("40002", "无访问权限", 403);

    /** 认证令牌已过期 */
    public static final ErrorCode TOKEN_EXPIRED =
            new ErrorCode("40003", "认证令牌已过期", 401);

    /** 认证令牌无效 */
    public static final ErrorCode TOKEN_INVALID =
            new ErrorCode("40004", "认证令牌无效", 401);

    /** 账号已被禁用 */
    public static final ErrorCode ACCOUNT_DISABLED =
            new ErrorCode("40005", "账号已被禁用", 403);

    /** 登录失败次数过多 */
    public static final ErrorCode LOGIN_ATTEMPT_EXCEEDED =
            new ErrorCode("40006", "登录失败次数过多，请稍后再试", 429);

    // ======================== 外部服务异常 (5xxxx) ========================

    /** 外部服务调用失败 */
    public static final ErrorCode EXTERNAL_SERVICE_ERROR =
            new ErrorCode("50001", "外部服务调用失败", 502);

    /** 外部服务调用超时 */
    public static final ErrorCode EXTERNAL_SERVICE_TIMEOUT =
            new ErrorCode("50002", "外部服务调用超时", 504);

    /** 外部服务触发限流 */
    public static final ErrorCode EXTERNAL_SERVICE_RATE_LIMIT =
            new ErrorCode("50003", "外部服务触发限流", 429);

    /** 外部服务返回数据异常 */
    public static final ErrorCode EXTERNAL_SERVICE_DATA_ERROR =
            new ErrorCode("50004", "外部服务返回数据异常", 502);
}
