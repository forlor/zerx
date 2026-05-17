package com.zerx.spring.web.advise;

import com.zerx.common.exception.AuthorizationException;
import com.zerx.common.exception.BusinessException;
import com.zerx.common.exception.ErrorCode;
import com.zerx.common.exception.ExternalServiceException;
import com.zerx.common.exception.NotFoundException;
import com.zerx.common.exception.ValidationException;
import com.zerx.common.model.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器 — 统一异常响应格式
 * <p>
 * 拦截 Controller 层抛出的各类异常，统一转换为 {@link Result} 格式返回给前端，
 * 并设置合适的 HTTP 状态码。
 * </p>
 *
 * <h3>异常处理优先级：</h3>
 * <ol>
 *   <li>{@link BusinessException} — 业务逻辑异常 → HTTP 400</li>
 *   <li>{@link ValidationException} — 参数校验异常 → HTTP 400</li>
 *   <li>{@link AuthorizationException} — 权限相关异常 → HTTP 401</li>
 *   <li>{@link NotFoundException} — 资源未找到 → HTTP 404</li>
 *   <li>{@link ExternalServiceException} — 外部服务异常 → HTTP 502</li>
 *   <li>{@link MethodArgumentNotValidException} — Bean 校验异常 → HTTP 400</li>
 *   <li>{@link ConstraintViolationException} — 约束违反异常 → HTTP 400</li>
 *   <li>{@link HttpMessageNotReadableException} — 请求体不可读 → HTTP 400</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} — 请求方法不支持 → HTTP 405</li>
 *   <li>{@link NoHandlerFoundException} — 无处理器 → HTTP 404</li>
 *   <li>{@link Exception} — 未知异常 → HTTP 500</li>
 * </ol>
 *
 * @author zerx
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务逻辑异常
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param ex 校验异常
     * @return 失败响应
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(ValidationException ex) {
        log.warn("Validation exception: code={}, field={}, message={}",
                ex.getCode(), ex.getField(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理权限相关异常
     *
     * @param ex 权限异常
     * @return 失败响应
     */
    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthorizationException(AuthorizationException ex) {
        log.warn("Authorization exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理资源未找到异常
     *
     * @param ex 资源未找到异常
     * @return 失败响应
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFoundException(NotFoundException ex) {
        log.warn("Not found exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理外部服务异常
     *
     * @param ex 外部服务异常
     * @return 失败响应
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleExternalServiceException(ExternalServiceException ex) {
        log.error("External service exception: code={}, message={}, service={}",
                ex.getCode(), ex.getMessage(), ex.getServiceName(), ex);
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理 Bean Validation 校验异常（{@code @Valid} 触发）
     *
     * @param ex 方法参数校验异常
     * @return 失败响应，包含所有字段错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Method argument not valid: {}", detail);
        return Result.fail(ErrorCode.PARAM_INVALID.code(),
                "参数校验失败: " + detail);
    }

    /**
     * 处理约束违反异常（JAX-RS 风格的 {@code @Validated} 触发）
     *
     * @param ex 约束违反异常
     * @return 失败响应，包含所有违规信息
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("Constraint violation: {}", detail);
        return Result.fail(ErrorCode.PARAM_INVALID.code(),
                "参数约束违反: " + detail);
    }

    /**
     * 处理请求体不可读异常（JSON 格式错误等）
     *
     * @param ex 请求体不可读异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Http message not readable: {}", ex.getMessage());
        return Result.fail(ErrorCode.BODY_REQUIRED);
    }

    /**
     * 处理 HTTP 请求方法不支持异常
     *
     * @param ex 请求方法不支持异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return Result.fail(ErrorCode.PARAM_FORMAT_ERROR.code(),
                "不支持的请求方法: " + ex.getMethod());
    }

    /**
     * 处理无处理器异常（404）
     *
     * @param ex 无处理器异常
     * @return 失败响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("No handler found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return Result.fail(ErrorCode.DATA_NOT_FOUND);
    }

    /**
     * 兜底处理所有未捕获的异常
     *
     * @param ex 未知异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}
