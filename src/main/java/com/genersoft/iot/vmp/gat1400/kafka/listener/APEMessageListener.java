package com.genersoft.iot.vmp.gat1400.kafka.listener;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.APEObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.DeviceObjectList;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotifications;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;
import com.genersoft.iot.vmp.gat1400.framework.service.APEDeviceService;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 采集设备消息监听器
 * from 消费kafka topic的采集设备
 * to PublishEntity定义的视图库回调
 */
public class APEMessageListener extends AbstractMessageListener<APEObject> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public APEMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.DEVICE.getValue());
    }

    @Override
    public APEObject messageConverter(String value) {
        return Optional.of(JSONObject.parseObject(value, APEObject.class))
                .filter(obj -> StringUtils.isNotBlank(obj.getApeID()))
                .orElse(null);
    }

    @Override
    public void scheduler() {
        if (checkConsumeCondition()) {
            APEDeviceService service = SpringContextHolder.getBean(APEDeviceService.class);
            List<APEObject> collect = service.list().stream().map(StructCodec::castApeObject).collect(Collectors.toList());
            try {
                super.notificationRequest(collect);
            } catch (IOException e) {
                log.warn("推送APE设备出错: {}", e.getMessage());
            }
        }
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<APEObject> partition) {
        DeviceObjectList deviceObjectList = new DeviceObjectList();
        deviceObjectList.setAPEObject(partition);

        SubscribeNotificationObject notificationObject = new SubscribeNotificationObject();
        notificationObject.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        notificationObject.setSubscribeID(publish.getSubscribeId());
        notificationObject.setTitle(publish.getTitle());
        notificationObject.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        notificationObject.setInfoIDs(partition
                .stream()
                .map(APEObject::getApeID)
                .collect(Collectors.joining(","))
        );
        notificationObject.setDeviceList(deviceObjectList);

        SubscribeNotifications subscribeNotifications = new SubscribeNotifications();
        subscribeNotifications.setSubscribeNotificationObject(Collections.singletonList(notificationObject));

        SubscribeNotificationRequest notificationRequest = new SubscribeNotificationRequest();
        notificationRequest.setSubscribeNotificationListObject(subscribeNotifications);
        return notificationRequest;
    }
}
