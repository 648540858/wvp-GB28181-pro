package com.genersoft.iot.vmp.onvif.impl;


import be.teletask.onvif.DiscoveryManager;
import be.teletask.onvif.OnvifManager;
import be.teletask.onvif.listeners.*;
import be.teletask.onvif.models.*;
import be.teletask.onvif.responses.OnvifResponse;
import com.genersoft.iot.vmp.onvif.IONVIFServer;
import com.genersoft.iot.vmp.onvif.dto.ONVIFCallBack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
/**
 * 处理onvif的各种操作
 */
@Service
public class ONVIFServerIMpl implements IONVIFServer {

    private final static Logger logger = LoggerFactory.getLogger(ONVIFServerIMpl.class);

    @Override
    public void search(int timeout, ONVIFCallBack<List<String>> callBack) {
        DiscoveryManager manager = new DiscoveryManager();
        manager.setDiscoveryTimeout(timeout);
        Map<String, Device> deviceMap = new HashMap<>();
        // 搜索设备
        manager.discover(new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted() {
                logger.info("Discovery started");
            }

            @Override
            public void onDevicesFound(List<Device> devices) {
                if (devices == null || devices.size() == 0) return;
                for (Device device : devices){
                    logger.info(device.getHostName());
                    deviceMap.put(device.getHostName(),  device);
                }
            }

            // 搜索结束
            @Override
            public void onDiscoveryFinished() {
                ArrayList<String> result = new ArrayList<>();
                for (Device device : deviceMap.values()) {
                    logger.info(device.getHostName());
                    result.add(device.getHostName());
                }
                callBack.run(0, result);
            }
        });
    }

    @Override
    public void getRTSPUrl(int timeout, OnvifDevice device, ONVIFCallBack<String> callBack) {
        if (device.getHostName() == null ){
            callBack.run(400, null);
        }
        OnvifManager onvifManager = new OnvifManager();
        onvifManager.setOnvifResponseListener(new OnvifResponseListener(){

            @Override
            public void onResponse(OnvifDevice onvifDevice, OnvifResponse response) {
                logger.info("[RESPONSE] " + onvifDevice.getHostName()
                        + "======" + response.getErrorCode()
                        + "======" + response.getErrorMessage());
            }

            @Override
            public void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage) {
                logger.info("[ERROR] " + onvifDevice.getHostName() + "======" + errorCode + "=======" + errorMessage);
                callBack.run(errorCode, errorMessage);
            }
        });

        try {
            onvifManager.getServices(device, (OnvifDevice onvifDevice, OnvifServices services) -> {
                if (services.getProfilesPath().equals("/onvif/Media")) {
                    onvifDevice.setPath(services);
                    onvifManager.getMediaProfiles(onvifDevice, new OnvifMediaProfilesListener() {
                        @Override
                        public void onMediaProfilesReceived(OnvifDevice device, List<OnvifMediaProfile> mediaProfiles) {
                            for (OnvifMediaProfile mediaProfile : mediaProfiles) {
                                logger.info(mediaProfile.getName());
                                logger.info(mediaProfile.getToken());
                                if (mediaProfile.getName().equals("mainStream")) {
                                    onvifManager.getMediaStreamURI(device, mediaProfile, (OnvifDevice onvifDevice,
                                                                                          OnvifMediaProfile profile, String uri) -> {

                                        uri = uri.replace("rtsp://", "rtsp://"+ device.getUsername() + ":"+ device.getPassword() + "@");
                                        logger.info(onvifDevice.getHostName() + "的地址" + uri);
                                        callBack.run(0, uri);
                                    });
                                }
                            }
                        }
                    });
                }
            });
        }catch (Exception e) {
            callBack.run(400, e.getMessage());
        }


    }
}
