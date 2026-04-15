package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.enums.MediaStreamUtil;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.service.ISourceOtherService;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements IMediaService {

    private final IRedisCatchStorage redisCatchStorage;

    private final IStreamProxyService streamProxyService;

    private final UserSetting userSetting;

    private final IUserService userService;

    private final IReceiveRtpServerService receiveRtpServerService;

    private final IRecordPlanService recordPlanService;

    private final Map<String, ISourceOtherService> sourceOtherServiceMap;


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
        if (recordPlanService.recording(app, stream) != null) {
            return false;
        }
        if (MediaStreamUtil.LOAD_MP4_APP.equals(app)) {
            // mp4点播流， 无人观看不关闭
            return true;
        }

        for (ISourceOtherService sourceOtherService : sourceOtherServiceMap.values()) {
            try {
                Boolean result = sourceOtherService.closeStreamOnNoneReader(mediaServerId, app, stream, schema);
                if (result != null) {
                    return result;
                }
            }catch (Exception e) {
                log.error("调用其他服务关闭无人观看流失败， app={}, stream={}, schema={}", app, stream, schema, e);
            }
        }

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
            return userSetting.getStreamOnDemand();
        }
    }
}
