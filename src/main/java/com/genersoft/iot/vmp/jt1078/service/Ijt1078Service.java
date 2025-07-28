package com.genersoft.iot.vmp.jt1078.service;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.request.J1205;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.github.pagehelper.PageInfo;

import javax.servlet.ServletOutputStream;
import java.io.OutputStream;
import java.util.List;

public interface Ijt1078Service {

    JTMediaStreamType checkStreamFromJt(String stream);

    JTDevice getDevice(String phoneNumber);

    JTChannel getChannel(Integer terminalDbId, Integer channelId);

    void updateDevice(JTDevice deviceInDb);

    PageInfo<JTDevice> getDeviceList(int page, int count, String query, Boolean online);

    void addDevice(JTDevice device);

    void deleteDeviceByPhoneNumber(String phoneNumber);

    void updateDeviceStatus(boolean connected, String phoneNumber);


    void ptzControl(String phoneNumber, Integer channelId, String command, int speed);

    void supplementaryLight(String phoneNumber, Integer channelId, String command);

    void wiper(String phoneNumber, Integer channelId, String command);

    JTDeviceConfig queryConfig(String phoneNumber, String[] params);

    void setConfig(String phoneNumber, JTDeviceConfig config);

    void connectionControl(String phoneNumber, JTDeviceConnectionControl control);

    void resetControl(String phoneNumber);

    void factoryResetControl(String phoneNumber);

    JTDeviceAttribute attribute(String phoneNumber);

    JTPositionBaseInfo queryPositionInfo(String phoneNumber);

    void tempPositionTrackingControl(String phoneNumber, Integer timeInterval, Long validityPeriod);

    void confirmationAlarmMessage(String phoneNumber, int alarmPackageNo, JTConfirmationAlarmMessageType alarmMessageType);

    int linkDetection(String phoneNumber);

    int textMessage(String phoneNumber,JTTextSign sign, int textType, String content);

    int telephoneCallback(String phoneNumber, Integer sign, String destPhoneNumber);

    int setPhoneBook(String phoneNumber, int type, List<JTPhoneBookContact> phoneBookContactList);

    JTPositionBaseInfo controlDoor(String phoneNumber, Boolean open);

    int setAreaForCircle(int attribute, String phoneNumber, List<JTCircleArea> circleAreaList);

    int deleteAreaForCircle(String phoneNumber, List<Long> ids);

    List<JTAreaOrRoute> queryAreaForCircle(String phoneNumber, List<Long> ids);

    int setAreaForRectangle(int i, String phoneNumber, List<JTRectangleArea> rectangleAreas);

    int deleteAreaForRectangle(String phoneNumber, List<Long> ids);

    List<JTAreaOrRoute> queryAreaForRectangle(String phoneNumber, List<Long> ids);

    int setAreaForPolygon(String phoneNumber, JTPolygonArea polygonArea);

    int deleteAreaForPolygon(String phoneNumber, List<Long> ids);

    List<JTAreaOrRoute> queryAreaForPolygon(String phoneNumber, List<Long> ids);

    int setRoute(String phoneNumber, JTRoute route);

    int deleteRoute(String phoneNumber, List<Long> ids);

    List<JTAreaOrRoute> queryRoute(String phoneNumber, List<Long> ids);

    JTDriverInformation queryDriverInformation(String phoneNumber);

    List<Long> shooting(String phoneNumber, JTShootingCommand shootingCommand);

    List<JTMediaDataInfo> queryMediaData(String phoneNumber, JTQueryMediaDataCommand queryMediaDataCommand);

    void uploadMediaData(String phoneNumber, JTQueryMediaDataCommand queryMediaDataCommand);

    void record(String phoneNumber, int command, Integer time, Integer save, Integer samplingRate);

    void uploadMediaDataForSingle(String phoneNumber, Long mediaId, Integer delete);

    JTMediaAttribute queryMediaAttribute(String phoneNumber);

    void changeStreamType(String phoneNumber, Integer channelId, Integer streamType);

    void recordDownload(String phoneNumber, Integer channelId, String startTime, String endTime, Integer alarmSign, Integer mediaType, Integer streamType, Integer storageType, OutputStream outputStream, CommonCallback<WVPResult<String>> fileCallback);

    PageInfo<JTChannel> getChannelList(int page, int count, int deviceId, String query);

    void updateChannel(JTChannel channel);

    void addChannel(JTChannel channel);

    void deleteChannelById(Integer id);

    JTDevice getDeviceById(Integer deviceId);

    void updateDevicePosition(String phoneNumber, Double longitude, Double latitude);

    JTChannel getChannelByDbId(Integer id);

    String getRecordTempUrl(String phoneNumber, Integer channelId, String startTime, String endTime, Integer alarmSign, Integer mediaType, Integer streamType, Integer storageType);

    void recordDownload(String filePath, ServletOutputStream outputStream);

    void snap(String phoneNumber, int channelId, ServletOutputStream outputStream);

    void uploadOneMedia(String phoneNumber, Long mediaId, ServletOutputStream outputStream, boolean delete);


}
