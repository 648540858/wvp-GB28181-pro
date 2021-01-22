package com.genersoft.iot.vmp.vmanager.SseController;

import com.genersoft.iot.vmp.gb28181.event.alarm.AlarmEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @description: SSE推送
 * @author: lawrencehj
 * @data: 2021-01-20
 */

@Controller
@RequestMapping("/api")
public class SseController {
    @Autowired 
    AlarmEventListener alarmEventListener;
    
   	//设置响应
    @RequestMapping("/emit")
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
