package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ZLMServerFactory {
    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;


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
    public int createRTPServer(MediaServer mediaServerItem, String app, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        int result = -1;
        // 查询此rtp server 是否已经存在
        ZLMResult<?> rtpInfoResult = zlmresTfulUtils.getRtpInfo(mediaServerItem, streamId);
        if(rtpInfoResult.getCode() == 0){
            if (rtpInfoResult.getExist() != null && rtpInfoResult.getExist()) {
                result = rtpInfoResult.getLocal_port();
                if (result == 0) {
                    // 此时说明rtpServer已经创建但是流还没有推上来
                    // 此时重新打开rtpServer
                    Map<String, Object> param = new HashMap<>();
                    param.put("stream_id", streamId);
                    ZLMResult<?> zlmResult = zlmresTfulUtils.closeRtpServer(mediaServerItem, param);
                    if (zlmResult != null ) {
                        if (zlmResult.getCode() == 0) {
                            return createRTPServer(mediaServerItem, streamId, app, ssrc, port,onlyAuto, reUsePort,disableAudio, tcpMode);
                        }else {
                            log.warn("[开启rtpServer], 重启RtpServer错误");
                        }
                    }
                }
                return result;
            }
        }else if(rtpInfoResult.getCode() == -2){
            return result;
        }

        Map<String, Object> param = new HashMap<>();

        if (tcpMode == null) {
            tcpMode = 0;
        }
        param.put("tcp_mode", tcpMode);
        param.put("app", app);
        param.put("stream_id", streamId);
        if (disableAudio != null) {
            param.put("only_track", disableAudio?2:0);
        }

        if (reUsePort != null) {
            param.put("re_use_port", reUsePort?"1":"0");
        }
        // 推流端口设置0则使用随机端口
        if (port == null) {
            param.put("port", 0);
        }else {
            param.put("port", port);
        }
        if (onlyAuto != null) {
            param.put("only_audio", onlyAuto?"1":"0");
        }
        if (ssrc != 0) {
            param.put("ssrc", ssrc);
        }

        ZLMResult<?> zlmResult = zlmresTfulUtils.openRtpServer(mediaServerItem, param);
        if (zlmResult != null) {
            if (zlmResult.getCode() == 0) {
                result= zlmResult.getPort();
            }else {
                log.error("创建RTP Server 失败 {}: ", zlmResult.getMsg());
            }
        }else {
            //  检查ZLM状态
            log.error("创建RTP Server 失败 {}: 请检查ZLM服务", param.get("port"));
        }
        return result;
    }

    public boolean closeRtpServer(MediaServer serverItem, String streamId) {
        boolean result = false;
        if (serverItem !=null){
            Map<String, Object> param = new HashMap<>();
            param.put("stream_id", streamId);
            ZLMResult<?> zlmResult = zlmresTfulUtils.closeRtpServer(serverItem, param);
            if (zlmResult != null ) {
                if (zlmResult.getCode() == 0) {
                    result = zlmResult.getHit() >= 1;
                }else {
                    log.error("关闭RTP Server 失败: " + zlmResult.getMsg());
                }
            }else {
                //  检查ZLM状态
                log.error("关闭RTP Server 失败: 请检查ZLM服务");
            }
        }
        return result;
    }

    public void closeRtpServer(MediaServer serverItem, String streamId, CommonCallback<Boolean> callback) {
        if (serverItem == null) {
            if (callback != null) {
                callback.run(false);
            }
            return;
        }
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id", streamId);
        zlmresTfulUtils.closeRtpServer(serverItem, param, zlmResult -> {
            if (zlmResult.getCode() == 0) {
                if (callback != null) {
                    callback.run(zlmResult.getHit() >= 1);
                }
                return;
            }else {
                log.error("关闭RTP Server 失败: " + zlmResult.getMsg());
            }
            if (callback != null) {
                callback.run(false);
            }
        });
    }


    /**
     * 调用zlm RESTFUL API —— startSendRtp
     */
    public ZLMResult<?> startSendRtpStream(MediaServer mediaServerItem, Map<String, Object>param) {
        return zlmresTfulUtils.startSendRtp(mediaServerItem, param);
    }

    /**
     * 调用zlm RESTFUL API —— startSendRtpPassive
     */
    public ZLMResult<?> startSendRtpPassive(MediaServer mediaServerItem, Map<String, Object>param) {
        return zlmresTfulUtils.startSendRtpPassive(mediaServerItem, param);
    }

    public ZLMResult<?> startSendRtpPassive(MediaServer mediaServerItem, Map<String, Object> param, ZLMRESTfulUtils.ResultCallback callback) {
        return zlmresTfulUtils.startSendRtpPassive(mediaServerItem, param, callback);
    }

    public ZLMResult<?> startSendRtpTalk(MediaServer mediaServer, Map<String, Object> param, ZLMRESTfulUtils.ResultCallback callback) {
        return zlmresTfulUtils.startSendRtpTalk(mediaServer, param, callback);
    }

    /**
     * 查询待转推的流是否就绪
     */
    public Boolean isStreamReady(MediaServer mediaServerItem, String app, String streamId) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.getMediaList(mediaServerItem, app, streamId);
        if (zlmResult == null || zlmResult.getCode() == -2) {
            return null;
        }
        ZLMResult<JSONArray> result = (ZLMResult<JSONArray>) zlmResult;
        return  (result.getCode() == 0
                && result.getData() != null
                && !result.getData().isEmpty());
    }

    public ZLMResult<?> startSendRtp(MediaServer mediaInfo, SendRtpInfo sendRtpItem) {
        String is_Udp = sendRtpItem.isTcp() ? "0" : "1";
        log.info("rtp/{}开始推流, 目标={}:{}，SSRC={}", sendRtpItem.getStream(), sendRtpItem.getIp(), sendRtpItem.getPort(), sendRtpItem.getSsrc());
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
        ZLMResult<?> zlmResult = null;
        if (sendRtpItem.getLocalPort() != 0) {
            if (sendRtpItem.isTcpActive()) {
                zlmResult = startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("is_udp", is_Udp);
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                zlmResult = startSendRtpStream(mediaInfo, param);
            }
        }else {
            if (sendRtpItem.isTcpActive()) {
                zlmResult = startSendRtpPassive(mediaInfo, param);
            }else {
                param.put("is_udp", is_Udp);
                param.put("dst_url", sendRtpItem.getIp());
                param.put("dst_port", sendRtpItem.getPort());
                zlmResult = startSendRtpStream(mediaInfo, param);
            }
        }
        return zlmResult;
    }

    public Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc) {
        boolean result = false;
        ZLMResult<?> zlmResult = zlmresTfulUtils.updateRtpServerSSRC(mediaServerItem, streamId, ssrc);
        if (zlmResult.getCode() == 0) {
            result= true;
            log.info("[更新RTPServer] 成功");
        } else {
            log.error("[更新RTPServer] 失败: {}, streamId：{}，ssrc：{}", zlmResult.getMsg(),
                    streamId, ssrc);
        }
        return result;
    }

    public ZLMResult<?> stopSendRtpStream(MediaServer mediaServerItem, SendRtpInfo sendRtpItem) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        return zlmresTfulUtils.stopSendRtp(mediaServerItem, param);
    }


}
