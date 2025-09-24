package com.genersoft.iot.vmp.gb28181.service;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.Preset;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;

import java.util.List;

public interface IGbChannelFrontEndService {


    void ptz(CommonGBChannel channel, String command, Integer horizonSpeed, Integer verticalSpeed, Integer zoomSpeed);

    void iris(CommonGBChannel channel, String command, Integer speed);

    void focus(CommonGBChannel channel, String command, Integer speed);

    void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback);

    void addPreset(CommonGBChannel channel, Integer presetId);

    void callPreset(CommonGBChannel channel, Integer presetId);

    void deletePreset(CommonGBChannel channel, Integer presetId);

    void addCruisePoint(CommonGBChannel channel, Integer cruiseId, Integer presetId);

    void deleteCruisePoint(CommonGBChannel channel, Integer cruiseId, Integer presetId);

    void setCruiseSpeed(CommonGBChannel channel, Integer cruiseId, Integer speed);

    void setCruiseTime(CommonGBChannel channel, Integer cruiseId, Integer time);

    void startCruise(CommonGBChannel channel, Integer cruiseId);

    void stopCruise(CommonGBChannel channel, Integer cruiseId);

    void startScan(CommonGBChannel channel, Integer scanId);

    void stopScan(CommonGBChannel channel, Integer scanId);

    void setScanLeft(CommonGBChannel channel, Integer scanId);

    void setScanRight(CommonGBChannel channel, Integer scanId);

    void setScanSpeed(CommonGBChannel channel, Integer scanId, Integer speed);

    void wiper(CommonGBChannel channel, String command);

    void auxiliarySwitch(CommonGBChannel channel, String command, Integer switchId);

}
