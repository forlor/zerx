package com.zerx.common.exception;

/**
 * 资源未找到异常
 * <p>
 * 用于查询指定资源但资源不存在等场景。
 * 对应 HTTP 状态码 404。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * User user = userRepository.findById(userId);
 * if (user == null) {
 *     throw new NotFoundException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
 * }
 *
 * // 携带资源标识信息
 * throw new NotFoundException(ErrorCode.DATA_NOT_FOUND, "订单不存在: " + orderId);
 * }</pre>
 *
 * @author zerx
 * @see ErrorCode#DATA_NOT_FOUND
 */
public class NotFoundException extends ZerxException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用预定义错误码构造
     *
     * @param errorCode 错误码
     */
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用预定义错误码和自定义消息构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     */
    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 使用自定义资源消息构造（默认使用 DATA_NOT_FOUND 错误码）
     *
     * @param message 资源描述信息
     */
    public NotFoundException(String message) {
        super(ErrorCode.DATA_NOT_FOUND, message);
    }
}
