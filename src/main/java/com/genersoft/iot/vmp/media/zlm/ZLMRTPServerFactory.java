package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForRtpServerTimeout;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookParam;
import com.genersoft.iot.vmp.media.zlm.dto.hook.OnRtpServerTimeoutHookParam;
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

    /**
     * 开启rtpServer
     * @param mediaServerItem zlm服务实例
     * @param streamId 流Id
     * @param ssrc ssrc
     * @param port 端口， 0/null为使用随机
     * @param reUsePort 是否重用端口
     * @param tcpMode 0/null udp 模式，1 tcp 被动模式, 2 tcp 主动模式。
     * @return
     */
    public int createRTPServer(MediaServerItem mediaServerItem, String streamId, int ssrc, Integer port, Boolean onlyAuto, Boolean reUsePort, Integer tcpMode) {
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
                            return createRTPServer(mediaServerItem, streamId, ssrc, port,onlyAuto, reUsePort, tcpMode);
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

        if (tcpMode == null) {
            tcpMode = 0;
        }
        param.put("tcp_mode", tcpMode);
        param.put("stream_id", streamId);
        if (reUsePort != null) {
            param.put("re_use_port", reUsePort?"1":"0");
        }
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
            logger.info("关闭RTP Server " +  jsonObject);
            if (jsonObject != null ) {
                if (jsonObject.getInteger("code") == 0) {
                    result = jsonObject.getInteger("hit") >= 1;
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

    public void closeRtpServer(MediaServerItem serverItem, String streamId, CommonCallback<Boolean> callback) {
        if (serverItem == null) {
            callback.run(false);
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id", streamId);
        zlmresTfulUtils.closeRtpServer(serverItem, param, jsonObject -> {
            if (jsonObject != null ) {
                if (jsonObject.getInteger("code") == 0) {
                    callback.run(jsonObject.getInteger("hit") == 1);
                    return;
                }else {
                    logger.error("关闭RTP Server 失败: " + jsonObject.getString("msg"));
                }
            }else {
                //  检查ZLM状态
                logger.error("关闭RTP Server 失败: 请检查ZLM服务");
            }
            callback.run(false);
        });


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
    public SendRtpItem createSendRtpItem(MediaServerItem serverItem, String ip, int port, String ssrc, String platformId,
                                         String deviceId, String channelId, boolean tcp, boolean rtcp, KeepPortCallback callback){

        // 默认为随机端口
        int localPort = 0;
        if (userSetting.getGbSendStreamStrict()) {
            if (userSetting.getGbSendStreamStrict()) {
                localPort = keepPort(serverItem, ssrc, localPort, callback);
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
    public SendRtpItem createSendRtpItem(MediaServerItem serverItem, String ip, int port, String ssrc, String platformId,
                                         String app, String stream, String channelId, boolean tcp, boolean rtcp, KeepPortCallback callback){
        // 默认为随机端口
        int localPort = 0;
        if (userSetting.getGbSendStreamStrict()) {
            localPort = keepPort(serverItem, ssrc, localPort, callback);
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

    public interface KeepPortCallback{
        Boolean keep(String ssrc);
    }

    /**
     * 保持端口，直到需要需要发流时再释放
     */
    public int keepPort(MediaServerItem serverItem, String ssrc, int localPort, KeepPortCallback keepPortCallback) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("port", localPort);
        param.put("enable_tcp", 1);
        param.put("stream_id", ssrc);
        param.put("re_use_port", 1);
        JSONObject jsonObject = zlmresTfulUtils.openRtpServer(serverItem, param);
        if (jsonObject.getInteger("code") == 0) {
            localPort = jsonObject.getInteger("port");
            HookSubscribeForRtpServerTimeout hookSubscribeForRtpServerTimeout = HookSubscribeFactory.on_rtp_server_timeout(ssrc, null, serverItem.getId());
            // 订阅 zlm启动事件, 新的zlm也会从这里进入系统
            int finalLocalPort = localPort;
            hookSubscribe.addSubscribe(hookSubscribeForRtpServerTimeout,
                    (MediaServerItem mediaServerItem, HookParam hookParam)->{
                        logger.info("[上级点播] {}->监听端口到期继续保持监听: {}", ssrc, finalLocalPort);
                        OnRtpServerTimeoutHookParam rtpServerTimeoutHookParam = (OnRtpServerTimeoutHookParam) hookParam;
                        if (ssrc.equals(rtpServerTimeoutHookParam.getSsrc())) {
                            if (keepPortCallback.keep(ssrc)) {
                                logger.info("[上级点播] {}->监听端口到期继续保持监听", ssrc);
                                keepPort(serverItem, ssrc, finalLocalPort, keepPortCallback);
                            }else {
                                logger.info("[上级点播] {}->发送取消，无需继续监听", ssrc);
                                releasePort(serverItem, ssrc);
                            }
                        }
                    });
            logger.info("[上级点播] {}->: {}", ssrc, localPort);
            return localPort;
        }else {
            logger.info("[上级点播] 监听端口失败: {}->{}", ssrc, localPort);
            return 0;
        }
    }

    /**
     * 释放保持的端口
     */
    public boolean releasePort(MediaServerItem serverItem, String ssrc) {
        logger.info("[保持端口] {}->释放监听端口", ssrc);
        boolean closeRTPServerResult = closeRtpServer(serverItem, ssrc);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        if (mediaInfo.getInteger("code") == -2) {
            return null;
        }
        return (mediaInfo.getInteger("code") == 0 && mediaInfo.getBoolean("online"));
    }

    /**
     * 查询待转推的流是否就绪
     */
    public Boolean isStreamReady(MediaServerItem mediaServerItem, String app, String streamId) {
        JSONObject mediaInfo = zlmresTfulUtils.getMediaList(mediaServerItem, app, streamId);
        if (mediaInfo == null || (mediaInfo.getInteger("code") == -2)) {
            return null;
        }
        return  (mediaInfo.getInteger("code") == 0
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

    public Boolean updateRtpServerSSRC(MediaServerItem mediaServerItem, String streamId, String ssrc) {
        boolean result = false;
        JSONObject jsonObject = zlmresTfulUtils.updateRtpServerSSRC(mediaServerItem, streamId, ssrc);
        if (jsonObject == null) {
            logger.error("[更新RTPServer] 失败: 请检查ZLM服务");
        } else if (jsonObject.getInteger("code") == 0) {
            result= true;
            logger.info("[更新RTPServer] 成功");
        } else {
            logger.error("[更新RTPServer] 失败: {}, streamId：{}，ssrc：{}->\r\n{}",jsonObject.getString("msg"),
                    streamId, ssrc, jsonObject);
        }
        return result;
    }
}
