package com.genersoft.iot.vmp.gat1400.backend.service;

import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.domain.vo.RegisterRequest;

public interface ISystemService {

    NodeDevice getDeviceById(String deviceId);

    NodeDevice register(RegisterRequest request);

    boolean unRegister(String deviceId);

    ResponseStatusObject keepalive(String deviceId);
}
