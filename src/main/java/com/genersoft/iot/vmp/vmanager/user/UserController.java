package com.genersoft.iot.vmp.vmanager.user;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "用户管理")
@CrossOrigin
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Value("${auth.username}")
    private String usernameConfig;

    @Value("${auth.password}")
    private String passwordConfig;

    @ApiOperation("登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码（32未md5加密）", dataTypeClass = String.class),
    })
    @GetMapping("/login")
    public String login(String username, String password){
        if (!StringUtils.isEmpty(username) && username.equals(usernameConfig)
                && !StringUtils.isEmpty(password) && password.equals(passwordConfig)) {
            return "success";
        }else {
            return "fail";
        }
    }
}
