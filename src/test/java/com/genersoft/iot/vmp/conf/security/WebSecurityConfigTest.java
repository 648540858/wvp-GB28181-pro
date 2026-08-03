package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSecurityConfigTest {

    @Test
    void shouldReadAllowedOriginsForEveryRequest() {
        RuntimeSecurityConfigService runtimeService = mock(RuntimeSecurityConfigService.class);
        WebSecurityConfig securityConfig = new WebSecurityConfig();
        ReflectionTestUtils.setField(securityConfig, "runtimeSecurityConfigService", runtimeService);
        RuntimeSecurityConfig first = runtimeConfig(List.of("https://first.example.com"));
        RuntimeSecurityConfig second = runtimeConfig(List.of("https://second.example.com"));
        when(runtimeService.getConfig()).thenReturn(first, second);

        CorsConfiguration firstResult = securityConfig.configurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());
        CorsConfiguration secondResult = securityConfig.configurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertEquals(first.getAllowedOrigins(), firstResult.getAllowedOrigins());
        assertEquals(second.getAllowedOrigins(), secondResult.getAllowedOrigins());
    }

    private RuntimeSecurityConfig runtimeConfig(List<String> allowedOrigins) {
        RuntimeSecurityConfig config = new RuntimeSecurityConfig();
        config.setAllowedOrigins(allowedOrigins);
        return config;
    }
}
