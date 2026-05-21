package com.zerx.component.oss;

import com.zerx.common.exception.BusinessException;
import com.zerx.common.exception.ErrorCode;

/**
 * 对象存储服务异常
 * <p>
 * 用于对象存储（OSS）操作失败的场景，包括上传、下载、删除、预签名等操作。
 * 对应错误码段位：5xxxx（50000-59999），归属外部服务异常分类。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 使用静态工厂方法快速创建
 * throw OssException.ossError("上传文件到 OSS 失败: bucket 权限不足");
 *
 * // 使用预定义错误码
 * throw new OssException(ErrorCode.EXTERNAL_SERVICE_ERROR, "连接 OSS 超时");
 *
 * // 携带原始异常
 * try {
 *     minioClient.putObject(putObjectArgs);
 * } catch (Exception e) {
 *     throw new OssException(ErrorCode.EXTERNAL_SERVICE_ERROR, "文件上传失败", e);
 * }
 * }</pre>
 *
 * @author zerx
 * @see BusinessException
 * @see ErrorCode#EXTERNAL_SERVICE_ERROR
 */
public class OssException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用预定义错误码构造
     *
     * @param errorCode 错误码
     */
    public OssException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用预定义错误码和原始异常构造
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public OssException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 使用预定义错误码和自定义消息构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息（覆盖默认描述）
     */
    public OssException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 使用预定义错误码、自定义消息和原始异常构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     * @param cause     原始异常
     */
    public OssException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    /**
     * 使用自定义错误码构造
     * <p>
     * 当预定义错误码不满足需求时，可直接指定错误码、描述和 HTTP 状态码。
     * </p>
     *
     * @param code       错误码（如 "50010"）
     * @param message    错误描述
     * @param httpStatus HTTP 状态码（如 502）
     */
    public OssException(String code, String message, int httpStatus) {
        super(ErrorCode.of(code, message, httpStatus));
    }

    /**
     * 使用自定义错误码和原始异常构造
     *
     * @param code       错误码
     * @param message    错误描述
     * @param httpStatus HTTP 状态码
     * @param cause      原始异常
     */
    public OssException(String code, String message, int httpStatus, Throwable cause) {
        super(ErrorCode.of(code, message, httpStatus), message, cause);
    }

    /**
     * 使用自定义消息构造
     * <p>
     * 使用默认的 OSS 外部服务错误码（{@link ErrorCode#EXTERNAL_SERVICE_ERROR}），
     * 仅覆盖描述信息。
     * </p>
     *
     * @param message 自定义异常消息
     */
    public OssException(String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
    }

    /**
     * 创建带有 OSS 错误码和详细描述的异常
     * <p>
     * 使用错误码 {@code "50010"}（OSS 操作失败），HTTP 状态码 {@code 502}。
     * 适用于通用的 OSS 操作失败场景。
     * </p>
     *
     * @param detail 错误详情描述
     * @return 配置好的 {@link OssException} 实例
     */
    public static OssException ossError(String detail) {
        return new OssException("50010", "OSS 操作失败: " + detail, 502);
    }

    /**
     * 创建带有 OSS 错误码、详细描述和原始异常的异常
     * <p>
     * 使用错误码 {@code "50010"}（OSS 操作失败），HTTP 状态码 {@code 502}。
     * 适用于需要保留异常调用链的 OSS 操作失败场景。
     * </p>
     *
     * @param detail 错误详情描述
     * @param cause  原始异常
     * @return 配置好的 {@link OssException} 实例
     */
    public static OssException ossError(String detail, Throwable cause) {
        return new OssException("50010", "OSS 操作失败: " + detail, 502, cause);
    }
}
