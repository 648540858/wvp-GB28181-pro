package com.genersoft.iot.vmp.media.abl;

import com.alibaba.fastjson2.JSONArray;
import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.SipConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpInfo;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.media.abl.bean.ABLMedia;
import com.genersoft.iot.vmp.media.abl.bean.ABLResult;
import com.genersoft.iot.vmp.media.abl.bean.AblServerConfig;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.RecordInfo;
import com.genersoft.iot.vmp.media.event.media.MediaRecordMp4Event;
import com.genersoft.iot.vmp.media.service.IMediaNodeServerService;
import com.genersoft.iot.vmp.service.bean.CloudRecordItem;
import com.genersoft.iot.vmp.service.bean.DownloadFileInfo;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.storager.dao.CloudRecordServiceMapper;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("abl")
public class ABLMediaNodeServerService implements IMediaNodeServerService {

    private final static Logger logger = LoggerFactory.getLogger(ABLMediaNodeServerService.class);

    @Autowired
    private ABLRESTfulUtils ablresTfulUtils;

    @Autowired
    private SipConfig sipConfig;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private CloudRecordServiceMapper cloudRecordServiceMapper;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Override
    public boolean initStopSendRtp(MediaServer mediaInfo, String app, String stream, String ssrc) {
        return false;
    }

    @Override
    public int createRTPServer(MediaServer mediaServer, String stream, long ssrc, Integer port, Boolean onlyAuto, Boolean disableAudio, Boolean reUsePort, Integer tcpMode) {
        Boolean recordSip = userSetting.getRecordSip();
        return ablresTfulUtils.openRtpServer(mediaServer, "rtp", stream, 96, port, tcpMode, disableAudio?1:0, recordSip, false);
    }

