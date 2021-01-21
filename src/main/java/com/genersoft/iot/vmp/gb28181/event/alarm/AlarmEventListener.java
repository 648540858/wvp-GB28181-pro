package com.genersoft.iot.vmp.gb28181.event.alarm;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @description: 报警事件监听
 * @author: lawrencehj
 * @data: 2021-01-20
 */

@Component
public class AlarmEventListener implements ApplicationListener<AlarmEvent> {

    private final static Logger logger = LoggerFactory.getLogger(AlarmEventListener.class);

    private static Map<String, SseEmitter> sseEmitters = new Hashtable<>();

    public void addSseEmitters(String browserId, SseEmitter sseEmitter) {
        sseEmitters.put(browserId, sseEmitter);
    }

    @Override
    public void onApplicationEvent(AlarmEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("设备报警事件触发，deviceId：" + event.getAlarmInfo().getDeviceId() + ", "
                    + event.getAlarmInfo().getAlarmDescription());
        }
        String msg = "<strong>设备编码：</strong> <i>" + event.getAlarmInfo().getDeviceId() + "</i>"
                    + "<br><strong>报警描述：</strong> <i>" + event.getAlarmInfo().getAlarmDescription() + "</i>"
                    + "<br><strong>报警时间：</strong> <i>" + event.getAlarmInfo().getAlarmTime() + "</i>"
                    + "<br><strong>报警位置：</strong> <i>" + event.getAlarmInfo().getLongitude() + "</i>"
                    + ", <i>" + event.getAlarmInfo().getLatitude() + "</i>";

        for (Iterator<Map.Entry<String, SseEmitter>> it = sseEmitters.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, SseEmitter> emitter = it.next();
            logger.info("推送到SSE连接，浏览器ID: " + emitter.getKey());
            try {
                emitter.getValue().send(msg);
            } catch (IOException | IllegalStateException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("SSE连接已关闭");
                }
                // 移除已关闭的连接
                it.remove();
                // e.printStackTrace();
            }
        }
    }
}
