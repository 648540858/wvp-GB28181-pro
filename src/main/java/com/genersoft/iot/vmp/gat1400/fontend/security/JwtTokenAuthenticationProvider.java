package com.genersoft.iot.vmp.gat1400.fontend.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

import cz.data.viid.framework.CacheService;
import cz.data.viid.framework.domain.core.LoginUser;

public class JwtTokenAuthenticationProvider implements AuthenticationProvider {
    private final CacheService cacheService;

    public JwtTokenAuthenticationProvider(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (authentication instanceof JwtTokenAuthentication) {
            JwtTokenAuthentication jwtTokenAuthentication = (JwtTokenAuthentication) authentication;
            LoginUser userinfo = this.userinfo(jwtTokenAuthentication.getJwt());
            if (Objects.nonNull(userinfo)) {
                jwtTokenAuthentication.setUserinfo(userinfo);
                jwtTokenAuthentication.setAuthenticated(true);
                return jwtTokenAuthentication;
            }
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtTokenAuthentication.class.isAssignableFrom(authentication);
    }

    public LoginUser userinfo(Jwt jwt) {
        String jti = jwt.getId();
        return cacheService.getLoginUser(jti);
    }
}
