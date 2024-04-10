package com.genersoft.iot.vmp.media.abl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.abl.bean.AblServerConfig;
import com.genersoft.iot.vmp.media.abl.bean.hook.OnStreamArriveABLHookParam;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
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

@Service("abl")
public class ABLMediaNodeServerService implements IMediaNodeServerService {

    private final static Logger logger = LoggerFactory.getLogger(ABLMediaNodeServerService.class);

    @Autowired
    private ABLRESTfulUtils ablresTfulUtils;

    @Autowired
    private SipConfig sipConfig;

    @Override
    public int createRTPServer(MediaServer mediaServer, String stream, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        return ablresTfulUtils.openRtpServer(mediaServer, "rtp", stream, 96, port, tcpMode, disableAudio?1:0);
    }

    @Override
    public void closeRtpServer(MediaServer mediaServer, String streamId) {
        closeRtpServer(mediaServer, streamId, null);
    }

    @Override
    public void closeRtpServer(MediaServer serverItem, String streamId, CommonCallback<Boolean> callback) {
       if (serverItem == null) {
           return;
       }
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id", streamId);
        param.put("force", 1);
        JSONObject jsonObject = ablresTfulUtils.closeStreams(serverItem, "rtp", streamId);
        logger.info("关闭RTP Server " +  jsonObject);
        if (jsonObject != null ) {
            if (jsonObject.getInteger("code") != 0) {
                logger.error("[closeRtpServer] 失败: " + jsonObject.getString("msg"));
            }
        }else {
            //  检查ZLM状态
            logger.error("[closeRtpServer] 失败: 请检查ZLM服务");
        }
    }

    @Override
    public void closeStreams(MediaServer mediaServer, String app, String streamId) {
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id", streamId);
        param.put("force", 1);
        JSONObject jsonObject = ablresTfulUtils.closeStreams(mediaServer, app, streamId);
        if (jsonObject != null ) {
            if (jsonObject.getInteger("code") != 0) {
                logger.error("[closeStreams] 失败: " + jsonObject.getString("msg"));
            }
        }else {
            //  检查ZLM状态
            logger.error("[closeStreams] 失败: 请检查ZLM服务");
        }
    }

    @Override
    public Boolean updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc) {
        return null;
    }

    @Override
    public boolean checkNodeId(MediaServer mediaServerItem) {
        logger.warn("[abl-checkNodeId] 未实现");
        return false;
    }

    @Override
    public void online(MediaServer mediaServerItem) {
        logger.warn("[abl-online] 未实现");
    }

    @Override
    public MediaServer checkMediaServer(String ip, int port, String secret) {
        MediaServer mediaServer = new MediaServer();
        mediaServer.setIp(ip);
        mediaServer.setHttpPort(port);
        mediaServer.setSecret(secret);
        JSONObject responseJSON = ablresTfulUtils.getServerConfig(mediaServer);
        JSONArray data = responseJSON.getJSONArray("params");
        if (data != null && !data.isEmpty()) {
            AblServerConfig config = AblServerConfig.getInstance(data);
            config.setServerIp(ip);
            config.setHttpServerPort(port);
            return new MediaServer(config, sipConfig.getIp());
        }
        return null;
    }

    @Override
    public boolean stopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        // TODO 需要记录开始发流返回的KEY，暂不做实现
        logger.warn("[abl-stopSendRtp] 未实现");
