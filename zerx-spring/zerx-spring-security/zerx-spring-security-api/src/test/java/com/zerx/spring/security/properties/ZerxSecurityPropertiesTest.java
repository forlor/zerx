package com.zerx.spring.security.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ZerxSecurityProperties} 单元测试
 *
 * @author zerx
 */
class ZerxSecurityPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValuesTest {

        @Test
        @DisplayName("默认启用状态为 true")
        void defaultEnabled() {
            var props = new ZerxSecurityProperties();
            assertTrue(props.isEnabled());
        }

        @Test
        @DisplayName("默认免认证 URL 包含 login、register、doc.html")
        void defaultPermitUrls() {
            var props = new ZerxSecurityProperties();
            var permitUrls = props.getPermitUrls();
            assertEquals(3, permitUrls.size());
            assertTrue(permitUrls.contains("/auth/login"));
            assertTrue(permitUrls.contains("/auth/register"));
            assertTrue(permitUrls.contains("/doc.html"));
        }

        @Test
        @DisplayName("默认 JWT 配置正确")
        void defaultJwtConfig() {
            var props = new ZerxSecurityProperties();
            var jwt = props.getJwt();

            assertNotNull(jwt);
            assertNotNull(jwt.getSecret());
            assertTrue(jwt.getSecret().length() >= 32);
            assertEquals(Duration.ofHours(2), jwt.getAccessTokenExpire());
            assertEquals(Duration.ofDays(7), jwt.getRefreshTokenExpire());
            assertEquals("zerx", jwt.getIssuer());
            assertEquals("Authorization", jwt.getHeaderName());
            assertEquals("Bearer ", jwt.getHeaderPrefix());
        }

        @Test
        @DisplayName("默认签名算法为 HS256")
        void defaultAlgorithm_isHs256() {
            var props = new ZerxSecurityProperties();
            assertEquals("HS256", props.getJwt().getAlgorithm());
        }

        @Test
        @DisplayName("默认 RSA 配置不为 null")
        void defaultRsaConfig_notNull() {
            var props = new ZerxSecurityProperties();
            assertNotNull(props.getJwt().getRsa());
        }
    }

    @Nested
    @DisplayName("自定义配置测试")
    class CustomConfigTest {

        @Test
        @DisplayName("可禁用安全模块")
        void setEnabled() {
            var props = new ZerxSecurityProperties();
            props.setEnabled(false);
            assertFalse(props.isEnabled());
        }

        @Test
        @DisplayName("可自定义免认证 URL")
        void setPermitUrls() {
            var props = new ZerxSecurityProperties();
            var customUrls = List.of("/public/**", "/api/v1/open/**");
            props.setPermitUrls(customUrls);
            assertEquals(customUrls, props.getPermitUrls());
        }

        @Test
        @DisplayName("可自定义 JWT 配置")
        void setJwtConfig() {
            var props = new ZerxSecurityProperties();
            var jwt = props.getJwt();
            jwt.setSecret("my-custom-secret-key-for-hs256-must-be-long-enough!!");
            jwt.setAccessTokenExpire(Duration.ofMinutes(30));
            jwt.setRefreshTokenExpire(Duration.ofDays(30));
            jwt.setIssuer("custom-issuer");
            jwt.setHeaderName("X-Token");
            jwt.setHeaderPrefix("Token ");

            assertEquals("my-custom-secret-key-for-hs256-must-be-long-enough!!", jwt.getSecret());
            assertEquals(Duration.ofMinutes(30), jwt.getAccessTokenExpire());
            assertEquals(Duration.ofDays(30), jwt.getRefreshTokenExpire());
            assertEquals("custom-issuer", jwt.getIssuer());
            assertEquals("X-Token", jwt.getHeaderName());
            assertEquals("Token ", jwt.getHeaderPrefix());
        }

        @Test
        @DisplayName("可自定义签名算法")
        void setAlgorithm() {
            var props = new ZerxSecurityProperties();
            props.getJwt().setAlgorithm("RS256");
            assertEquals("RS256", props.getJwt().getAlgorithm());
        }

        @Test
        @DisplayName("可自定义 RSA 密钥配置")
        void setRsaConfig() {
            var props = new ZerxSecurityProperties();
            var rsa = props.getJwt().getRsa();
            rsa.setPublicKey("classpath:keys/public.pem");
            rsa.setPrivateKey("file:/etc/zerx/private.pem");

            assertEquals("classpath:keys/public.pem", rsa.getPublicKey());
            assertEquals("file:/etc/zerx/private.pem", rsa.getPrivateKey());
        }

        @Test
        @DisplayName("可替换整个 RSA 配置对象")
        void replaceRsaObject() {
            var props = new ZerxSecurityProperties();
            var newRsa = new ZerxSecurityProperties.Rsa();
            newRsa.setPublicKey("base64-public-key");
            newRsa.setPrivateKey("base64-private-key");
            props.getJwt().setRsa(newRsa);

            assertEquals("base64-public-key", props.getJwt().getRsa().getPublicKey());
            assertEquals("base64-private-key", props.getJwt().getRsa().getPrivateKey());
        }

        @Test
        @DisplayName("可整体替换 Jwt 对象")
        void replaceJwtObject() {
            var props = new ZerxSecurityProperties();
            var newJwt = new ZerxSecurityProperties.Jwt();
            newJwt.setIssuer("new-issuer");
            props.setJwt(newJwt);
            assertEquals("new-issuer", props.getJwt().getIssuer());
        }

        @Test
        @DisplayName("默认角色规则为空列表")
        void defaultRoleRules_isEmpty() {
            var props = new ZerxSecurityProperties();
            assertNotNull(props.getRoleRules());
            assertTrue(props.getRoleRules().isEmpty());
        }

        @Test
        @DisplayName("可自定义角色规则")
        void setRoleRules() {
            var props = new ZerxSecurityProperties();
            var rules = new ArrayList<ZerxSecurityProperties.RoleRule>();

            var rule1 = new ZerxSecurityProperties.RoleRule();
            rule1.setPath("/api/admin/**");
            rule1.setRole("ADMIN");

            var rule2 = new ZerxSecurityProperties.RoleRule();
            rule2.setPath("/api/vip/**");
            rule2.setRole("VIP");

            rules.add(rule1);
            rules.add(rule2);
            props.setRoleRules(rules);

            assertEquals(2, props.getRoleRules().size());
            assertEquals("/api/admin/**", props.getRoleRules().get(0).getPath());
            assertEquals("ADMIN", props.getRoleRules().get(0).getRole());
            assertEquals("/api/vip/**", props.getRoleRules().get(1).getPath());
            assertEquals("VIP", props.getRoleRules().get(1).getRole());
        }
    }
}
