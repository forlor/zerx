package com.zerx.spring.security.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ZerxAuthenticationEntryPoint} 单元测试
 *
 * @author zerx
 */
class ZerxAuthenticationEntryPointTest {

    private ZerxAuthenticationEntryPoint entryPoint;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        entryPoint = new ZerxAuthenticationEntryPoint();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void commence_sets401StatusCode() throws Exception {
        var exception = new org.springframework.security.authentication.BadCredentialsException("unauthorized");

        entryPoint.commence(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void commence_setsJsonContentType() throws Exception {
        var exception = new org.springframework.security.authentication.BadCredentialsException("unauthorized");

        entryPoint.commence(request, response, exception);

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    }

    @Test
    void commence_writesCorrectJsonBody() throws Exception {
        var exception = new org.springframework.security.authentication.BadCredentialsException("unauthorized");

        entryPoint.commence(request, response, exception);

        String body = response.getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("code").asText()).isEqualTo("401");
        assertThat(json.get("message").asText()).isEqualTo("未认证，请先登录");
        assertThat(json.get("data").isNull()).isTrue();
    }

    @Test
    void commence_worksWhenAuthenticationExceptionMessageIsNull() throws Exception {
        var exception = new org.springframework.security.authentication.BadCredentialsException((String) null);

        entryPoint.commence(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

        String body = response.getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        assertThat(json.get("code").asText()).isEqualTo("401");
        assertThat(json.get("message").asText()).isEqualTo("未认证，请先登录");
    }
}
