package com.genersoft.iot.vmp.gat1400.fontend.api;

import com.alibaba.fastjson.JSONObject;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import javax.annotation.Resource;
import javax.validation.Valid;

import cz.data.viid.fe.domain.AdminLoginDto;
import cz.data.viid.fe.service.PlatformService;
import cz.data.viid.framework.domain.core.BaseResponse;
import cz.data.viid.framework.domain.core.SimpleDataResponse;
import cz.data.viid.framework.domain.vo.LoginUserInfoVo;

@RestController
public class PlatformController {

    @Resource
    PlatformService platformService;

    @PostMapping("/api/admin/login")
    public Object login(@Valid AdminLoginDto request) {
        LoginUserInfoVo userInfo = platformService.login(request);
        if (Objects.isNull(userInfo)) {
            return BaseResponse.error(500, "账号密码错误");
        }
        return userInfo;
    }

    @DeleteMapping("/api/admin/logout")
    public BaseResponse logout() {
        platformService.logout();
        return BaseResponse.success();
    }

    @GetMapping("/api/admin/route")
    public SimpleDataResponse<JSONObject> getInfo() {
        return new SimpleDataResponse<>(platformService.getRoutes());
    }

    @PostMapping("/api/admin/login/logs")
    public BaseResponse loginLog() {
        return BaseResponse.success();
    }

    @GetMapping("/api/admin/viid/metrics")
    public SimpleDataResponse<JSONObject> metric() {
        return new SimpleDataResponse<>(platformService.metrics());
    }
}
