package com.genersoft.iot.vmp.conf.security;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import com.genersoft.iot.vmp.storager.dao.SecurityConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeSecurityConfigServiceTest {

    private UserSetting userSetting;

    private ValueOperations<String, String> valueOperations;

    private SecurityConfigMapper securityConfigMapper;

    private RuntimeSecurityConfigService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userSetting = new UserSetting();
        userSetting.setServerId("security-test");
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        securityConfigMapper = mock(SecurityConfigMapper.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(securityConfigMapper.getConfig(anyString())).thenReturn(null);
        when(securityConfigMapper.update(anyString(), anyString(), anyString())).thenReturn(0, 1, 1);
        service = new RuntimeSecurityConfigService(userSetting, redisTemplate, securityConfigMapper);
        service.initialize();
    }

    @Test
    void shouldPersistAndApplyRuntimeConfigurationImmediately() {
        RuntimeSecurityConfig request = new RuntimeSecurityConfig();
        request.setInterfaceAuthentication(false);
        request.setInterfaceAuthenticationExcludes(List.of(" /api/public/** ", "/api/public/**"));
        request.setLoginTimeoutMinutes(120L);
        request.setAllowedOrigins(List.of("https://console.example.com"));
        request.setDocEnabled(false);
        request.setContentTypeOptionsEnabled(true);

        RuntimeSecurityConfig result = service.update(request);

        verify(valueOperations, atLeastOnce()).set(anyString(), anyString());
        verify(securityConfigMapper, atLeastOnce()).update(anyString(), anyString(), anyString());
        assertFalse(userSetting.getInterfaceAuthentication());
        assertEquals(120L, userSetting.getLoginTimeout());
        assertTrue(userSetting.getContentTypeOptionsEnabled());
        assertEquals(List.of("/api/public/**"), result.getInterfaceAuthenticationExcludes());
        assertTrue(service.isAuthenticationExcluded("/api/public/devices"));
    }

    @Test
    void shouldRejectOriginContainingAPath() {
        RuntimeSecurityConfig request = service.getConfig();
        request.setAllowedOrigins(List.of("https://console.example.com/admin"));

        assertThrows(IllegalArgumentException.class, () -> service.update(request));
    }

    @Test
    void shouldRejectExcludeMatchingEveryRequest() {
        RuntimeSecurityConfig request = service.getConfig();
        request.setInterfaceAuthenticationExcludes(List.of("/**"));

        assertThrows(IllegalArgumentException.class, () -> service.update(request));
    }

    @Test
    void shouldAllowZeroForNonExpiringLogin() {
        RuntimeSecurityConfig request = service.getConfig();
        request.setLoginTimeoutMinutes(0L);

        RuntimeSecurityConfig result = service.update(request);

        assertEquals(0L, result.getLoginTimeoutMinutes());
        assertEquals(0L, userSetting.getLoginTimeout());
    }

    @Test
    void shouldUseTwoHoursAsDefaultLoginTimeout() {
        assertEquals(120L, service.getConfig().getLoginTimeoutMinutes());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRestoreConfigurationFromDatabaseAndSyncRedis() {
        RuntimeSecurityConfig persisted = service.getConfig();
        persisted.setLoginTimeoutMinutes(0L);
        persisted.setAllowedOrigins(List.of("https://saved.example.com"));
        UserSetting restoredUserSetting = new UserSetting();
        restoredUserSetting.setServerId("restored-server");
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> restoredValueOperations = mock(ValueOperations.class);
        SecurityConfigMapper restoredMapper = mock(SecurityConfigMapper.class);
        when(redisTemplate.opsForValue()).thenReturn(restoredValueOperations);
        when(restoredMapper.getConfig("restored-server")).thenReturn(JSON.toJSONString(persisted));
        RuntimeSecurityConfigService restoredService = new RuntimeSecurityConfigService(
                restoredUserSetting, redisTemplate, restoredMapper);

        restoredService.initialize();

        assertEquals(0L, restoredService.getConfig().getLoginTimeoutMinutes());
        assertEquals(List.of("https://saved.example.com"), restoredService.getConfig().getAllowedOrigins());
        verify(restoredValueOperations).set(anyString(), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFillMissingFieldsInLegacyDatabaseConfiguration() {
        UserSetting restoredUserSetting = new UserSetting();
        restoredUserSetting.setServerId("legacy-server");
        restoredUserSetting.setLoginTimeout(120L);
        restoredUserSetting.setContentTypeOptionsEnabled(true);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> restoredValueOperations = mock(ValueOperations.class);
        SecurityConfigMapper restoredMapper = mock(SecurityConfigMapper.class);
        when(redisTemplate.opsForValue()).thenReturn(restoredValueOperations);
        when(restoredMapper.getConfig("legacy-server"))
                .thenReturn("{\"interfaceAuthentication\":false,\"jwtExpirationMinutes\":45,"
                        + "\"docEnabled\":true}");
        when(restoredMapper.update(anyString(), anyString(), anyString())).thenReturn(1);
        RuntimeSecurityConfigService restoredService = new RuntimeSecurityConfigService(
                restoredUserSetting, redisTemplate, restoredMapper);

        restoredService.initialize();

        RuntimeSecurityConfig restored = restoredService.getConfig();
        assertFalse(restored.getInterfaceAuthentication());
        assertTrue(restored.getDocEnabled());
        assertEquals(45L, restored.getLoginTimeoutMinutes());
        assertTrue(restored.getContentTypeOptionsEnabled());
        verify(restoredMapper).update(anyString(), anyString(), anyString());
        verify(restoredValueOperations).set(anyString(), anyString());
    }
}
