package com.genersoft.iot.vmp.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.play.PlayController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 兼容LiveGBS的API：实时直播
 */
@CrossOrigin
@RestController
@RequestMapping(value = "/api/v1/stream")
public class ApiStreamController {

    private final static Logger logger = LoggerFactory.getLogger(ApiStreamController.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorager storager;

    @Value("${media.closeWaitRTPInfo}")
    private boolean closeWaitRTPInfo;


    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    /**
     * 实时直播 - 开始直播
     * @param serial 设备编号
     * @param channel 通道序号 默认值: 1
     * @param code 通道编号,通过 /api/v1/device/channellist 获取的 ChannelList.ID, 该参数和 channel 二选一传递即可
     * @param cdn TODO 转推 CDN 地址, 形如: [rtmp|rtsp]://xxx, encodeURIComponent
     * @param audio TODO 是否开启音频, 默认 开启
     * @param transport 流传输模式， 默认 UDP
     * @param checkchannelstatus TODO 是否检查通道状态, 默认 false, 表示 拉流前不检查通道状态是否在线
     * @param transportmode TODO 当 transport=TCP 时有效, 指示流传输主被动模式, 默认被动
     * @param timeout TODO 拉流超时(秒),
     * @return
     */
    @RequestMapping(value = "/start")
    private JSONObject start(String serial ,
                             @RequestParam(required = false)Integer channel ,
                             @RequestParam(required = false)String code,
                             @RequestParam(required = false)String cdn,
                             @RequestParam(required = false)String audio,
                             @RequestParam(required = false)String transport,
                             @RequestParam(required = false)String checkchannelstatus ,
                             @RequestParam(required = false)String transportmode,
                             @RequestParam(required = false)String timeout

    ){
        int getEncoding = closeWaitRTPInfo?  1: 0;
        Device device = storager.queryVideoDevice(serial);

        if (device == null ) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + serial + " ]未找到");
            return result;
        }else if (device.getOnline() == 0) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + code + " ]offline");
            return result;
        }

        DeviceChannel deviceChannel = storager.queryChannel(serial, code);
        if (deviceChannel == null) {
            JSONObject result = new JSONObject();
            result.put("error","channel[ " + code + " ]未找到");
            return result;
        }else if (deviceChannel.getStatus() == 0) {
            JSONObject result = new JSONObject();
            result.put("error","channel[ " + code + " ]offline");
            return result;
        }

        // 查询是否已经在播放
        StreamInfo streamInfo = storager.queryPlayByDevice(device.getDeviceId(), code);
        if (streamInfo == null) {
            logger.debug("streamInfo 等于null, 重新点播");
            streamInfo = cmder.playStreamCmd(device, code);
        }else {
            logger.debug("streamInfo 不等于null, 向流媒体查询是否正在推流");
            String streamId = String.format("%08x", Integer.parseInt(streamInfo.getSsrc())).toUpperCase();
            JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(streamId);
            if (rtpInfo.getBoolean("exist")) {
                logger.debug("向流媒体查询正在推流, 直接返回: " + streamInfo.getRtsp());
                JSONObject result = new JSONObject();
                result.put("StreamID", streamInfo.getSsrc());
                result.put("DeviceID", device.getDeviceId());
                result.put("ChannelID", code);
                result.put("ChannelName", deviceChannel.getName());
                result.put("ChannelCustomName", "");
                result.put("FLV", streamInfo.getFlv());
                result.put("WS_FLV", streamInfo.getWs_flv());
                result.put("RTMP", streamInfo.getRtmp());
                result.put("HLS", streamInfo.getHls());
                result.put("RTSP", streamInfo.getRtsp());
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
                result.put("ChannelPTZType", 0);
                return result;
            } else {
                logger.debug("向流媒体查询没有推流, 重新点播");
                storager.stopPlay(streamInfo);
                streamInfo = cmder.playStreamCmd(device, code);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("设备预览 API调用，deviceId：%s ，channelId：%s",serial, code));
            logger.debug("设备预览 API调用，ssrc："+streamInfo.getSsrc()+",ZLMedia streamId:"+Integer.toHexString(Integer.parseInt(streamInfo.getSsrc())));
        }
        boolean lockFlag = true;
        long startTime = System.currentTimeMillis();
        while (lockFlag) {
            try {
                if (System.currentTimeMillis() - startTime > 10 * 1000) {
                    storager.stopPlay(streamInfo);
                    logger.info("播放等待超时");
                    JSONObject result = new JSONObject();
                    result.put("error","timeout");
                    return result;
                } else {

                    StreamInfo streamInfoNow = storager.queryPlayByDevice(serial, code);
                    logger.debug("正在向流媒体查询");
                    if (streamInfoNow != null && streamInfoNow.getFlv() != null) {
                        streamInfo = streamInfoNow;
                        logger.debug("向流媒体查询到: " + streamInfoNow.getRtsp());
                        lockFlag = false;
                        continue;
                    } else {
                        Thread.sleep(2000);
                        continue;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(streamInfo!=null) {
            JSONObject result = new JSONObject();
            result.put("StreamID", streamInfo.getSsrc());
            result.put("DeviceID", device.getDeviceId());
            result.put("ChannelID", code);
            result.put("ChannelName", deviceChannel.getName());
            result.put("ChannelCustomName", "");
            result.put("FLV", streamInfo.getFlv());
            result.put("WS_FLV", streamInfo.getWs_flv());
            result.put("RTMP", streamInfo.getRtmp());
            result.put("HLS", streamInfo.getHls());
            result.put("RTSP", streamInfo.getRtsp());
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
            result.put("ChannelPTZType", 0);
            return result;
        } else {
            logger.warn("设备预览API调用失败！");
            JSONObject result = new JSONObject();
            result.put("error","调用失败");
            return result;
        }
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
        StreamInfo streamInfo = storager.queryPlayByDevice(serial, code);
        if (streamInfo == null) {
            JSONObject result = new JSONObject();
            result.put("error","未找到流信息");
            return result;
        }
        cmder.streamByeCmd(streamInfo.getSsrc());
        storager.stopPlay(streamInfo);
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
