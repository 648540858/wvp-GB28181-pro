package com.genersoft.iot.vmp.vmanager.user;

import com.genersoft.iot.vmp.conf.security.SecurityUtils;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.security.sasl.AuthenticationException;

@Api(tags = "用户管理")
@CrossOrigin
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private IUserService userService;

    @ApiOperation("登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "密码（32位md5加密）", dataTypeClass = String.class),
    })
    @GetMapping("/login")
    public String login(String username, String password){
        LoginUser user;
        try {
            user = SecurityUtils.login(username, password, authenticationManager);
        } catch (AuthenticationException e) {
            e.printStackTrace();
            return "fail";
        }
        if (user != null) {
            return "success";
        }else {
            return "fail";
        }
    }

    @ApiOperation("修改密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "username", value = "用户名", dataTypeClass = String.class),
            @ApiImplicitParam(name = "oldpassword", value = "旧密码（已md5加密的密码）", dataTypeClass = String.class),
            @ApiImplicitParam(name = "password", value = "新密码（未md5加密的密码）", dataTypeClass = String.class),
    })
    @PostMapping("/changePassword")
    public String changePassword(String oldpassword, String password){
        // 获取当前登录用户id
        String username = SecurityUtils.getUserInfo().getUsername();
        LoginUser user = null;
        try {
            user = SecurityUtils.login(username, oldpassword, authenticationManager);
            if (user != null) {
                int userId = SecurityUtils.getUserId();
                boolean result = userService.changePassword(userId, DigestUtils.md5DigestAsHex(password.getBytes()));
                if (result) {
                    return "success";
                }
            }
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        return "fail";
    }
}
