package com.genersoft.iot.vmp.web;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    @Value("${auth.username}")
    private String username;

    @Value("${auth.password}")
    private String password;

    @RequestMapping("/login")
    public Object devices(String username, String password){
        if (!StringUtils.isEmpty(username) && username.equals(username)
                && !StringUtils.isEmpty(password) && password.equals(password)) {
            return "success";
        }else {
            return "fait";
        }
    }
}
