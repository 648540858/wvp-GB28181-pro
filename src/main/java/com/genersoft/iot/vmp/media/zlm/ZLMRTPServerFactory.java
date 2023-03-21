package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ZLMRTPServerFactory {

    private Logger logger = LoggerFactory.getLogger("ZLMRTPServerFactory");

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private ZlmHttpHookSubscribe hookSubscribe;

    private int[] portRangeArray = new int[2];

    public int getFreePort(MediaServerItem mediaServerItem, int startPort, int endPort, List<Integer> usedFreelist) {
        if (endPort <= startPort) {
            return -1;
        }
        if (usedFreelist == null) {
            usedFreelist = new ArrayList<>();
        }
        JSONObject listRtpServerJsonResult = zlmresTfulUtils.listRtpServer(mediaServerItem);
        if (listRtpServerJsonResult != null) {
            JSONArray data = listRtpServerJsonResult.getJSONArray("data");
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    JSONObject dataItem = data.getJSONObject(i);
                    usedFreelist.add(dataItem.getInteger("port"));
                }
            }
        }

        Map<String, Object> param = new HashMap<>();
        int result = -1;
        // 设置推流端口
        if (startPort%2 == 1) {
            startPort ++;
        }
        boolean checkPort = false;
        for (int i = startPort; i < endPort  + 1; i+=2) {
            if (!usedFreelist.contains(i)){
                checkPort = true;
                startPort = i;
                break;
            }
        }
        if (!checkPort) {
            logger.warn("未找到节点{}上范围[{}-{}]的空闲端口", mediaServerItem.getId(), startPort, endPort);
            return -1;
        }
        param.put("port", startPort);
        String stream = UUID.randomUUID().toString();
        param.put("enable_tcp", 1);
        param.put("stream_id", stream);
