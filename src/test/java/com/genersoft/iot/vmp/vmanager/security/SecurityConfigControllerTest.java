package com.genersoft.iot.vmp.vmanager.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.conf.security.RuntimeSecurityConfigService;
import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.storager.dao.dto.Role;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.vmanager.bean.SecurityConfigInfo;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigControllerTest {

    @BeforeAll
    static void initializeJwtKey() throws Exception {
        ReflectionTestUtils.setField(JwtUtils.class, "rsaJsonWebKey", RsaJwkGenerator.generateJwk(2048));
    }

    @Mock
    private UserSetting userSetting;

    @Mock
    private Environment environment;

    @Mock
    private RuntimeSecurityConfigService runtimeSecurityConfigService;

    @InjectMocks
    private SecurityConfigController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnSanitizedRuntimeSecurityConfiguration() {
        RuntimeSecurityConfig runtimeConfig = runtimeConfig();
        when(runtimeSecurityConfigService.getConfig()).thenReturn(runtimeConfig);
        when(userSetting.getJwkFile()).thenReturn(null);
        when(userSetting.isSipCacheServerConnections()).thenReturn(false);
        when(environment.getProperty("server.ssl.enabled", Boolean.class, false)).thenReturn(true);
        when(environment.getProperty("server.port", Integer.class, 8080)).thenReturn(18080);

        SecurityConfigInfo result = controller.getConfig();

        assertTrue(result.getInterfaceAuthentication());
        assertTrue(result.getHttpsEnabled());
        assertFalse(result.getAllowAllOrigins());
        assertFalse(result.getFastjson1Present());
        assertEquals("Fastjson2", result.getJsonLibrary());
        assertEquals("config/jwk.json（默认外部文件）", result.getJwkFile());
        assertEquals(List.of("/api/public/**"), result.getInterfaceAuthenticationExcludes());
        assertFalse(result.getWarnings().stream().anyMatch(message -> message.contains("Fastjson 1.x")));
    }

    @Test
    void shouldUpdateRuntimeConfigurationForAdmin() throws Exception {
        RuntimeSecurityConfig runtimeConfig = runtimeConfig();
        when(runtimeSecurityConfigService.update(runtimeConfig)).thenReturn(runtimeConfig);
        when(runtimeSecurityConfigService.getConfig()).thenReturn(runtimeConfig);
        when(environment.getProperty("server.ssl.enabled", Boolean.class, false)).thenReturn(false);
        when(environment.getProperty("server.port", Integer.class, 8080)).thenReturn(8080);
        setAdminAuthentication();
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityConfigInfo result = controller.updateConfig(runtimeConfig, response);

        assertEquals(120L, result.getJwtExpirationMinutes());
        assertTrue(result.getContentTypeOptionsEnabled());
        assertTrue(response.containsHeader(JwtUtils.getHeader()));
        JwtClaims claims = claims(response.getHeader(JwtUtils.getHeader()));
        long validitySeconds = claims.getExpirationTime().getValue() - claims.getIssuedAt().getValue();
        assertTrue(validitySeconds >= 7199 && validitySeconds <= 7201);
    }

    @Test
    void shouldRenewCurrentTokenWithoutExpirationWhenTimeoutIsZero() throws Exception {
        RuntimeSecurityConfig runtimeConfig = runtimeConfig();
        runtimeConfig.setLoginTimeoutMinutes(0L);
        when(runtimeSecurityConfigService.update(runtimeConfig)).thenReturn(runtimeConfig);
        when(runtimeSecurityConfigService.getConfig()).thenReturn(runtimeConfig);
        when(environment.getProperty("server.ssl.enabled", Boolean.class, false)).thenReturn(false);
        when(environment.getProperty("server.port", Integer.class, 8080)).thenReturn(8080);
        setAdminAuthentication();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.updateConfig(runtimeConfig, response);

        assertNull(claims(response.getHeader(JwtUtils.getHeader())).getExpirationTime());
    }

    @Test
    void shouldRejectRuntimeConfigurationUpdateForNonAdmin() {
        ControllerException exception = assertThrows(ControllerException.class,
                () -> controller.updateConfig(runtimeConfig(), new MockHttpServletResponse()));

        assertEquals(403, exception.getCode());
    }

    private RuntimeSecurityConfig runtimeConfig() {
        RuntimeSecurityConfig config = new RuntimeSecurityConfig();
        config.setInterfaceAuthentication(true);
        config.setInterfaceAuthenticationExcludes(List.of("/api/public/**"));
        config.setAllowedOrigins(List.of("https://console.example.com"));
        config.setLoginTimeoutMinutes(120L);
        config.setDocEnabled(false);
        config.setContentTypeOptionsEnabled(true);
        return config;
    }

    private void setAdminAuthentication() {
        Role role = new Role();
        role.setId(1);
        User user = new User();
        user.setUsername("admin");
        user.setRole(role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private JwtClaims claims(String token) throws Exception {
        return new JwtConsumerBuilder()
                .setSkipSignatureVerification()
                .setSkipAllValidators()
                .build()
                .processToClaims(token);
    }
}
