package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.service.IVideoSquareService;
import com.genersoft.iot.vmp.storager.dao.VideoSquareMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class VideoSquareServiceImpl implements IVideoSquareService {

    @Autowired
    private VideoSquareMapper videoSquareMapper;

    @Override
    public JSONArray selectVideoTree() {
        JSONArray jsonArray = new JSONArray();
        List<Device> devices = videoSquareMapper.selectDevices();
        if (ObjectUtils.anyNotNull(devices)) {
            List<DeviceChannel> deviceChannels = videoSquareMapper.selectDeviceChannels();
            devices.forEach(device -> {
                String deviceId1 = device.getDeviceId();
                JSONObject parent = new JSONObject();
                parent.put("title", deviceId1);
                parent.put("key", deviceId1);
                jsonArray.add(parent);
                JSONArray children = new JSONArray();
                parent.put("children", children);
                deviceChannels.forEach(deviceChannel -> {
                    String deviceId2 = deviceChannel.getDeviceId();
                    if (deviceId1.equals(deviceId2)) {
                        JSONObject childrenObj = new JSONObject();
                        childrenObj.put("title", deviceChannel.getName());
                        childrenObj.put("key", deviceId1 + "_" + deviceChannel.getChannelId());
                        childrenObj.put("slots", JSON.parse("{icon: 'cameraVideo'}"));
                        children.add(childrenObj);
                    }
                });
            });
        }
        return jsonArray;
    }
}