//        param.put("port", 0);
        JSONObject openRtpServerResultJson = zlmresTfulUtils.openRtpServer(mediaServerItem, param);

        if (openRtpServerResultJson != null) {
            if (openRtpServerResultJson.getInteger("code") == 0) {
                result= openRtpServerResultJson.getInteger("port");
                Map<String, Object> closeRtpServerParam = new HashMap<>();
                closeRtpServerParam.put("stream_id", stream);
                zlmresTfulUtils.closeRtpServer(mediaServerItem, closeRtpServerParam);
            }else {
                usedFreelist.add(startPort);
                startPort +=2;
                result = getFreePort(mediaServerItem, startPort, endPort,usedFreelist);
            }
        }else {
            //  检查ZLM状态
            logger.error("创建RTP Server 失败 {}: 请检查ZLM服务", param.get("port"));
        }
        return result;
    }

    public int createRTPServer(MediaServerItem mediaServerItem, String streamId, int ssrc, Integer port, Boolean onlyAuto) {
        int result = -1;
        // 查询此rtp server 是否已经存在
        JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaServerItem, streamId);
        logger.info(JSONObject.toJSONString(rtpInfo));
        if(rtpInfo.getInteger("code") == 0){
            if (rtpInfo.getBoolean("exist")) {
                result = rtpInfo.getInteger("local_port");
                if (result == 0) {
                    // 此时说明rtpServer已经创建但是流还没有推上来
                    // 此时重新打开rtpServer
                    Map<String, Object> param = new HashMap<>();
                    param.put("stream_id", streamId);
                    JSONObject jsonObject = zlmresTfulUtils.closeRtpServer(mediaServerItem, param);
                    if (jsonObject != null ) {
                        if (jsonObject.getInteger("code") == 0) {
                            return createRTPServer(mediaServerItem, streamId, ssrc, port, onlyAuto);
                        }else {
                            logger.warn("[开启rtpServer], 重启RtpServer错误");
                        }
                    }
                }
                return result;
            }
        }else if(rtpInfo.getInteger("code") == -2){
            return result;
        }

        Map<String, Object> param = new HashMap<>();

        param.put("enable_tcp", 1);
        param.put("stream_id", streamId);
        // 推流端口设置0则使用随机端口
        if (port == null) {
            param.put("port", 0);
        }else {
            param.put("port", port);
        }
        param.put("ssrc", ssrc);
        if (onlyAuto != null) {
            param.put("only_audio", onlyAuto?"1":"0");
        }
        JSONObject openRtpServerResultJson = zlmresTfulUtils.openRtpServer(mediaServerItem, param);
        logger.info(JSONObject.toJSONString(openRtpServerResultJson));
        if (openRtpServerResultJson != null) {
            if (openRtpServerResultJson.getInteger("code") == 0) {
                result= openRtpServerResultJson.getInteger("port");
            }else {
                logger.error("创建RTP Server 失败 {}: ", openRtpServerResultJson.getString("msg"));
            }
        }else {
            //  检查ZLM状态
            logger.error("创建RTP Server 失败 {}: 请检查ZLM服务", param.get("port"));
        }
        return result;
    }

    public boolean closeRtpServer(MediaServerItem serverItem, String streamId) {
        boolean result = false;
        if (serverItem !=null){
            Map<String, Object> param = new HashMap<>();
            param.put("stream_id", streamId);
            JSONObject jsonObject = zlmresTfulUtils.closeRtpServer(serverItem, param);
            if (jsonObject != null ) {
                if (jsonObject.getInteger("code") == 0) {
                    result = jsonObject.getInteger("hit") == 1;
                }else {
                    logger.error("关闭RTP Server 失败: " + jsonObject.getString("msg"));
                }
            }else {
                //  检查ZLM状态
                logger.error("关闭RTP Server 失败: 请检查ZLM服务");
            }
        }
        return result;
    }


    /**
     * 创建一个国标推流
     * @param ip 推流ip
     * @param port 推流端口
     * @param ssrc 推流唯一标识
     * @param platformId 平台id
     * @param channelId 通道id
     * @param tcp 是否为tcp
     * @return SendRtpItem
     */
    public SendRtpItem createSendRtpItem(MediaServerItem serverItem, String ip, int port, String ssrc, String platformId, String deviceId, String channelId, boolean tcp, boolean rtcp){

        // 默认为随机端口
        int localPort = 0;
        if (userSetting.getGbSendStreamStrict()) {
            if (userSetting.getGbSendStreamStrict()) {
                localPort = keepPort(serverItem, ssrc);
                if (localPort == 0) {
                    return null;
                }
            }
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setIp(ip);
        sendRtpItem.setPort(port);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setPlatformId(platformId);
        sendRtpItem.setDeviceId(deviceId);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setRtcp(rtcp);
        sendRtpItem.setApp("rtp");
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setMediaServerId(serverItem.getId());
        return sendRtpItem;
    }

    /**
     * 创建一个直播推流
     * @param ip 推流ip
     * @param port 推流端口
     * @param ssrc 推流唯一标识
     * @param platformId 平台id
     * @param channelId 通道id
     * @param tcp 是否为tcp
     * @return SendRtpItem
     */
    public SendRtpItem createSendRtpItem(MediaServerItem serverItem, String ip, int port, String ssrc, String platformId, String app, String stream, String channelId, boolean tcp, boolean rtcp){
        // 默认为随机端口
        int localPort = 0;
        if (userSetting.getGbSendStreamStrict()) {
            localPort = keepPort(serverItem, ssrc);
            if (localPort == 0) {
                return null;
            }
        }
        SendRtpItem sendRtpItem = new SendRtpItem();
        sendRtpItem.setIp(ip);
        sendRtpItem.setPort(port);
        sendRtpItem.setSsrc(ssrc);
        sendRtpItem.setApp(app);
        sendRtpItem.setStream(stream);
        sendRtpItem.setPlatformId(platformId);
        sendRtpItem.setChannelId(channelId);
        sendRtpItem.setTcp(tcp);
        sendRtpItem.setLocalPort(localPort);
        sendRtpItem.setServerId(userSetting.getServerId());
        sendRtpItem.setMediaServerId(serverItem.getId());
        sendRtpItem.setRtcp(rtcp);
        return sendRtpItem;
    }

    /**
     * 保持端口，直到需要需要发流时再释放
     */
    public int keepPort(MediaServerItem serverItem, String ssrc) {
        int localPort = 0;
        Map<String, Object> param = new HashMap<>(3);
        param.put("port", 0);
        param.put("enable_tcp", 1);
        param.put("stream_id", ssrc);
        JSONObject jsonObject = zlmresTfulUtils.openRtpServer(serverItem, param);
        if (jsonObject.getInteger("code") == 0) {
            localPort = jsonObject.getInteger("port");
            HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(ssrc, null, serverItem.getId());
            // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
            hookSubscribe.addSubscribe(hookSubscribeForRtpServerTimeout,
                    (MediaServerItem mediaServerItem, JSONObject response)->{
                        logger.info("[保持端口] {}->监听端口到期继续保持监听", ssrc);
                        keepPort(serverItem, ssrc);
                    });
        }
        logger.info("[保持端口] {}->监听端口: {}", ssrc, localPort);
        return localPort;
    }

    /**
     * 释放保持的端口
     */
    public boolean releasePort(MediaServerItem serverItem, String ssrc) {
        logger.info("[保持端口] {}->释放监听端口", ssrc);
        boolean closeRTPServerResult = closeRtpServer(serverItem, ssrc);
        HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(ssrc, null, serverItem.getId());
        // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
        hookSubscribe.removeSubscribe(hookSubscribeForRtpServerTimeout);
        return closeRTPServerResult;
    }

    /**
     * 调用zlm RESTFUL API —— startSendRtp
     */
    public JSONObject startSendRtpStream(MediaServerItem mediaServerItem, Map<String, Object>param) {
        return zlmresTfulUtils.startSendRtp(mediaServerItem, param);
    }

    /**
     * 调用zlm RESTFUL API —— startSendRtpPassive
     */
    public JSONObject startSendRtpPassive(MediaServerItem mediaServerItem, Map<String, Object>param) {
        return zlmresTfulUtils.startSendRtpPassive(mediaServerItem, param);
    }

    public JSONObject startSendRtpPassive(MediaServerItem mediaServerItem, Map<String, Object>param, ZLMRESTfulUtils.RequestCallback callback) {
        return zlmresTfulUtils.startSendRtpPassive(mediaServerItem, param, callback);
    }

    /**
     * 查询待转推的流是否就绪
     */
    public Boolean isRtpReady(MediaServerItem mediaServerItem, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServerItem,"rtp", "rtsp", streamId);
        return (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online"));
    }

    /**
     * 查询待转推的流是否就绪
     */
    public Boolean isStreamReady(MediaServerItem mediaServerItem, String app, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaList(mediaServerItem, app, streamId);
        return mediaInfo != null && (mediaInfo.getInteger("code") == 0

                && mediaInfo.getJSONArray("data") != null
                && mediaInfo.getJSONArray("data").size() > 0);
    }

    /**
     * 查询转推的流是否有其它观看者
     * @param streamId
     * @return
     */
    public int totalReaderCount(MediaServerItem mediaServerItem, String app, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaInfo(mediaServerItem, app, "rtsp", streamId);
        if (mediaInfo == null) {
            return 0;
        }
        Integer code = mediaInfo.getInteger("code");
        if (code < 0) {
            logger.warn("查询流({}/{})是否有其它观看者时得到： {}", app, streamId, mediaInfo.getString("msg"));
            return -1;
        }
        if ( code == 0 && mediaInfo.getBoolean("online") != null && ! mediaInfo.getBoolean("online")) {
            logger.warn("查询流({}/{})是否有其它观看者时得到： {}", app, streamId, mediaInfo.getString("msg"));
            return -1;
        }
        return mediaInfo.getInteger("totalReaderCount");
    }

    /**
     * 调用zlm RESTful API —— stopSendRtp
     */
    public Boolean stopSendRtpStream(MediaServerItem mediaServerItem, Map<String, Object>param) {
        Boolean result = false;
        JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaServerItem, param);
        if (jsonObject == null) {
            logger.error("[停止RTP推流] 失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            result= true;
            logger.info("[停止RTP推流] 成功");
        } else {
            logger.warn("[停止RTP推流] 失败: {}, 参数：{}->\r\n{}",jsonObject.getString("msg"), JSON.toJSON(param), jsonObject);
        }
        return result;
    }

    public JSONObject startSendRtp(MediaServerItem mediaInfo, SendRtpItem sendRtpItem) {
        String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
        logger.info("rtp/{}开始推流, 目标={}:{}，SSRC={}", sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc());
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app",sendRtpItem.getApp());
        param.put("stream",sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
        }

        if (mediaInfo == null) {
            return null;
        }
        // 如果是非严格模式，需要关闭端口占用
        JSONObject startSendRtpStreamResult = null;
        if (sendRtpItem.getLocalPort() != 0) {
            HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(sendRtpItem.getSsrc(), null, mediaInfo.getId());
            hookSubscribe.removeSubscribe(hookSubscribeForRtpServerTimeout);
            if (releasePort(mediaInfo, sendRtpItem.getSsrc())) {
                if (sendRtpItem.isTcpActive()) {
                    startSendRtpStreamResult = startSendRtpPassive(mediaInfo, param);
                    System.out.println(JSON.toJSON(param));
                }else {
                    param.put("is_udp", is_Udp);
                    param.put("dst_url", sendRtpItem.getIp());
                    param.put("dst_port", sendRtpItem.getPort());
                    startSendRtpStreamResult = startSendRtpStream(mediaInfo, param);
                }
            }
        }else {
            if (sendRtpItem.isTcpActive()) {
                startSendRtpStreamResult = startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("is_udp", is_Udp);
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                startSendRtpStreamResult = startSendRtpStream(mediaInfo, param);
            }
        }
        return startSendRtpStreamResult;
    }
}