    @Override
    public void closeRtpServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback) {
       if (mediaServer == null) {
           return;
       }
        ABLResult result = ablresTfulUtils.closeStreams(mediaServer, "rtp", streamId);
        logger.info("关闭RTP Server " +  result);
        if (result.getCode() != 0) {
            logger.error("[closeRtpServer] 失败: {}", result.getMemo());
        }
    }

    @Override
    public int createJTTServer(MediaServer mediaServer, String stream, Integer port, Boolean disableVideo, Boolean disableAudio, Integer tcpMode) {
        Boolean recordSip = userSetting.getRecordSip();
        return ablresTfulUtils.openRtpServer(mediaServer, "1078", stream, 96, port, tcpMode, disableAudio?1:0, recordSip, true);
    }

    @Override
    public void closeJTTServer(MediaServer mediaServer, String streamId, CommonCallback<Boolean> callback) {
        if (mediaServer == null) {
            return;
        }
        ABLResult result = ablresTfulUtils.closeStreams(mediaServer, "1078", streamId);
        logger.info("关闭JT-RTP Server " +  result);
        if (result.getCode() != 0) {
            logger.error("[JT-closeRtpServer] 失败: {}", result.getMemo());
        }
    }

    @Override
    public void closeStreams(MediaServer mediaServer, String app, String streamId) {
        ABLResult result = ablresTfulUtils.closeStreams(mediaServer, app, streamId);
        if (result.getCode() != 0) {
            logger.error("[closeStreams] 失败: {}", result.getMemo());
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
        ABLResult result = ablresTfulUtils.getServerConfig(mediaServer);
        JSONArray data = result.getParams();
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
        ABLResult result = ablresTfulUtils.getMediaList(mediaServer, app, stream);
        if (result.getCode() != 0) {
            return null;
        }
        if (result.getMediaList() == null || result.getMediaList().isEmpty()) {
            return new ArrayList<>();
        }
        List<StreamInfo> streamInfoList = new ArrayList<>();
        for (int i = 0; i < result.getMediaList().size(); i++) {
            ABLMedia ablMedia = result.getMediaList().get(i);
            MediaInfo mediaInfo = MediaInfo.getInstance(ablMedia, mediaServer);
            StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, app, stream, mediaInfo, null, callId, true);
            if (streamInfo != null) {
                streamInfoList.add(streamInfo);
            }
        }
        return streamInfoList;
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServer, String app, String stream, MediaInfo mediaInfo,
                                                  String addr, String callId, boolean isPlay) {
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

        String flvFile = String.format("%s/%s.flv%s", app, stream, callIdParam);
        if ((mediaServer.getFlvPort() & 1) == 1) {
            // 奇数端口 默认ssl端口
            streamInfoResult.setFlv(addr, null, mediaServer.getFlvPort(), flvFile);
        }else {
            streamInfoResult.setFlv(addr, mediaServer.getFlvPort(),null,  flvFile);
        }
        if ((mediaServer.getWsFlvPort() & 1) == 1) {
            // 奇数端口 默认ssl端口
            streamInfoResult.setWsFlv(addr, null, mediaServer.getWsFlvPort(), flvFile);
        }else {
            streamInfoResult.setWsFlv(addr, mediaServer.getWsFlvPort(),null,  flvFile);
        }
        String mp4File = String.format("%s/%s.mp4%s", app, stream, callIdParam);
        if ((mediaServer.getMp4Port() & 1) == 1) {
            // 奇数端口 默认ssl端口
            streamInfoResult.setFmp4(addr, null, mediaServer.getMp4Port(), mp4File);
        }else {
            streamInfoResult.setFmp4(addr, mediaServer.getMp4Port(), null, mp4File);
        }

        streamInfoResult.setHls(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setTs(addr, mediaServer.getHttpPort(), mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
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

    @Override
    public Boolean connectRtpServer(MediaServer mediaServerItem, String address, int port, String stream) {
        logger.warn("[abl-connectRtpServer] 未实现");
        return null;
    }

    @Override
    public void getSnap(MediaServer mediaServer, String app, String stream, int timeoutSec, int expireSec, String path, String fileName) {
        ablresTfulUtils.getSnap(mediaServer, app, stream, timeoutSec, path, fileName);
    }

    @Override
    public MediaInfo getMediaInfo(MediaServer mediaServer, String app, String stream) {
        ABLResult ablResult = ablresTfulUtils.getMediaList(mediaServer, app, stream);
        if (ablResult.getCode() != 0) {
            return null;
        }
        if (ablResult.getMediaList() == null || ablResult.getMediaList().isEmpty()) {
            return null;
        }
        return MediaInfo.getInstance(ablResult.getMediaList().get(0), mediaServer);
    }

    @Override
    public Boolean pauseRtpCheck(MediaServer mediaServer, String streamKey) {
        ABLResult ablResult = ablresTfulUtils.pauseRtpServer(mediaServer, streamKey);
        return ablResult.getCode() == 0;
    }

    @Override
    public Boolean resumeRtpCheck(MediaServer mediaServer, String streamKey) {
        ABLResult ablResult = ablresTfulUtils.resumeRtpServer(mediaServer, streamKey);
        return ablResult.getCode() == 0;
    }

    @Override
    public String getFfmpegCmd(MediaServer mediaServer, String cmdKey) {
        return "";
    }

    @Override
    public Boolean delFFmpegSource(MediaServer mediaServer, String streamKey) {
        ABLResult ablResult = ablresTfulUtils.delFFmpegProxy(mediaServer, streamKey);
        return ablResult.getCode() == 0;
    }

    @Override
    public Boolean delStreamProxy(MediaServer mediaServer, String streamKey) {
        ABLResult ablResult = ablresTfulUtils.delStreamProxy(mediaServer, streamKey);
        return ablResult.getCode() == 0;
    }

    @Override
    public Map<String, String> getFFmpegCMDs(MediaServer mediaServer) {
        return new HashMap<>();
    }

    // 接受进度通知
//    @EventListener
//    public void onApplicationEvent(MediaRecordProcessEvent event) {
//        CloudRecordItem cloudRecordItem = cloudRecordServiceMapper.getListByFileName(event.getApp(), event.getStream(), event.getFileName());
//        if (cloudRecordItem == null) {
//            cloudRecordItem = CloudRecordItem.getInstance(event);
//            cloudRecordItem.setStartTime(event.getStartTime());
//            cloudRecordItem.setEndTime(event.getEndTime());
//            cloudRecordServiceMapper.add(cloudRecordItem);
//        }else {
//            cloudRecordServiceMapper.updateTimeLen(cloudRecordItem.getId(), (long)event.getCurrentFileDuration() * 1000, System.currentTimeMillis());
//        }
//    }
    @EventListener
    public void onApplicationEvent(MediaRecordMp4Event event) {
        InviteInfo inviteInfo = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, null, event.getStream());
        if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
            return;
        }
        List<CloudRecordItem> cloudRecordItemList = cloudRecordServiceMapper.getList(null, event.getApp(), event.getStream(),
                null, null, null, null, null, null);
        if (cloudRecordItemList.isEmpty()) {
            return;
        }
        long startTime = cloudRecordItemList.get(cloudRecordItemList.size() - 1).getStartTime();
        long endTime = cloudRecordItemList.get(0).getEndTime();
        ABLResult ablResult = ablresTfulUtils.queryRecordList(event.getMediaServer(), event.getApp(), event.getStream(), DateUtil.timestampMsToUrlToyyyy_MM_dd_HH_mm_ss(startTime),
                DateUtil.timestampMsToUrlToyyyy_MM_dd_HH_mm_ss(endTime));
        if (ablResult.getCode() != 0) {
            return;
        }
        if (ablResult.getUrl() == null) {
            return;
        }
        String download = ablResult.getUrl().getDownload();
        DownloadFileInfo downloadFileInfo = new DownloadFileInfo();
        downloadFileInfo.setHttpPath(download);
        downloadFileInfo.setHttpsPath(download);
        inviteInfo.getStreamInfo().setDownLoadFilePath(downloadFileInfo);
        inviteStreamService.updateInviteInfo(inviteInfo);
    }

    @Override
    public Long updateDownloadProcess(MediaServer mediaServer, String app, String stream) {
        List<CloudRecordItem> list = cloudRecordServiceMapper.getList(null, app, stream, null,
                null, null, null, null, null);
        if (list.isEmpty()) {
            return null;
        }
        long downloadProcess = 0L;
        for (CloudRecordItem cloudRecordItem : list) {
            downloadProcess += (long) cloudRecordItem.getTimeLen();
        }
        return downloadProcess;
    }

    @Override
    public WVPResult<String> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enableAudio, boolean enableMp4, String rtpType, Integer timeout) {

        ABLResult result = ablresTfulUtils.addStreamProxy(mediaServer, app, stream, url, !enableAudio, enableMp4, rtpType, timeout);
        if (result.getCode() != 0) {
            return WVPResult.fail(ErrorCode.ERROR100.getCode(), result.getMemo());
        }else {
            return WVPResult.success(result.getKey());
        }
    }

    @Override
    public Integer startSendRtpPassive(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
        logger.warn("[abl-startSendRtpPassive] 未实现");
        return 0;
    }

    @Override
    public Integer startSendRtpTalk(MediaServer mediaServer, SendRtpInfo sendRtpItem, Integer timeout) {
        logger.warn("[abl-startSendRtpTalk] 未实现");
        return 0;
    }

    @Override
    public void startSendRtpStream(MediaServer mediaServer, SendRtpInfo sendRtpItem) {
        logger.warn("[abl-startSendRtpStream] 未实现");
    }

    @Override
    public String startProxy(MediaServer mediaServer, StreamProxy streamProxy) {

        MediaInfo mediaInfo = getMediaInfo(mediaServer, streamProxy.getApp(), streamProxy.getStream());

        if (mediaInfo != null) {
            closeStreams(mediaServer, streamProxy.getApp(), streamProxy.getStream());
        }

        ABLResult ablResult = null;
        if ("ffmpeg".equalsIgnoreCase(streamProxy.getType())){
            if (streamProxy.getTimeout() == 0) {
                streamProxy.setTimeout(15);
            }
            ablResult = ablresTfulUtils.addFFmpegProxy(mediaServer, streamProxy.getApp(), streamProxy.getStream(), streamProxy.getSrcUrl().trim(),
                    !streamProxy.isEnableAudio(), streamProxy.isEnableMp4(), streamProxy.getRtspType(), streamProxy.getTimeout());
        }else {
            ablResult = ablresTfulUtils.addStreamProxy(mediaServer, streamProxy.getApp(), streamProxy.getStream(), streamProxy.getSrcUrl().trim(),
                    streamProxy.isEnableAudio(), streamProxy.isEnableMp4(), streamProxy.getRtspType(), streamProxy.getTimeout());
        }
        if (ablResult.getCode() != 0) {
            throw new ControllerException(ablResult.getCode(), ablResult.getMemo());
        }else {
            String key = ablResult.getKey();
            if (key == null) {
                throw new ControllerException(ablResult.getCode(), "代理结果异常： " + ablResult);
            }else {
                return key;
            }
        }
    }

    @Override
    public void stopProxy(MediaServer mediaServer, String streamKey, String type) {
        ABLResult ablResult = null;
        if ("ffmpeg".equalsIgnoreCase(type)){
            ablResult = ablresTfulUtils.delFFmpegProxy(mediaServer, streamKey);
        }else {
            ablResult = ablresTfulUtils.delStreamProxy(mediaServer, streamKey);
        }
        if (ablResult.getCode() != 0) {
            throw new ControllerException(ablResult.getCode(), ablResult.getMemo());
        }
    }

    @Override
    public List<String> listRtpServer(MediaServer mediaServer) {
        ABLResult ablResult = ablresTfulUtils.getMediaList(mediaServer, "rtp", null);
        if (ablResult.getCode() != 0) {
            return null;
        }
        if (ablResult.getMediaList() == null || ablResult.getMediaList().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < ablResult.getMediaList().size(); i++) {
            ABLMedia ablMedia = ablResult.getMediaList().get(i);
            result.add(ablMedia.getStream());
        }
        return result;
    }

    @Override
    public void loadMP4File(MediaServer mediaServer, String app, String stream, String filePath, String fileName, ErrorCallback<StreamInfo> callback) {
        String buildStream = String.format("%s__ReplayFMP4RecordFile__%s", stream, fileName);
        StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, app, buildStream, null, null, null, true);
        callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
    }

    @Override
    public void loadMP4FileForDate(MediaServer mediaServer, String app, String stream, String date, String dateDir, ErrorCallback<StreamInfo> callback) {
        // 解析为 LocalDate
        LocalDate localDate = LocalDate.parse(date, DateUtil.DateFormatter);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime endOfDay = localDate.atTime(23, 59,59, 999);
        String startTime = DateUtil.urlFormatter.format(startOfDay);
        String endTime = DateUtil.urlFormatter.format(endOfDay);

        ABLResult ablResult = ablresTfulUtils.queryRecordList(mediaServer, app, stream, startTime, endTime);
        if (ablResult.getCode() != 0) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), ablResult.getMemo());
        }
        String resultApp = ablResult.getApp();
        String resultStream = ablResult.getStream();
        StreamInfo streamInfo = getStreamInfoByAppAndStream(mediaServer, resultApp, resultStream, null, null,null, true);
        streamInfo.setKey(ablResult.getKey());
        if (callback != null) {
            callback.run(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMsg(), streamInfo);
        }
    }

    @Override
    public void seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema) {
        ABLResult ablResult = ablresTfulUtils.controlRecordPlay(mediaServer, app, stream, "seek", "120");
        if (ablResult.getCode() != 0) {
            log.warn("[abl-seek] 失败：{}", ablResult.getMemo());
        }
    }

    @Override
    public void setRecordSpeed(MediaServer mediaServer, String app, String stream, Integer speed, String schema) {
        ABLResult ablResult = ablresTfulUtils.controlRecordPlay(mediaServer, app, stream, "scale", speed + "");
        if (ablResult.getCode() != 0) {
            log.warn("[abl-倍速] 失败：{}", ablResult.getMemo());
        }
    }

    @Override
    public DownloadFileInfo getDownloadFilePath(MediaServer mediaServer, RecordInfo recordInfo) {
        // 将filePath作为独立参数传入，避免%符号解析问题
        String pathTemplate = "%s://%s:%s/%s/%s__ReplayFMP4RecordFile__%s?download_speed=16";

        DownloadFileInfo info = new DownloadFileInfo();
        if ((mediaServer.getMp4Port() & 1) == 1) {
            info.setHttpsPath(
                    String.format(
                            pathTemplate,
                            "https",
                            mediaServer.getStreamIp(),
                            mediaServer.getMp4Port(),
                            recordInfo.getApp(),
                            recordInfo.getStream(),
                            recordInfo.getFileName()
                    )
            );
        }else {
            info.setHttpPath(
                    String.format(
                            pathTemplate,
                            "http",
                            mediaServer.getStreamIp(),
                            mediaServer.getMp4Port(),
                            recordInfo.getApp(),
                            recordInfo.getStream(),
                            recordInfo.getFileName()
                    )
            );
        }
        return info;
    }
}
