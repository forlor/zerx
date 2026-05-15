package com.zerx.common.exception;

/**
 * 外部服务异常
 * <p>
 * 用于调用第三方服务（支付网关、短信平台、外部 API 等）失败的场景。
 * 对应错误码段位：5xxxx（50000-59999）。
 * 支持记录外部服务的详细信息（服务名称、请求路径等），便于排查问题。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * try {
 *     paymentGateway.charge(amount);
 * } catch (PaymentException e) {
 *     throw new ExternalServiceException(
 *         ErrorCode.EXTERNAL_SERVICE_ERROR,
 *         "支付网关扣款失败",
 *         e
 *     );
 * }
 *
 * // 带服务名称
 * throw new ExternalServiceException(
 *     ErrorCode.EXTERNAL_SERVICE_TIMEOUT,
 *     "短信服务调用超时",
 *     "SMS"
 * );
 * }</pre>
 *
 * @author zerx
 * @see ErrorCode#EXTERNAL_SERVICE_ERROR
 */
public class ExternalServiceException extends ZerxException {

    private static final long serialVersionUID = 1L;

    /** 外部服务名称（可选，用于日志记录和问题排查） */
    private final String serviceName;

    /**
     * 使用预定义错误码构造
     *
     * @param errorCode 错误码
     */
    public ExternalServiceException(ErrorCode errorCode) {
        super(errorCode);
        this.serviceName = null;
    }

    /**
     * 使用预定义错误码和自定义消息构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     */
    public ExternalServiceException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.serviceName = null;
    }

    /**
     * 使用预定义错误码、自定义消息和原始异常构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     * @param cause     原始异常
     */
    public ExternalServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
        this.serviceName = null;
    }

    /**
     * 使用预定义错误码、自定义消息和服务名称构造
     *
     * @param errorCode   错误码
     * @param message     自定义异常消息
     * @param serviceName 外部服务名称（如 "SMS"、"PaymentGateway"）
     */
    public ExternalServiceException(ErrorCode errorCode, String message, String serviceName) {
        super(errorCode, message);
        this.serviceName = serviceName;
    }

    /**
     * 使用预定义错误码、自定义消息、服务名称和原始异常构造
     *
     * @param errorCode   错误码
     * @param message     自定义异常消息
     * @param cause       原始异常
     * @param serviceName 外部服务名称
     */
    public ExternalServiceException(ErrorCode errorCode, String message, Throwable cause, String serviceName) {
        super(errorCode, message, cause);
        this.serviceName = serviceName;
    }

    /**
     * 获取外部服务名称
     *
     * @return 服务名称，可能为 null
     */
    public String getServiceName() {
        return serviceName;
    }
}
