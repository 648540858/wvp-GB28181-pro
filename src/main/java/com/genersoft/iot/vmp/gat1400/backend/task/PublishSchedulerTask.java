package com.genersoft.iot.vmp.gat1400.backend.task;

import com.genersoft.iot.vmp.gat1400.backend.service.IPublishService;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDServer;
import com.genersoft.iot.vmp.gat1400.framework.service.VIIDServerService;
import com.genersoft.iot.vmp.gat1400.kafka.listener.APEMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.CustomMessageListener;
import com.genersoft.iot.vmp.gat1400.kafka.listener.TollgateMessageListener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PublishSchedulerTask {

    @Autowired
    VIIDServerService viidServerService;
    @Autowired
    IPublishService publishService;

    @Scheduled(cron = "0 5 0 * * ?")
    public void scheduler() {
        VIIDServer setting = viidServerService.getCurrentServer();
        for (VIIDPublish publish : publishService.list()) {
            String[] strings = StringUtils.split(publish.getSubscribeDetail(), ",");
            Set<String> subscribeDetails = Arrays.stream(strings).collect(Collectors.toSet());
            for (String subscribeDetail : subscribeDetails) {
                try {
                    CustomMessageListener messageListener = null;
                    if (Constants.SubscribeDetail.TOLLGATE.equalsValue(subscribeDetail)) {
                        messageListener = new TollgateMessageListener(publish);
                    } else if (Constants.SubscribeDetail.DEVICE.equalsValue(subscribeDetail)) {
                        messageListener = new APEMessageListener(publish);
                    }
                    if (Objects.nonNull(messageListener)) {
                        messageListener.configure(setting);
                        messageListener.scheduler();
                    }
                } catch (Exception e) {
                    if (Constants.SubscribeDetail.TOLLGATE.equalsValue(subscribeDetail)) {
                        log.warn("定时推送卡口数据失败: {}", e.getMessage());
                    } else if (Constants.SubscribeDetail.DEVICE.equalsValue(subscribeDetail)) {
                        log.warn("定时推送APE设备数据失败: {}", e.getMessage());
                    }
                }
            }

        }
    }
}
