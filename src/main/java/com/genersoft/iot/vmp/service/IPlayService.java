package com.genersoft.iot.vmp.service;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.IMediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;

/**
 * 点播处理
 */
public interface IPlayService {

    void onPublishHandlerForPlayBack(IMediaServerItem mediaServerItem,JSONObject resonse, String deviceId, String channelId, String uuid);
    void onPublishHandlerForPlay(IMediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId, String uuid);

    PlayResult play(IMediaServerItem mediaServerItem, String deviceId, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent);

    IMediaServerItem getNewMediaServerItem(Device device);
}
