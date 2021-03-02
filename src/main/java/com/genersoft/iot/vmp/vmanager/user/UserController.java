package com.genersoft.iot.vmp.vmanager.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class UserController {

    @Value("${auth.username}")
    private String usernameConfig;

    @Value("${auth.password}")
    private String passwordConfig;

    @RequestMapping("/user/login")
    public String login(String username, String password){
        if (!StringUtils.isEmpty(username) && username.equals(usernameConfig)
                && !StringUtils.isEmpty(password) && password.equals(passwordConfig)) {
            return "success";
        }else {
            return "fail";
        }
    }
}
