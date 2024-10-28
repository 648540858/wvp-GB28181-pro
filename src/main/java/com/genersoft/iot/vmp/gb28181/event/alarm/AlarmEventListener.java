package com.genersoft.iot.vmp.gb28181.event.alarm;

import com.genersoft.iot.vmp.gb28181.bean.SSEMessage;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
@Slf4j
@Component
public class AlarmEventListener implements ApplicationListener<AlarmEvent> {

    private static final Map<String, PrintWriter> SSE_CACHE = new ConcurrentHashMap<>();

    public void addSseEmitter(String browserId, PrintWriter writer) {
        SSE_CACHE.put(browserId, writer);
        log.info("[SSE推送] 连接已建立, 浏览器 ID: {}, 当前在线数: {}", browserId, SSE_CACHE.size());
    }

    public void removeSseEmitter(String browserId, PrintWriter writer) {
        SSE_CACHE.remove(browserId, writer);
        log.info("[SSE推送] 连接已断开, 浏览器 ID: {}, 当前在线数: {}", browserId, SSE_CACHE.size());
    }

    @Override
    public void onApplicationEvent(@NotNull AlarmEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("设备报警事件触发, deviceId: {}, {}", event.getAlarmInfo().getDeviceId(), event.getAlarmInfo().getAlarmDescription());
        }

        log.info("设备报警事件触发, deviceId: {}, {}", event.getAlarmInfo().getDeviceId(), event.getAlarmInfo().getAlarmDescription());


        String msg = "<strong>设备：</strong> <i>" + event.getAlarmInfo().getDeviceId() + "</i>"
                + "<br><strong>通道编号：</strong> <i>" + event.getAlarmInfo().getChannelId() + "</i>"
                + "<br><strong>报警描述：</strong> <i>" + event.getAlarmInfo().getAlarmDescription() + "</i>"
                + "<br><strong>报警时间：</strong> <i>" + event.getAlarmInfo().getAlarmTime() + "</i>";

        for (Iterator<Map.Entry<String, PrintWriter>> it = SSE_CACHE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, PrintWriter> response = it.next();

            try {
                PrintWriter writer = response.getValue();

                if (writer.checkError()) {
                    it.remove();
                    continue;
                }

                String sseMsg = "event:message\n" +
                        "data:" + msg + "\n" +
                        "\n";
                System.out.println(
                        SSEMessage.getInstance("message", event.getAlarmInfo()).ecode()
                );
                writer.write(SSEMessage.getInstance("message", event.getAlarmInfo()).ecode());
                writer.flush();
            } catch (Exception e) {
                log.error("[发送SSE] 失败", e);
                it.remove();
            }
        }
    }
}
