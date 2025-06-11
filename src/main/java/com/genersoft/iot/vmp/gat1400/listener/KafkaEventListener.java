package com.genersoft.iot.vmp.gat1400.listener;

import com.alibaba.fastjson.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cz.data.viid.framework.S3StorageService;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.VIIDFace;
import cz.data.viid.framework.domain.entity.VIIDMotorVehicle;
import cz.data.viid.framework.domain.entity.VIIDNonMotorVehicle;
import cz.data.viid.framework.domain.entity.VIIDPerson;
import cz.data.viid.framework.service.VIIDFaceService;
import cz.data.viid.framework.service.VIIDMotorVehicleService;
import cz.data.viid.framework.service.VIIDNonMotorVehicleService;
import cz.data.viid.framework.service.VIIDPersonService;
import cz.data.viid.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnExpression("${VIID_DB_STORAGE:false}")
public class KafkaEventListener {

    @Autowired
    VIIDFaceService viidFaceService;
    @Autowired
    VIIDPersonService viidPersonService;
    @Autowired
    VIIDMotorVehicleService viidMotorVehicleService;
    @Autowired
    VIIDNonMotorVehicleService viidNonMotorVehicleService;
    @Autowired
    S3StorageService storageService;

    @KafkaListener(topicPattern = "^" + Constants.DEFAULT_TOPIC_PREFIX.FACE_RECORD + ".*"
            , groupId = Constants.KAFKA_CONSUMER.APP_DEFAULT_GROUP, autoStartup = "true")
    public void faceHandler(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.info("kafka消费者-接收到人脸数据: {}", records.size());
        List<VIIDFace> faceList = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            try {
                String value = record.value();
                VIIDFace face = JSONObject.parseObject(value, VIIDFace.class);
                if (Objects.isNull(face))
                    continue;
                if (StringUtils.isNotBlank(face.getFaceAppearTime())) {
                    face.setDataTime(DateUtil.parseViidDateTime(face.getFaceAppearTime()));
                }
                storageService.subImageListStorage(face.getDeviceId(), face.getSubImageList());
                face.setDataTime(LocalDateTime.now());
                faceList.add(face);
            } catch (Exception e) {
                log.warn("人脸数据处理失败: {}", e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(faceList)) {
            viidFaceService.saveOrUpdateBatch(faceList);
        }
        ack.acknowledge();
    }

    @KafkaListener(topicPattern = "^" + Constants.DEFAULT_TOPIC_PREFIX.PERSON_RECORD + ".*"
            , groupId = Constants.KAFKA_CONSUMER.APP_DEFAULT_GROUP, autoStartup = "true")
    public void personHandler(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("接收到人员数据: {}", records.size());
        List<VIIDPerson> personList = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            try {
                String value = record.value();
                VIIDPerson person = JSONObject.parseObject(value, VIIDPerson.class);
                if (Objects.isNull(person))
                    continue;
                if (StringUtils.isNotBlank(person.getPersonAppearTime())) {
                    person.setDataTime(DateUtil.parseViidDateTime(person.getPersonAppearTime()));
                }
                storageService.subImageListStorage(person.getDeviceId(), person.getSubImageList());
                person.setDataTime(LocalDateTime.now());
                personList.add(person);
            } catch (Exception e) {
                log.warn("人员数据处理失败: {}", e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(personList)) {
            viidPersonService.saveOrUpdateBatch(personList);
        }
        ack.acknowledge();
    }

    @KafkaListener(topicPattern = "^" + Constants.DEFAULT_TOPIC_PREFIX.MOTOR_VEHICLE + ".*"
            , groupId = Constants.KAFKA_CONSUMER.APP_DEFAULT_GROUP, autoStartup = "true")
    public void motorVehicleHandler(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("接收到机动车数据: {}", records.size());
        List<VIIDMotorVehicle> motorVehicleList = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            try {
                String value = record.value();
                VIIDMotorVehicle motorVehicle = JSONObject.parseObject(value, VIIDMotorVehicle.class);
                if (Objects.isNull(motorVehicle))
                    continue;
                if (StringUtils.isNotBlank(motorVehicle.getPassTime())) {
                    motorVehicle.setDataTime(DateUtil.parseViidDateTime(motorVehicle.getPassTime()));
                }
                storageService.subImageListStorage(motorVehicle.getDeviceId(), motorVehicle.getSubImageList());
                motorVehicle.setDataTime(LocalDateTime.now());
                motorVehicleList.add(motorVehicle);
            } catch (Exception e) {
                log.warn("机动车数据处理失败: {}", e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(motorVehicleList)) {
            viidMotorVehicleService.saveOrUpdateBatch(motorVehicleList);
        }
        ack.acknowledge();
    }

    @KafkaListener(topicPattern = "^" + Constants.DEFAULT_TOPIC_PREFIX.NON_MOTOR_VEHICLE + ".*"
            , groupId = Constants.KAFKA_CONSUMER.APP_DEFAULT_GROUP, autoStartup = "true")
    public void nonMotorVehicleHandler(List<ConsumerRecord<String, String>> records, Acknowledgment ack) {
        log.debug("接收到非机动车数据: {}", records.size());
        List<VIIDNonMotorVehicle> motorVehicleList = new ArrayList<>();
        for (ConsumerRecord<String, String> record : records) {
            try {
                String value = record.value();
                VIIDNonMotorVehicle motorVehicle = JSONObject.parseObject(value, VIIDNonMotorVehicle.class);
                if (Objects.isNull(motorVehicle))
                    continue;
                if (StringUtils.isNotBlank(motorVehicle.getAppearTime())) {
                    motorVehicle.setDataTime(DateUtil.parseViidDateTime(motorVehicle.getAppearTime()));
                }
                storageService.subImageListStorage(motorVehicle.getDeviceId(), motorVehicle.getSubImageList());
                motorVehicle.setDataTime(LocalDateTime.now());
                motorVehicleList.add(motorVehicle);
            } catch (Exception e) {
                log.warn("非机动车数据处理失败: {}", e.getMessage());
            }
        }
        if (CollectionUtils.isNotEmpty(motorVehicleList)) {
            viidNonMotorVehicleService.saveOrUpdateBatch(motorVehicleList);
        }
        ack.acknowledge();
    }

}
