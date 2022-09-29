package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamCallback;
import com.genersoft.iot.vmp.gb28181.bean.InviteStreamInfo;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.bean.InviteTimeOutCallback;
import com.genersoft.iot.vmp.service.bean.PlayBackCallback;
import com.genersoft.iot.vmp.service.bean.SSRCInfo;
import com.genersoft.iot.vmp.vmanager.bean.AudioBroadcastResult;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.AudioBroadcastEvent;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 点播处理
 */
public interface IPlayService {

    void onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId, String uuid);

    void talk(MediaServerItem mediaServerItem, Device device, String channelId,
              ZlmHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
              Runnable timeoutCallback);

    void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
              ZlmHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
              InviteTimeOutCallback timeoutCallback, String uuid);
    PlayResult play(MediaServerItem mediaServerItem, String deviceId, String channelId, ZlmHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent, Runnable timeoutCallback);

    MediaServerItem getNewMediaServerItem(Device device);

    void onPublishHandlerForDownload(InviteStreamInfo inviteStreamInfo, String deviceId, String channelId, String toString);

    DeferredResult<WVPResult<StreamInfo>> playBack(String deviceId, String channelId, String startTime, String endTime, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);
    DeferredResult<WVPResult<StreamInfo>> playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId, String channelId, String startTime, String endTime, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);

    void zlmServerOffline(String mediaServerId);

    DeferredResult<WVPResult<StreamInfo>> download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);
    DeferredResult<WVPResult<StreamInfo>> download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId,  String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);

    StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream);

    void zlmServerOnline(String mediaServerId);

    AudioBroadcastResult audioBroadcast(Device device, String channelId);
    void stopAudioBroadcast(String deviceId, String channelId);

    void audioBroadcastCmd(Device device, String channelId, int timeout, AudioBroadcastEvent event) throws InvalidArgumentException, ParseException, SipException;
}
