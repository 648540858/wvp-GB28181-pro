package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.security.dto.LoginUser;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.dao.dto.User;
import com.genersoft.iot.vmp.utils.JwtUtil;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    @Autowired
    private IUserService userService;
    @Autowired
    private AuthenticationManager authenticationManager;


    @RequestMapping("/login")
    public Map<String, Object> devices(String name, String passwd){
        User user = userService.getUser(name, passwd);
        Map<String, Object> map = new HashMap<>();
        map.put("code","0");
        map.put("msg","登陆失败");
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(name, passwd);
            Authentication authenticate = authenticationManager.authenticate(authenticationToken);
            if (Objects.isNull(authenticate)) {
                //用户名密码错误
                return map;
            }
            String token = JwtUtil.createJWT(name);
        //把token和用户信息存到redis中
            RedisUtil.set("Token_" + name, token);
            Map<String, Object> loginUsermap = new HashMap<>();
                loginUsermap.put("user",user);
                 loginUsermap.put("loginTime", LocalDateTime.now());
            String s = JSON.toJSONString(loginUsermap);
            RedisUtil.set("UserDetails_" + name, s);

            map.put("token", token);
            map.put("msg","登陆成功");
             SecurityContextHolder.getContext().setAuthentication(authenticate);

            return map;

    }
}
