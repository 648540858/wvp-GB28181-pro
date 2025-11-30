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
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.hook.Hook;
import com.genersoft.iot.vmp.media.event.hook.HookSubscribe;
import com.genersoft.iot.vmp.media.event.hook.HookType;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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

    @Autowired
    private HookSubscribe subscribe;

    @Override
    public int createRTPServer(MediaServer mediaServer, String streamId, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        return zlmServerFactory.createRTPServer(mediaServer, "rtp", streamId, ssrc, port, onlyAuto, disableAudio, reUsePort, tcpMode);
    }

    @Override
    public void closeRtpServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback) {
        zlmServerFactory.closeRtpServer(mediaServer, streamId, callback);
    }

    @Override
    public int createJTTServer(MediaServer mediaServer, String streamId, Integer port, Boolean disableVideo, Boolean disableAudio, Integer tcpMode) {
        return zlmServerFactory.createRTPServer(mediaServer, "1078", streamId, 0, port, disableVideo, disableAudio, false, tcpMode);
    }

    @Override
    public void closeJTTServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback) {
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
        ZLMResult<List<JSONObject>> mediaServerConfig = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (mediaServerConfig != null) {
            List<JSONObject> data = mediaServerConfig.getData();
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
        mediaServer.setSecret(secret);
        ZLMResult<List<JSONObject>> mediaServerConfigResult = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (mediaServerConfigResult == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接失败");
        }
        List<JSONObject> configList = mediaServerConfigResult.getData();
        if (configList == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置失败");
        }
        ZLMServerConfig zlmServerConfig = JSON.parseObject(JSON.toJSONString(configList.get(0)), ZLMServerConfig.class);
        if (zlmServerConfig == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "读取配置失败");
        }
        mediaServer.setId(zlmServerConfig.getGeneralMediaServerId());
        mediaServer.setHttpSSlPort(zlmServerConfig.getHttpSSLport());
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
        ZLMResult<?> zlmResult = zlmresTfulUtils.stopSendRtp(mediaInfo, param);
        if (zlmResult.getCode() == 0) {
            log.info("[停止发流] 成功: 参数：{}", JSON.toJSONString(param));
            return true;
        }else {
            log.info("停止发流结果: {}, 参数：{}", zlmResult.getMsg(), JSON.toJSONString(param));
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
        ZLMResult<?> zlmResult = zlmresTfulUtils.stopSendRtp(mediaInfo, param);
        if (zlmResult.getCode() != 0 ) {
            log.error("停止发流失败: {}, 参数：{}", zlmResult.getMsg(), JSON.toJSONString(param));
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        log.info("[zlm-deleteRecordDirectory] 删除磁盘文件, server: {} {}:{}->{}/{}", mediaServer.getId(), app, stream, date, fileName);
        ZLMResult<?> zlmResult = zlmresTfulUtils.deleteRecordDirectory(mediaServer, app,
                stream, date, fileName);
        if (zlmResult.getCode() == 0) {
            return true;
        }else {
            log.info("[zlm-deleteRecordDirectory] 删除磁盘文件错误, server: {} {}:{}->{}/{}, 结果： {}", mediaServer.getId(), app, stream, date, fileName, zlmResult);
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "删除磁盘文件失败");
        }
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId) {
        List<StreamInfo> streamInfoList = new ArrayList<>();
        ZLMResult<JSONArray> zlmResult = zlmresTfulUtils.getMediaList(mediaServer, app, stream);
        if (zlmResult != null) {
            if (zlmResult.getCode() == 0) {
                if (zlmResult.getData() == null) {
                    return streamInfoList;
                }
                for (int i = 0; i < zlmResult.getData().size(); i++) {
                    JSONObject mediaJSON = zlmResult.getData().getJSONObject(0);
                    MediaInfo mediaInfo = MediaInfo.getInstance(mediaJSON, mediaServer, userSetting.getServerId());
                    StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, mediaInfo.getApp(),
                            mediaInfo.getStream(), mediaInfo, null, callId, true);
                    if (streamInfo != null) {
                        streamInfoList.add(streamInfo);
                    }
                }
            }
        }
        return streamInfoList;
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServer, String address, int port, String stream) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.connectRtpServer(mediaServer, address, port, stream);
        log.info("[TCP主动连接对方] 结果： {}", zlmResult);
        return zlmResult.getCode() == 0;
    }

    @Override
    public void getSnap(MediaServer mediaServer, String app, String stream, int timeoutSec, int expireSec, String path, String fileName) {
        String streamUrl;
        if (mediaServer.getRtspPort() != 0) {
            streamUrl = String.format("rtsp://127.0.0.1:%s/%s/%s", mediaServer.getRtspPort(), app, stream);
        } else {
            streamUrl = String.format("http://127.0.0.1:%s/%s/%s.live.mp4", mediaServer.getHttpPort(), app, stream);
        }
        zlmresTfulUtils.getSnap(mediaServer, streamUrl, timeoutSec, expireSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        ZLMResult<JSONObject> zlmResult = zlmresTfulUtils.getMediaInfo(mediaServer, app, "rtsp", stream);
        if (zlmResult.getCode() != 0 || zlmResult.getData() == null || zlmResult.getData().getString("app") == null ) {
            return null;
        }
        return MediaInfo.getInstance(zlmResult.getData(), mediaServer, userSetting.getServerId());
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.pauseRtpCheck(mediaServer, streamKey);
        return zlmResult.getCode() == 0;
    }

    @Override
    public Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.resumeRtpCheck(mediaServer, streamKey);
        return zlmResult.getCode() == 0;
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        ZLMResult<List<JSONObject>> mediaServerConfigResult = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (mediaServerConfigResult == null || mediaServerConfigResult.getCode() != 0) {
            log.warn("[getFfmpegCmd] 获取流媒体配置失败");
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "获取流媒体配置失败");
        }
        List<JSONObject> data = mediaServerConfigResult.getData();
        JSONObject mediaServerConfig = data.get(0);
        if (ObjectUtils.isEmpty(cmdKey)) {
            cmdKey = "ffmpeg.cmd";
        }
       return mediaServerConfig.getString(cmdKey);
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url,
                                            boolean enableAudio, boolean enableMp4, String rtpType, Integer timeout) {
        ZLMResult<StreamProxyResult> zlmResult = zlmresTfulUtils.addStreamProxy(mediaServer, app, stream, url, enableAudio, enableMp4, rtpType, timeout);
        if (zlmResult.getCode() != 0) {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), "添加代理失败");
        }else {
            StreamProxyResult data = zlmResult.getData();
            if (data == null) {
                return WVPResult.fail(ErrorCode.ERROR100.getCode(), "代理结果异常");
            }else {
                return WVPResult.success(data.getKey());
            }
        }
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        ZLMResult<FlagData> flagDataZLMResult = zlmresTfulUtils.delFFmpegSource(mediaServer, streamKey);
        return flagDataZLMResult != null && flagDataZLMResult.getCode() == 0;
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServer, String streamKey) {
        ZLMResult<FlagData> flagDataZLMResult = zlmresTfulUtils.delStreamProxy(mediaServer, streamKey);
        return flagDataZLMResult != null && flagDataZLMResult.getCode() == 0;
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        Map<String, String> result = new HashMap<>();
        ZLMResult<List<JSONObject>> mediaServerConfigResult = zlmresTfulUtils.getMediaServerConfig(mediaServer);
        if (mediaServerConfigResult != null && mediaServerConfigResult.getCode() == 0
                && !mediaServerConfigResult.getData().isEmpty()){
            JSONObject jsonObject = mediaServerConfigResult.getData().get(0);

            for (String key : jsonObject.keySet()) {
                if (key.startsWith("ffmpeg.cmd")){
                    result.put(key, jsonObject.getString(key));
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
        param.put("enable_origin_recv_limit", "1");
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

        ZLMResult<?> zlmResult = zlmServerFactory.startSendRtpPassive(mediaServer, param, null);
        if (zlmResult.getCode() != 0 ) {
            log.error("启动监听TCP被动推流失败: {}, 参数：{}", zlmResult.getMsg(), JSON.toJSONString(param));
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
        log.info("调用ZLM-TCP被动推流接口成功： 本地端口: {}",  zlmResult.getLocal_port());
        return zlmResult.getLocal_port();
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
        param.put("enable_origin_recv_limit", "1");
        if (!sendRtpItem.isTcp()) {
            // udp模式下开启rtcp保活
            param.put("udp_rtcp_timeout", sendRtpItem.isRtcp() ? "500" : "0");
        }
        param.put("dst_url", sendRtpItem.getIp());
        param.put("dst_port", sendRtpItem.getPort());
        ZLMResult<?> zlmResult = zlmresTfulUtils.startSendRtp(mediaServer, param);
        if (zlmResult == null ) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "连接zlm失败");
        }else if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
        log.info("[推流结果]：{} ，参数： {}", zlmResult, JSONObject.toJSONString(param));
    }

    @Override
    public Integer startSendRtpTalk(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
        Map<String, Object> param = new HashMap<>(12);
        param.put("vhost","__defaultVhost__");
        param.put("app", sendRtpItem.getApp());
        param.put("stream", sendRtpItem.getStream());
        param.put("ssrc", sendRtpItem.getSsrc());
        param.put("pt", sendRtpItem.getPt());
        param.put("type", sendRtpItem.isUsePs() ? "1" : "0");
        param.put("only_audio", sendRtpItem.isOnlyAudio() ? "1" : "0");
        param.put("recv_stream_id", sendRtpItem.getReceiveStream());
        param.put("enable_origin_recv_limit", "1");
        ZLMResult<?> zlmResult = zlmServerFactory.startSendRtpTalk(mediaServer, param, null);
        if (zlmResult.getCode() != 0 ) {
            log.error("启动监听TCP被动推流失败: {}, 参数：{}", zlmResult.getMsg(), JSON.toJSONString(param));
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
        log.info("调用ZLM-TCP被动推流接口, 成功 本地端口： {}",  zlmResult.getLocal_port());
        return zlmResult.getLocal_port();
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
    public String startProxy(MediaServer mediaServer, StreamProxy streamProxy) {
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
            closeStreams(mediaServer, streamProxy.getApp(), streamProxy.getStream());
        }

        ZLMResult<StreamProxyResult> zlmResult = null;
        if ("ffmpeg".equalsIgnoreCase(streamProxy.getType())){
            if (streamProxy.getTimeout() == 0) {
                streamProxy.setTimeout(15);
            }
            zlmResult = zlmresTfulUtils.addFFmpegSource(mediaServer, streamProxy.getSrcUrl().trim(), dstUrl,
                    streamProxy.getTimeout(), streamProxy.isEnableAudio(), streamProxy.isEnableMp4(),
                    streamProxy.getFfmpegCmdKey());
        }else {
            zlmResult = zlmresTfulUtils.addStreamProxy(mediaServer, streamProxy.getApp(), streamProxy.getStream(), streamProxy.getSrcUrl().trim(),
                    streamProxy.isEnableAudio(), streamProxy.isEnableMp4(), streamProxy.getRtspType(), streamProxy.getTimeout());
        }
        if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }else {
            StreamProxyResult data = zlmResult.getData();
            if (data == null) {
                throw new ControllerException(zlmResult.getCode(), "代理结果异常： " + zlmResult);
            }else {
                return data.getKey();
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
    public void stopProxy(MediaServer mediaServer, String streamKey, String type) {
        ZLMResult<FlagData> zlmResult = zlmresTfulUtils.delStreamProxy(mediaServer, streamKey);
        if (zlmResult == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }else if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
    }

    @Override
    public List<String> listRtpServer(MediaServer mediaServer) {
        ZLMResult<List<RtpServerResult>> zlmResult = zlmresTfulUtils.listRtpServer(mediaServer);
        List<String> result = new ArrayList<>();
        if (zlmResult.getCode() != 0) {
            return result;
        }
        List<RtpServerResult> data = zlmResult.getData();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (RtpServerResult datum : data) {
            result.add(datum.getStream_id());
        }
        return result;
    }

    @Override
    public void loadMP4File(MediaServer mediaServer, String app, String stream, String filePath, String fileName, ErrorCallback<StreamInfo> callback) {
        String buildApp = "mp4_record";
        String buildStream = app + "_" + stream + "_" + fileName + "_" + RandomStringUtils.randomAlphabetic(6).toLowerCase();

        Hook hook = Hook.getInstance(HookType.on_media_arrival, buildApp, buildStream, mediaServer.getServerId());
        subscribe.addSubscribe(hook, (hookData) -> {
            StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, buildApp, buildStream, hookData.getMediaInfo(), null, null, true);
            if (callback != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }
        });

        ZLMResult<?> zlmResult = zlmresTfulUtils.loadMP4File(mediaServer, buildApp, buildStream, filePath);

        if (zlmResult == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
    }

    @Override
    public void loadMP4FileForDate(MediaServer mediaServer, String app, String stream, String date, String dateDir, ErrorCallback<StreamInfo> callback) {
        String buildApp = "mp4_record";
        String buildStream = app + "_" + stream + "_" + date;
        MediaInfo mediaInfo = getMediaInfo(mediaServer, buildApp, buildStream);
        if (mediaInfo != null) {
            if (callback != null) {
                StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, buildApp, buildStream, mediaInfo, null, null, true);
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }
            return;
        }

        Hook hook = Hook.getInstance(HookType.on_media_arrival, buildApp, buildStream, mediaServer.getServerId());
        subscribe.addSubscribe(hook, (hookData) -> {
            StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, buildApp, buildStream, hookData.getMediaInfo(), null, null, true);
            if (callback != null) {
                callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
            }
        });

        ZLMResult<?> zlmResult = zlmresTfulUtils.loadMP4File(mediaServer, buildApp, buildStream, dateDir);

        if (zlmResult == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
    }

    @Override
    public void seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.seekRecordStamp(mediaServer, app, stream, stamp, schema);
        if (zlmResult == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
    }

    @Override
    public void setRecordSpeed(MediaServer mediaServer, String app, String stream, Integer speed, String schema) {
        ZLMResult<?> zlmResult = zlmresTfulUtils.setRecordSpeed(mediaServer, app, stream, speed, schema);
        if (zlmResult == null) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "请求失败");
        }
        if (zlmResult.getCode() != 0) {
            throw new ControllerException(zlmResult.getCode(), zlmResult.getMsg());
        }
    }

    @Override
    public DownloadFileInfo getDownloadFilePath(MediaServer mediaServerItem, RecordInfo recordInfo) {

        // 将filePath作为独立参数传入，避免%符号解析问题
        String pathTemplate = "%s://%s:%s/index/api/downloadFile?file_path=%s";

        DownloadFileInfo info = new DownloadFileInfo();

        // filePath作为第4个参数
        info.setHttpPath(String.format(pathTemplate,
                "http",
                mediaServerItem.getStreamIp(),
                mediaServerItem.getHttpPort(),
                recordInfo.getFilePath()));

        // 同样作为第4个参数
        if (mediaServerItem.getHttpSSlPort() > 0) {
            info.setHttpsPath(String.format(pathTemplate,
                    "https",
                    mediaServerItem.getStreamIp(),
                    mediaServerItem.getHttpSSlPort(),
                    recordInfo.getFilePath()));
        }
        return info;
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo, String addr, String callId, boolean isPlay) {
        StreamInfo streamInfoResult = new StreamInfo();
        streamInfoResult.setStream(stream);
        streamInfoResult.setApp(app);
        if (addr == null) {
            addr = mediaServer.getStreamIp();
        }

        streamInfoResult.setIp(addr);
        if (mediaInfo != null) {
            streamInfoResult.setServerId(mediaInfo.getServerId());
        }else {
            streamInfoResult.setServerId(userSetting.getServerId());
        }

        streamInfoResult.setMediaServer(mediaServer);
        Map<String, String> param = new HashMap<>();
        if (!ObjectUtils.isEmpty(callId)) {
            param.put("callId", callId);
        }
        if (mediaInfo != null && !ObjectUtils.isEmpty(mediaInfo.getOriginTypeStr()))  {
            if (!ObjectUtils.isEmpty(mediaInfo.getOriginTypeStr())) {
                param.put("originTypeStr", mediaInfo.getOriginTypeStr());
            }
            if (!ObjectUtils.isEmpty(mediaInfo.getVideoCodec())) {
                param.put("videoCodec", mediaInfo.getVideoCodec());
            }
            if (!ObjectUtils.isEmpty(mediaInfo.getAudioCodec())) {
                param.put("audioCodec", mediaInfo.getAudioCodec());
            }
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

        String mp4File = String.format("%s/%s.live.mp4%s", app, stream, callIdParam);
        streamInfoResult.setFmp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), mp4File);
        streamInfoResult.setWsMp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), mp4File);

        streamInfoResult.setHls(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setWsHls(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam);

        streamInfoResult.setTs(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setWsTs(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam);

        streamInfoResult.setRtc(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam, isPlay);

        streamInfoResult.setMediaInfo(mediaInfo);

        if (!"broadcast".equalsIgnoreCase(app) && !ObjectUtils.isEmpty(mediaServer.getTranscodeSuffix()) && !"null".equalsIgnoreCase(mediaServer.getTranscodeSuffix())) {
            String newStream = stream + "_" + mediaServer.getTranscodeSuffix();
            mediaServer.setTranscodeSuffix(null);
            StreamInfo transcodeStreamInfo = getStreamInfoByAppAndStream(mediaServer, app, newStream, null, addr, callId, isPlay);
            streamInfoResult.setTranscodeStream(transcodeStreamInfo);
        }
        return streamInfoResult;
    }
}
