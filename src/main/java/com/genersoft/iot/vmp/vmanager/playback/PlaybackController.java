package com.genersoft.iot.vmp.vmanager.playback;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.sip.message.Response;

@CrossOrigin
@RestController
@RequestMapping("/api")
public class PlaybackController {

    private final static Logger logger = LoggerFactory.getLogger(PlaybackController.class);

    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private VideoStreamSessionManager streamSession;

    @Autowired
    private IPlayService playService;

    @Autowired
    private DeferredResultHolder resultHolder;

    @GetMapping("/playback/{deviceId}/{channelId}")
    public DeferredResult<ResponseEntity<String>> play(@PathVariable String deviceId, @PathVariable String channelId, String startTime,
                                                       String endTime) {

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("设备回放 API调用，deviceId：%s ，channelId：%s", deviceId, channelId));
        }
        RequestMessage msg = playService.createCallbackPlayMsg();
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<ResponseEntity<String>>();
        // 超时处理
        result.onTimeout(() -> {
            logger.warn(String.format("设备回放超时，deviceId：%s ，channelId：%s", deviceId, channelId));
            StreamInfo streamInfo = streamSession.getPlayBackStreamInfo(channelId);
            streamSession.remove(streamInfo);
            msg.setData("Timeout");
            resultHolder.invokeResult(msg);
        });
        Device device = storager.queryVideoDevice(deviceId);
        StreamInfo oldStreamInfo = streamSession.getPlayBackStreamInfo(channelId);
        if (oldStreamInfo != null) {
            // TODO 只能停止自己之前的回放，不能停止别人的回放，否则会导致别人无法播放
            cmder.stopStreamByeCmd(oldStreamInfo, null);
        }
        resultHolder.put(msg.getId(), result);
        cmder.playbackStreamCmd(device, channelId, startTime, endTime, (JSONObject response) -> {
            logger.info("收到订阅消息： " + response.toJSONString());
            playService.onPublishHandlerForPlayBack(response, deviceId, channelId, msg);
        }, event -> {
            StreamInfo streamInfo = streamSession.getPlayBackStreamInfo(channelId);
            streamSession.remove(streamInfo);
            Response response = event.getResponse();
            msg.setData(String.format("回放失败， 错误码： %s, %s", response.getStatusCode(), response.getReasonPhrase()));
            resultHolder.invokeResult(msg);
        });

        return result;
    }

    @RequestMapping("/playback/{channelId}/{ssrc}/stop")
    public ResponseEntity<String> playStop(@PathVariable String channelId, @PathVariable String ssrc) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("设备录像回放停止 API调用，ssrc：%s", ssrc));
        }
        if (ssrc == null) {
            logger.warn("设备录像回放停止API调用失败！");
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, ssrc);
        if (streamInfo == null) {
            logger.warn("回放播流不存在！");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        cmder.stopStreamByeCmd(streamInfo, null);
        JSONObject json = new JSONObject();
        json.put("ssrc", ssrc);
        return new ResponseEntity<>(json.toString(), HttpStatus.OK);
    }
}
