package com.genersoft.iot.vmp.gat1400.backend.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import javax.annotation.Resource;

import cz.data.viid.be.service.IDeviceManage;
import cz.data.viid.be.service.IVIIDNotificationService;
import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.fe.DictContextHolder;
import cz.data.viid.fe.security.SecurityContext;
import cz.data.viid.framework.S3StorageService;
import cz.data.viid.framework.domain.dto.FaceObject;
import cz.data.viid.framework.domain.dto.MotorVehicleObject;
import cz.data.viid.framework.domain.dto.NonMotorVehicle;
import cz.data.viid.framework.domain.dto.PersonObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.entity.APEDevice;
import cz.data.viid.framework.service.TollgateDeviceService;

/**
 * 处理设备上推数据
 */
@Service
public class DeviceManage implements IDeviceManage {

    @Resource
    IVIIDNotificationService notificationService;
    @Resource
    S3StorageService storageService;
    @Resource
    TollgateDeviceService tollgateService;

    @Override
    public List<ResponseStatusObject> deviceFaceHandler(List<FaceObject> faceObjects) {
        String deviceId = SecurityContext.getRequestDeviceId();
        for (FaceObject faceObject : faceObjects) {
            faceObject.setDeviceID(deviceId);
            storageService.subImageListStorage(deviceId, faceObject.getSubImageList());
        }
        return notificationService.faceHandle(faceObjects, KeepaliveAction.CURRENT_SERVER_ID);
    }

    @Override
    public List<ResponseStatusObject> devicePersonHandler(List<PersonObject> personObjects) {
        String deviceId = SecurityContext.getRequestDeviceId();
        for (PersonObject personObject : personObjects) {
            personObject.setDeviceID(deviceId);
            storageService.subImageListStorage(deviceId, personObject.getSubImageList());
        }
        return notificationService.personHandle(personObjects, KeepaliveAction.CURRENT_SERVER_ID);
    }

    @Override
    public List<ResponseStatusObject> deviceMotorVehicleHandler(List<MotorVehicleObject> motorVehicleObjects) {
        String deviceId = SecurityContext.getRequestDeviceId();
        APEDevice device = SecurityContext.requireVIIDDevice();
        for (MotorVehicleObject vehicle : motorVehicleObjects) {
            vehicle.setDeviceID(deviceId);
            if (StringUtils.isBlank(vehicle.getDirection())) {
                vehicle.setDirection(DictContextHolder.analysisDirection(device));
            }
            if (StringUtils.isBlank(vehicle.getTollgateID())) {
                String tollgateId = tollgateService.findTollgateIdByDeviceId(deviceId);
                vehicle.setTollgateID(tollgateId);
            }
            storageService.subImageListStorage(deviceId, vehicle.getSubImageList());
        }
        return notificationService.motorVehicleHandle(motorVehicleObjects, KeepaliveAction.CURRENT_SERVER_ID);
    }

    @Override
    public List<ResponseStatusObject> deviceNonMotorVehicleHandler(List<NonMotorVehicle> nonMotorVehicleObjects) {
        String deviceId = SecurityContext.getRequestDeviceId();
        for (NonMotorVehicle nonMotorVehicle : nonMotorVehicleObjects) {
            nonMotorVehicle.setDeviceID(deviceId);
            storageService.subImageListStorage(deviceId, nonMotorVehicle.getSubImageList());
        }
        return notificationService.nonMotorVehicleHandle(nonMotorVehicleObjects, KeepaliveAction.CURRENT_SERVER_ID);
    }
}
