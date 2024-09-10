package com.genersoft.iot.vmp.vmanager;

import com.genersoft.iot.vmp.conf.security.JwtUtils;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private HookSubscribe subscribe;


    @GetMapping("/hook/list")
    @Operation(summary = "查询角色", security = @SecurityRequirement(name = JwtUtils.HEADER))
    public List<Hook> all(){
        return subscribe.getAll();
    }
}
