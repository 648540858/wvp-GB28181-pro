package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionStatus;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.service.IDeviceChannelService;
import com.genersoft.iot.vmp.gb28181.service.IInviteStreamService;
import com.genersoft.iot.vmp.gb28181.session.SipInviteSessionManager;
import com.genersoft.iot.vmp.jt1078.bean.JTMediaStreamType;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078PlayService;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.bean.ResultForOnPublish;
import com.genersoft.iot.vmp.media.zlm.dto.StreamAuthorityInfo;
import com.genersoft.iot.vmp.service.IMediaService;
import com.genersoft.iot.vmp.service.IReceiveRtpServerService;
import com.genersoft.iot.vmp.service.IRecordPlanService;
import com.genersoft.iot.vmp.service.IUserService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.streamProxy.bean.StreamProxy;
import com.genersoft.iot.vmp.streamProxy.service.IStreamProxyService;
import com.genersoft.iot.vmp.utils.MediaServerUtils;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    private Ijt1078Service ijt1078Service;

    @Autowired
    private Ijt1078PlayService jt1078PlayService;

    @Autowired
    private IReceiveRtpServerService receiveRtpServerService;


    @Autowired
    private IRecordPlanService recordPlanService;

    @Override
    public boolean authenticatePlay(String app, String stream, String callId) {
        if (app == null || stream == null) {
            return false;
        }
        if (MediaStreamUtil.RTP_APP.equals(app)) {
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

        if (MediaStreamUtil.RTP_APP.equals(app)) {
            return receiveRtpServerService.getAuthenticateInfo(stream);
        }else {
            ResultForOnPublish result = new ResultForOnPublish();
            // app 非 RTP_APP 的流， 如果是国标对讲或者广播则默认获取声音并且不录制， 其他的流先查询是否有代理配置，如果没有代理配置再进行鉴权
            if (MediaStreamUtil.GB28181_TALK.equals(app) || MediaStreamUtil.GB28181_BROADCAST.equals(app) || MediaStreamUtil.JT_TALK.equals(app)) {
                result.setEnable_mp4(false);
                result.setEnable_audio(true);
                return result;
            }
            if (MediaStreamUtil.LOAD_MP4_APP.equals(app) ) {
                result.setEnable_mp4(false);
                result.setEnable_audio(true);
                return result;
            }
            StreamProxy streamProxyItem = streamProxyService.getStreamProxyByAppAndStream(app, stream);
            if (streamProxyItem != null) {
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
            result.setEnable_mp4(userSetting.getRecordPushLive());
            return result;
        }
    }

    @Override
    public boolean closeStreamOnNoneReader(String mediaServerId, String app, String stream, String schema) {
        boolean result = false;
        if (recordPlanService.recording(app, stream) != null) {
            return false;
        }
        // 国标类型的流
        switch (app) {
            case MediaStreamUtil.RTP_APP -> {
                result = userSetting.getStreamOnDemand();
                if (MediaStreamUtil.isGB28181(app, stream)) {
                    // 国标流， 点播/录像回放/录像下载
                    InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, stream);
                    if (inviteInfo != null) {
                        if (inviteInfo.getStatus() == InviteSessionStatus.ok) {
                            // 录像下载
                            if (inviteInfo.getType() == InviteSessionType.DOWNLOAD) {
                                return false;
                            }
                            DeviceChannel deviceChannel = deviceChannelService.getOneForSourceById(inviteInfo.getChannelId());
                            if (deviceChannel == null) {
                                return false;
                            }
                        }
                    }
                    return result;
                } else if (MediaStreamUtil.isJT1078(app, stream)) {
                    // 判断是否是1078播放类型
                    JTMediaStreamType jtMediaStreamType = ijt1078Service.checkStreamFromJt(stream);
                    if (jtMediaStreamType != null) {
                        String[] streamParamArray = stream.split("_");
                        if (jtMediaStreamType.equals(JTMediaStreamType.PLAY)) {
                            jt1078PlayService.stopPlay(streamParamArray[0], Integer.parseInt(streamParamArray[1]));
                        } else if (jtMediaStreamType.equals(JTMediaStreamType.PLAYBACK)) {
                            jt1078PlayService.stopPlayback(streamParamArray[0], Integer.parseInt(streamParamArray[1]));
                        }
                    }
                    return false;
                }
            }
            case MediaStreamUtil.GB28181_TALK, MediaStreamUtil.GB28181_BROADCAST -> {
                return false;
            }
            case MediaStreamUtil.LOAD_MP4_APP -> {
                return true;
            }
            case null, default -> {
                // 非国标流 推流/拉流代理
                // 拉流代理
                StreamProxy streamProxy = streamProxyService.getStreamProxyByAppAndStream(app, stream);
                if (streamProxy != null) {
                    if (streamProxy.isEnableDisableNoneReader()) {
                        // 无人观看停用
                        // 修改数据
                        streamProxyService.stopByAppAndStream(app, stream);
                        return true;
                    } else {
                        // 无人观看不做处理
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return result;
    }
}
