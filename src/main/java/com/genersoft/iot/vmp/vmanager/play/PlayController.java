package com.genersoft.iot.vmp.vmanager.play;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.media.zlm.ZLMRESTfulUtils;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.play.bean.PlayResult;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.UUID;

import javax.sip.message.Response;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PlayController {

    private final static Logger logger = LoggerFactory.getLogger(PlayController.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private DeferredResultHolder resultHolder;

    @Autowired
    private IPlayService playService;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @GetMapping("/play/{deviceId}/{channelId}")
    public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId,
                                                       @PathVariable String channelId) {
        Device device = storager.queryVideoDevice(deviceId);

        RequestMessage msg = playService.createCallbackPlayMsg();
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>();
        // 超时处理
        result.onTimeout(() -> {
            logger.warn(String.format("设备点播超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            // 释放rtpserver
            cmder.closeRTPServer(device, channelId);
            StreamInfo streamInfo = streamSession.getPlayStreamInfo(channelId);
            streamSession.remove(streamInfo);
            msg.setData("Timeout");
            resultHolder.invokeResult(msg);
        });
        resultHolder.put(msg.getId(), result);

        // 判断是否已经存在点播
        StreamInfo oldStreamInfo = streamSession.getPlayStreamInfo(channelId);
        if (oldStreamInfo == null) {
            // 发送点播消息
            playStreamCmd(device, channelId, msg);
            return result;
        }

        // 若已有人点播，直接播放
        String streamId = oldStreamInfo.getStreamId();
        String mediaServerIp = oldStreamInfo.getMediaServerIp();
        JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaServerIp, streamId);
        if (rtpInfo.getBoolean("exist")) {
            msg.setData(JSON.toJSONString(oldStreamInfo));
            resultHolder.invokeResult(msg);
            return result;
        }

        // 若已有人点播，但已超时自动断开，则重新发起点播
        storager.stopPlay(oldStreamInfo.getDeviceID(), oldStreamInfo.getChannelId());
        streamSession.remove(oldStreamInfo);

        playStreamCmd(device, channelId, msg);
        return result;
    }

    private void playStreamCmd(Device device, String channelId, RequestMessage msg) {
        cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
            logger.info("收到点播回调消息： " + response.toJSONString());
            playService.onPublishHandlerForPlay(response, device.getDeviceId(), channelId, msg);
        }, event -> {
            StreamInfo streamInfo = streamSession.getPlayStreamInfo(channelId);
            streamSession.remove(streamInfo);
            Response response = event.getResponse();
            int statusCode = response.getStatusCode();
            String errMsg;
            if (503 == statusCode) {
                errMsg = "点播失败，请检查在NVR上是否可以正常打开监控，并检查NVR和SIP是否连通， 错误码： %s, %s";
            } else {
                errMsg = "点播失败，错误码： %s, %s";
            }
            msg.setData(String.format(errMsg, statusCode, response.getReasonPhrase()));
            resultHolder.invokeResult(msg);
        });
    }

    @PostMapping("/play/{channelId}/{streamId}/stop")
    public DeferredResult<ResponseEntity<String>> playStop(@PathVariable String channelId, @PathVariable String streamId) {

        logger.debug(String.format("设备预览/回放停止API调用，streamId：%s", streamId));

        RequestMessage msg = playService.createCallbackPlayMsg();
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>();
        // 超时处理
        result.onTimeout(() -> {
            logger.warn(String.format("设备预览/回放停止超时，streamId：%s ", streamId));
            msg.setData("Timeout");
            resultHolder.invokeResult(msg);
        });

        // 录像查询以channelId作为deviceId查询
        resultHolder.put(msg.getId(), result);

        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);
        if (streamInfo == null) {
            msg.setData("streamId not found");
            resultHolder.invokeResult(msg);
        } else {
            cmder.stopStreamByeCmd(streamInfo, event -> {
                msg.setData(String.format("success"));
                resultHolder.invokeResult(msg);
            });
        }
        return result;
    }

    /**
     * 将不是h264的视频通过ffmpeg 转码为h264 + aac
     *
     * @param streamId 流ID
     */
    @PostMapping("/play/{channelId}/{streamId}/convert")
    public ResponseEntity<String> playConvert(@PathVariable String channelId, @PathVariable String streamId) {
        StreamInfo streamInfo = streamSession.getPlayStreamInfo(channelId);
        if (streamInfo == null) {
            logger.warn("视频转码API调用失败！, 视频流已经停止!");
            return new ResponseEntity<String>("未找到视频流信息, 视频流可能已经停止", HttpStatus.OK);
        }
        String mediaServerIp = streamInfo.getMediaServerIp();
        JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaServerIp, streamId);
        if (!rtpInfo.getBoolean("exist")) {
            logger.warn("视频转码API调用失败！, 视频流已停止推流!");
            return new ResponseEntity<String>("推流信息在流媒体中不存在, 视频流可能已停止推流", HttpStatus.OK);
        } else {
            MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
            String dstUrl = String.format("rtmp://%s:%s/convert/%s", mediaServerIp, mediaInfo.getRtmpPort(),
                    streamId);
            String srcUrl = String.format("rtsp://%s:%s/rtp/%s", mediaServerIp, mediaInfo.getRtspPort(), streamId);
            JSONObject jsonObject = zlmresTfulUtils.addFFmpegSource(mediaServerIp, srcUrl, dstUrl, "1000000");
            System.out.println(jsonObject);
            JSONObject result = new JSONObject();
            if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                result.put("code", 0);
                JSONObject data = jsonObject.getJSONObject("data");
                if (data != null) {
                    result.put("key", data.getString("key"));
                    StreamInfo streamInfoResult = new StreamInfo();
                    streamInfoResult.setRtmp(dstUrl);
                    streamInfoResult.setRtsp(String.format("rtsp://%s:%s/convert/%s", mediaServerIp, mediaInfo.getRtspPort(), streamId));
                    streamInfoResult.setStreamId(streamId);
                    streamInfoResult.setFlv(String.format("http://%s:%s/convert/%s.flv", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setWs_flv(String.format("ws://%s:%s/convert/%s.flv", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setHls(String.format("http://%s:%s/convert/%s/hls.m3u8", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setWs_hls(String.format("ws://%s:%s/convert/%s/hls.m3u8", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setFmp4(String.format("http://%s:%s/convert/%s.live.mp4", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setWs_fmp4(String.format("ws://%s:%s/convert/%s.live.mp4", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setTs(String.format("http://%s:%s/convert/%s.live.ts", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    streamInfoResult.setWs_ts(String.format("ws://%s:%s/convert/%s.live.ts", mediaServerIp, mediaInfo.getHttpPort(), streamId));
                    result.put("data", streamInfoResult);
                }
            } else {
                result.put("code", 1);
                result.put("msg", "cover fail");
            }
            return new ResponseEntity<>(result.toJSONString(), HttpStatus.OK);
        }
    }

    /**
     * 结束转码
     *
     * @param key
     * @return
     */
    @PostMapping("/play/convert/stop/{channelId}/{streamId}/{key}")
    public ResponseEntity<String> playConvertStop(@PathVariable String channelId, @PathVariable String streamId, @PathVariable String key) {
        String mediaServerIp = streamSession.getMediaServerIp(channelId, streamId);
        JSONObject jsonObject = zlmresTfulUtils.delFFmpegSource(mediaServerIp, key);
        System.out.println(jsonObject);
        JSONObject result = new JSONObject();
        if (jsonObject != null && jsonObject.getInteger("code") == 0) {
            result.put("code", 0);
            JSONObject data = jsonObject.getJSONObject("data");
            if (data != null && data.getBoolean("flag")) {
                result.put("code", "0");
                result.put("msg", "success");
            } else {

			}
		}else {
			result.put("code", 1);
			result.put("msg", "delFFmpegSource fail");
		}
		return new ResponseEntity<String>( result.toJSONString(), HttpStatus.OK);
	}

	/**
     * 语音广播命令API接口
     *
     * @param deviceId
     */
    @GetMapping("/broadcast/{deviceId}")
    @PostMapping("/broadcast/{deviceId}")
    public DeferredResult<ResponseEntity<String>> broadcastApi(@PathVariable String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("语音广播API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
		cmder.audioBroadcastCmd(device, event -> {
			Response response = event.getResponse();
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Description", String.format("语音广播操作失败，错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>(3 * 1000L);
		result.onTimeout(() -> {
			logger.warn(String.format("语音广播操作超时, 设备未返回应答指令"));
			RequestMessage msg = new RequestMessage();
			msg.setId(DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Error", "Timeout. Device did not response to broadcast command.");
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId, result);
		return result;
	}

}

