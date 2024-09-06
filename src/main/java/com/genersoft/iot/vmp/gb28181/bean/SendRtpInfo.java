package com.genersoft.iot.vmp.gb28181.bean;

import com.genersoft.iot.vmp.service.bean.RequestPushStreamMsg;

import com.genersoft.iot.vmp.common.VideoManagerConstants;
import lombok.Data;

@Data
public class SendRtpInfo {

    /**
     * 推流ip
     */
    private String ip;

    /**
     * 推流端口
     */
    private int port;

    /**
     * 推流标识
     */
    private String ssrc;

    /**
     * 平台id
     */
    private String platformId;

    /**
     * 平台名称
     */
    private String platformName;

     /**
     * 对应设备id
     */
    private String deviceId;

    /**
     * 直播流的应用名
     */
    private String app;

   /**
     * 通道id
     */
    private Integer channelId;

    /**
     * 推流状态
     * 0 等待设备推流上来
     * 1 等待上级平台回复ack
     * 2 推流中
     */
    private int status = 0;


    /**
     * 设备推流的streamId
     */
    private String stream;

    /**
     * 是否为tcp
     */
    private boolean tcp;

    /**
     * 是否为tcp主动模式
     */
    private boolean tcpActive;

    /**
     * 自己推流使用的IP
     */
    private String localIp;

    /**
     * 自己推流使用的端口
     */
    private int localPort;

    /**
     * 使用的流媒体
     */
    private String mediaServerId;

    /**
     * 使用的服务的ID
     */
    private String serverId;

    /**
     *  invite 的 callId
     */
    private String callId;

    /**
     *  invite 的 fromTag
     */
    private String fromTag;

    /**
     *  invite 的 toTag
     */
    private String toTag;

    /**
     * 发送时，rtp的pt（uint8_t）,不传时默认为96
     */
    private int pt = 96;

    /**
     * 发送时，rtp的负载类型。为true时，负载为ps；为false时，为es；
     */
    private boolean usePs = true;

    /**
     * 当usePs 为false时，有效。为1时，发送音频；为0时，发送视频；不传时默认为0
     */
    private boolean onlyAudio = false;

    /**
     * 是否开启rtcp保活
     */
    private boolean rtcp = false;


    /**
     * 播放类型
     */
    private InviteStreamType playType;

    /**
     * 发流的同时收流
     */
    private String receiveStream;

    /**
     * 上级的点播类型
     */
    private String sessionName;

    public static SendRtpInfo getInstance(RequestPushStreamMsg requestPushStreamMsg) {
        SendRtpInfo sendRtpItem = new SendRtpInfo();
        sendRtpItem.setMediaServerId(requestPushStreamMsg.getMediaServerId());
        sendRtpItem.setApp(requestPushStreamMsg.getApp());
        sendRtpItem.setStream(requestPushStreamMsg.getStream());
        sendRtpItem.setIp(requestPushStreamMsg.getIp());
        sendRtpItem.setPort(requestPushStreamMsg.getPort());
        sendRtpItem.setSsrc(requestPushStreamMsg.getSsrc());
        sendRtpItem.setTcp(requestPushStreamMsg.isTcp());
        sendRtpItem.setLocalPort(requestPushStreamMsg.getSrcPort());
        sendRtpItem.setPt(requestPushStreamMsg.getPt());
        sendRtpItem.setUsePs(requestPushStreamMsg.isPs());
        sendRtpItem.setOnlyAudio(requestPushStreamMsg.isOnlyAudio());
        return sendRtpItem;

    }

    public static SendRtpInfo getInstance(String app, String stream, String ssrc, String dstIp, Integer dstPort, boolean tcp, int sendLocalPort, Integer pt) {
        SendRtpInfo sendRtpItem = new SendRtpInfo();
        sendRtpItem.setApp(app);
        sendRtpItem.setStream(stream);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setLocalPort(sendLocalPort);
        sendRtpItem.setIp(dstIp);
        sendRtpItem.setPort(dstPort);
        if (pt != null) {
            sendRtpItem.setPt(pt);
        }

        return sendRtpItem;
    }

    @Override
    public String toString() {
        return "SendRtpItem{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", ssrc='" + ssrc + '\'' +
                ", platformId='" + platformId + '\'' +
                ", platformName='" + platformName + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", app='" + app + '\'' +
                ", channelId='" + channelId + '\'' +
                ", status=" + status +
                ", stream='" + stream + '\'' +
                ", tcp=" + tcp +
                ", tcpActive=" + tcpActive +
                ", localIp='" + localIp + '\'' +
                ", localPort=" + localPort +
                ", mediaServerId='" + mediaServerId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", CallId='" + callId + '\'' +
                ", fromTag='" + fromTag + '\'' +
                ", toTag='" + toTag + '\'' +
                ", pt=" + pt +
                ", usePs=" + usePs +
                ", onlyAudio=" + onlyAudio +
                ", rtcp=" + rtcp +
                ", playType=" + playType +
                ", receiveStream='" + receiveStream + '\'' +
                ", sessionName='" + sessionName + '\'' +
                '}';
    }

    public String getRedisKey() {
        return VideoManagerConstants.SEND_RTP_INFO_PREFIX +
                serverId + "_"
                + mediaServerId + "_"
                + platformId + "_"
                + channelId + "_"
                + stream + "_"
                + callId;
    }
}
