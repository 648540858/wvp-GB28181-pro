package com.genersoft.iot.vmp.gb28181.event.alarm;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 报警事件监听器.
 *
 * @author lawrencehj
 * @author <a href="mailto:xiaoQQya@126.com">xiaoQQya</a>
 * @since 2021/01/20
 */
@Component
public class AlarmEventListener implements ApplicationListener<AlarmEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AlarmEventListener.class);

    private static final Map<String, PrintWriter> SSE_CACHE = new ConcurrentHashMap<>();

    public void addSseEmitter(String browserId, PrintWriter writer) {
        SSE_CACHE.put(browserId, writer);
        logger.info("SSE 在线数量: {}", SSE_CACHE.size());
    }

    public void removeSseEmitter(String browserId, PrintWriter writer) {
        SSE_CACHE.remove(browserId, writer);
        logger.info("SSE 在线数量: {}", SSE_CACHE.size());
    }

    @Override
    public void onApplicationEvent(@NotNull AlarmEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("设备报警事件触发, deviceId: {}, {}", event.getAlarmInfo().getDeviceId(), event.getAlarmInfo().getAlarmDescription());
        }

        String msg = "<strong>设备编号：</strong> <i>" + event.getAlarmInfo().getDeviceId() + "</i>"
                + "<br><strong>通道编号：</strong> <i>" + event.getAlarmInfo().getChannelId() + "</i>"
                + "<br><strong>报警描述：</strong> <i>" + event.getAlarmInfo().getAlarmDescription() + "</i>"
                + "<br><strong>报警时间：</strong> <i>" + event.getAlarmInfo().getAlarmTime() + "</i>";

        for (Iterator<Map.Entry<String, PrintWriter>> it = SSE_CACHE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PrintWriter> response = it.next();
            logger.info("推送到 SSE 连接, 浏览器 ID: {}", response.getKey());
            try {
                PrintWriter writer = response.getValue();

                if (writer.checkError()) {
                    it.remove();
                    continue;
                }

                String sseMsg = "event:message\n" +
                        "data:" + msg + "\n" +
                        "\n";
                writer.write(sseMsg);
                writer.flush();
            } catch (Exception e) {
                it.remove();
            }
        }
    }
}
