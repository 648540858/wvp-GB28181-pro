package com.genersoft.iot.vmp.gat1400.backend.service;

import com.genersoft.iot.vmp.gat1400.backend.domain.dto.DispositionNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.APEObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.FaceObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.MotorVehicleObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.NonMotorVehicle;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.PersonObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubscribeNotificationObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.TollgateObject;

import java.util.List;


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
