package com.genersoft.iot.vmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.ZLMHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.ZLMMediaListManager;
import com.genersoft.iot.vmp.media.zlm.ZLMRTPServerFactory;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 监听下级发送推送信息，并发送国标推流消息上级
 * @author lin
 */
@Component
public class RedisGbPlayMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisGbPlayMsgListener.class);

    public static final String WVP_PUSH_STREAM_KEY = "WVP_PUSH_STREAM";

    /**
     * 流媒体不存在的错误玛
     */
    public static final  int ERROR_CODE_MEDIA_SERVER_NOT_FOUND = -1;

    /**
     * 离线的错误玛
     */
    public static final  int ERROR_CODE_OFFLINE = -2;

    /**
     * 超时的错误玛
     */
    public static final  int ERROR_CODE_TIMEOUT = -3;

    private Map<String, PlayMsgCallback> callbacks = new ConcurrentHashMap<>();
    private Map<String, PlayMsgCallbackForStartSendRtpStream> callbacksForStartSendRtpStream = new ConcurrentHashMap<>();
    private Map<String, PlayMsgErrorCallback> callbacksForError = new ConcurrentHashMap<>();

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private RedisUtil redis;

    @Autowired
    private ZLMMediaListManager zlmMediaListManager;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private ZLMMediaListManager mediaListManager;

    @Autowired
    private ZLMHttpHookSubscribe subscribe;


    public interface PlayMsgCallback{
        void handler(ResponseSendItemMsg responseSendItemMsg);
    }

    public interface PlayMsgCallbackForStartSendRtpStream{
        void handler(JSONObject jsonObject);
    }

    public interface PlayMsgErrorCallback{
        void handler(WVPResult wvpResult);
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        JSONObject msgJSON = JSON.parseObject(message.getBody(), JSONObject.class);
        WvpRedisMsg wvpRedisMsg = JSON.toJavaObject(msgJSON, WvpRedisMsg.class);
        if (!userSetting.getServerId().equals(wvpRedisMsg.getToId())) {
            return;
        }
        if (WvpRedisMsg.isRequest(wvpRedisMsg)) {
            logger.info("[收到REDIS通知] 请求： {}", new String(message.getBody()));

            switch (wvpRedisMsg.getCmd()){
                case WvpRedisMsgCmd.GET_SEND_ITEM:
                    RequestSendItemMsg content = JSON.toJavaObject((JSONObject)wvpRedisMsg.getContent(), RequestSendItemMsg.class);
                    requestSendItemMsgHand(content, wvpRedisMsg.getFromId(), wvpRedisMsg.getSerial());
                    break;
                case WvpRedisMsgCmd.REQUEST_PUSH_STREAM:
                    RequestPushStreamMsg param = JSON.toJavaObject((JSONObject)wvpRedisMsg.getContent(), RequestPushStreamMsg.class);;
                    requestPushStreamMsgHand(param, wvpRedisMsg.getFromId(), wvpRedisMsg.getSerial());
                    break;
                default:
                    break;
            }

        }else {
            logger.info("[收到REDIS通知] 回复： {}", new String(message.getBody()));
            switch (wvpRedisMsg.getCmd()){
                case WvpRedisMsgCmd.GET_SEND_ITEM:

                    WVPResult content  = JSON.toJavaObject((JSONObject)wvpRedisMsg.getContent(), WVPResult.class);

                    String key = wvpRedisMsg.getSerial();
                    switch (content.getCode()) {
                        case 0:
                            ResponseSendItemMsg responseSendItemMsg =JSON.toJavaObject((JSONObject)content.getData(), ResponseSendItemMsg.class);
                            PlayMsgCallback playMsgCallback = callbacks.get(key);
                            if (playMsgCallback != null) {
                                callbacksForError.remove(key);
                                playMsgCallback.handler(responseSendItemMsg);
                            }
                            break;
                        case ERROR_CODE_MEDIA_SERVER_NOT_FOUND:
                        case ERROR_CODE_OFFLINE:
                        case ERROR_CODE_TIMEOUT:
                            PlayMsgErrorCallback errorCallback = callbacksForError.get(key);
                            if (errorCallback != null) {
                                callbacks.remove(key);
                                errorCallback.handler(content);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case WvpRedisMsgCmd.REQUEST_PUSH_STREAM:
                    WVPResult wvpResult  = JSON.toJavaObject((JSONObject)wvpRedisMsg.getContent(), WVPResult.class);
                    String serial = wvpRedisMsg.getSerial();
                    switch (wvpResult.getCode()) {
                        case 0:
                            JSONObject jsonObject = (JSONObject)wvpResult.getData();
                            PlayMsgCallbackForStartSendRtpStream playMsgCallback = callbacksForStartSendRtpStream.get(serial);
                            if (playMsgCallback != null) {
                                callbacksForError.remove(serial);
                                playMsgCallback.handler(jsonObject);
                            }
                            break;
                        case ERROR_CODE_MEDIA_SERVER_NOT_FOUND:
                        case ERROR_CODE_OFFLINE:
                        case ERROR_CODE_TIMEOUT:
                            PlayMsgErrorCallback errorCallback = callbacksForError.get(serial);
                            if (errorCallback != null) {
                                callbacks.remove(serial);
                                errorCallback.handler(wvpResult);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }




    }

    /**
     * 处理收到的请求推流的请求
     */
    private void requestPushStreamMsgHand(RequestPushStreamMsg requestPushStreamMsg, String fromId, String serial) {
        MediaServerItem mediaInfo = mediaServerService.getOne(requestPushStreamMsg.getMediaServerId());
        if (mediaInfo == null) {
            // TODO 回复错误
            return;
        }
        String is_Udp = requestPushStreamMsg.isTcp() ? "0" : "1";
        Map<String, Object> param = new HashMap<>();
        param.put("vhost","__defaultVhost__");
        param.put("app",requestPushStreamMsg.getApp());
        param.put("stream",requestPushStreamMsg.getStream());
        param.put("ssrc", requestPushStreamMsg.getSsrc());
        param.put("dst_url",requestPushStreamMsg.getIp());
        param.put("dst_port", requestPushStreamMsg.getPort());
        param.put("is_udp", is_Udp);
        param.put("src_port", requestPushStreamMsg.getSrcPort());
        param.put("pt", requestPushStreamMsg.getPt());
        param.put("use_ps", requestPushStreamMsg.isPs() ? "1" : "0");
        param.put("only_audio", requestPushStreamMsg.isOnlyAudio() ? "1" : "0");
        JSONObject jsonObject = zlmrtpServerFactory.startSendRtpStream(mediaInfo, param);
        // 回复消息
        responsePushStream(jsonObject, fromId, serial);
    }

    private void responsePushStream(JSONObject content, String toId, String serial) {

        WVPResult<JSONObject> result = new WVPResult<>();
        result.setCode(0);
        result.setData(content);

        WvpRedisMsg response = WvpRedisMsg.getResponseInstance(userSetting.getServerId(), toId,
                WvpRedisMsgCmd.REQUEST_PUSH_STREAM, serial, result);
        JSONObject jsonObject = (JSONObject)JSON.toJSON(response);
        redis.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
    }

    /**
     * 处理收到的请求sendItem的请求
     */
    private void requestSendItemMsgHand(RequestSendItemMsg content, String toId, String serial) {
        MediaServerItem mediaServerItem = mediaServerService.getOne(content.getMediaServerId());
        if (mediaServerItem == null) {
            logger.info("[回复推流信息] 流媒体{}不存在 ", content.getMediaServerId());

            WVPResult<SendRtpItem> result = new WVPResult<>();
            result.setCode(ERROR_CODE_MEDIA_SERVER_NOT_FOUND);
            result.setMsg("流媒体不存在");

            WvpRedisMsg response = WvpRedisMsg.getResponseInstance(userSetting.getServerId(), toId,
                    WvpRedisMsgCmd.GET_SEND_ITEM, serial, result);

            JSONObject jsonObject = (JSONObject)JSON.toJSON(response);
            redis.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
            return;
        }
        // 确定流是否在线
        boolean streamReady = zlmrtpServerFactory.isStreamReady(mediaServerItem, content.getApp(), content.getStream());
        if (streamReady) {
            logger.info("[回复推流信息]  {}/{}", content.getApp(), content.getStream());
            responseSendItem(mediaServerItem, content, toId, serial);
        }else {
            // 流已经离线
            // 发送redis消息以使设备上线
            logger.info("[ app={}, stream={} ]通道离线，发送redis信息控制设备开始推流",content.getApp(), content.getStream());

            String taskKey = UUID.randomUUID().toString();
            // 设置超时
            dynamicTask.startDelay(taskKey, ()->{
                logger.info("[ app={}, stream={} ] 等待设备开始推流超时", content.getApp(), content.getStream());
                WVPResult<SendRtpItem> result = new WVPResult<>();
                result.setCode(ERROR_CODE_TIMEOUT);
                WvpRedisMsg response = WvpRedisMsg.getResponseInstance(
                        userSetting.getServerId(), toId, WvpRedisMsgCmd.GET_SEND_ITEM, serial, result
                );
                JSONObject jsonObject = (JSONObject)JSON.toJSON(response);
                redis.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
            }, userSetting.getPlatformPlayTimeout());

            // 添加订阅
            JSONObject subscribeKey = new JSONObject();
            subscribeKey.put("app", content.getApp());
            subscribeKey.put("stream", content.getStream());
            subscribeKey.put("regist", true);
            subscribeKey.put("schema", "rtmp");
            subscribeKey.put("mediaServerId", mediaServerItem.getId());
            subscribe.addSubscribe(ZLMHttpHookSubscribe.HookType.on_stream_changed, subscribeKey,
                    (MediaServerItem mediaServerItemInUse, JSONObject json)->{
                        dynamicTask.stop(taskKey);
                        responseSendItem(mediaServerItem, content, toId, serial);
                    });

            MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(1, content.getApp(), content.getStream(),
                    content.getChannelId(), content.getPlatformId(), content.getPlatformName(), content.getServerId(),
                    content.getMediaServerId());
            redisCatchStorage.sendStreamPushRequestedMsg(messageForPushChannel);

        }
    }

    /**
     * 将获取到的sendItem发送出去
     */
    private void responseSendItem(MediaServerItem mediaServerItem, RequestSendItemMsg content, String toId, String serial) {
        SendRtpItem sendRtpItem = zlmrtpServerFactory.createSendRtpItem(mediaServerItem, content.getIp(),
                content.getPort(), content.getSsrc(), content.getPlatformId(),
                content.getApp(), content.getStream(), content.getChannelId(),
                content.getTcp());

        WVPResult<ResponseSendItemMsg> result = new WVPResult<>();
        result.setCode(0);
        ResponseSendItemMsg responseSendItemMsg = new ResponseSendItemMsg();
        responseSendItemMsg.setSendRtpItem(sendRtpItem);
        responseSendItemMsg.setMediaServerItem(mediaServerItem);
        result.setData(responseSendItemMsg);

        WvpRedisMsg response = WvpRedisMsg.getResponseInstance(
                userSetting.getServerId(), toId, WvpRedisMsgCmd.GET_SEND_ITEM, serial, result
        );
        JSONObject jsonObject = (JSONObject)JSON.toJSON(response);
        redis.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
    }

    /**
     * 发送消息要求下级生成推流信息
     * @param serverId 下级服务ID
     * @param app 应用名
     * @param stream 流ID
     * @param ip 目标IP
     * @param port 目标端口
     * @param ssrc  ssrc
     * @param platformId 平台国标编号
     * @param channelId 通道ID
     * @param isTcp 是否使用TCP
     * @param callback 得到信息的回调
     */
    public void sendMsg(String serverId, String mediaServerId, String app, String stream, String ip, int port, String ssrc,
                        String platformId, String channelId, boolean isTcp, String platformName, PlayMsgCallback callback, PlayMsgErrorCallback errorCallback) {
        RequestSendItemMsg requestSendItemMsg = RequestSendItemMsg.getInstance(
                serverId, mediaServerId, app, stream, ip, port, ssrc, platformId, channelId, isTcp, platformName);
        requestSendItemMsg.setServerId(serverId);
        String key = UUID.randomUUID().toString();
        WvpRedisMsg redisMsg = WvpRedisMsg.getRequestInstance(userSetting.getServerId(), serverId, WvpRedisMsgCmd.GET_SEND_ITEM,
                key, requestSendItemMsg);

        JSONObject jsonObject = (JSONObject)JSON.toJSON(redisMsg);
        logger.info("[请求推流SendItem] {}: {}", serverId, jsonObject);
        callbacks.put(key, callback);
        callbacksForError.put(key, errorCallback);
        dynamicTask.startDelay(key, ()->{
            callbacks.remove(key);
            callbacksForError.remove(key);
            WVPResult<Object> wvpResult = new WVPResult<>();
            wvpResult.setCode(ERROR_CODE_TIMEOUT);
            wvpResult.setMsg("timeout");
            errorCallback.handler(wvpResult);
        }, userSetting.getPlatformPlayTimeout());
        redis.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
    }

    /**
     * 发送请求推流的消息
     * @param param 推流参数
     * @param callback 回调
     */
    public void sendMsgForStartSendRtpStream(String serverId, RequestPushStreamMsg param, PlayMsgCallbackForStartSendRtpStream callback) {
        String key = UUID.randomUUID().toString();
        WvpRedisMsg redisMsg = WvpRedisMsg.getRequestInstance(userSetting.getServerId(), serverId,
                WvpRedisMsgCmd.REQUEST_PUSH_STREAM, key, param);

        JSONObject jsonObject = (JSONObject)JSON.toJSON(redisMsg);
        logger.info("[REDIS 请求其他平台推流] {}: {}", serverId, jsonObject);
        dynamicTask.startDelay(key, ()->{
            callbacksForStartSendRtpStream.remove(key);
            callbacksForError.remove(key);
        }, userSetting.getPlatformPlayTimeout());
        callbacksForStartSendRtpStream.put(key, callback);
        callbacksForError.put(key, (wvpResult)->{
            logger.info("[REDIS 请求其他平台推流] 失败: {}", wvpResult.getMsg());
            callbacksForStartSendRtpStream.remove(key);
            callbacksForError.remove(key);
        });
        redis.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
    }
}
