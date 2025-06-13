package com.genersoft.iot.vmp.gat1400.backend.service;

import java.util.List;

import cz.data.viid.framework.domain.dto.FaceObject;
import cz.data.viid.framework.domain.dto.MotorVehicleObject;
import cz.data.viid.framework.domain.dto.NonMotorVehicle;
import cz.data.viid.framework.domain.dto.PersonObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;

public interface IDeviceManage {

    List<ResponseStatusObject> deviceFaceHandler(List<FaceObject> faceObjects);

    List<ResponseStatusObject> devicePersonHandler(List<PersonObject> PersonObjects);

    List<ResponseStatusObject> deviceMotorVehicleHandler(List<MotorVehicleObject> MotorVehicleObjects);

    List<ResponseStatusObject> deviceNonMotorVehicleHandler(List<NonMotorVehicle> NonMotorVehicleObjects);

}
