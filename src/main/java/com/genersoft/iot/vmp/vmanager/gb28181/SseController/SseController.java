package com.genersoft.iot.vmp.vmanager.gb28181.SseController;

import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEventListener;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @description: SSE推送
 * @author: lawrencehj
 * @data: 2021-01-20
 */
@Api(tags = "SSE推送")
@CrossOrigin
@Controller
@RequestMapping("/api")
public class SseController {
    @Autowired
    AlarmEventListener alarmEventListener;

    @ApiOperation("浏览器推送")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "browserId", value = "浏览器ID", dataTypeClass = String.class),
    })
    @PostMapping("/emit")
    public SseEmitter emit(@RequestParam String browserId) {
        final SseEmitter sseEmitter = new SseEmitter(0L);
        try {
            alarmEventListener.addSseEmitters(browserId, sseEmitter);
        }catch (Exception e){
            sseEmitter.completeWithError(e);
        }
        return sseEmitter;
    }
}
