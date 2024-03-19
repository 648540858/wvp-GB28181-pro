package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.media.event.MediaServerChangeEvent;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.event.HookZlmServerStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理zlm流媒体节点的状态
 */
public class ZLMMediaServerStatusManger {

    private final static Logger logger = LoggerFactory.getLogger(ZLMMediaServerStatusManger.class);

    private Map<Object, MediaServerItem> offlineZlmMap = new ConcurrentHashMap<>();

    private final String type = "zlm";

    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(MediaServerChangeEvent event) {
        if (event.getMediaServerItem() == null
                || !type.equals(event.getMediaServerItem().getType())
                || event.getMediaServerItem().isStatus()) {
            return;
        }
        logger.info("[ZLM 待上线节点变化] ID：" + event.getMediaServerItem().getId());
        offlineZlmMap.put(event.getMediaServerItem().getId(), event.getMediaServerItem());
    }

    @Async("taskExecutor")
    @EventListener
    public void onApplicationEvent(HookZlmServerStartEvent event) {
        if (event.getMediaServerItem() == null
                || !type.equals(event.getMediaServerItem().getType())
                || event.getMediaServerItem().isStatus()) {
            return;
        }
        logger.info("[ZLM-HOOK事件-服务启动] ID：" + event.getMediaServerItem().getId());
        offlineZlmMap.remove(event.getMediaServerItem().getId());
    }

    @Scheduled(fixedDelay = )   //每天的0点执行
    public void execute(){

    }

}
