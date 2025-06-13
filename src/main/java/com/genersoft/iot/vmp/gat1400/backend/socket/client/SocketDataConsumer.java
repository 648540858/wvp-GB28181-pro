package com.genersoft.iot.vmp.gat1400.backend.socket.client;


import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.gat1400.framework.SpringContextHolder;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;

import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SocketDataConsumer implements Consumer<String> {

    private final String deviceId;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private SocketDataConsumer(String deviceId, KafkaTemplate<String, String> template) {
        this.deviceId = deviceId;
        this.kafkaTemplate = template;
    }

    @Override
    public void accept(String text) {
        try {
            JSONObject payload = JSONObject.parseObject(text);
            String type = payload.getString("type");
            JSONObject data = payload.getJSONObject("data");
            String topic = null;
            if (Constants.SubscribeDetail.DEVICE.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.APE_DEVICE, deviceId);
            } else if (Constants.SubscribeDetail.TOLLGATE.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.TOLLGATE_DEVICE, deviceId);
            } else if (Constants.SubscribeDetail.PERSON_INFO.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.PERSON_RECORD, deviceId);
            } else if (Constants.SubscribeDetail.FACE_INFO.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.FACE_RECORD, deviceId);
            } else if (Constants.SubscribeDetail.PLATE_INFO.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.MOTOR_VEHICLE, deviceId);
            } else if (Constants.SubscribeDetail.PLATE_MIRCO_INFO.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.NON_MOTOR_VEHICLE, deviceId);
            } else if (Constants.SubscribeDetail.RAW.equalsValue(type)) {
                topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.RAW, deviceId);
            }
            if (StringUtils.isNotBlank(topic)) {
                kafkaTemplate.send(topic, data.toJSONString());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static SocketDataConsumer from(String deviceId) {
        KafkaTemplate<String, String> kafkaTemplate = SpringContextHolder.getBean(KafkaTemplate.class);
        return new SocketDataConsumer(deviceId, kafkaTemplate);
    }
}
