package com.zerx.spring.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.common.model.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * 未认证处理器 — 处理 401 Unauthorized 响应
 * <p>
 * 当未认证用户访问受保护资源时，Spring Security 调用此处理器，
 * 以 JSON 格式返回统一的错误响应。
 * </p>
 *
 * @author zerx
 */
public class ZerxAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(ZerxAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * {@inheritDoc}
     * <p>
     * 返回 JSON 响应：{@code Result.fail("401", "未认证，请先登录")}
     * </p>
     */
    @Override
    public void commence(@SuppressWarnings("NullableProblems") HttpServletRequest request,
                         @SuppressWarnings("NullableProblems") HttpServletResponse response,
                         @SuppressWarnings("NullableProblems") AuthenticationException authException)
            throws IOException {
        log.debug("Unauthorized access: {}", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail("401", "未认证，请先登录")));
    }
}
