package com.genersoft.iot.vmp.conf.security;

import org.springframework.stereotype.Component;

/**
 * 密码复杂度校验器（硬编码规则，无需配置）
 * <p>
 * 规则：
 * 1. 长度 6-20 位
 * 2. 必须同时包含字母、数字和特殊符号（字母不区分大小写）
 */
@Component
public class PasswordComplexityValidator {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+`\\-={}:;'<>?,./]).{6,20}$";

    /**
     * 校验密码是否符合复杂度要求
     *
     * @param password 待校验的密码（原始明文）
     * @return 符合要求返回 true
     */
    public boolean validatePassword(String password) {
        if (password == null) {
            return false;
        }
        return password.matches(PASSWORD_PATTERN);
    }

    /**
     * 获取复杂度规则的中文提示
     */
    public String getValidationErrorMessage() {
        return "密码长度6-20位，必须包含字母、数字和特殊符号";
    }
}
