package com.genersoft.iot.vmp.gat1400.backend.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Resource;

import cz.data.viid.be.service.ISystemService;
import cz.data.viid.be.task.action.KeepaliveAction;
import cz.data.viid.fe.security.SecurityContext;
import cz.data.viid.framework.SpringContextHolder;
import cz.data.viid.framework.domain.dto.DeviceIdObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.entity.APEDevice;
import cz.data.viid.framework.domain.entity.NodeDevice;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.RegisterRequest;
import cz.data.viid.framework.exception.VIIDAuthException;
import cz.data.viid.framework.service.APEDeviceService;
import cz.data.viid.framework.service.VIIDServerService;
import cz.data.viid.listener.event.ServerOfflineEvent;
import cz.data.viid.utils.StructCodec;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SystemService implements ISystemService {

    @Autowired
    KeepaliveAction keepaliveAction;
    @Autowired
    VIIDServerService viidServerService;
    @Resource
    APEDeviceService apeDeviceService;

    @Override
    public NodeDevice getDeviceById(String deviceId) {
        //标准国标编号逻辑
        if (StructCodec.isDeviceId(deviceId)) {
            APEDevice device = apeDeviceService.getById(deviceId);
            if (Objects.nonNull(device))
                return NodeDevice.fromDevice(device);
            return null;
        } else if (StructCodec.isServerId(deviceId)) {
            VIIDServer server = viidServerService.getByIdAndEnabled(deviceId);
            if (Objects.nonNull(server))
                return NodeDevice.fromServer(server);
            return null;
        } else {
            //非标准国标编号逻辑
            NodeDevice device = null;
            APEDevice apeDevice = apeDeviceService.getById(deviceId);
            if (Objects.nonNull(apeDevice)) {
                device = NodeDevice.fromDevice(apeDevice);
            } else {
                VIIDServer server = viidServerService.getByIdAndEnabled(deviceId);
                if (Objects.nonNull(server))
                    device = NodeDevice.fromServer(server);
            }
            return device;
        }
    }

    @Override
    public NodeDevice register(RegisterRequest request) {
        String deviceId = Optional.ofNullable(request)
                .map(RegisterRequest::getRegisterObject)
                .map(DeviceIdObject::getDeviceId)
                .orElse(null);
        NodeDevice device = SecurityContext.getNodeDevice();
        if (device == null)
            throw new VIIDAuthException("未登记设备ID");
        if (!device.getDeviceId().equals(deviceId))
            throw new VIIDAuthException("认证和注册设备编号不一致");
        return device;
    }

    @Override
    public boolean unRegister(String deviceId) {
        SpringContextHolder.publishEvent(new ServerOfflineEvent(deviceId));
        return true;
    }

    @Override
    public ResponseStatusObject keepalive(String deviceId) {
        log.info("保活心跳:{}",deviceId);
        NodeDevice device = keepaliveAction.keepalive(deviceId);
        ResponseStatusObject statusObject;
        if (Objects.nonNull(device)) {
            statusObject = new ResponseStatusObject(deviceId, null, "0", "保活成功");
        } else {
            statusObject = new ResponseStatusObject(deviceId, null, "500", "保活失败,请先注册");
        }
        return statusObject;
    }
}
