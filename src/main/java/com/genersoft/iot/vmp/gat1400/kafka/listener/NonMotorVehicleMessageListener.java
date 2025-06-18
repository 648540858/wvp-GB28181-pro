package com.genersoft.iot.vmp.gat1400.kafka.listener;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.NonMotorVehicle;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.NonMotorVehicleObjectList;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotifications;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 非机动车信息消息监听器
 * from 消费默认topic的非机动车数据
 * to PublishEntity定义的视图库回调
 */
public class NonMotorVehicleMessageListener extends AbstractMessageListener<NonMotorVehicle> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public NonMotorVehicleMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.PLATE_MIRCO_INFO.getValue());
    }

    @Override
    public NonMotorVehicle messageConverter(String value) {
        return Optional.of(value)
                .map(ele -> JSONObject.parseObject(ele, NonMotorVehicle.class))
                .filter(ele -> StringUtils.isNotBlank(ele.getNonMotorVehicleID()))
                .map(ele -> wrap(ele, NonMotorVehicle::getSubImageList))
                .orElse(null);
    }

    public SubscribeNotificationRequest packHandler(List<NonMotorVehicle> partition) {
        SubscribeNotifications subscribeNotifications = new SubscribeNotifications();
        List<SubscribeNotificationObject> notificationObjects = Optional.of(partition)
                .map(this::NonMotorVehicleListBuilder)
                .map(this::SubscribeNotificationObjectBuilder)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        subscribeNotifications.setSubscribeNotificationObject(notificationObjects);
        SubscribeNotificationRequest notificationRequest = new SubscribeNotificationRequest();
        notificationRequest.setSubscribeNotificationListObject(subscribeNotifications);
        return notificationRequest;
    }

    private SubscribeNotificationObject SubscribeNotificationObjectBuilder(NonMotorVehicleObjectList objects) {
        SubscribeNotificationObject model = new SubscribeNotificationObject();
        model.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        model.setSubscribeID(publish.getSubscribeId());
        model.setTitle(publish.getTitle());
        model.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        model.setInfoIDs(objects.getNonMotorVehicleObject()
                .stream()
                .map(NonMotorVehicle::getNonMotorVehicleID)
                .collect(Collectors.joining(","))
        );
        model.setNonMotorVehicleObjectList(objects);
        return model;
    }

    public NonMotorVehicleObjectList NonMotorVehicleListBuilder(List<NonMotorVehicle> data) {
        NonMotorVehicleObjectList model = new NonMotorVehicleObjectList();
        model.setNonMotorVehicleObject(data);
        return model;
    }
}
