package com.genersoft.iot.vmp.gat1400.kafka.listener;

import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;

import java.util.List;


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
