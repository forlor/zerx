package com.zerx.common.exception;

/**
 * 权限相关异常
 * <p>
 * 用于未认证、无权限、Token 失效等认证授权相关场景。
 * 对应错误码段位：4xxxx（40000-49999）。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 未登录
 * if (currentUser == null) {
 *     throw new AuthorizationException(ErrorCode.UNAUTHORIZED);
 * }
 *
 * // 无权限访问某资源
 * if (!currentUser.hasRole("ADMIN")) {
 *     throw new AuthorizationException(ErrorCode.FORBIDDEN, "需要管理员权限");
 * }
 *
 * // Token 过期
 * if (token.isExpired()) {
 *     throw new AuthorizationException(ErrorCode.TOKEN_EXPIRED);
 * }
 * }</pre>
 *
 * @author zerx
 * @see ErrorCode#UNAUTHORIZED
 * @see ErrorCode#FORBIDDEN
 */
public class AuthorizationException extends ZerxException {

    private static final long serialVersionUID = 1L;

    /**
     * 使用预定义错误码构造
     *
     * @param errorCode 错误码
     */
    public AuthorizationException(ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 使用预定义错误码和原始异常构造
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public AuthorizationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 使用预定义错误码和自定义消息构造
     *
     * @param errorCode 错误码
     * @param message   自定义异常消息
     */
    public AuthorizationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
