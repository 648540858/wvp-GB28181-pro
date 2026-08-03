package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.security.dto.RuntimeSecurityConfig;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldApplyUpdatedExcludeAndContentTypeHeader() throws Exception {
        RuntimeSecurityConfigService runtimeService = mock(RuntimeSecurityConfigService.class);
        RuntimeSecurityConfig runtimeConfig = new RuntimeSecurityConfig();
        runtimeConfig.setInterfaceAuthentication(true);
        runtimeConfig.setDocEnabled(true);
        runtimeConfig.setContentTypeOptionsEnabled(true);
        when(runtimeService.getConfig()).thenReturn(runtimeConfig);
        when(runtimeService.isAuthenticationExcluded("/api/public/devices")).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "runtimeSecurityConfigService", runtimeService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public/devices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(response));
    }
}
