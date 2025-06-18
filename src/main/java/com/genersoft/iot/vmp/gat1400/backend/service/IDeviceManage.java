package com.genersoft.iot.vmp.gat1400.backend.service;

import com.genersoft.iot.vmp.gat1400.framework.domain.dto.FaceObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.MotorVehicleObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.NonMotorVehicle;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.PersonObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;

import java.util.List;


public interface IDeviceManage {

    List<ResponseStatusObject> deviceFaceHandler(List<FaceObject> faceObjects);

    List<ResponseStatusObject> devicePersonHandler(List<PersonObject> PersonObjects);

    List<ResponseStatusObject> deviceMotorVehicleHandler(List<MotorVehicleObject> MotorVehicleObjects);

    List<ResponseStatusObject> deviceNonMotorVehicleHandler(List<NonMotorVehicle> NonMotorVehicleObjects);

}
