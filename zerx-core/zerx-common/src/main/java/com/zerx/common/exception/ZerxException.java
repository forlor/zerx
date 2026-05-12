package com.zerx.common.exception;

/**
 * Zerx 异常基类（抽象）
 * <p>
 * 所有 Zerx 体系内的自定义异常都应继承此基类。
 * 每个异常实例携带一个 {@link ErrorCode}，统一管理错误码、描述信息和 HTTP 状态码映射，
 * 便于全局异常处理器统一转换为标准响应格式。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>所有子异常均为 {@link RuntimeException}（非受检异常），避免强制 catch 的样板代码</li>
 *   <li>支持链式异常原因设置（cause），便于异常链追踪和问题排查</li>
 *   <li>提供便捷方法获取错误码、HTTP 状态码等属性</li>
 * </ul>
 *
 * <h3>继承体系：</h3>
 * <pre>
 * ZerxException (抽象基类)
 * ├── BusinessException      业务逻辑异常
 * ├── ValidationException    参数校验异常
 * ├── AuthorizationException 权限相关异常
 * ├── NotFoundException      资源未找到异常
 * └── ExternalServiceException 外部服务异常
 * </pre>
 *
 * @author zerx
 * @see ErrorCode
 */
public abstract class ZerxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final ErrorCode errorCode;

    /**
     * 通过错误码构造异常
     *
     * @param errorCode 错误码
     */
    protected ZerxException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 通过错误码和原始异常构造（支持异常链）
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    protected ZerxException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 通过错误码和自定义消息构造
     * <p>
     * 当需要覆盖 ErrorCode 的默认描述信息时使用。
     * </p>
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     */
    protected ZerxException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 通过错误码、自定义消息和原始异常构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     * @param cause     原始异常
     */
    protected ZerxException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 获取错误码
     *
     * @return ErrorCode 实例
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误码字符串
     *
     * @return 错误码（如 "20001"）
     */
    public String getCode() {
        return errorCode.code();
    }

    /**
     * 获取 HTTP 状态码
     *
     * @return HTTP 状态码（如 400、404、500）
     */
    public int getHttpStatus() {
        return errorCode.httpStatus();
    }
}
