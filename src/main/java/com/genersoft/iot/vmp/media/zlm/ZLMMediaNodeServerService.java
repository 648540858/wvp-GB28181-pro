package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.ZLMServerConfig;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Slf4j
@Service("zlm")
public class ZLMMediaNodeServerService implements IMediaNodeServerService {


    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private UserSetting userSetting;

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
        mediaServer.setServerId(userSetting.getServerId());
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
        mediaServer.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
        mediaServer.setFlvSSLPort(zlmServerConfig.getHttpSSLport());
        mediaServer.setWsFlvSSLPort(zlmServerConfig.getHttpSSLport());
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
        if (jsonObject.getInteger("code") != null && jsonObject.getInteger("code") == 0) {
            log.info("[停止发流] 成功: 参数：{}", JSON.toJSONString(param));
            return true;
        }else {
            log.info("停止发流结果: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            return false;
        }
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
            log.error("停止发流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        log.info("[zlm-deleteRecordDirectory] 删除磁盘文件, server: {} {}:{}->{}/{}", mediaServer.getId(), app, stream, date, fileName);
        JSONObject jsonObject = zlmresTfulUtils.deleteRecordDirectory(mediaServer, app,
                stream, date, fileName);
        if (jsonObject.getInteger("code") == 0) {
            return true;
        }else {
            log.info("[zlm-deleteRecordDirectory] 删除磁盘文件错误, server: {} {}:{}->{}/{}, 结果： {}", mediaServer.getId(), app, stream, date, fileName, jsonObject);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "删除磁盘文件失败");
        }
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId) {
        List<StreamInfo> streamInfoList = new ArrayList<>();
        JSONObject mediaList = zlmresTfulUtils.getMediaList(mediaServer, app, stream);
        if (mediaList != null) {
            if (mediaList.getInteger("code") == 0) {
                JSONArray dataArray = mediaList.getJSONArray("data");
                if (dataArray == null) {
                    return streamInfoList;
                }
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject mediaJSON = dataArray.getJSONObject(0);
                    MediaInfo mediaInfo = MediaInfo.getInstance(mediaJSON, mediaServer, userSetting.getServerId());
                    StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, mediaInfo.getApp(), mediaInfo.getStream(), mediaInfo, callId, true);
                    if (streamInfo != null) {
                        streamInfoList.add(streamInfo);
                    }
                }
            }
        }
        return streamInfoList;
    }

    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String callId, boolean isPlay) {
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setServerId(userSetting.getServerId());
        streamInfoResult.setStream(stream);
        streamInfoResult.setApp(app);
        String addr = mediaServer.getStreamIp();
        streamInfoResult.setIp(addr);
        streamInfoResult.setMediaServer(mediaServer);

        Map<String, String> param = new HashMap<>();
        if (!ObjectUtils.isEmpty(callId)) {
            param.put("callId", callId);
        }
        if (mediaInfo != null && !ObjectUtils.isEmpty(mediaInfo.getOriginTypeStr()))  {
            param.put("originTypeStr", mediaInfo.getOriginTypeStr());
        }
        StringBuilder callIdParamBuilder = new StringBuilder();
        if (!param.isEmpty()) {
            callIdParamBuilder.append("?");
            for (Map.Entry<String, String> entry : param.entrySet()) {
                callIdParamBuilder.append(entry.getKey()).append("=").append(entry.getValue());
                callIdParamBuilder.append("&");
            }
            callIdParamBuilder.deleteCharAt(callIdParamBuilder.length() - 1);
        }

        String callIdParam = callIdParamBuilder.toString();

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
        if (mediaInfo != null) {
            streamInfoResult.setOriginType(mediaInfo.getOriginType());
            streamInfoResult.setOriginTypeStr(mediaInfo.getOriginTypeStr());
        }
        return streamInfoResult;
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream) {
        JSONObject jsonObject = zlmresTfulUtils.connectRtpServer(mediaServer, address, port, stream);
        log.info("[TCP主动连接对方] 结果： {}", jsonObject);
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
        return MediaInfo.getInstance(jsonObject, mediaServer, userSetting.getServerId());
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
            log.warn("[getFfmpegCmd] 获取流媒体配置失败");
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
            log.warn("[getFfmpegCmd] 添加FFMPEG代理失败");
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
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url,
                                            boolean enableAudio, boolean enableMp4, String rtpType, Integer timeout) {
        JSONObject jsonObject = zlmresTfulUtils.addStreamProxy(mediaServer, app, stream, url, enableAudio, enableMp4, rtpType, timeout);
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
    public Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
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
            log.error("启动监听TCP被动推流失败: {}, 参数：{}", jsonObject.getString("msg"), JSON.toJSONString(param));
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
        log.info("调用ZLM-TCP被动推流接口, 结果： {}",  jsonObject);
        log.info("启动监听TCP被动推流成功[ {}/{} ]，{}->{}:{}, " , sendRtpItem.getApp(), sendRtpItem.getStream(),
                jsonObject.getString("local_port"), param.get("dst_url"), param.get("dst_port"));
        return jsonObject.getInteger("local_port");
    }

    @Override
    public void startSendRtpStream(MediaServer mediaServer, SendRtpInfo sendRtpItem) {
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
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "500" : "0");
        }
        param.put("dst_url", sendRtpItem.getIp());
        param.put("dst_port", sendRtpItem.getPort());
        JSONObject jsonObject = zlmresTfulUtils.startSendRtp(mediaServer, param);
        if (jsonObject == null ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接zlm失败");
        }else if (jsonObject.getInteger("code") != 0) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
        log.info("[推流结果]：{} ，参数： {}",jsonObject, JSONObject.toJSONString(param));
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        MediaInfo mediaInfo = getMediaInfo(mediaServer, app, stream);
        if (mediaInfo == null) {
            log.warn("[获取下载进度] 查询进度失败, 节点Id： {}， {}/{}", mediaServer.getId(), app, stream);
            return null;
        }
        return mediaInfo.getDuration();
    }

    @Override
    public StreamInfo startProxy(MediaServer mediaServer, StreamProxy streamProxy) {
        String dstUrl;
        if ("ffmpeg".equalsIgnoreCase(streamProxy.getType())) {

            String ffmpegCmd = getFfmpegCmd(mediaServer, streamProxy.getFfmpegCmdKey());

            if (ffmpegCmd == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "ffmpeg拉流代理无法获取ffmpeg cmd");
            }
            String schema = getSchemaFromFFmpegCmd(ffmpegCmd);
            if (schema == null) {
                throw new ControllerException(ErrorCode.ERROR100.getCode(), "ffmpeg拉流代理无法从ffmpeg cmd中获取到输出格式");
            }
            int port;
            String schemaForUri;
            if (schema.equalsIgnoreCase("rtsp")) {
                port = mediaServer.getRtspPort();
                schemaForUri = schema;
            }else if (schema.equalsIgnoreCase("flv")) {
                if (mediaServer.getRtmpPort() == 0) {
                    throw new ControllerException(ErrorCode.ERROR100.getCode(), "ffmpeg拉流代理播放时发现未设置rtmp端口");
                }
                port = mediaServer.getRtmpPort();
                schemaForUri = "rtmp";
            }else {
                port = mediaServer.getRtmpPort();
                schemaForUri = schema;
            }

            dstUrl = String.format("%s://%s:%s/%s/%s", schemaForUri, "127.0.0.1", port, streamProxy.getApp(),
                    streamProxy.getStream());
        }else {
            dstUrl = String.format("rtsp://%s:%s/%s/%s", "127.0.0.1", mediaServer.getRtspPort(), streamProxy.getApp(),
                    streamProxy.getStream());
        }
        MediaInfo mediaInfo = getMediaInfo(mediaServer, streamProxy.getApp(), streamProxy.getStream());

        if (mediaInfo != null) {
            if (mediaInfo.getOriginUrl() != null && mediaInfo.getOriginUrl().equals(streamProxy.getSrcUrl())) {
                log.info("[启动拉流代理] 已存在， 直接返回， app： {}, stream: {}", mediaInfo.getApp(), streamProxy.getStream());
                return getStreamInfoByAppAndStream(mediaServer, streamProxy.getApp(), streamProxy.getStream(), mediaInfo, null, true);
            }
            closeStreams(mediaServer, streamProxy.getApp(), streamProxy.getStream());
        }

        JSONObject jsonObject = null;
        if ("ffmpeg".equalsIgnoreCase(streamProxy.getType())){
            if (streamProxy.getTimeout() == 0) {
                streamProxy.setTimeout(15);
            }
            jsonObject = zlmresTfulUtils.addFFmpegSource(mediaServer, streamProxy.getSrcUrl().trim(), dstUrl,
                    streamProxy.getTimeout(), streamProxy.isEnableAudio(), streamProxy.isEnableMp4(),
                    streamProxy.getFfmpegCmdKey());
        }else {
            jsonObject = zlmresTfulUtils.addStreamProxy(mediaServer, streamProxy.getApp(), streamProxy.getStream(), streamProxy.getSrcUrl().trim(),
                    streamProxy.isEnableAudio(), streamProxy.isEnableMp4(), streamProxy.getRtspType(), streamProxy.getTimeout());
        }
        if (jsonObject == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }else if (jsonObject.getInteger("code") != 0) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }else {
            JSONObject data = jsonObject.getJSONObject("data");
            if (data == null) {
                throw new ControllerException(jsonObject.getInteger("code"), "代理结果异常： " + jsonObject);
            }else {
                streamProxy.setStreamKey(data.getString("key"));
                // 由于此时流未注册，手动拼装流信息
                mediaInfo = new MediaInfo();
                mediaInfo.setApp(streamProxy.getApp());
                mediaInfo.setStream(streamProxy.getStream());
                mediaInfo.setOriginType(4);
                mediaInfo.setOriginTypeStr("pull");
                return getStreamInfoByAppAndStream(mediaServer, streamProxy.getApp(), streamProxy.getStream(), mediaInfo, null, true);
            }
        }
    }

    private String getSchemaFromFFmpegCmd(String ffmpegCmd) {
        ffmpegCmd = ffmpegCmd.replaceAll(" + ", " ");
        String[] paramArray = ffmpegCmd.split(" ");
        if (paramArray.length == 0) {
            return null;
        }
        for (int i = 0; i < paramArray.length; i++) {
            if (paramArray[i].equalsIgnoreCase("-f")) {
                if (i + 1 < paramArray.length - 1) {
                    return paramArray[i+1];
                }else {
                    return null;
                }

            }
        }
        return null;
    }

    @Override
    public void stopProxy(MediaServer mediaServer, String streamKey) {
        JSONObject jsonObject = zlmresTfulUtils.delStreamProxy(mediaServer, streamKey);
        if (jsonObject == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }else if (jsonObject.getInteger("code") != 0) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
    }

    @Override
    public List<String> listRtpServer(MediaServer mediaServer) {
        JSONObject jsonObject = zlmresTfulUtils.listRtpServer(mediaServer);
        List<String> result = new ArrayList<>();
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            return result;
        }
        JSONArray data = jsonObject.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (int i = 0; i < data.size(); i++) {
            JSONObject dataJSONObject = data.getJSONObject(i);
            result.add(dataJSONObject.getString("stream_id"));
        }
        return result;
    }

    @Override
    public void loadMP4File(MediaServer mediaServer, String app, String stream, String datePath) {
        JSONObject jsonObject =  zlmresTfulUtils.loadMP4File(mediaServer, app, stream, datePath);
        if (jsonObject == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (jsonObject.getInteger("code") != 0) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
    }

    @Override
    public void seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema) {
        JSONObject jsonObject =  zlmresTfulUtils.seekRecordStamp(mediaServer, app, stream, stamp, schema);
        if (jsonObject == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (jsonObject.getInteger("code") != 0) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
    }

    @Override
    public void setRecordSpeed(MediaServer mediaServer, String app, String stream, Integer speed, String schema) {
        JSONObject jsonObject =  zlmresTfulUtils.setRecordSpeed(mediaServer, app, stream, speed, schema);
        if (jsonObject == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (jsonObject.getInteger("code") != 0) {
            throw new ControllerException(jsonObject.getInteger("code"), jsonObject.getString("msg"));
        }
    }
}
