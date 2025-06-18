package com.genersoft.iot.vmp.gat1400.kafka.listener;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotifications;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.TollgateObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.TollgateObjectList;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.TollgateDevice;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDPublish;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;
import com.genersoft.iot.vmp.gat1400.framework.service.TollgateDeviceService;
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
 * 视频目录信息消息监听器
 * from 消费默认topic的过车数据
 * to PublishEntity定义的视图库回调
 */
public class TollgateMessageListener extends AbstractMessageListener<TollgateObject> {
    private final Logger log = LoggerFactory.getLogger(getClass());

    public TollgateMessageListener(VIIDPublish publish) {
        super(publish, Constants.SubscribeDetail.TOLLGATE.getValue());
    }


    @Override
    public TollgateObject messageConverter(String value) {
        return Optional.of(value)
                .map(ele -> JSONObject.parseObject(ele, TollgateObject.class))
                .filter(ele -> StringUtils.isNotBlank(ele.getTollgateID()))
                .orElse(null);
    }

    @Override
    public void scheduler() {
        if (checkConsumeCondition()) {
            TollgateDeviceService service = SpringContextHolder.getBean(TollgateDeviceService.class);
            List<TollgateDevice> devices = service.list();
            List<TollgateObject> collect = devices.stream().map(StructCodec::castTollgateObject).collect(Collectors.toList());
            try {
                super.notificationRequest(collect);
            } catch (IOException e) {
                log.warn("推送卡口设备出错: {}", e.getMessage());
            }
        }
    }

    @Override
    public SubscribeNotificationRequest packHandler(List<TollgateObject> partition) {
        long size = partition.size();
        SubscribeNotifications subscribeNotifications = new SubscribeNotifications();
        List<SubscribeNotificationObject> notificationObjects = Optional.of(partition)
                .map(this::TollgateObjectBuilder)
                .map(this::SubscribeNotificationObjectBuilder)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        subscribeNotifications.setSubscribeNotificationObject(notificationObjects);

        SubscribeNotificationRequest notificationRequest = new SubscribeNotificationRequest();
        notificationRequest.setSubscribeNotificationListObject(subscribeNotifications);
        return notificationRequest;
    }

    private TollgateObjectList TollgateObjectBuilder(List<TollgateObject> data) {
        TollgateObjectList model = new TollgateObjectList();
        model.setTollgateObject(data);
        return model;
    }

    private SubscribeNotificationObject SubscribeNotificationObjectBuilder(TollgateObjectList objects) {
        SubscribeNotificationObject model = new SubscribeNotificationObject();
        model.setNotificationID(StructCodec.randomNotificationID(publish.getSubscribeId()));
        model.setSubscribeID(publish.getSubscribeId());
        model.setTitle(publish.getTitle());
        model.setTriggerTime(DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        model.setInfoIDs(objects.getTollgateObject()
                .stream()
                .map(TollgateObject::getTollgateID)
                .collect(Collectors.joining(","))
        );
        model.setTollgateObjectList(objects);
        return model;
    }
}
