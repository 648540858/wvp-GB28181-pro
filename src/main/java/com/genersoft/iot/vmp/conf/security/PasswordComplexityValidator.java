package com.genersoft.iot.vmp.conf.security;

import org.springframework.stereotype.Component;

/**
 * 密码复杂度校验器（硬编码规则，无需配置）
 * <p>
 * 规则：
 * 1. 长度至少 10 位
 * 2. 必须同时包含大写字母、小写字母、数字和特殊符号
 * 3. 密码中不能包含用户名
 */
@Component
public class PasswordComplexityValidator {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[~!@#$%^&*()_+`\\-={}:;'<>?,./]).{10,}$";

    /**
     * 校验密码是否符合复杂度要求
     *
     * @param password 待校验的密码（原始明文）
     * @param username 用户名（用于校验密码中不能包含用户名）
     * @return 符合要求返回 true
     */
    public boolean validatePassword(String password, String username) {
        if (password == null || username == null) {
            return false;
        }
        if (password.length() < 10) {
            return false;
        }
        if (!password.matches(PASSWORD_PATTERN)) {
            return false;
        }
        // 密码中不能包含用户名（忽略大小写）
        if (password.toLowerCase().contains(username.toLowerCase())) {
            return false;
        }
        return true;
    }

    /**
     * 获取复杂度规则的中文提示
     */
    public String getValidationErrorMessage() {
        return "密码长度至少10位，必须同时包含大写字母、小写字母、数字和特殊符号，且不能包含用户名";
    }
}
