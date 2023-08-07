package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * API兼容：实时直播
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})

@RestController
@RequestMapping(value = "/api/v1/stream")
public class ApiStreamController {

    private final static Logger logger = LoggerFactory.getLogger(ApiStreamController.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IInviteStreamService inviteStreamService;

    /**
     * 实时直播 - 开始直播
     * @param serial 设备编号
     * @param channel 通道序号 默认值: 1
     * @param code 通道编号,通过 /api/v1/device/channellist 获取的 ChannelList.ID, 该参数和 channel 二选一传递即可
     * @param cdn 转推 CDN 地址, 形如: [rtmp|rtsp]://xxx, encodeURIComponent
     * @param audio 是否开启音频, 默认 开启
     * @param transport 流传输模式， 默认 UDP
     * @param checkchannelstatus 是否检查通道状态, 默认 false, 表示 拉流前不检查通道状态是否在线
     * @param transportmode 当 transport=TCP 时有效, 指示流传输主被动模式, 默认被动
     * @param timeout 拉流超时(秒),
     * @return
     */
    @RequestMapping(value = "/start")
    private DeferredResult<JSONObject> start(String serial ,
                                             @RequestParam(required = false)Integer channel ,
                                             @RequestParam(required = false)String code,
                                             @RequestParam(required = false)String cdn,
                                             @RequestParam(required = false)String audio,
                                             @RequestParam(required = false)String transport,
                                             @RequestParam(required = false)String checkchannelstatus ,
                                             @RequestParam(required = false)String transportmode,
                                             @RequestParam(required = false)String timeout

    ){
        DeferredResult<JSONObject> resultDeferredResult = new DeferredResult<>(userSetting.getPlayTimeout().longValue() + 10);
        Device device = storager.queryVideoDevice(serial);
        if (device == null ) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + serial + " ]未找到");
            resultDeferredResult.setResult(result);
            return resultDeferredResult;
        }else if (device.isOnLine()) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + code + " ]offline");
            resultDeferredResult.setResult(result);
            return resultDeferredResult;
        }
        resultDeferredResult.onTimeout(()->{
            logger.info("播放等待超时");
            JSONObject result = new JSONObject();
            result.put("error","timeout");
            resultDeferredResult.setResult(result);

             // 清理RTP server
        });

        DeviceChannel deviceChannel = storager.queryChannel(serial, code);
        if (deviceChannel == null) {
            JSONObject result = new JSONObject();
            result.put("error","channel[ " + code + " ]未找到");
            resultDeferredResult.setResult(result);
            return resultDeferredResult;
        }else if (!deviceChannel.isStatus()) {
            JSONObject result = new JSONObject();
            result.put("error","channel[ " + code + " ]offline");
            resultDeferredResult.setResult(result);
            return resultDeferredResult;
        }
        MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);


        playService.play(newMediaServerItem, serial, code, null, (errorCode, msg, data) -> {
            if (errorCode == InviteErrorCode.SUCCESS.getCode()) {
                InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, serial, code);
                if (inviteInfo != null && inviteInfo.getStreamInfo() != null) {
                    JSONObject result = new JSONObject();
                    result.put("StreamID", inviteInfo.getStreamInfo().getStream());
                    result.put("DeviceID", device.getDeviceId());
                    result.put("ChannelID", code);
                    result.put("ChannelName", deviceChannel.getName());
                    result.put("ChannelCustomName", "");
                    result.put("FLV", inviteInfo.getStreamInfo().getFlv().getUrl());
                    if(inviteInfo.getStreamInfo().getHttps_flv() != null) {
                        result.put("HTTPS_FLV", inviteInfo.getStreamInfo().getHttps_flv().getUrl());
                    }
                    result.put("WS_FLV", inviteInfo.getStreamInfo().getWs_flv().getUrl());
                    if(inviteInfo.getStreamInfo().getWss_flv() != null) {
                        result.put("WSS_FLV", inviteInfo.getStreamInfo().getWss_flv().getUrl());
                    }
                    result.put("RTMP", inviteInfo.getStreamInfo().getRtmp().getUrl());
                    if (inviteInfo.getStreamInfo().getRtmps() != null) {
                        result.put("RTMPS", inviteInfo.getStreamInfo().getRtmps().getUrl());
                    }
                    result.put("HLS", inviteInfo.getStreamInfo().getHls().getUrl());
                    if (inviteInfo.getStreamInfo().getHttps_hls() != null) {
                        result.put("HTTPS_HLS", inviteInfo.getStreamInfo().getHttps_hls().getUrl());
                    }
                    result.put("RTSP", inviteInfo.getStreamInfo().getRtsp().getUrl());
                    if (inviteInfo.getStreamInfo().getRtsps() != null) {
                        result.put("RTSPS", inviteInfo.getStreamInfo().getRtsps().getUrl());
                    }
                    result.put("WEBRTC", inviteInfo.getStreamInfo().getRtc().getUrl());
                    if (inviteInfo.getStreamInfo().getRtcs() != null) {
                        result.put("HTTPS_WEBRTC", inviteInfo.getStreamInfo().getRtcs().getUrl());
                    }
                    result.put("CDN", "");
                    result.put("SnapURL", "");
                    result.put("Transport", device.getTransport());
                    result.put("StartAt", "");
                    result.put("Duration", "");
                    result.put("SourceVideoCodecName", "");
                    result.put("SourceVideoWidth", "");
                    result.put("SourceVideoHeight", "");
                    result.put("SourceVideoFrameRate", "");
                    result.put("SourceAudioCodecName", "");
                    result.put("SourceAudioSampleRate", "");
                    result.put("AudioEnable", "");
                    result.put("Ondemand", "");
                    result.put("InBytes", "");
                    result.put("InBitRate", "");
                    result.put("OutBytes", "");
                    result.put("NumOutputs", "");
                    result.put("CascadeSize", "");
                    result.put("RelaySize", "");
                    result.put("ChannelPTZType", "0");
                    resultDeferredResult.setResult(result);
                }
            }else {
                JSONObject result = new JSONObject();
                result.put("error", "channel[ " + code + " ] " + msg);
                resultDeferredResult.setResult(result);
            }
        });

        return resultDeferredResult;
    }

    /**
     * 实时直播 - 直播流停止
     * @param serial 设备编号
     * @param channel 通道序号
     * @param code 通道国标编号
     * @param check_outputs
     * @return
     */
    @RequestMapping(value = "/stop")
    @ResponseBody
    private JSONObject stop(String serial ,
                             @RequestParam(required = false)Integer channel ,
                             @RequestParam(required = false)String code,
                             @RequestParam(required = false)String check_outputs

    ){

        InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, serial, code);
        if (inviteInfo == null) {
            JSONObject result = new JSONObject();
            result.put("error","未找到流信息");
            return result;
        }
        Device device = deviceService.getDevice(serial);
        if (device == null) {
            JSONObject result = new JSONObject();
            result.put("error","未找到设备");
            return result;
        }
        try {
            cmder.streamByeCmd(device, code, inviteInfo.getStream(), null);
        } catch (InvalidArgumentException | ParseException | SipException | SsrcTransactionNotFoundException e) {
            JSONObject result = new JSONObject();
            result.put("error","发送BYE失败：" + e.getMessage());
            return result;
        }
        inviteStreamService.removeInviteInfo(inviteInfo);
        storager.stopPlay(inviteInfo.getDeviceId(), inviteInfo.getChannelId());
        return null;
    }

    /**
     * 实时直播 - 直播流保活
     * @param serial 设备编号
     * @param channel 通道序号
     * @param code 通道国标编号
     * @return
     */
    @RequestMapping(value = "/touch")
    @ResponseBody
    private JSONObject touch(String serial ,String t,
                            @RequestParam(required = false)Integer channel ,
                            @RequestParam(required = false)String code,
                            @RequestParam(required = false)String autorestart,
                            @RequestParam(required = false)String audio,
                            @RequestParam(required = false)String cdn
    ){
        return null;
    }
}
