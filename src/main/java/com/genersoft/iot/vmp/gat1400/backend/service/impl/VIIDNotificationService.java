package com.genersoft.iot.vmp.gat1400.backend.service.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;

import cz.data.viid.be.domain.dto.DispositionNotificationObject;
import cz.data.viid.be.service.IVIIDNotificationService;
import cz.data.viid.fe.security.SecurityContext;
import cz.data.viid.framework.S3StorageService;
import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.dto.APEObject;
import cz.data.viid.framework.domain.dto.DeviceObjectList;
import cz.data.viid.framework.domain.dto.FaceObject;
import cz.data.viid.framework.domain.dto.FaceObjectList;
import cz.data.viid.framework.domain.dto.MotorVehicleListObject;
import cz.data.viid.framework.domain.dto.MotorVehicleObject;
import cz.data.viid.framework.domain.dto.NonMotorVehicle;
import cz.data.viid.framework.domain.dto.NonMotorVehicleObjectList;
import cz.data.viid.framework.domain.dto.PersonListObject;
import cz.data.viid.framework.domain.dto.PersonObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.dto.SubscribeNotificationObject;
import cz.data.viid.framework.domain.dto.TollgateObject;
import cz.data.viid.framework.domain.dto.TollgateObjectList;
import cz.data.viid.framework.domain.entity.APEDevice;
import cz.data.viid.framework.domain.entity.TollgateDevice;
import cz.data.viid.framework.domain.entity.VIIDSubscribe;
import cz.data.viid.framework.service.APEDeviceService;
import cz.data.viid.framework.service.TollgateDeviceService;
import cz.data.viid.framework.service.VIIDSubscribeService;
import cz.data.viid.utils.JsonCommon;
import cz.data.viid.utils.ResponseUtil;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VIIDNotificationService implements IVIIDNotificationService {

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Resource
    APEDeviceService apeDeviceService;
    @Resource
    TollgateDeviceService tollgateDeviceService;
    @Autowired
    VIIDSubscribeService viidSubscribeService;
    @Autowired
    S3StorageService s3StorageService;

    @Override
    public List<ResponseStatusObject> dispositionHandler(List<DispositionNotificationObject> notificationObjects) {
        String serverId = SecurityContext.getRequestDeviceId();
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(notificationObjects)) {
            String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.DISPOSITION_RECORD, serverId);
            for (DispositionNotificationObject notificationObject : notificationObjects) {
                String id = notificationObject.getNotificationID();
                statusObjects.add(new ResponseStatusObject(id, null, "0", "正常"));
                kafkaTemplate.send(topic, JsonCommon.toJson(notificationObject));
            }
        } else {
            statusObjects.add(ResponseUtil.emptyDataStatusObject());
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> deviceHandle(List<APEObject> data, String ownerApsId) {
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.APE_DEVICE, ownerApsId);
        for (APEObject deviceObject : data) {
            try {
                kafkaTemplate.send(topic, JsonCommon.toJson(deviceObject));
                APEDevice device = StructCodec.castApeDevice(deviceObject);
                device.setOwnerApsId(ownerApsId);
                apeDeviceService.saveOrUpdate(device);
                statusObjects.add(new ResponseStatusObject(deviceObject.getApeID(), null, "0", "操作成功"));
            } catch (Exception e) {
                statusObjects.add(new ResponseStatusObject(deviceObject.getApeID(), null, "500", "操作失败"));
                log.error("上推设备数据至kafka错误", e);
            }
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> tollgateHandle(List<TollgateObject> data, String ownerApsId) {
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.TOLLGATE_DEVICE, ownerApsId);
        for (TollgateObject tollgateObject : data) {
            try {
                kafkaTemplate.send(topic, JsonCommon.toJson(tollgateObject));
                TollgateDevice tollgate = StructCodec.castTollgateDevice(tollgateObject);
                tollgateDeviceService.saveOrUpdate(tollgate);
                statusObjects.add(new ResponseStatusObject(tollgateObject.getTollgateID(), null, "0", "操作成功"));
            } catch (Exception e) {
                statusObjects.add(new ResponseStatusObject(tollgateObject.getTollgateID(), null, "500", "操作失败"));
                log.error("上推设备数据至kafka错误", e);
            }
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> faceHandle(List<FaceObject> data, String ownerApsId) {
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.FACE_RECORD, ownerApsId);
        log.info("人脸抓拍之后，kafka推送消息主题：{}",topic);
        for (FaceObject faceObject : data) {
            s3StorageService.subImageListStorage(null, faceObject.getSubImageList());
            kafkaTemplate.send(topic, JsonCommon.toJson(faceObject.validateDataFormat()));
            statusObjects.add(new ResponseStatusObject(faceObject.getFaceID(), null, "0", "操作成功"));
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> personHandle(List<PersonObject> data, String ownerApsId) {
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.PERSON_RECORD, ownerApsId);
        for (PersonObject personObject : data) {
            s3StorageService.subImageListStorage(null, personObject.getSubImageList());
            kafkaTemplate.send(topic, JsonCommon.toJson(personObject.validateDataFormat()));
            statusObjects.add(new ResponseStatusObject(personObject.getPersonID(), null, "0", "操作成功"));
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> motorVehicleHandle(List<MotorVehicleObject> data, String ownerApsId) {
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.MOTOR_VEHICLE, ownerApsId);
        for (MotorVehicleObject motorVehicle : data) {
            s3StorageService.subImageListStorage(null, motorVehicle.getSubImageList());
            kafkaTemplate.send(topic, JsonCommon.toJson(motorVehicle.validateMotorVehicle()));
            statusObjects.add(new ResponseStatusObject(motorVehicle.getMotorVehicleID(), null, "0", "操作成功"));
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> nonMotorVehicleHandle(List<NonMotorVehicle> data, String ownerApsId) {
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        String topic = StringUtils.join(Constants.DEFAULT_TOPIC_PREFIX.NON_MOTOR_VEHICLE, ownerApsId);
        for (NonMotorVehicle nonMotorVehicle : data) {
            s3StorageService.subImageListStorage(null, nonMotorVehicle.getSubImageList());
            kafkaTemplate.send(topic, JsonCommon.toJson(nonMotorVehicle.validateDataFormat()));
            statusObjects.add(new ResponseStatusObject(nonMotorVehicle.getNonMotorVehicleID(), null, "0", "操作成功"));
        }
        return statusObjects;
    }

    @Override
    public List<ResponseStatusObject> subscribeNotification(SubscribeNotificationObject notificationObject) {
        VIIDSubscribe subscribe = viidSubscribeService.getCacheById(notificationObject.getSubscribeID());
        if (subscribe == null) {
            return Collections.singletonList(ResponseUtil.subscribeNotExists());
        }
        String ownerApsId = subscribe.getServerId();
        Set<String> details = Stream.of(StringUtils.split(subscribe.getSubscribeDetail(), ","))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        List<ResponseStatusObject> statusObjects = new ArrayList<>();
        for (String detail : details) {
            if (Constants.SubscribeDetail.DEVICE.equalsValue(detail)) {
                Optional.of(notificationObject)
                        .map(SubscribeNotificationObject::getDeviceList)
                        .map(DeviceObjectList::getAPEObject)
                        .map(data -> deviceHandle(data, ownerApsId))
                        .ifPresent(statusObjects::addAll);
            } else if (Constants.SubscribeDetail.TOLLGATE.equalsValue(detail)) {
                Optional.of(notificationObject)
                        .map(SubscribeNotificationObject::getTollgateObjectList)
                        .map(TollgateObjectList::getTollgateObject)
                        .map(data -> tollgateHandle(data, ownerApsId))
                        .ifPresent(statusObjects::addAll);
            } else if (Constants.SubscribeDetail.PERSON_INFO.equalsValue(detail)) {
                Optional.of(notificationObject)
                        .map(SubscribeNotificationObject::getPersonObjectList)
                        .map(PersonListObject::getPersonObject)
                        .map(data -> personHandle(data, ownerApsId))
                        .ifPresent(statusObjects::addAll);
            } else if (Constants.SubscribeDetail.FACE_INFO.equalsValue(detail)) {
                Optional.of(notificationObject)
                        .map(SubscribeNotificationObject::getFaceObjectList)
                        .map(FaceObjectList::getFaceObject)
                        .map(data -> faceHandle(data, ownerApsId))
                        .ifPresent(statusObjects::addAll);
            } else if (Constants.SubscribeDetail.PLATE_INFO.equalsValue(detail)) {
                Optional.of(notificationObject)
                        .map(SubscribeNotificationObject::getMotorVehicleObjectList)
                        .map(MotorVehicleListObject::getMotorVehicleObject)
                        .map(data -> motorVehicleHandle(data, ownerApsId))
                        .ifPresent(statusObjects::addAll);
            } else if (Constants.SubscribeDetail.PLATE_MIRCO_INFO.equalsValue(detail)) {
                Optional.of(notificationObject)
                        .map(SubscribeNotificationObject::getNonMotorVehicleObjectList)
                        .map(NonMotorVehicleObjectList::getNonMotorVehicleObject)
                        .map(data -> nonMotorVehicleHandle(data, ownerApsId))
                        .ifPresent(statusObjects::addAll);
            }
        }
        if (statusObjects.isEmpty()) {
            statusObjects.add(ResponseUtil.emptyDataStatusObject());
        }
        return statusObjects;
    }
}
