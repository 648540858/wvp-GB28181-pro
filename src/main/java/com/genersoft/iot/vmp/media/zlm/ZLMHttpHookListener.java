package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.session.VideoStreamSessionManager;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommander;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import com.genersoft.iot.vmp.vmanager.service.IPlayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description:针对 ZLMediaServer的hook事件监听
 * @author: swwheihei
 * @date: 2020年5月8日 上午10:46:48
 */
@RestController
@RequestMapping("/index/hook")
public class ZLMHttpHookListener {

    private final static Logger logger = LoggerFactory.getLogger(ZLMHttpHookListener.class);


    @Autowired
    private SIPCommander cmder;

    @Autowired
    private IPlayService playService;

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    @Autowired
    private ZLMHttpHookSubscribe subscribe;

    @Autowired
    MediaConfig mediaConfig;

    @Autowired
    private VideoStreamSessionManager streamSession;

    /**
     * 流量统计事件，播放器或推流器断开时并且耗用流量超过特定阈值时会触发此事件，阈值通过配置文件general.flowThreshold配置；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_flow_report", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onFlowReport(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_flow_report API调用，参数：" + json.toString());
        }
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * 访问http文件服务器上hls之外的文件时触发。
     */
    @ResponseBody
    @PostMapping(value = "/on_http_access", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onHttpAccess(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_http_access API 调用，参数：" + json.toString());
        }
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("err", "");
        ret.put("path", "");
        ret.put("second", 600);
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * 播放器鉴权事件，rtsp/rtmp/http-flv/ws-flv/hls的播放都将触发此鉴权事件。
     */
    @ResponseBody
    @PostMapping(value = "/on_play", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onPlay(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_play API调用，参数：" + json.toString());
        }
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * rtsp/rtmp/rtp推流鉴权事件。
     */
    @ResponseBody
    @PostMapping(value = "/on_publish", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onPublish(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_publish API调用，参数：" + json.toString());
        }

        ZLMHttpHookSubscribe.Event subscribe = this.subscribe.getSubscribe(ZLMHttpHookSubscribe.HookType.on_publish, json);
        if (subscribe != null) {
            subscribe.response(json);
        }

        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        ret.put("enableHls", true);
        ret.put("enableMP4", false);
        ret.put("enableRtxp", true);
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * 录制mp4完成后通知事件；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_record_mp4", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onRecordMp4(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_record_mp4 API调用，参数：" + json.toString());
        }
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * rtsp专用的鉴权事件，先触发on_rtsp_realm事件然后才会触发on_rtsp_auth事件。
     */
    @ResponseBody
    @PostMapping(value = "/on_rtsp_realm", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onRtspRealm(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_rtsp_realm API调用，参数：" + json.toString());
        }
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("realm", "");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }


    /**
     * 该rtsp流是否开启rtsp专用方式的鉴权事件，开启后才会触发on_rtsp_auth事件。需要指出的是rtsp也支持url参数鉴权，它支持两种方式鉴权。
     */
    @ResponseBody
    @PostMapping(value = "/on_rtsp_auth", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onRtspAuth(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_rtsp_auth API调用，参数：" + json.toString());
        }
        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("encrypted", false);
        ret.put("passwd", "test");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * shell登录鉴权，ZLMediaKit提供简单的telnet调试方式，使用telnet 127.0.0.1 9000能进入MediaServer进程的shell界面。
     */
    @ResponseBody
    @PostMapping(value = "/on_shell_login", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onShellLogin(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_shell_login API调用，参数：" + json.toString());
        }
        // TODO 如果是带有rtpstream则开启按需拉流
        // String app = json.getString("app");
        // String stream = json.getString("stream");

        ZLMHttpHookSubscribe.Event subscribe = this.subscribe.getSubscribe(ZLMHttpHookSubscribe.HookType.on_publish, json);
        if (subscribe != null) subscribe.response(json);

        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }

    /**
     * rtsp/rtmp流注册或注销时触发此事件；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_changed", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onStreamChanged(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_stream_changed API调用，参数：" + json.toString());
        }

        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");

        // 流消失移除redis play
        String app = json.getString("app");
        String streamId = json.getString("stream");
        String schema = json.getString("schema");
        boolean regist = json.getBoolean("regist");
        if (!"rtp".equals(app) || regist) {
            if (!"rtp".equals(app) && "rtsp".equals(schema)) {
                zlmMediaListManager.updateMediaList();
            }
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }

        String[] s = streamId.split("_");
        if (s.length != 4) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        String channelId = s[3];
        // TODO channelId
        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);
        if (null != streamInfo) {
            cmder.stopStreamByeCmd(streamInfo, null);
        }
        return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
    }

    /**
     * 流无人观看时事件，用户可以通过此事件选择是否关闭无人看的流。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_none_reader", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onStreamNoneReader(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_stream_none_reader API调用，参数：" + json.toString());
        }

        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("close", true);

        String app = json.getString("app");
        String streamId = json.getString("stream");
        if (!"rtp".equals(app) || streamId.indexOf("gb_play") < 0) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        String[] s = streamId.split("_");
        if (s.length != 4) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        String channelId = s[3];
        // TODO channelId
        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);
        if (null != streamInfo) {
            if (redisCatchStorage.isChannelSendingRTP(streamInfo.getChannelId())) {
                ret.put("close", false);
            } else {
                cmder.stopStreamByeCmd(streamInfo, null);
            }
        }
        return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
    }

    /**
     * 流未找到事件，用户可以在此事件触发时，立即去拉流，这样可以实现按需拉流；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_stream_not_found", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onStreamNotFound(@RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_stream_not_found API调用，参数：" + json.toString());
        }

        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        if (!mediaConfig.getAutoApplyPlay()) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        String app = json.getString("app");
        String streamId = json.getString("stream");

        if (!"rtp".equals(app) || streamId.indexOf("gb_play") < 0) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        String[] s = streamId.split("_");
        if (s.length != 4) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        String deviceId = s[2];
        String channelId = s[3];
        StreamInfo streamInfo = streamSession.getStreamInfo(channelId, streamId);

        if (streamInfo != null) {
            return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
        }
        Device device = storager.queryVideoDevice(deviceId);
        if (device != null) {
            RequestMessage msg = playService.createCallbackPlayMsg();
            cmder.playStreamCmd(device, channelId, (JSONObject response) -> {
                logger.info("收到订阅消息： " + response.toJSONString());
                playService.onPublishHandlerForPlay(response, deviceId, channelId, msg);
            }, null);
        }
        return new ResponseEntity<>(ret.toString(), HttpStatus.OK);
    }

    /**
     * 服务器启动事件，可以用于监听服务器崩溃重启；此事件对回复不敏感。
     */
    @ResponseBody
    @PostMapping(value = "/on_server_started", produces = "application/json;charset=UTF-8")
    public ResponseEntity<String> onServerStarted(HttpServletRequest request, @RequestBody JSONObject json) {

        if (logger.isDebugEnabled()) {
            logger.debug("ZLM HOOK on_server_started API调用，参数：" + json.toString());
        }

        MediaServerConfig mediaServerConfig = JSON.toJavaObject(json, MediaServerConfig.class);
        redisCatchStorage.updateMediaInfo(mediaServerConfig);
        // TODO Auto-generated method stub

        JSONObject ret = new JSONObject();
        ret.put("code", 0);
        ret.put("msg", "success");
        return new ResponseEntity<String>(ret.toString(), HttpStatus.OK);
    }
}
