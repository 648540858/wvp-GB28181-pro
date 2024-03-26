package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.bean.MediaInfo;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookListener;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookType;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.media.zlm.dto.StreamProxyItem;
import com.genersoft.iot.vmp.media.zlm.dto.hook.HookResultForOnPublish;
import com.genersoft.iot.vmp.service.*;
import com.genersoft.iot.vmp.service.bean.MessageForPushChannel;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.OtherPsSendInfo;
import com.genersoft.iot.vmp.vmanager.bean.OtherRtpSendInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MediaServiceImpl implements IMediaService {

    private final static Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private MediaConfig mediaConfig;

    @Autowired
    private IStreamProxyService streamProxyService;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private IUserService userService;

    @Autowired
    private IInviteStreamService inviteStreamService;

    @Autowired
    private VideoStreamSessionManager sessionManager;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private ISIPCommander commander;



    @Override
    public StreamInfo getStreamInfoByAppAndStream(MediaServer mediaServerItem, String app, String stream, MediaInfo mediaInfo, String callId) {
        return getStreamInfoByAppAndStream(mediaServerItem, app, stream, mediaInfo, null, callId, true);
    }

    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, String addr, boolean authority) {
        StreamInfo streamInfo = null;
        if (mediaServerId == null) {
            mediaServerId = mediaConfig.getId();
        }
        MediaServer mediaInfo = mediaServerService.getOne(mediaServerId);
        if (mediaInfo == null) {
            return null;
        }
        String calld = null;
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo != null) {
            calld = streamAuthorityInfo.getCallId();
        }
        List<StreamInfo> streamInfoList = mediaServerService.getMediaList(mediaInfo, app, stream, calld);
        if (streamInfoList.isEmpty()) {
            return null;
        }else {
            return streamInfoList.get(0);
        }
    }



    @Override
    public StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream, String mediaServerId, boolean authority) {
        return getStreamInfoByAppAndStreamWithCheck(app, stream, mediaServerId, null, authority);
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
        streamInfoResult.setMediaServerId(mediaServer.getId());
        String callIdParam = ObjectUtils.isEmpty(callId)?"":"?callId=" + callId;
        streamInfoResult.setRtmp(addr, mediaServer.getRtmpPort(),mediaServer.getRtmpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtsp(addr, mediaServer.getRtspPort(),mediaServer.getRtspSSLPort(), app,  stream, callIdParam);
        streamInfoResult.setFlv(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setFmp4(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setHls(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setTs(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam);
        streamInfoResult.setRtc(addr, mediaServer.getHttpPort(),mediaServer.getHttpSSlPort(), app,  stream, callIdParam, isPlay);

        streamInfoResult.setMediaInfo(mediaInfo);
        return streamInfoResult;
    }

    @Override
    public boolean authenticatePlay(String app, String stream, String callId) {
        if (app == null || stream == null) {
            return false;
        }
        if ("rtp".equals(app)) {
            return true;
        }
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        return (streamAuthorityInfo != null && streamAuthorityInfo.getCallId() != null && !streamAuthorityInfo.getCallId().equals(callId));
    }

    @Override
    public ResultForOnPublish authenticatePublish(MediaServer mediaServer, String app, String stream, String params) {
        // 推流鉴权的处理
        if (!"rtp".equals(app)) {
            StreamProxyItem streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(app, stream);
            if (streamProxyItem != null) {
                ResultForOnPublish result = new ResultForOnPublish();
                result.setEnable_audio(streamProxyItem.isEnableAudio());
                result.setEnable_mp4(streamProxyItem.isEnableMp4());
                return result;
            }
            if (userSetting.getPushAuthority()) {
                // 对于推流进行鉴权
                Map<String, String> paramMap = urlParamToMap(params);
                // 推流鉴权
                if (params == null) {
                    logger.info("推流鉴权失败： 缺少必要参数：sign=md5(user表的pushKey)");
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }

                String sign = paramMap.get("sign");
                if (sign == null) {
                    logger.info("推流鉴权失败： 缺少必要参数：sign=md5(user表的pushKey)");
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }
                // 推流自定义播放鉴权码
                String callId = paramMap.get("callId");
                // 鉴权配置
                boolean hasAuthority = userService.checkPushAuthority(callId, sign);
                if (!hasAuthority) {
                    logger.info("推流鉴权失败： sign 无权限: callId={}. sign={}", callId, sign);
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(app, stream, mediaServer.getId());
                streamAuthorityInfo.setCallId(callId);
                streamAuthorityInfo.setSign(sign);
                // 鉴权通过
                redisCatchStorage.updateStreamAuthorityInfo(app, stream, streamAuthorityInfo);
            }
        } else {
            zlmMediaListManager.sendStreamEvent(app, stream, mediaServer.getId());
        }


        ResultForOnPublish result = new ResultForOnPublish();
        result.setEnable_audio(true);


        // 是否录像
        if ("rtp".equals(app)) {
            result.setEnable_mp4(userSetting.getRecordSip());
        } else {
            result.setEnable_mp4(userSetting.isRecordPushLive());
        }
        // 国标流
        if ("rtp".equals(app)) {

            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, stream);

            // 单端口模式下修改流 ID
            if (!mediaServer.isRtpEnable() && inviteInfo == null) {
                String ssrc = String.format("%010d", Long.parseLong(stream, 16));
                inviteInfo = inviteStreamService.getInviteInfoBySSRC(ssrc);
                if (inviteInfo != null) {
                    result.setStream_replace(inviteInfo.getStream());
                    logger.info("[ZLM HOOK]推流鉴权 stream: {} 替换为 {}", stream, inviteInfo.getStream());
                }
            }

            // 设置音频信息及录制信息
            List<SsrcTransaction> ssrcTransactionForAll = sessionManager.getSsrcTransactionForAll(null, null, null, stream);
            if (ssrcTransactionForAll != null && ssrcTransactionForAll.size() == 1) {

                // 为录制国标模拟一个鉴权信息, 方便后续写入录像文件时使用
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(app, stream, mediaServer.getId());
                streamAuthorityInfo.setApp(app);
                streamAuthorityInfo.setStream(ssrcTransactionForAll.get(0).getStream());
                streamAuthorityInfo.setCallId(ssrcTransactionForAll.get(0).getSipTransactionInfo().getCallId());

                redisCatchStorage.updateStreamAuthorityInfo(app, ssrcTransactionForAll.get(0).getStream(), streamAuthorityInfo);

                String deviceId = ssrcTransactionForAll.get(0).getDeviceId();
                String channelId = ssrcTransactionForAll.get(0).getChannelId();
                DeviceChannel deviceChannel = storager.queryChannel(deviceId, channelId);
                if (deviceChannel != null) {
                    result.setEnable_audio(deviceChannel.isHasAudio());
                }
                // 如果是录像下载就设置视频间隔十秒
                if (ssrcTransactionForAll.get(0).getType() == InviteSessionType.DOWNLOAD) {
                    // 获取录像的总时长，然后设置为这个视频的时长
                    InviteInfo inviteInfoForDownload = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD, deviceId, channelId, stream);
                    if (inviteInfoForDownload != null && inviteInfoForDownload.getStreamInfo() != null) {
                        String startTime = inviteInfoForDownload.getStreamInfo().getStartTime();
                        String endTime = inviteInfoForDownload.getStreamInfo().getEndTime();
                        long difference = DateUtil.getDifference(startTime, endTime) / 1000;
                        result.setMp4_max_second((int) difference);
                        result.setEnable_mp4(true);
                        // 设置为2保证得到的mp4的时长是正常的
                        result.setModify_stamp(2);
                    }
                }
                // 如果是talk对讲，则默认获取声音
                if (ssrcTransactionForAll.get(0).getType() == InviteSessionType.TALK) {
                    result.setEnable_audio(true);
                }
            }
        } else if (app.equals("broadcast")) {
            result.setEnable_audio(true);
        } else if (app.equals("talk")) {
            result.setEnable_audio(true);
        }
        if (app.equalsIgnoreCase("rtp")) {
            String receiveKey = VideoManagerConstants.WVP_OTHER_RECEIVE_RTP_INFO + userSetting.getServerId() + "_" + stream;
            OtherRtpSendInfo otherRtpSendInfo = (OtherRtpSendInfo) redisTemplate.opsForValue().get(receiveKey);

            String receiveKeyForPS = VideoManagerConstants.WVP_OTHER_RECEIVE_PS_INFO + userSetting.getServerId() + "_" + stream;
            OtherPsSendInfo otherPsSendInfo = (OtherPsSendInfo) redisTemplate.opsForValue().get(receiveKeyForPS);
            if (otherRtpSendInfo != null || otherPsSendInfo != null) {
                result.setEnable_mp4(true);
            }
        }
        return result;
    }

    private Map<String, String> urlParamToMap(String params) {
        HashMap<String, String> map = new HashMap<>();
        if (ObjectUtils.isEmpty(params)) {
            return map;
        }
        String[] paramsArray = params.split("&");
        if (paramsArray.length == 0) {
            return map;
        }
        for (String param : paramsArray) {
            String[] paramArray = param.split("=");
            if (paramArray.length == 2) {
                map.put(paramArray[0], paramArray[1]);
            }
        }
        return map;
    }

    @Override
    public boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        boolean result = false;
        // 国标类型的流
        if ("rtp".equals(app)) {
            result = userSetting.getStreamOnDemand();
            // 国标流， 点播/录像回放/录像下载
            InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, stream);
            // 点播
            if (inviteInfo != null) {
                // 录像下载
                if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
                    return false;
                }
                // 收到无人观看说明流也没有在往上级推送
                if (redisCatchStorage.isChannelSendingRTP(inviteInfo.getChannelId())) {
                    List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServerByChannelId(
                            inviteInfo.getChannelId());
                    if (!sendRtpItems.isEmpty()) {
                        for (SendRtpItem sendRtpItem : sendRtpItems) {
                            ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(sendRtpItem.getPlatformId());
                            try {
                                commanderForPlatform.streamByeCmd(parentPlatform, sendRtpItem.getCallId());
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                logger.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                            }
                            redisCatchStorage.deleteSendRTPServer(parentPlatform.getServerGBId(), sendRtpItem.getChannelId(),
                                    sendRtpItem.getCallId(), sendRtpItem.getStream());
                            if (InviteStreamType.PUSH == sendRtpItem.getPlayType()) {
                                MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(0,
                                        sendRtpItem.getApp(), sendRtpItem.getStream(), sendRtpItem.getChannelId(),
                                        sendRtpItem.getPlatformId(), parentPlatform.getName(), userSetting.getServerId(), sendRtpItem.getMediaServerId());
                                messageForPushChannel.setPlatFormIndex(parentPlatform.getId());
                                redisCatchStorage.sendPlatformStopPlayMsg(messageForPushChannel);
                            }
                        }
                    }
                }
                Device device = deviceService.getDevice(inviteInfo.getDeviceId());
                if (device != null) {
                    try {
                        // 多查询一次防止已经被处理了
                        InviteInfo info = inviteStreamService.getInviteInfo(inviteInfo.getType(),
                                inviteInfo.getDeviceId(), inviteInfo.getChannelId(), inviteInfo.getStream());
                        if (info != null) {
                            commander.streamByeCmd(device, inviteInfo.getChannelId(),
                                    inviteInfo.getStream(), null);
                        } else {
                            logger.info("[无人观看] 未找到设备的点播信息： {}， 流：{}", inviteInfo.getDeviceId(), stream);
                        }
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        logger.error("[无人观看]点播， 发送BYE失败 {}", e.getMessage());
                    }
                } else {
                    logger.info("[无人观看] 未找到设备： {}，流：{}", inviteInfo.getDeviceId(), stream);
                }

                inviteStreamService.removeInviteInfo(inviteInfo.getType(), inviteInfo.getDeviceId(),
                        inviteInfo.getChannelId(), inviteInfo.getStream());
                storager.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
                return result;
            }
            SendRtpItem sendRtpItem = redisCatchStorage.querySendRTPServer(null, null, stream, null);
            if (sendRtpItem != null && "talk".equals(sendRtpItem.getApp())) {
                return false;
            }
        } else if ("talk".equals(app) || "broadcast".equals(app)) {
            return false;
        } else {
            // 非国标流 推流/拉流代理
            // 拉流代理
            StreamProxyItem streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(app, stream);
            if (streamProxyItem != null) {
                if (streamProxyItem.isEnableRemoveNoneReader()) {
                    // 无人观看自动移除
                    result = true;
                    streamProxyService.del(app, stream);
                    String url = streamProxyItem.getUrl() != null ? streamProxyItem.getUrl() : streamProxyItem.getSrcUrl();
                    logger.info("[{}/{}]<-[{}] 拉流代理无人观看已经移除", app, stream, url);
                } else if (streamProxyItem.isEnableDisableNoneReader()) {
                    // 无人观看停用
                    result = true;
                    // 修改数据
                    streamProxyService.stop(app, stream);
                } else {
                    // 无人观看不做处理
                    result = false;
                }
            }
        }
        return result;
    }
}
