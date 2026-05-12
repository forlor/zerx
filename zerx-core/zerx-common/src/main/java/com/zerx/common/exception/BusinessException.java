package com.zerx.common.exception;

/**
 * 业务逻辑异常
 * <p>
 * 用于业务规则校验失败、业务流程中断等场景。
 * 对应错误码段位：2xxxx（20000-29999）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用预定义错误码
 * if (order.getStatus() == Status.CANCELLED) {
 *     throw new BusinessException(ErrorCode.STATE_CONFLICT);
 * }
 *
 * // 自定义错误码
 * throw new BusinessException(new ErrorCode("20010", "优惠券已过期", 400));
 *
 * // 自定义消息（覆盖 ErrorCode 默认描述）
 * throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH, "账户余额不足，当前余额: 100");
 *
 * // 携带原始异常
 * try {
 *     processPayment();
 * } catch (PaymentException e) {
 *     throw new BusinessException(ErrorCode.OPERATION_FAILED, "支付处理失败", e);
 * }
 * }</pre>
 *
 * @author zerx
 * @see ErrorCode#BUSINESS_ERROR
 */
public class BusinessException extends ZerxException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用预定义错误码构造
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用预定义错误码和原始异常构造
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 使用预定义错误码和自定义消息构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息（覆盖默认描述）
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 使用预定义错误码、自定义消息和原始异常构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     * @param cause     原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
