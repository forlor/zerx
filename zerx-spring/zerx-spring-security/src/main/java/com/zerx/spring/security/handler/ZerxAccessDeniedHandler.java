package com.zerx.spring.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * 访问拒绝处理器 — 处理 403 Forbidden 响应
 * <p>
 * 当已认证但权限不足的用户访问资源时，Spring Security 调用此处理器，
 * 以 JSON 格式返回统一的错误响应。
 * </p>
 *
 * @author zerx
 */
public class ZerxAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ZerxAccessDeniedHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     * <p>
     * 返回 JSON 响应：{@code Result.fail("403", "没有访问权限")}
     * </p>
     */
    @Override
    public void handle(@SuppressWarnings("NullableProblems") HttpServletRequest request,
                       @SuppressWarnings("NullableProblems") HttpServletResponse response,
                       @SuppressWarnings("NullableProblems") AccessDeniedException accessDeniedException)
            throws IOException {
        log.debug("Access denied: {} for URI: {}", accessDeniedException.getMessage(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail("403", "没有访问权限")));
    }
}
