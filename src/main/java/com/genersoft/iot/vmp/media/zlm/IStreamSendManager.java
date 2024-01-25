package com.genersoft.iot.vmp.media.zlm;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;

import java.util.List;

public interface IStreamSendManager {

    void add(SendRtpItem sendRtpItem);

    void update(SendRtpItem sendRtpItem);

    SendRtpItem getByCallId(String callId);

    List<SendRtpItem> getByAppAndStream(String app, String stream);

    List<SendRtpItem> getByMediaServerId(String mediaServerId);

    List<SendRtpItem> getBySourceId(String sourceId);

    List<SendRtpItem> getByDestId(String destId);

    List<SendRtpItem> getByByChanelId(String channelId);

    void remove(String id);

    void removeByCallID(String id);

    void remove(List<SendRtpItem> sendRtpItemList);
}
