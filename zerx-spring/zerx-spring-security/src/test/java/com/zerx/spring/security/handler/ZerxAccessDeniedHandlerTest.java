package com.zerx.spring.security.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ZerxAccessDeniedHandler} 单元测试
 *
 * @author zerx
 */
class ZerxAccessDeniedHandlerTest {

    private ZerxAccessDeniedHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new ZerxAccessDeniedHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void handle_sets403StatusCode() throws Exception {
        var exception = new AccessDeniedException("forbidden");

        handler.handle(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void handle_setsJsonContentType() throws Exception {
        var exception = new AccessDeniedException("forbidden");

        handler.handle(request, response, exception);

        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
    }

    @Test
    void handle_writesCorrectJsonBody() throws Exception {
        var exception = new AccessDeniedException("forbidden");

        handler.handle(request, response, exception);

        String body = response.getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("code").asText()).isEqualTo("403");
        assertThat(json.get("message").asText()).isEqualTo("没有访问权限");
        assertThat(json.get("data").isNull()).isTrue();
    }

    @Test
    void handle_worksWhenAccessDeniedExceptionMessageIsNull() throws Exception {
        var exception = new AccessDeniedException((String) null);

        handler.handle(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

        String body = response.getContentAsString();
        JsonNode json = objectMapper.readTree(body);

        assertThat(json.get("code").asText()).isEqualTo("403");
        assertThat(json.get("message").asText()).isEqualTo("没有访问权限");
    }
}
