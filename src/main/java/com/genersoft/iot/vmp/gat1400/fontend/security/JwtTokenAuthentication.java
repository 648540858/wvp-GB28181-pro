package com.genersoft.iot.vmp.gat1400.fontend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;

import cz.data.viid.framework.domain.core.LoginUser;

public class JwtTokenAuthentication extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final String principal;
    private LoginUser userinfo;

    public JwtTokenAuthentication(Jwt jwt, String principal) {
        super(Collections.emptyList());
        this.jwt = jwt;
        this.principal = principal;
    }

    @Override
    public Object getCredentials() {
        return userinfo;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setUserinfo(LoginUser userinfo) {
        this.userinfo = userinfo;
    }
}
