package com.genersoft.iot.vmp.gat1400.backend.service;


import com.genersoft.iot.vmp.gat1400.framework.domain.dto.ResponseStatusObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.NodeDevice;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.RegisterRequest;

public interface ISystemService {

    NodeDevice getDeviceById(String deviceId);

    NodeDevice register(RegisterRequest request);

    boolean unRegister(String deviceId);

    ResponseStatusObject keepalive(String deviceId);
}
