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
    public String devices(String name, String passwd){
        if (!StringUtils.isEmpty(name) && name.equals(username)
                && !StringUtils.isEmpty(passwd) && passwd.equals(password)) {
            return "success";
        }else {
            return "fail";
        }
    }
}
