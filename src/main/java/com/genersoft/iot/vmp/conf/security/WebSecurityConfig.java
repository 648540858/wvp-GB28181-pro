package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.UserSetting;
import org.junit.jupiter.api.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * 配置Spring Security
 * @author lin
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(1)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final static Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private DefaultUserDetailsServiceImpl userDetailsService;
    /**
     * 登出成功的处理
     */
    @Autowired
    private LoginFailureHandler loginFailureHandler;
    /**
     * 登录成功的处理
     */
    @Autowired
    private LoginSuccessHandler loginSuccessHandler;
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

//    @Bean
//    JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
//        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager());
//        return jwtAuthenticationFilter;
//    }


    /**
     * 描述: 静态资源放行，这里的放行，是不走 Spring Security 过滤器链
     **/
    @Override
    public void configure(WebSecurity web) {

        if (!userSetting.isInterfaceAuthentication()) {
            web.ignoring().antMatchers("**");
        }else {
            // 可以直接访问的静态数据
            web.ignoring()
                    .antMatchers("/")
                    .antMatchers("/#/**")
                    .antMatchers("/static/**")
                    .antMatchers("/index.html")
                    .antMatchers("/doc.html") // "/webjars/**", "/swagger-resources/**", "/v3/api-docs/**"
                    .antMatchers("/webjars/**")
                    .antMatchers("/swagger-resources/**")
                    .antMatchers("/v3/api-docs/**")
                    .antMatchers("/favicon.ico")
                    .antMatchers("/js/**");
            List<String> interfaceAuthenticationExcludes = userSetting.getInterfaceAuthenticationExcludes();
            for (String interfaceAuthenticationExclude : interfaceAuthenticationExcludes) {
                if (interfaceAuthenticationExclude.split("/").length < 4 ) {
                    logger.warn("{}不满足两级目录，已忽略", interfaceAuthenticationExclude);
                }else {
                    web.ignoring().antMatchers(interfaceAuthenticationExclude);
                }

            }
        }
    }

    /**
     * 配置认证方式
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
        http.headers().contentTypeOptions().disable()
                .and().cors()
                .and().csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                // 配置拦截规则
                .and()
                .authorizeRequests()
                .antMatchers("/api/user/login","/index/hook/**").permitAll()
                .anyRequest().authenticated()
                // 异常处理器
                .and()
                .exceptionHandling()
                .authenticationEntryPoint(anonymousAuthenticationEntryPoint)
//                .accessDeniedHandler(jwtAccessDeniedHandler)
                // 配置自定义的过滤器
//                .and()
//                .addFilter(jwtAuthenticationFilter)
                // 验证码过滤器放在UsernamePassword过滤器之前
//                .addFilterBefore(captchaFilter, UsernamePasswordAuthenticationFilter.class)
        ;
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
//        // 设置允许添加静态文件
//        http.headers().contentTypeOptions().disable();
//        http.authorizeRequests()
//                // 放行接口
//                .antMatchers("/api/user/login","/index/hook/**").permitAll()
//                // 除上面外的所有请求全部需要鉴权认证
//                .anyRequest().authenticated()
//                // 禁用session
//                .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                // 异常处理(权限拒绝、登录失效等)
//                .and().exceptionHandling()
//                // 匿名用户访问无权限资源时的异常处理
//                .authenticationEntryPoint(anonymousAuthenticationEntryPoint)
//                // 登录 允许所有用户
//                .and().formLogin()
//                // 登录成功处理逻辑 在这里给出JWT
//                .successHandler(loginSuccessHandler)
//                // 登录失败处理逻辑
//                .failureHandler(loginFailureHandler)
//                // 登出
//                .and().logout().logoutUrl("/api/user/logout").permitAll()
//                // 登出成功处理逻辑
//                .logoutSuccessHandler(logoutHandler)
//                // 配置自定义的过滤器
//                .and()
//                .addFilter(jwtAuthenticationFilter())
//        ;

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
