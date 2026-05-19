package com.zerx.spring.security.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerx.spring.security.filter.ZerxJwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

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

    @Nested
    @DisplayName("基础响应测试")
    class BasicResponseTest {

        @Test
        @DisplayName("无错误属性时返回默认 401")
        void noErrorAttribute_default401() throws Exception {
            var exception = new org.springframework.security.authentication.BadCredentialsException("unauthorized");
            entryPoint.commence(request, response, exception);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("success").asBoolean()).isFalse();
            assertThat(json.get("code").asText()).isEqualTo("401");
            assertThat(json.get("message").asText()).isEqualTo("未认证，请先登录");
        }

        @Test
        @DisplayName("AuthenticationException 消息为 null 时不抛异常")
        void nullMessage_noException() throws Exception {
            var exception = new org.springframework.security.authentication.BadCredentialsException((String) null);
            entryPoint.commence(request, response, exception);

            assertThat(response.getStatus()).isEqualTo(401);
            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("code").asText()).isEqualTo("401");
            assertThat(json.get("message").asText()).isEqualTo("未认证，请先登录");
        }
    }

    @Nested
    @DisplayName("错误粒度测试")
    class ErrorGranularityTest {

        @Test
        @DisplayName("token_expired 返回 TOKEN_EXPIRED")
        void tokenExpired_returnsExpired() throws Exception {
            request.setAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR,
                    ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_EXPIRED);

            var exception = new org.springframework.security.authentication.BadCredentialsException("expired");
            entryPoint.commence(request, response, exception);

            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("code").asText()).isEqualTo("TOKEN_EXPIRED");
            assertThat(json.get("message").asText()).isEqualTo("令牌已过期，请重新登录");
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("token_invalid 返回 TOKEN_INVALID")
        void tokenInvalid_returnsInvalid() throws Exception {
            request.setAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR,
                    ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_INVALID);

            var exception = new org.springframework.security.authentication.BadCredentialsException("invalid");
            entryPoint.commence(request, response, exception);

            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("code").asText()).isEqualTo("TOKEN_INVALID");
            assertThat(json.get("message").asText()).isEqualTo("无效的认证令牌");
        }

        @Test
        @DisplayName("token_blacklisted 返回 TOKEN_BLACKLISTED")
        void tokenBlacklisted_returnsBlacklisted() throws Exception {
            request.setAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR,
                    ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_BLACKLISTED);

            var exception = new org.springframework.security.authentication.BadCredentialsException("blacklisted");
            entryPoint.commence(request, response, exception);

            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("code").asText()).isEqualTo("TOKEN_BLACKLISTED");
            assertThat(json.get("message").asText()).isEqualTo("令牌已失效，请重新登录");
        }

        @Test
        @DisplayName("token_type_rejected 返回 TOKEN_INVALID")
        void tokenTypeRejected_returnsInvalid() throws Exception {
            request.setAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR,
                    ZerxJwtAuthenticationFilter.AUTH_ERROR_TOKEN_TYPE_REJECTED);

            var exception = new org.springframework.security.authentication.BadCredentialsException("type rejected");
            entryPoint.commence(request, response, exception);

            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("code").asText()).isEqualTo("TOKEN_INVALID");
            assertThat(json.get("message").asText()).isEqualTo("无效的认证令牌");
        }

        @Test
        @DisplayName("未知错误码返回默认 401")
        void unknownErrorCode_default401() throws Exception {
            request.setAttribute(ZerxJwtAuthenticationFilter.ATTR_AUTH_ERROR, "unknown_error");

            var exception = new org.springframework.security.authentication.BadCredentialsException("unknown");
            entryPoint.commence(request, response, exception);

            JsonNode json = objectMapper.readTree(response.getContentAsString());
            assertThat(json.get("code").asText()).isEqualTo("401");
            assertThat(json.get("message").asText()).isEqualTo("未认证，请先登录");
        }
    }
}
