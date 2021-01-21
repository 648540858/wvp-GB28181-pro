package com.genersoft.iot.vmp.gb28181.event.alarm;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
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

    private static SseEmitter emitter = new SseEmitter();

    public void addSseEmitters(SseEmitter sseEmitter) {
        emitter = sseEmitter;
    }

    @Override
    public void onApplicationEvent(AlarmEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("设备报警事件触发，deviceId：" + event.getAlarmInfo().getDeviceId() + ", "
                    + event.getAlarmInfo().getAlarmDescription());
        }
        try {
            String msg = "<strong>设备编码：</strong> <i>" + event.getAlarmInfo().getDeviceId() + "</i>"
                        + "<br><strong>报警描述：</strong> <i>" + event.getAlarmInfo().getAlarmDescription() + "</i>"
                        + "<br><strong>报警时间：</strong> <i>" + event.getAlarmInfo().getAlarmTime() + "</i>"
                        + "<br><strong>定位经度：</strong> <i>" + event.getAlarmInfo().getLongitude() + "</i>"
                        + "<br><strong>定位纬度：</strong> <i>" + event.getAlarmInfo().getLatitude() + "</i>";
            emitter.send(msg);
        } catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("SSE 通道已关闭");
            }
            // e.printStackTrace();
        }
    }
}
