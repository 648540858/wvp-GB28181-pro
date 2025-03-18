package com.genersoft.iot.vmp.service.redisMsg;

import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;

public interface IRedisRpcPlayService {


    void play(String serverId, Integer channelId, ErrorCallback<StreamInfo> callback);

    void stop(String serverId, InviteSessionType type, int channelId, String stream);

    void playback(String serverId, Integer channelId, String startTime, String endTime, ErrorCallback<StreamInfo> callback);

    void download(String serverId, Integer channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<StreamInfo> callback);

    void queryRecordInfo(String serverId, Integer channelId, String startTime, String endTime, ErrorCallback<RecordInfo> callback);

    void pauseRtp(String serverId, String streamId);

    void resumeRtp(String serverId, String streamId);

    String frontEndCommand(String serverId, Integer channelId, int cmdCode, int parameter1, int parameter2, int combindCode2);

    void playPush(Integer id, ErrorCallback<StreamInfo> callback);

    StreamInfo playProxy(String serverId, int id);

    void stopProxy(String serverId, int id);

    DownloadFileInfo getRecordPlayUrl(String serverId, Integer recordId);


    AudioBroadcastResult audioBroadcast(String serverId, String deviceId, String channelDeviceId, Boolean broadcastMode);
}
