package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 配置Spring Security
 *
 * @author lin
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(1)
@Slf4j
public class WebSecurityConfig {

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DefaultUserDetailsServiceImpl userDetailsService;
    /**
     * 登出成功的处理
     */
    @Autowired
    private LogoutHandler logoutHandler;
    /**
     * 未登录的处理
     */
    @Autowired
    private AnonymousAuthenticationEntryPoint anonymousAuthenticationEntryPoint;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // 设置不隐藏 未找到用户异常
        provider.setHideUserNotFoundExceptions(true);
        // 用户认证service - 查询数据库的逻辑
        provider.setUserDetailsService(userDetailsService);
        // 设置密码加密算法
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> defaultExcludes = new ArrayList<>();
        defaultExcludes.add("/");
        defaultExcludes.add("/#/**");
        defaultExcludes.add("/static/**");

        defaultExcludes.add("/swagger-ui.html");
        defaultExcludes.add("/swagger-ui/**");
        defaultExcludes.add("/swagger-resources/**");
        defaultExcludes.add("/doc.html");
        defaultExcludes.add("/doc.html#/**");
        defaultExcludes.add("/v3/api-docs/**");

        defaultExcludes.add("/index.html");
        defaultExcludes.add("/webjars/**");

        defaultExcludes.add("/js/**");
        defaultExcludes.add("/api/device/query/snap/**");
        defaultExcludes.add("/record_proxy/*/**");
        defaultExcludes.add("/api/emit");
        defaultExcludes.add("/favicon.ico");
        defaultExcludes.add("/api/user/login");
        defaultExcludes.add("/index/hook/**");
        defaultExcludes.add("/api/device/query/snap/**");
        defaultExcludes.add("/index/hook/abl/**");
        defaultExcludes.add("/api/jt1078/playback/download");
        defaultExcludes.add("/api/jt1078/snap");

        if (userSetting.getInterfaceAuthentication() && !userSetting.getInterfaceAuthenticationExcludes().isEmpty()) {
            defaultExcludes.addAll(userSetting.getInterfaceAuthenticationExcludes());
        }

        http
                .headers(headers -> headers.contentTypeOptions(contentType -> contentType.disable()))
                .cors(cors -> cors.configurationSource(configurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 配置拦截规则
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(defaultExcludes.toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                // 异常处理器
                .exceptionHandling(exception -> exception.authenticationEntryPoint(anonymousAuthenticationEntryPoint))
                .logout(logout -> logout.logoutUrl("/api/user/logout")
                        .permitAll()
                        .logoutSuccessHandler(logoutHandler));

        return http.build();
    }

    CorsConfigurationSource configurationSource() {
        // 配置跨域
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedHeaders(Arrays.asList("*"));
        corsConfiguration.setAllowedMethods(Arrays.asList("*"));
        corsConfiguration.setMaxAge(3600L);
        if (userSetting.getAllowedOrigins() != null && !userSetting.getAllowedOrigins().isEmpty()) {
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedOrigins(userSetting.getAllowedOrigins());
        } else {
            // 在SpringBoot 2.4及以上版本处理跨域时，遇到错误提示：当allowCredentials为true时，allowedOrigins不能包含特殊值"*"。
            // 解决方法是明确指定allowedOrigins或使用allowedOriginPatterns。
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.addAllowedOriginPattern(CorsConfiguration.ALL); // 默认全部允许所有跨域
        }

        corsConfiguration.setExposedHeaders(Arrays.asList(JwtUtils.getHeader()));

        UrlBasedCorsConfigurationSource url = new UrlBasedCorsConfigurationSource();
        url.registerCorsConfiguration("/**", corsConfiguration);
        return url;
    }

    /**
     * 描述: 密码加密算法 BCrypt 推荐使用
     **/
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}