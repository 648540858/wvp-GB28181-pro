package com.genersoft.iot.vmp.service.redisMsg;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;

public interface IRedisRpcService {

    SendRtpInfo getSendRtpItem(String callId);

    WVPResult startSendRtp(String callId, SendRtpInfo sendRtpItem);

    WVPResult stopSendRtp(String callId);

    long waitePushStreamOnline(SendRtpInfo sendRtpItem, CommonCallback<Integer> callback);

    void stopWaitePushStreamOnline(SendRtpInfo sendRtpItem);

    void rtpSendStopped(String callId);

    void removeCallback(long key);

    long onStreamOnlineEvent(String app, String stream, CommonCallback<StreamInfo> callback);
    void unPushStreamOnlineEvent(String app, String stream);

    void subscribeCatalog(int id, int cycle);

    void subscribeMobilePosition(int id, int cycle, int interval);

    boolean updatePlatform(String serverId, Platform platform);

    void catalogEventPublish(String serverId, CatalogEvent catalogEvent);

    WVPResult<SyncStatus> devicesSync(String serverId, String deviceId);

    SyncStatus getChannelSyncStatus(String serverId, String deviceId);

    WVPResult<String> deviceBasicConfig(String serverId, Device device, BasicParam basicParam);

    WVPResult<String> deviceConfigQuery(String serverId, Device device, String channelId, String configType);

    void teleboot(String serverId, Device device);

    WVPResult<String> recordControl(String serverId, Device device, String channelId, String recordCmdStr);

    WVPResult<String> guard(String serverId, Device device, String guardCmdStr);

    WVPResult<String> resetAlarm(String serverId, Device device, String channelId, String alarmMethod, String alarmType);

    void iFrame(String serverId, Device device, String channelId);

    WVPResult<String> homePosition(String serverId, Device device, String channelId, Boolean enabled, Integer resetTime, Integer presetIndex);

    void dragZoomIn(String serverId, Device device, String channelId, int length, int width, int midpointx, int midpointy, int lengthx, int lengthy);

    void dragZoomOut(String serverId, Device device, String channelId, int length, int width, int midpointx, int midpointy, int lengthx, int lengthy);

    WVPResult<String> deviceStatus(String serverId, Device device);

    WVPResult<String> alarm(String serverId, Device device, String startPriority, String endPriority, String alarmMethod, String alarmType, String startTime, String endTime);

    WVPResult<Object> deviceInfo(String serverId, Device device);

    WVPResult<Object> queryPreset(String serverId, Device device, String channelId);
}
