package com.genersoft.iot.vmp.gat1400.backend.service;

import java.util.List;

import cz.data.viid.be.domain.dto.DispositionNotificationObject;
import cz.data.viid.framework.domain.dto.APEObject;
import cz.data.viid.framework.domain.dto.FaceObject;
import cz.data.viid.framework.domain.dto.MotorVehicleObject;
import cz.data.viid.framework.domain.dto.NonMotorVehicle;
import cz.data.viid.framework.domain.dto.PersonObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.dto.SubscribeNotificationObject;
import cz.data.viid.framework.domain.dto.TollgateObject;

public interface IVIIDNotificationService {

    List<ResponseStatusObject> dispositionHandler(List<DispositionNotificationObject> notificationObjects);

    List<ResponseStatusObject> deviceHandle(List<APEObject> data, String ownerApsId);

    List<ResponseStatusObject> tollgateHandle(List<TollgateObject> data, String ownerApsId);

    List<ResponseStatusObject> faceHandle(List<FaceObject> data, String ownerApsId);

    List<ResponseStatusObject> personHandle(List<PersonObject> data, String ownerApsId);

    List<ResponseStatusObject> motorVehicleHandle(List<MotorVehicleObject> data, String ownerApsId);

    List<ResponseStatusObject> nonMotorVehicleHandle(List<NonMotorVehicle> data, String ownerApsId);

    List<ResponseStatusObject> subscribeNotification(SubscribeNotificationObject notificationObject);
}
