package com.genersoft.iot.vmp.jt1078.service;

import com.genersoft.iot.vmp.common.GeneralCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
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

    void playback(String deviceId, String channelId, String startTime, String endTime, GeneralCallback<StreamInfo> callback);

    void stopPlay(String deviceId, String channelId);

    void pausePlay(String deviceId, String channelId);

    void continueLivePlay(String deviceId, String channelId);

    List<J1205.JRecordItem> getRecordList(String deviceId, String channelId, String startTime, String endTime);

    void stopPlayback(String deviceId, String channelId);

    void ptzControl(String deviceId, String channelId, String command, int speed);

    void supplementaryLight(String deviceId, String channelId, String command);

    void wiper(String deviceId, String channelId, String command);

    JTDeviceConfig queryConfig(String deviceId, String[] params, GeneralCallback<StreamInfo> callback);

    void setConfig(String deviceId, JTDeviceConfig config);

    void connectionControl(String deviceId, JTDeviceConnectionControl control);

    void resetControl(String deviceId);

    void factoryResetControl(String deviceId);

    JTDeviceAttribute attribute(String deviceId);

    JTPositionBaseInfo queryPositionInfo(String deviceId);

    void tempPositionTrackingControl(String deviceId, Integer timeInterval, Long validityPeriod);

    void confirmationAlarmMessage(String deviceId, int alarmPackageNo, JConfirmationAlarmMessageType alarmMessageType);

    int linkDetection(String deviceId);

    int textMessage(String deviceId,JTTextSign sign, int textType, String content);

    int telephoneCallback(String deviceId, Integer sign, String phoneNumber);

    int setPhoneBook(String deviceId, int type, List<JTPhoneBookContact> phoneBookContactList);

    JTPositionBaseInfo controlDoor(String deviceId, Boolean open);

    int setAreaForCircle(int attribute, String deviceId, List<JTCircleArea> circleAreaList);

    int deleteAreaForCircle(String deviceId, List<Long> ids);

    List<JTAreaOrRoute> queryAreaForCircle(String deviceId, List<Long> ids);

    int setAreaForRectangle(int i, String deviceId, List<JTRectangleArea> rectangleAreas);

    int deleteAreaForRectangle(String deviceId, List<Long> ids);

    List<JTAreaOrRoute> queryAreaForRectangle(String deviceId, List<Long> ids);

    int setAreaForPolygon(String deviceId, JTPolygonArea polygonArea);

    int deleteAreaForPolygon(String deviceId, List<Long> ids);

    List<JTAreaOrRoute> queryAreaForPolygon(String deviceId, List<Long> ids);

    int setRoute(String deviceId, JTRoute route);

    int deleteRoute(String deviceId, List<Long> ids);

    List<JTAreaOrRoute> queryRoute(String deviceId, List<Long> ids);

    JTDriverInformation queryDriverInformation(String deviceId);

    List<Long> shooting(String deviceId, JTShootingCommand shootingCommand);

    List<JTMediaDataInfo> queryMediaData(String deviceId, JTQueryMediaDataCommand queryMediaDataCommand);

    void uploadMediaData(String deviceId, JTQueryMediaDataCommand queryMediaDataCommand);

    void record(String deviceId, int command, Integer time, Integer save, Integer samplingRate);
}
