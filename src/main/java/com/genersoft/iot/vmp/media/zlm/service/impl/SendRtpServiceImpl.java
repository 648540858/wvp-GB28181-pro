package com.genersoft.iot.vmp.media.zlm.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.SendRtpPortManager;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.service.ISendRtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SendRtpServiceImpl implements ISendRtpService {

    @Autowired
    private SendRtpPortManager sendRtpPortManager;

    /**
     * 创建一个国标推流
     * @param serverItem 推流使用的流媒体
     * @param dstIp 目标IP
     * @param dstPort 目标端口
     * @param ssrc SSRC
     * @param sourceId 推流信息唯一标识
     * @param callId 关联的Invite会话callId
     * @param tcp 是否使用TCP推流
     * @param rtcp 是否开启RTCP保活，tcp为false时有效
     * @return SendRtpItem
     */
    public SendRtpItem createSendRtpInfo(MediaServerItem serverItem, String dstIp, int dstPort, String ssrc, String sourceId,
                                         String callId, boolean tcp, boolean rtcp){

//        int localPort = sendRtpPortManager.getNextPort(serverItem);
//        if (localPort == 0) {
//            return null;
//        }
//        SendRtpItem sendRtpItem = new SendRtpItem();
//        sendRtpItem.setIp(ip);
//        sendRtpItem.setPort(port);
//        sendRtpItem.setSsrc(ssrc);
//        sendRtpItem.setPlatformId(platformId);
//        sendRtpItem.setChannelId(channelId);
//        sendRtpItem.setTcp(tcp);
//        sendRtpItem.setRtcp(rtcp);
//        sendRtpItem.setApp("rtp");
//        sendRtpItem.setLocalPort(localPort);
//        sendRtpItem.setServerId(userSetting.getServerId());
//        sendRtpItem.setMediaServerId(serverItem.getId());
//        return sendRtpItem;
        return null;
    }
}
