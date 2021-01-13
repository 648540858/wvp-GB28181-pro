package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.vmanager.play.PlayController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class UserController {

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);


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
