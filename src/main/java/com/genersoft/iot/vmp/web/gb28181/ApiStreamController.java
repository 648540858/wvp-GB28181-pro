package com.genersoft.iot.vmp.web.gb28181;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.UserSetup;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.DeviceChannel;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IPlayService;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.gb28181.play.bean.PlayResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * API兼容：实时直播
 */
@SuppressWarnings(value = {"rawtypes", "unchecked"})
@CrossOrigin
@RestController
@RequestMapping(value = "/api/v1/stream")
public class ApiStreamController {

    private final static Logger logger = LoggerFactory.getLogger(ApiStreamController.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private UserSetup userSetup;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private IPlayService playService;

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
        DeferredResult<JSONObject> resultDeferredResult = new DeferredResult<>(userSetup.getPlayTimeout() + 10);
        Device device = storager.queryVideoDevice(serial);
        if (device == null ) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + serial + " ]未找到");
            resultDeferredResult.setResult(result);
        }else if (device.getOnline() == 0) {
            JSONObject result = new JSONObject();
            result.put("error","device[ " + code + " ]offline");
            resultDeferredResult.setResult(result);
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
        }else if (deviceChannel.getStatus() == 0) {
            JSONObject result = new JSONObject();
            result.put("error","channel[ " + code + " ]offline");
            resultDeferredResult.setResult(result);
        }
        MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);
        PlayResult play = playService.play(newMediaServerItem, serial, code, (mediaServerItem, response)->{
            StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(serial, code);
            JSONObject result = new JSONObject();
            result.put("StreamID", streamInfo.getStream());
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
            result.put("ChannelPTZType", "0");
            resultDeferredResult.setResult(result);
//            Class<?> aClass = responseEntity.getClass().getSuperclass();
//            Field body = null;
//            try {
//                // 使用反射动态修改返回的body
//                body = aClass.getDeclaredField("body");
//                body.setAccessible(true);
//                body.set(responseEntity, result);
//            } catch (NoSuchFieldException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
        }, (eventResult) -> {
            JSONObject result = new JSONObject();
            result.put("error", "channel[ " + code + " ] " + eventResult.msg);
            resultDeferredResult.setResult(result);
        }, null);
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

        StreamInfo streamInfo = redisCatchStorage.queryPlayByDevice(serial, code);
        if (streamInfo == null) {
            JSONObject result = new JSONObject();
            result.put("error","未找到流信息");
            return result;
        }
        cmder.streamByeCmd(serial, code, streamInfo.getStream());
        redisCatchStorage.stopPlay(streamInfo);
        storager.stopPlay(streamInfo.getDeviceID(), streamInfo.getChannelId());
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
