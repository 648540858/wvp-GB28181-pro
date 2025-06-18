package com.genersoft.iot.vmp.gat1400.fontend.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtTokenAuthenticationConverter implements Converter<Jwt, JwtTokenAuthentication> {

    @Override
    public JwtTokenAuthentication convert(Jwt jwt) {
        String principalClaimValue = jwt.getClaimAsString("sub");
        return new JwtTokenAuthentication(jwt, principalClaimValue);
    }
}
