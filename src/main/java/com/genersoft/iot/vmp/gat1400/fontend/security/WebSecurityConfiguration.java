package com.genersoft.iot.vmp.gat1400.fontend.security;

import com.genersoft.iot.vmp.gat1400.framework.CacheService;
import com.genersoft.iot.vmp.gat1400.utils.SecurityUtil;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebSecurityConfiguration {

    @Order(9)
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   CacheService cacheService) throws Exception {
        log.info("载入默认Jwt鉴权Security过滤链");
        return http
                .requestMatchers(request -> request.mvcMatchers("/api/**"))
                .authorizeRequests(
                        request -> request.mvcMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                                .anyRequest().authenticated()
                )
                .oauth2ResourceServer(
                        config -> config.bearerTokenResolver(SecurityUtil::bearerTokenResolver)
                                .jwt()
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtTokenAuthenticationConverter())
                                .and()
                                .accessDeniedHandler(new DataAccessDeniedHandler())
                                .authenticationEntryPoint(new DataAuthExceptionEntryPoint())
                )
                .authenticationProvider(new JwtTokenAuthenticationProvider(cacheService))
                .formLogin(FormLoginConfigurer::disable)
                .csrf(CsrfConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public JwtTokenAuthenticationConverter jwtTokenAuthenticationConverter() {
        return new JwtTokenAuthenticationConverter();
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        log.info("载入Jwt解码器...");
        // 基于 JwkSetUri 等创建对应的 NimbusJwtDecoder
        Set<JWSAlgorithm> jwsAlgs = new HashSet<>();
        jwsAlgs.addAll(JWSAlgorithm.Family.RSA);
        jwsAlgs.addAll(JWSAlgorithm.Family.EC);
        jwsAlgs.addAll(JWSAlgorithm.Family.HMAC_SHA);
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
        JWSKeySelector<SecurityContext> jwsKeySelector = new JWSVerificationKeySelector<>(jwsAlgs, jwkSource);
        jwtProcessor.setJWSKeySelector(jwsKeySelector);
        jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
        });
        NimbusJwtDecoder jwtDecoder = new NimbusJwtDecoder(jwtProcessor);
        jwtDecoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return jwtDecoder;
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        JWKSet jwkSet;
        try {
            ClassPathResource resource = new ClassPathResource("jwk.json");
            InputStreamReader reader = new InputStreamReader(resource.getInputStream());
            jwkSet = JWKSet.parse(FileCopyUtils.copyToString(reader));
        } catch (ParseException | IOException e) {
            throw new RuntimeException(e);
        }
        return new ImmutableJWKSet<>(jwkSet);
    }
}
