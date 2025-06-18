package com.genersoft.iot.vmp.gat1400.kafka.listener;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.FaceObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.FaceObjectList;
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
 * 人脸信息消息监听器
 * from 消费kafka topic的人脸数据
 * to PublishEntity定义的视图库回调
 */
public class FaceMessageListener extends AbstractMessageListener<FaceObject> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public FaceMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.FACE_INFO.getValue());
    }

    @Override
    public FaceObject messageConverter(String value) {
        return Optional.of(value)
                .map(ele -> JSONObject.parseObject(value, FaceObject.class))
                .filter(ele -> StringUtils.isNotBlank(ele.getFaceID()))
                .map(ele -> wrap(ele, FaceObject::getSubImageList))
                .orElse(null);
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<FaceObject> partition) {
        FaceObjectList faceObjectList = new FaceObjectList();
        faceObjectList.setFaceObject(partition);

        SubscribeNotificationObject notificationObject = new SubscribeNotificationObject();
        notificationObject.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        notificationObject.setSubscribeID(publish.getSubscribeId());
        notificationObject.setTitle(publish.getTitle());
        notificationObject.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        notificationObject.setInfoIDs(partition
                .stream()
                .map(FaceObject::getFaceID)
                .collect(Collectors.joining(","))
        );
        notificationObject.setFaceObjectList(faceObjectList);

        SubscribeNotifications subscribeNotifications = new SubscribeNotifications();
        subscribeNotifications.setSubscribeNotificationObject(Collections.singletonList(notificationObject));

        SubscribeNotificationRequest notificationRequest = new SubscribeNotificationRequest();
        notificationRequest.setSubscribeNotificationListObject(subscribeNotifications);
        return notificationRequest;
    }
}
