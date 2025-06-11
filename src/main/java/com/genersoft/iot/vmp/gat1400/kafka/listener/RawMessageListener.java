package com.genersoft.iot.vmp.gat1400.kafka.listener;

import java.util.List;

import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.vo.SubscribeNotificationRequest;

/**
 * 测试用监听器
 */
public class RawMessageListener extends AbstractMessageListener<String> {

    public RawMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.RAW.getValue());
    }

    @Override
    public String messageConverter(String value) {
        return value;
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<String> partition) {
        return new SubscribeNotificationRequest();
    }
}
