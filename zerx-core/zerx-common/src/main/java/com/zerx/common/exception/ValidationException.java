package com.zerx.common.exception;

/**
 * 参数校验异常
 * <p>
 * 用于请求参数格式错误、必填参数缺失、参数值超出范围等场景。
 * 对应错误码段位：3xxxx（30000-39999）。
 * 通常由框架层（如 Spring MVC 参数绑定）或手动校验时抛出。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 手动校验参数
 * if (StringUtil.isBlank(name)) {
 *     throw new ValidationException(ErrorCode.PARAM_REQUIRED, "name");
 * }
 *
 * // 格式校验
 * if (!StringUtil.isMobile(phone)) {
 *     throw new ValidationException(ErrorCode.PARAM_FORMAT_ERROR, "手机号格式不正确");
 * }
 * }</pre>
 *
 * @author zerx
 * @see ErrorCode#PARAM_REQUIRED
 */
public class ValidationException extends ZerxException {

    private static final long serialVersionUID = 1L;

    /** 引起校验失败的字段名（可选） */
    private final String field;

    /**
     * 使用预定义错误码构造
     *
     * @param errorCode 错误码
     */
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
        this.field = null;
    }

    /**
     * 使用预定义错误码和自定义消息构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     */
    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.field = null;
    }

    /**
     * 使用预定义错误码和字段名构造
     * <p>
     * 字段名用于精确定位校验失败的具体参数，便于前端定位展示错误信息。
     * </p>
     *
     * @param errorCode 错误码
     * @param field     引起校验失败的字段名
     */
    public ValidationException(ErrorCode errorCode, String field) {
        super(errorCode, errorCode.message() + ": " + field);
        this.field = field;
    }

    /**
     * 使用预定义错误码、自定义消息和字段名构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     * @param field     引起校验失败的字段名
     */
    public ValidationException(ErrorCode errorCode, String message, String field) {
        super(errorCode, message);
        this.field = field;
    }

    /**
     * 获取引起校验失败的字段名
     *
     * @return 字段名，可能为 null
     */
    public String getField() {
        return field;
    }
}
