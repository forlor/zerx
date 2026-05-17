package com.zerx.spring.web;

import com.zerx.common.exception.*;
import com.zerx.common.model.Result;
import com.zerx.spring.web.advise.GlobalExceptionHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 全局异常处理器测试
 *
 * @author zerx
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_shouldReturnFailResult() {
        BusinessException ex = new BusinessException(ErrorCode.BUSINESS_ERROR);
        Result<Void> result = handler.handleBusinessException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.BUSINESS_ERROR.code(), result.code());
        assertEquals(ErrorCode.BUSINESS_ERROR.message(), result.message());
        assertNull(result.data());
    }

    @Test
    void handleBusinessException_withCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH, "余额不足: 当前余额 0");
        Result<Void> result = handler.handleBusinessException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.BALANCE_NOT_ENOUGH.code(), result.code());
        assertEquals("余额不足: 当前余额 0", result.message());
    }

    @Test
    void handleValidationException_shouldReturnFailResult() {
        ValidationException ex = new ValidationException(ErrorCode.PARAM_REQUIRED, "name 不能为空", "name");
        Result<Void> result = handler.handleValidationException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.PARAM_REQUIRED.code(), result.code());
        assertEquals("name 不能为空", result.message());
    }

    @Test
    void handleAuthorizationException_shouldReturnUnauthorizedResult() {
        AuthorizationException ex = new AuthorizationException(ErrorCode.UNAUTHORIZED);
        Result<Void> result = handler.handleAuthorizationException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.UNAUTHORIZED.code(), result.code());
    }

    @Test
    void handleAuthorizationException_tokenExpired() {
        AuthorizationException ex = new AuthorizationException(ErrorCode.TOKEN_EXPIRED);
        Result<Void> result = handler.handleAuthorizationException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.TOKEN_EXPIRED.code(), result.code());
    }

    @Test
    void handleNotFoundException_shouldReturnNotFoundResult() {
        NotFoundException ex = new NotFoundException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        Result<Void> result = handler.handleNotFoundException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DATA_NOT_FOUND.code(), result.code());
        assertEquals("用户不存在", result.message());
    }

    @Test
    void handleNotFoundException_withDefaultConstructor() {
        NotFoundException ex = new NotFoundException("资源不存在");
        Result<Void> result = handler.handleNotFoundException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DATA_NOT_FOUND.code(), result.code());
    }

    @Test
    void handleExternalServiceException_shouldReturnErrorResult() {
        ExternalServiceException ex = new ExternalServiceException(
                ErrorCode.EXTERNAL_SERVICE_ERROR, "支付服务调用失败", "PaymentGateway");
        Result<Void> result = handler.handleExternalServiceException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.EXTERNAL_SERVICE_ERROR.code(), result.code());
        assertEquals("支付服务调用失败", result.message());
    }

    @Test
    void handleMethodArgumentNotValidException_shouldContainFieldDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        var bindingResult = mock(org.springframework.validation.BindingResult.class);
        var fieldError = new org.springframework.validation.FieldError("user", "email", "邮箱格式不正确");
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(fieldError));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Result<Void> result = handler.handleMethodArgumentNotValidException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.PARAM_INVALID.code(), result.code());
        assertTrue(result.message().contains("email"));
        assertTrue(result.message().contains("邮箱格式不正确"));
    }

    @Test
    void handleConstraintViolationException_shouldContainViolationMessages() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("值不能为负数");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        Result<Void> result = handler.handleConstraintViolationException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.PARAM_INVALID.code(), result.code());
        assertTrue(result.message().contains("值不能为负数"));
    }

    @Test
    void handleHttpMessageNotReadableException_shouldReturnBodyRequired() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");
        Result<Void> result = handler.handleHttpMessageNotReadableException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.BODY_REQUIRED.code(), result.code());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException_shouldReturnMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PATCH");
        Result<Void> result = handler.handleHttpRequestMethodNotSupportedException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.PARAM_FORMAT_ERROR.code(), result.code());
        assertTrue(result.message().contains("PATCH"));
    }

    @Test
    void handleNoHandlerFoundException_shouldReturnNotFound() {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/api/nonexistent", null);
        Result<Void> result = handler.handleNoHandlerFoundException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.DATA_NOT_FOUND.code(), result.code());
    }

    @Test
    void handleException_shouldReturnSystemError() {
        Exception ex = new RuntimeException("unexpected error");
        Result<Void> result = handler.handleException(ex);

        assertFalse(result.isSuccess());
        assertEquals(ErrorCode.SYSTEM_ERROR.code(), result.code());
        assertEquals(ErrorCode.SYSTEM_ERROR.message(), result.message());
    }
}