//        ablresTfulUtils.stopSendRtp()
        return false;
    }

    @Override
    public boolean deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName) {
        logger.warn("[abl-deleteRecordDirectory] 未实现");
        return false;
    }

    @Override
    public List<StreamInfo> getMediaList(MediaServer mediaServer, String app, String stream, String callId) {
        logger.warn("[abl-getMediaList] 未实现");
        JSONObject jsonObject = ablresTfulUtils.getMediaList(mediaServer, app, stream);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            return null;
        }
        JSONArray mediaList = jsonObject.getJSONArray("mediaList");
        if (mediaList == null || mediaList.isEmpty()) {
            return new ArrayList<>();
        }
        List<StreamInfo> streamInfoList = new ArrayList<>();
        for (int i = 0; i < mediaList.size(); i++) {
            JSONObject mediaJSON = mediaList.getJSONObject(i);
            OnStreamArriveABLHookParam onStreamArriveABLHookParam = mediaJSON.to(OnStreamArriveABLHookParam.class);
            MediaInfo mediaInfo = MediaInfo.getInstance(onStreamArriveABLHookParam, mediaServer);
            StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, app, stream, mediaInfo, callId, true);
            if (streamInfo != null) {
                streamInfoList.add(streamInfo);
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
        String flvFile = String.format("%s/%s.flv%s", app, stream, callIdParam);
        streamInfoResult.setFlv(addr, mediaServer.getFlvPort(),mediaServer.getHttpSSlPort(), flvFile);
        streamInfoResult.setWsFlv(addr, mediaServer.getWsFlvPort(),mediaServer.getHttpSSlPort(), flvFile);
        streamInfoResult.setFmp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setHls(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setTs(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtc(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam, isPlay);

        streamInfoResult.setMediaInfo(mediaInfo);
        streamInfoResult.setOriginType(mediaInfo.getOriginType());
        return streamInfoResult;
    }

    @Override
    public Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream) {
        logger.warn("[abl-connectRtpServer] 未实现");
        return null;
    }

    @Override
    public void getSnap(MediaServer mediaServerItem, String app, String stream, int timeoutSec, int expireSec, String path, String fileName) {
        ablresTfulUtils.getSnap(mediaServerItem, app, stream, timeoutSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        JSONObject jsonObject = ablresTfulUtils.getMediaList(mediaServer, app, stream);
        if (jsonObject == null || jsonObject.getInteger("code") != 0) {
            return null;
        }
        JSONArray mediaList = jsonObject.getJSONArray("mediaList");
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        MediaInfo mediaInfo = null;
        for (int i = 0; i < mediaList.size(); i++) {
            JSONObject mediaJSON = mediaList.getJSONObject(i);
            OnStreamArriveABLHookParam onStreamArriveABLHookParam = mediaJSON.to(OnStreamArriveABLHookParam.class);
            if (onStreamArriveABLHookParam == null) {
                continue;
            }
            mediaInfo = MediaInfo.getInstance(onStreamArriveABLHookParam, mediaServer);

        }
        return mediaInfo;
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        logger.warn("[abl-pauseRtpCheck] 未实现");
        return null;
    }

    @Override
    public Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        logger.warn("[abl-resumeRtpCheck] 未实现");
        return null;
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        logger.warn("[abl-getFfmpegCmd] 未实现");
        return null;
    }

    @Override
    public WVPResult<String> addFFmpegSource(MediaServer mediaServer, String srcUrl, String dstUrl, int timeoutMs, boolean enableAudio, boolean enableMp4, String ffmpegCmdKey) {
        logger.warn("[abl-addFFmpegSource] 未实现");
        return null;
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType) {
        logger.warn("[abl-addStreamProxy] 未实现");
        return null;
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        logger.warn("[abl-delFFmpegSource] 未实现");
        return null;
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServer, String streamKey) {
        logger.warn("[abl-delStreamProxy] 未实现");
        return null;
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        logger.warn("[abl-getFFmpegCMDs] 未实现");
        return null;
    }

    @Override
    public void startSendRtpPassive(MediaServer mediaServer, SendRtpItem sendRtpItem, Integer timeout) {
        logger.warn("[abl-startSendRtpPassive] 未实现");
    }

    @Override
    public void startSendRtpStream(MediaServer mediaServer, SendRtpItem sendRtpItem) {
        logger.warn("[abl-startSendRtpStream] 未实现");
    }
}
