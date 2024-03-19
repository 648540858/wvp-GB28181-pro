package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.media.event.MediaServerChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

/**
 * 管理zlm流媒体节点的状态
 */
public class ZLMMediaServerStatusManger {

    private final static Logger logger = LoggerFactory.getLogger(ZLMMediaServerStatusManger.class);

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

    }
}
