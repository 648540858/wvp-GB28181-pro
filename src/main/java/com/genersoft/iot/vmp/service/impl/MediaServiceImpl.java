package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.*;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.utils.DateUtil;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.OtherPsSendInfo;
import com.genersoft.iot.vmp.vmanager.bean.OtherRtpSendInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MediaServiceImpl implements IMediaService {

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

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
    private IDeviceChannelService deviceChannelService;

    @Autowired
    private SipInviteSessionManager sessionManager;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private IGbChannelService channelService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Autowired
    private ISIPCommander commander;

    @Override
    public boolean authenticatePlay(String app, String stream, String callId) {
        if (app == null || stream == null) {
            return false;
        }
        if ("rtp".equals(app)) {
            return true;
        }
        StreamAuthorityInfo streamAuthorityInfo = redisCatchStorage.getStreamAuthorityInfo(app, stream);
        if (streamAuthorityInfo == null || streamAuthorityInfo.getCallId() == null) {
            return true;
        }
        return streamAuthorityInfo.getCallId().equals(callId);
    }

    @Override
    public ResultForOnPublish authenticatePublish(MediaServer mediaServer, String app, String stream, String params) {
        // 推流鉴权的处理
        if (!"rtp".equals(app)) {
            StreamProxy streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(app, stream);
            if (streamProxyItem != null) {
                ResultForOnPublish result = new ResultForOnPublish();
                result.setEnable_audio(streamProxyItem.isEnableAudio());
                result.setEnable_mp4(streamProxyItem.isEnableMp4());
                return result;
            }
            if (userSetting.getPushAuthority()) {
                // 对于推流进行鉴权
                Map<String, String> paramMap = MediaServerUtils.urlParamToMap(params);
                // 推流鉴权
                if (params == null) {
                    log.info("推流鉴权失败： 缺少必要参数：sign=md5(user表的pushKey)");
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }

                String sign = paramMap.get("sign");
                if (sign == null) {
                    log.info("推流鉴权失败： 缺少必要参数：sign=md5(user表的pushKey)");
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }
                // 推流自定义播放鉴权码
                String callId = paramMap.get("callId");
                // 鉴权配置
                boolean hasAuthority = userService.checkPushAuthority(callId, sign);
                if (!hasAuthority) {
                    log.info("推流鉴权失败： sign 无权限: callId={}. sign={}", callId, sign);
                    throw new ControllerException(ErrorCode.ERROR401.getCode(), "Unauthorized");
                }
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(app, stream, mediaServer.getId());
                streamAuthorityInfo.setCallId(callId);
                streamAuthorityInfo.setSign(sign);
                // 鉴权通过
                redisCatchStorage.updateStreamAuthorityInfo(app, stream, streamAuthorityInfo);
            }
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
                    log.info("[ZLM HOOK]推流鉴权 stream: {} 替换为 {}", stream, inviteInfo.getStream());
                    stream = inviteInfo.getStream();
                }
            }

            // 设置音频信息及录制信息
            SsrcTransaction ssrcTransaction = sessionManager.getSsrcTransactionByStream(stream);
            if (ssrcTransaction != null ) {

                // 为录制国标模拟一个鉴权信息, 方便后续写入录像文件时使用
                StreamAuthorityInfo streamAuthorityInfo = StreamAuthorityInfo.getInstanceByHook(app, stream, mediaServer.getId());
                streamAuthorityInfo.setApp(app);
                streamAuthorityInfo.setStream(ssrcTransaction.getStream());
                streamAuthorityInfo.setCallId(ssrcTransaction.getSipTransactionInfo().getCallId());

                redisCatchStorage.updateStreamAuthorityInfo(app, ssrcTransaction.getStream(), streamAuthorityInfo);

                String deviceId = ssrcTransaction.getDeviceId();
                Integer channelId = ssrcTransaction.getChannelId();
                DeviceChannel deviceChannel = deviceChannelService.getOneById(channelId);
                if (deviceChannel != null) {
                    result.setEnable_audio(deviceChannel.isHasAudio());
                }
                // 如果是录像下载就设置视频间隔十秒
                if (ssrcTransaction.getType() == InviteSessionType.DOWNLOAD) {
                    // 获取录像的总时长，然后设置为这个视频的时长
                    InviteInfo inviteInfoForDownload = inviteStreamService.getInviteInfo(InviteSessionType.DOWNLOAD,  channelId, stream);
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
                if (ssrcTransaction.getType() == InviteSessionType.TALK) {
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
                DeviceChannel deviceChannel = deviceChannelService.getOneById(inviteInfo.getChannelId());
                if (deviceChannel == null) {
                    return false;
                }
                // 收到无人观看说明流也没有在往上级推送
                if (redisCatchStorage.isChannelSendingRTP(deviceChannel.getDeviceId())) {
                    List<SendRtpInfo> sendRtpItems = redisCatchStorage.querySendRTPServerByChannelId(deviceChannel.getDeviceId());
                    if (!sendRtpItems.isEmpty()) {
                        for (SendRtpInfo sendRtpItem : sendRtpItems) {
                            Platform parentPlatform = platformService.queryPlatformByServerGBId(sendRtpItem.getPlatformId());
                            CommonGBChannel channel = channelService.getOne(sendRtpItem.getChannelId());
                            if (channel == null) {
                                continue;
                            }
                            try {
                                commanderForPlatform.streamByeCmd(parentPlatform, sendRtpItem, channel);
                            } catch (SipException | InvalidArgumentException | ParseException e) {
                                log.error("[命令发送失败] 国标级联 发送BYE: {}", e.getMessage());
                            }
                            redisCatchStorage.deleteSendRTPServer(parentPlatform.getServerGBId(), channel.getGbDeviceId(),
                                    sendRtpItem.getCallId(), sendRtpItem.getStream());
                            if (InviteStreamType.PUSH == sendRtpItem.getPlayType()) {
                                redisCatchStorage.sendPlatformStopPlayMsg(sendRtpItem, parentPlatform, channel);
                            }
                        }
                    }
                }
                Device device = deviceService.getDeviceByDeviceId(inviteInfo.getDeviceId());
                if (device != null) {
                    try {
                        DeviceChannel channel = deviceChannelService.getOneById(inviteInfo.getChannelId());
                        // 多查询一次防止已经被处理了
                        InviteInfo info = inviteStreamService.getInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
                        if (info != null && channel != null) {
                            commander.streamByeCmd(device, channel.getDeviceId(), inviteInfo.getStream(), null);
                        } else {
                            log.info("[无人观看] 未找到设备的点播信息： {}， 流：{}", inviteInfo.getDeviceId(), stream);
                        }
                    } catch (InvalidArgumentException | ParseException | SipException |
                             SsrcTransactionNotFoundException e) {
                        log.error("[无人观看]点播， 发送BYE失败 {}", e.getMessage());
                    }
                } else {
                    log.info("[无人观看] 未找到设备： {}，流：{}", inviteInfo.getDeviceId(), stream);
                }

                inviteStreamService.removeInviteInfo(inviteInfo.getType(), inviteInfo.getChannelId(), inviteInfo.getStream());
                deviceChannelService.stopPlay(inviteInfo.getChannelId());
                return result;
            }
            SendRtpInfo sendRtpItem = redisCatchStorage.querySendRTPServer(null, null, stream, null);
            if (sendRtpItem != null && "talk".equals(sendRtpItem.getApp())) {
                return false;
            }
        } else if ("talk".equals(app) || "broadcast".equals(app)) {
            return false;
        } else {
            // 非国标流 推流/拉流代理
            // 拉流代理
            StreamProxy streamProxy = streamProxyService.getStreamProxyByAppAndStream(app, stream);
            if (streamProxy != null) {
                if (streamProxy.isEnableRemoveNoneReader()) {
                    // 无人观看自动移除
                    result = true;
                    streamProxyService.delteByAppAndStream(app, stream);
                    log.info("[{}/{}]<-[{}] 拉流代理无人观看已经移除", app, stream, streamProxy.getSrcUrl());
                } else if (streamProxy.isEnableDisableNoneReader()) {
                    // 无人观看停用
                    result = true;
                    // 修改数据
                    streamProxyService.stopByAppAndStream(app, stream);
                } else {
                    // 无人观看不做处理
                    result = false;
                }
            }
        }
        return result;
    }
}
