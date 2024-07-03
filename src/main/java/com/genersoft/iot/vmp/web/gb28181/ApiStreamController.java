package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.InviteInfo;
import com.genersoft.iot.vmp.common.InviteSessionType;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.conf.exception.SsrcTransactionNotFoundException;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.service.IDeviceService;
import com.genersoft.iot.vmp.service.IInviteStreamService;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.service.bean.InviteErrorCode;
import com.genersoft.iot.vmp.storager.IVideoManagerStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * API兼容：实时直播
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/stream")
public class ApiStreamController {

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private UserSetting userSetting;

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
    @GetMapping("/start")
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
        DeferredResult<JSONObject> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue() + 10);
        Device device = storager.queryVideoDevice(serial);
        if (device == null ) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","device[ " + serial + " ]未找到");
            result.setResult(resultJSON);
            return result;
        }else if (!device.isOnLine()) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","device[ " + code + " ]offline");
            result.setResult(resultJSON);
            return result;
        }
        result.onTimeout(()->{
            log.info("播放等待超时");
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","timeout");
            result.setResult(resultJSON);
            inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, serial, code);
            storager.stopPlay(serial, code);
             // 清理RTP server
        });

        DeviceChannel deviceChannel = storager.queryChannel(serial, code);
        if (deviceChannel == null) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","channel[ " + code + " ]未找到");
            result.setResult(resultJSON);
            return result;
        }else if (!deviceChannel.getStatus().equalsIgnoreCase("ON")) {
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("error","channel[ " + code + " ]offline");
            result.setResult(resultJSON);
            return result;
        }
        MediaServer newMediaServerItem = playService.getNewMediaServerItem(device);

        playService.play(newMediaServerItem, serial, code, null, (errorCode, msg, data) -> {
            if (errorCode == InviteErrorCode.SUCCESS.getCode()) {
                if (data != null) {
                    StreamInfo streamInfo = (StreamInfo)data;
                    JSONObject resultJjson = new JSONObject();
                    resultJjson.put("StreamID", streamInfo.getStream());
                    resultJjson.put("DeviceID", serial);
                    resultJjson.put("ChannelID", code);
                    resultJjson.put("ChannelName", deviceChannel.getName());
                    resultJjson.put("ChannelCustomName", "");
                    if (streamInfo.getTranscodeStream() != null) {
                        resultJjson.put("FLV", streamInfo.getTranscodeStream().getFlv().getUrl());
                    }else {
                        resultJjson.put("FLV", streamInfo.getFlv().getUrl());

                    }
                    if(streamInfo.getHttps_flv() != null) {
                        if (streamInfo.getTranscodeStream() != null) {
                            resultJjson.put("HTTPS_FLV", streamInfo.getTranscodeStream().getHttps_flv().getUrl());
                        }else {
                            resultJjson.put("HTTPS_FLV", streamInfo.getHttps_flv().getUrl());
                        }
                    }

                    if (streamInfo.getTranscodeStream() != null) {
                        resultJjson.put("WS_FLV", streamInfo.getTranscodeStream().getWs_flv().getUrl());
                    }else {
                        resultJjson.put("WS_FLV", streamInfo.getWs_flv().getUrl());
                    }

                    if(streamInfo.getWss_flv() != null) {
                        if (streamInfo.getTranscodeStream() != null) {
                            resultJjson.put("WSS_FLV", streamInfo.getTranscodeStream().getWss_flv().getUrl());
                        }else {
                            resultJjson.put("WSS_FLV", streamInfo.getWss_flv().getUrl());
                        }
                    }
                    resultJjson.put("RTMP", streamInfo.getRtmp().getUrl());
                    if (streamInfo.getRtmps() != null) {
                        resultJjson.put("RTMPS", streamInfo.getRtmps().getUrl());
                    }
                    resultJjson.put("HLS", streamInfo.getHls().getUrl());
                    if (streamInfo.getHttps_hls() != null) {
                        resultJjson.put("HTTPS_HLS", streamInfo.getHttps_hls().getUrl());
                    }
                    resultJjson.put("RTSP", streamInfo.getRtsp().getUrl());
                    if (streamInfo.getRtsps() != null) {
                        resultJjson.put("RTSPS", streamInfo.getRtsps().getUrl());
                    }
                    resultJjson.put("WEBRTC", streamInfo.getRtc().getUrl());
                    if (streamInfo.getRtcs() != null) {
                        resultJjson.put("HTTPS_WEBRTC", streamInfo.getRtcs().getUrl());
                    }
                    resultJjson.put("CDN", "");
                    resultJjson.put("SnapURL", "");
                    resultJjson.put("Transport", device.getTransport());
                    resultJjson.put("StartAt", "");
                    resultJjson.put("Duration", "");
                    resultJjson.put("SourceVideoCodecName", "");
                    resultJjson.put("SourceVideoWidth", "");
                    resultJjson.put("SourceVideoHeight", "");
                    resultJjson.put("SourceVideoFrameRate", "");
                    resultJjson.put("SourceAudioCodecName", "");
                    resultJjson.put("SourceAudioSampleRate", "");
                    resultJjson.put("AudioEnable", "");
                    resultJjson.put("Ondemand", "");
                    resultJjson.put("InBytes", "");
                    resultJjson.put("InBitRate", "");
                    resultJjson.put("OutBytes", "");
                    resultJjson.put("NumOutputs", "");
                    resultJjson.put("CascadeSize", "");
                    resultJjson.put("RelaySize", "");
                    resultJjson.put("ChannelPTZType", "0");
                    result.setResult(resultJjson);
                }else {
                    JSONObject resultJjson = new JSONObject();
                    resultJjson.put("error", "channel[ " + code + " ] " + msg);
                    result.setResult(resultJjson);
                }
            }else {
                JSONObject resultJjson = new JSONObject();
                resultJjson.put("error", "channel[ " + code + " ] " + msg);
                result.setResult(resultJjson);
            }
        });

        return result;
    }

    /**
     * 实时直播 - 直播流停止
     * @param serial 设备编号
     * @param channel 通道序号
     * @param code 通道国标编号
     * @param check_outputs
     * @return
     */
    @GetMapping("/stop")
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
    @GetMapping("/touch")
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
