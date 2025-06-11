package com.genersoft.iot.vmp.gat1400.kafka.listener;

import com.alibaba.fastjson.JSONObject;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cz.data.viid.be.domain.vo.MotorVehicleRequest;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.MotorVehicleListObject;
import cz.data.viid.framework.domain.dto.MotorVehicleObject;
import cz.data.viid.framework.domain.dto.SubscribeNotificationObject;
import cz.data.viid.framework.domain.dto.SubscribeNotifications;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.SubscribeNotificationRequest;
import cz.data.viid.rpc.MotorVehicleClient;
import cz.data.viid.utils.JsonCommon;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

/**
 * 车辆信息消息监听器
 * from 消费默认topic的过车数据
 * to PublishEntity定义的视图库回调
 */
@Slf4j
public class MotorVehicleMessageListener extends AbstractMessageListener<MotorVehicleObject> {
    protected MotorVehicleClient motorVehicleClient;

    public MotorVehicleMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.PLATE_INFO.getValue());
    }

    @Override
    public void configure(VIIDServer setting) {
        super.configure(setting);
        this.motorVehicleClient = SpringContextHolder.getBean(MotorVehicleClient.class);
    }

    @Override
    public MotorVehicleObject messageConverter(String value) {
        MotorVehicleObject object;
        if ("1".equals(publish.getResultImageDeclare())) {
            object = JsonCommon.parseObject(value, MotorVehicleObject.class, false);
        } else {
            object = JSONObject.parseObject(value, MotorVehicleObject.class);
        }
        return Optional.ofNullable(object)
                .map(MotorVehicleObject::validateMotorVehicle)
                .map(ele -> wrap(ele, MotorVehicleObject::getSubImageList))
                .orElse(null);
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<MotorVehicleObject> partitions) {
        SubscribeNotifications subscribeNotifications = new SubscribeNotifications();
        List<SubscribeNotificationObject> notificationObjects = Optional.of(partitions)
                .map(this::MotorVehicleObjectsBuilder)
                .map(this::SubscribeNotificationObjectBuilder)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        subscribeNotifications.setSubscribeNotificationObject(notificationObjects);
        SubscribeNotificationRequest notificationRequest = new SubscribeNotificationRequest();
        notificationRequest.setSubscribeNotificationListObject(subscribeNotifications);
        return notificationRequest;
    }

    @Override
    protected void notificationRequest(List<MotorVehicleObject> collect) throws IOException {
        super.notificationRequest(collect);
        if (Constants.VIID_SERVER.TRANSMISSION.DEVICE.equals(server.getTransmission())) {
            MotorVehicleRequest request = new MotorVehicleRequest();
            MotorVehicleListObject objects = new MotorVehicleListObject();
            objects.setMotorVehicleObject(collect);
            request.setMotorVehicleListObject(objects);
            motorVehicleClient.addMotorVehicle(URI.create(publish.getReceiveAddr()), request);
        }
    }

    private SubscribeNotificationObject SubscribeNotificationObjectBuilder(MotorVehicleListObject objects) {
        SubscribeNotificationObject model = new SubscribeNotificationObject();
        model.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        model.setSubscribeID(publish.getSubscribeId());
        model.setTitle(publish.getTitle());
        model.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        model.setInfoIDs(objects.getMotorVehicleObject()
                .stream()
                .map(MotorVehicleObject::getMotorVehicleID)
                .collect(Collectors.joining(","))
        );
        model.setMotorVehicleObjectList(objects);
        return model;
    }

    private MotorVehicleListObject MotorVehicleObjectsBuilder(List<MotorVehicleObject> data) {
        MotorVehicleListObject model = new MotorVehicleListObject();
        model.setMotorVehicleObject(data);
        return model;
    }
}
