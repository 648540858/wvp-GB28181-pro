package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.conf.DynamicTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SseSessionManager {

    private static final Map<String, SseEmitter> sseSessionMap = new ConcurrentHashMap<>();

    @Autowired
    private DynamicTask dynamicTask;

    public SseEmitter conect(String browserId){
        SseEmitter sseEmitter = new SseEmitter(0L);
        sseEmitter.onError((err)-> {
            log.error("[SSE推送] 连接错误, 浏览器 ID: {}, {}", browserId, err.getMessage());
            sseSessionMap.remove(browserId);
            sseEmitter.completeWithError(err);
        });

//        sseEmitter.onTimeout(() -> {
//            log.info("[SSE推送] 连接超时, 浏览器 ID: {}", browserId);
//            sseSessionMap.remove(browserId);
//            sseEmitter.complete();
//            dynamicTask.stop(key);
//        });

        sseEmitter.onCompletion(() -> {
            log.info("[SSE推送] 连接结束, 浏览器 ID: {}", browserId);
            sseSessionMap.remove(browserId);
        });

        sseSessionMap.put(browserId, sseEmitter);

        log.info("[SSE推送] 连接已建立, 浏览器 ID: {}, 当前在线数: {}", browserId, sseSessionMap.size());
        return sseEmitter;
    }

    @Scheduled(fixedRate = 1000)   //每1秒执行一次
    public void execute(){
        if (sseSessionMap.isEmpty()){
            return;
        }
        sendForAll("keepalive", "alive");
    }


    public void sendForAll(String event, Object data) {
        for (String browserId : sseSessionMap.keySet()) {
            SseEmitter sseEmitter = sseSessionMap.get(browserId);
            if (sseEmitter == null) {
                continue;
            };
            try {
                sseEmitter.send(SseEmitter.event().name(event).data(data));
            } catch (Exception e) {
                log.error("[SSE推送] 发送失败: {}", e.getMessage());
                sseSessionMap.remove(browserId);
                sseEmitter.completeWithError(e);
            }
        }
    }
}
