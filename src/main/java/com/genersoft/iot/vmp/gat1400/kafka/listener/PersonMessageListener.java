package com.genersoft.iot.vmp.gat1400.kafka.listener;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.PersonObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotifications;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;
import com.genersoft.iot.vmp.gat1400.utils.StructCodec;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.PersonListObject;

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
 * 自动采集的人员消息监听器
 * from 消费kafka topic的自动采集的人员数据
 * to PublishEntity定义的视图库回调
 */
public class PersonMessageListener extends AbstractMessageListener<PersonObject> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public PersonMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.PERSON_INFO.getValue());
    }

    @Override
    public PersonObject messageConverter(String value) {
        return Optional.of(value)
                .map(ele -> JSONObject.parseObject(ele, PersonObject.class))
                .filter(ele -> StringUtils.isNotBlank(ele.getPersonID()))
                .map(ele -> wrap(ele, PersonObject::getSubImageList))
                .orElse(null);
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<PersonObject> partition) {
        PersonListObject personList = new PersonListObject();
        personList.setPersonObject(partition);

        SubscribeNotificationObject notificationObject = new SubscribeNotificationObject();
        notificationObject.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        notificationObject.setSubscribeID(publish.getSubscribeId());
        notificationObject.setTitle(publish.getTitle());
        notificationObject.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        notificationObject.setInfoIDs(partition
                .stream()
                .map(PersonObject::getPersonID)
                .collect(Collectors.joining(","))
        );
        notificationObject.setPersonObjectList(personList);

        SubscribeNotifications notifications = new SubscribeNotifications();
        notifications.setSubscribeNotificationObject(Collections.singletonList(notificationObject));

        SubscribeNotificationRequest notificationRequest = new SubscribeNotificationRequest();
        notificationRequest.setSubscribeNotificationListObject(notifications);
        return notificationRequest;
    }
}
