package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("zlm")
public class ZLMMediaNodeServerService implements IMediaNodeServerService {

    private final static Logger logger = LoggerFactory.getLogger(ZLMMediaNodeServerService.class);

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Override
    public int createRTPServer(MediaServer mediaServer, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        return zlmServerFactory.createRTPServer(mediaServer, streamId, ssrc, port, onlyAuto, reUsePort, tcpMode);
    }

    @Override
    public void closeRtpServer(MediaServer mediaServer, String streamId) {
        zlmServerFactory.closeRtpServer(mediaServer, streamId);
    }

    @Override
    public void closeRtpServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback) {
        zlmServerFactory.closeRtpServer(mediaServer, streamId, callback);
    }

    @Override
    public void closeStreams(MediaServer mediaServer, String app, String stream) {
        zlmresTfulUtils.closeStreams(mediaServer, app, stream);
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServer, String streamId, String ssrc) {
        return zlmServerFactory.updateRtpServerSSRC(mediaServer, streamId, ssrc);
    }

    @Override
    public boolean checkNodeId(MediaServer mediaServer) {
        if (mediaServer == null) {
            return false;
        }
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (responseJSON != null) {
            JSONArray data = responseJSON.getJSONArray("data");
            if (data != null && !data.isEmpty()) {
                ZLMServerConfig zlmServerConfig= JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
                return zlmServerConfig.getGeneralMediaServerId().equals(mediaServer.getId());
            }else {
                return false;
            }

        }else {
            return false;
        }
    }

    @Override
    public void online(MediaServer mediaServer) {

    }

    @Override
    public MediaServer checkMediaServer(String ip, int port, String secret) {
        MediaServer mediaServer = new MediaServer();
        mediaServer.setIp(ip);
        mediaServer.setHttpPort(port);
        mediaServer.setFlvPort(port);
        mediaServer.setWsFlvPort(port);
        mediaServer.setSecret(secret);
        JSONObject responseJSON = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (responseJSON == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接失败");
        }
        JSONArray data = responseJSON.getJSONArray("data");
        if (data == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置失败");
        }
        ZLMServerConfig zlmServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), ZLMServerConfig.class);
        if (zlmServerConfig == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置失败");
        }
        mediaServer.setId(zlmServerConfig.getGeneralMediaServerId());
        mediaServer.setHttpSSlPort(zlmServerConfig.getHttpPort());
        mediaServer.setFlvSSLPort(zlmServerConfig.getHttpPort());
        mediaServer.setWsFlvSSLPort(zlmServerConfig.getHttpPort());
        mediaServer.setRtmpPort(zlmServerConfig.getRtmpPort());
        mediaServer.setRtmpSSlPort(zlmServerConfig.getRtmpSslPort());
        mediaServer.setRtspPort(zlmServerConfig.getRtspPort());
        mediaServer.setRtspSSLPort(zlmServerConfig.getRtspSSlport());
        mediaServer.setRtpProxyPort(zlmServerConfig.getRtpProxyPort());
        mediaServer.setStreamIp(ip);

        mediaServer.setHookIp("127.0.0.1");
        mediaServer.setSdpIp(ip);
        mediaServer.setType("zlm");
        return mediaServer;
    }

    @Override
    public boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        if (!ObjectUtils.isEmpty(ssrc)) {
            param.put("ssrc", ssrc);
        }
        JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaInfo, param);
        if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
            logger.error("停止发流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
        return true;

    }

    @Override
    public boolean initStopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        if (!ObjectUtils.isEmpty(ssrc)) {
            param.put("ssrc", ssrc);
        }
        JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaInfo, param);
        if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
            logger.error("停止发流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        logger.info("[zlm-deleteRecordDirectory] 删除磁盘文件, server: {} {}:{}->{}/{}", mediaServer.getId(), app, stream, date, fileName);
        JSONObject jsonObject = zlmresTfulUtils.deleteRecordDirectory(mediaServer, app,
                stream, date, fileName);
        if (jsonObject.getInteger("code") == 0) {
            return true;
        }else {
            logger.info("[zlm-deleteRecordDirectory] 删除磁盘文件错误, server: {} {}:{}->{}/{}, 结果： {}", mediaServer.getId(), app, stream, date, fileName, jsonObject);
            return false;
        }
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId) {
        List<StreamInfo> streamInfoList = new ArrayList<>();
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaServer, app, stream);
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray data = mediaList.getJSONArray("data");
                if (data == null) {
                    return null;
                }
                JSONObject mediaJSON = data.getJSONObject(0);
                MediaInfo mediaInfo = MediaInfo.getInstance(mediaJSON, mediaServer);
                mediaInfo.setOnline(true);
                StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, app, stream, mediaInfo, callId, true);
                if (streamInfo != null) {
                    streamInfoList.add(streamInfo);
                }
            }
        }
        return streamInfoList;
    }

    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String callId, boolean isPlay) {
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setStream(stream);
        streamInfoResult.setApp(app);
        String addr = mediaServer.getStreamIp();
        streamInfoResult.setIp(addr);
        streamInfoResult.setMediaServerId(mediaServer.getId());
        String callIdParam = ObjectUtils.isEmpty(callId)?"":"?callId=" + callId;
        streamInfoResult.setRtmp(addr, mediaServer.getRtmpPort(),mediaServer.getRtmpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtsp(addr, mediaServer.getRtspPort(),mediaServer.getRtspSSLPort(), app,  stream, callIdParam);
        String flvFile = String.format("%s/%s.live.flv%s", app, stream, callIdParam);
        streamInfoResult.setFlv(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), flvFile);
        streamInfoResult.setWsFlv(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), flvFile);
        streamInfoResult.setFmp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setHls(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setTs(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtc(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam, isPlay);

        streamInfoResult.setMediaInfo(mediaInfo);
        streamInfoResult.setOriginType(mediaInfo.getOriginType());
        return streamInfoResult;
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream) {
        JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServer, address, port, stream);
        logger.info("[TCP主动连接对方] 结果： {}", jsonObject);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public void getSnap(MediaServer mediaServer, String streamUrl, int timeoutSec, int expireSec, String path, String fileName) {
        zlmresTfulUtils.getSnap(mediaServer, streamUrl, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        JSONObject jsonObject = zlmresTfulUtils.getMediaInfo(mediaServer, app, "rtsp", stream);
        if (jsonObject.getInteger("code") != 0) {
            return null;
        }
        MediaInfo mediaInfo = MediaInfo.getInstance(jsonObject, mediaServer);
        mediaInfo.setOnline(true);
        return mediaInfo;
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmresTfulUtils.pauseRtpCheck(mediaServer, streamKey);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmresTfulUtils.resumeRtpCheck(mediaServer, streamKey);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        JSONObject jsonObject = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (jsonObject.getInteger("code") != 0) {
            logger.warn("[getFfmpegCmd] 获取流媒体配置失败");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取流媒体配置失败");
        }
        JSONArray dataArray = jsonObject.getJSONArray("data");
        JSONObject mediaServerConfig = dataArray.getJSONObject(0);
        if (ObjectUtils.isEmpty(cmdKey)) {
            cmdKey = "ffmpeg.cmd";
        }
       return mediaServerConfig.getString(cmdKey);
    }

    @Override
    public WVPResult<String> addFFmpegSource(MediaServer mediaServer, String srcUrl, String dstUrl, int timeoutMs, boolean enableAudio, boolean enableMp4, String ffmpegCmdKey) {
        JSONObject jsonObject = zlmresTfulUtils.addFFmpegSource(mediaServer, srcUrl, dstUrl, timeoutMs, enableAudio, enableMp4, ffmpegCmdKey);
        if (jsonObject.getInteger("code") != 0) {
            logger.warn("[getFfmpegCmd] 添加FFMPEG代理失败");
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "添加FFMPEG代理失败");
        }else {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "代理结果异常： " + jsonObject);
            }else {
                return WVPResult.success(data.getString("key"));
            }
        }
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType) {
        JSONObject jsonObject = zlmresTfulUtils.addStreamProxy(mediaServer, app, stream, url, enableAudio, enableMp4, rtpType);
        if (jsonObject.getInteger("code") != 0) {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "添加代理失败");
        }else {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "代理结果异常： " + jsonObject);
            }else {
                return WVPResult.success(data.getString("key"));
            }
        }
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmresTfulUtils.delFFmpegSource(mediaServer, streamKey);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmresTfulUtils.delStreamProxy(mediaServer, streamKey);
        return jsonObject.getInteger("code") == 0;
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        Map<String, String> result = new HashMap<>();
        JSONObject mediaServerConfigResuly = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (mediaServerConfigResuly != null && mediaServerConfigResuly.getInteger("code") == 0
                && mediaServerConfigResuly.getJSONArray("data").size() > 0){
            JSONObject mediaServerConfig = mediaServerConfigResuly.getJSONArray("data").getJSONObject(0);

            for (String key : mediaServerConfig.keySet()) {
                if (key.startsWith("ffmpeg.cmd")){
                    result.put(key, mediaServerConfig.getString(key));
                }
            }
        }
        return result;
    }

    @Override
    public void startSendRtpPassive(MediaServer mediaServer, SendRtpItem sendRtpItem, Integer timeout) {
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", sendRtpItem.isTcp() ? "0" : "1");
        param.put("recv_stream_id", sendRtpItem.getReceiveStream());
        if (timeout  != null) {
            param.put("close_delay_ms", timeout);
        }
        if (!sendRtpItem.isTcp()) {
            // 开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp()? "1":"0");
        }
        if (!sendRtpItem.isTcpActive()) {
            param.put("dst_url",sendRtpItem.getIp());
            param.put("dst_port", sendRtpItem.getPort());
        }

        JSONObject jsonObject = zlmServerFactory.startSendRtpPassive(mediaServer, param, null);
        if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
            logger.error("启动监听TCP被动推流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
        logger.info("调用ZLM-TCP被动推流接口, 结果： {}",  jsonObject);
        logger.info("启动监听TCP被动推流成功[ {}/{} ]，{}->{}:{}, " , sendRtpItem.getApp(), sendRtpItem.getStream(),
                jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
    }

    @Override
    public void startSendRtpStream(MediaServer mediaServer, SendRtpItem sendRtpItem) {
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost", "__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("src_port", sendRtpItem.getLocalPort());
        param.put("pt", sendRtpItem.getPt());
        param.put("use_ps", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("is_udp", sendRtpItem.isTcp() ? "0" : "1");
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "1" : "0");
        }
        param.put("dst_url", sendRtpItem.getIp());
        param.put("dst_port", sendRtpItem.getPort());
        JSONObject jsonObject = zlmresTfulUtils.startSendRtp(mediaServer, param);
        if (jsonObject == null || jsonObject.getInteger("code") != 0 ) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        MediaInfo mediaInfo = getMediaInfo(mediaServer, app, stream);
        if (mediaInfo == null) {
            logger.warn("[获取下载进度] 查询进度失败, 节点Id： {}， {}/{}", mediaServer.getId(), app, stream);
            return null;
        }
        return mediaInfo.getDuration();
    }
}
