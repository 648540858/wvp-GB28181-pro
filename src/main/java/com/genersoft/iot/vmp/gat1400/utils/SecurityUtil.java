package com.genersoft.iot.vmp.gat1400.utils;

import com.genersoft.iot.vmp.gat1400.framework.domain.core.LoginUser;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;


public class SecurityUtil {

    public static LoginUser requireDataUser() {
        return Objects.requireNonNull(getDataUser(), "请求上下文不存在用户信息");
    }

    /**
     * 获取用户
     *
     * @return user
     */
    public static LoginUser getDataUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            Object principal = authentication.getCredentials();
            if (principal instanceof LoginUser) {
                LoginUser user = (LoginUser) principal;
                return user;
            }
        }
        return null;
    }


    public static String bearerTokenResolver(HttpServletRequest request) {
        return removeTokenType(request.getHeader("Authorization"));
    }

    public static String removeTokenType(String token) {
        if (StringUtils.startsWith(token, "Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }
}
