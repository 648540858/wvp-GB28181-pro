package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamCallback;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamInfo;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.bean.InviteTimeOutCallback;
import com.genersoft.iot.vmp.service.bean.PlayBackCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 点播处理
 */
public interface IPlayService {

    void onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId, String uuid);

    void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
              ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
              InviteTimeOutCallback timeoutCallback, String uuid);
    PlayResult play(MediaServerItem mediaServerItem, String deviceId, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent, Runnable timeoutCallback);

    MediaServerItem getNewMediaServerItem(Device device);

    void onPublishHandlerForDownload(InviteStreamInfo inviteStreamInfo, String deviceId, String channelId, String toString);

    DeferredResult<ResponseEntity<String>> playBack(String deviceId, String channelId, String startTime, String endTime, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);
    DeferredResult<ResponseEntity<String>> playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId, String channelId, String startTime, String endTime, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);

    void zlmServerOffline(String mediaServerId);

    DeferredResult<ResponseEntity<String>> download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);
    DeferredResult<ResponseEntity<String>> download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId,  String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);

    StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream);
}
