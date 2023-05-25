package com.genersoft.iot.vmp.conf.security;

import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.security.sasl.AuthenticationException;
import java.time.LocalDateTime;

public class SecurityUtils {

    /**
     * 描述根据账号密码进行调用security进行认证授权 主动调
     * 用AuthenticationManager的authenticate方法实现
     * 授权成功后将用户信息存入SecurityContext当中
     * @param username 用户名
     * @param password 密码
     * @param authenticationManager 认证授权管理器,
     * @see  AuthenticationManager
     * @return UserInfo  用户信息
     */
    public static LoginUser login(String username, String password, AuthenticationManager authenticationManager) throws AuthenticationException {
        //使用security框架自带的验证token生成器  也可以自定义。
        UsernamePasswordAuthenticationToken token =new UsernamePasswordAuthenticationToken(username,password);
        //认证 如果失败，这里会自动异常后返回，所以这里不需要判断返回值是否为空，确定是否登录成功
        Authentication authenticate = authenticationManager.authenticate(token);
        LoginUser user = (LoginUser) authenticate.getPrincipal();

        SecurityContextHolder.getContext().setAuthentication(token);

        return user;
    }

    /**
     * 获取当前登录的所有认证信息
     * @return
     */
    public static Authentication getAuthentication(){
        SecurityContext context = SecurityContextHolder.getContext();
        return context.getAuthentication();
    }

    /**
     * 获取当前登录用户信息
     * @return
     */
    public static LoginUser getUserInfo(){
        Authentication authentication = getAuthentication();
        if(authentication!=null){
            Object principal = authentication.getPrincipal();
            if(principal!=null && !"anonymousUser".equals(principal.toString())){

                User user = (User) principal;
                return new LoginUser(user, LocalDateTime.now());
            }
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     * @return
     */
    public static int getUserId(){
        LoginUser user = getUserInfo();
        return user.getId();
    }

    /**
     * 生成BCryptPasswordEncoder密码
     *
     * @param password 密码
     * @return 加密字符串
     */
    public static String encryptPassword(String password) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        return passwordEncoder.encode(password);
    }
}
