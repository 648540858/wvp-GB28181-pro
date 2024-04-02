package com.genersoft.iot.vmp.jt1078.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.github.pagehelper.PageInfo;

import java.util.List;

public interface Ijt1078Service {
    JTDevice getDevice(String terminalId);

    void updateDevice(JTDevice deviceInDb);

    PageInfo<JTDevice> getDeviceList(int page, int count, String query, Boolean online);

    void addDevice(JTDevice device);

    void deleteDeviceByDeviceId(String deviceId);

    void updateDeviceStatus(boolean connected, String terminalId);

    void play(String deviceId, String channelId, GeneralCallback<StreamInfo> callback);

    void stopPlay(String deviceId, String channelId);
}
