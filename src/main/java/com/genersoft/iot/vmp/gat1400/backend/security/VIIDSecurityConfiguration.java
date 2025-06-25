package com.genersoft.iot.vmp.gat1400.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@Order(3)
public class VIIDSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    DigestAuthenticationEntryPoint digestAuthenticationEntryPoint;
    @Autowired
    DigestAuthenticationFilter digestAuthenticationFilter;
    @Autowired
    VIIDAuthenticationFilter viidAuthenticationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("载入视图库交互接口Security过滤链");
        http
            .requestMatchers(request -> request.mvcMatchers("/VIID/**"))
            .authorizeRequests()
            .mvcMatchers(HttpMethod.POST,
                    "/VIID/System/UnRegister",
                    "/VIID/System/Keepalive").permitAll()
            .anyRequest().authenticated()
            .and()
            .headers().frameOptions().disable()
            .and()
            .exceptionHandling()
            .authenticationEntryPoint(digestAuthenticationEntryPoint)
            .accessDeniedHandler(new VIIDAccessDeniedHandler(digestAuthenticationEntryPoint))
            .and()
            .addFilterBefore(viidAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(digestAuthenticationFilter, viidAuthenticationFilter.getClass())
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
//            .build();

    }

}
