package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

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


    /**
     * 配置认证方式
     *
     * @param auth
     * @throws Exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // 设置不隐藏 未找到用户异常
        provider.setHideUserNotFoundExceptions(true);
        // 用户认证service - 查询数据库的逻辑
        provider.setUserDetailsService(userDetailsService);
        // 设置密码加密算法
        provider.setPasswordEncoder(passwordEncoder());
        auth.authenticationProvider(provider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
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



        if (userSetting.getInterfaceAuthentication() && !userSetting.getInterfaceAuthenticationExcludes().isEmpty()) {
            defaultExcludes.addAll(userSetting.getInterfaceAuthenticationExcludes());
        }

        http.headers().contentTypeOptions().disable()
                .and().cors().configurationSource(configurationSource())
                .and().csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                // 配置拦截规则
                .and()
                .authorizeRequests()
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .antMatchers(defaultExcludes.toArray(new String[0])).permitAll()
                .anyRequest().authenticated()
                // 异常处理器
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(anonymousAuthenticationEntryPoint)
                .and().logout().logoutUrl("/api/user/logout").permitAll()
                .logoutSuccessHandler(logoutHandler)
        ;
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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
        }else {
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
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 描述: 注入AuthenticationManager管理器
     **/
    @Override
    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }
}
