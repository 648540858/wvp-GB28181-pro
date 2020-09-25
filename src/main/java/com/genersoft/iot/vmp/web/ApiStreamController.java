package com.genersoft.iot.vmp.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.play.PlayController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

        Device device = storager.queryVideoDevice(serial);
        if (device == null ) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + serial + " ]未找到");
            return result;
        }
        DeviceChannel deviceChannel = storager.queryChannel(serial, code);
        if (deviceChannel == null) {
            JSONObject result = new JSONObject();
            result.put("error","channel[ " + code + " ]未找到");
            return result;
        }
        // 查询是否已经在播放
        StreamInfo streamInfo = storager.queryPlay(device.getDeviceId(), code);
        if (streamInfo == null) streamInfo = cmder.playStreamCmd(device, code);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("设备预览 API调用，deviceId：%s ，channelId：%s",serial, code));
            logger.debug("设备预览 API调用，ssrc："+streamInfo.getSsrc()+",ZLMedia streamId:"+Integer.toHexString(Integer.parseInt(streamInfo.getSsrc())));
        }

        if(streamInfo!=null) {
            JSONObject result = new JSONObject();
            result.put("StreamID", streamInfo.getSsrc());
            result.put("DeviceID", device.getDeviceId());
            result.put("ChannelID", code);
            result.put("ChannelName", deviceChannel.getName());
            result.put("ChannelCustomName ", "");
            result.put("FLV ", streamInfo.getFlv());
            result.put("WS_FLV ", streamInfo.getWS_FLV());
            result.put("RTMP", streamInfo.getRTMP());
            result.put("HLS", streamInfo.getHLS());
            result.put("RTSP", streamInfo.getRTSP());
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
        StreamInfo streamInfo = storager.queryPlay(serial, code);
        if (streamInfo == null) {
            JSONObject result = new JSONObject();
            result.put("error","未找到流信息");
            return result;
        }
        cmder.streamByeCmd(streamInfo.getSsrc());
        storager.stopPlay(serial, code);
        return null;
    }

    /**
     * 实时直播 - 直播流保活
     * @param serial 设备编号
     * @param channel 通道序号
     * @param code 通道国标编号
     * @param check_outputs
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
