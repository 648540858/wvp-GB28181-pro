package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;

import java.util.List;

public interface IStreamSendManager {

    void update(SendRtpItem sendRtpItem);

    List<SendRtpItem> getAll();

    SendRtpItem getByCallId(String callId);

    List<SendRtpItem> getByAppAndStream(String app, String stream);

    List<SendRtpItem> getByMediaServerId(String mediaServerId);

    List<SendRtpItem> getBySourceId(String sourceId);

    List<SendRtpItem> getByDestId(String destId);

    List<SendRtpItem> getByByChanelId(String channelId);

    void remove(String id);

    void removeByCallID(String id);

    void remove(SendRtpItem sendRtpItem);

    void remove(List<SendRtpItem> sendRtpItemList);
}
