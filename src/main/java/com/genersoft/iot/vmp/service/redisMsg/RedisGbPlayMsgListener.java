package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.common.VideoManagerConstants;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;
import com.genersoft.iot.vmp.media.zlm.ZLMServerFactory;
import com.genersoft.iot.vmp.media.zlm.ZlmHttpHookSubscribe;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeFactory;
import com.genersoft.iot.vmp.media.zlm.dto.HookSubscribeForStreamChange;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.bean.*;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


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
    private RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    private ZLMServerFactory zlmServerFactory;

    @Autowired
    private IMediaServerService mediaServerService;


    @Autowired
    private DynamicTask dynamicTask;


    @Autowired
    private ZlmHttpHookSubscribe subscribe;

    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;


    public interface PlayMsgCallback{
        void handler(ResponseSendItemMsg responseSendItemMsg) throws ParseException;
    }

    public interface PlayMsgCallbackForStartSendRtpStream{
        void handler(JSONObject jsonObject);
    }

    public interface PlayMsgErrorCallback{
        void handler(WVPResult wvpResult);
    }

    @Override
    public void onMessage(Message message, byte[] bytes) {
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        JSONObject msgJSON = JSON.parseObject(msg.getBody(), JSONObject.class);
                        WvpRedisMsg wvpRedisMsg = JSON.to(WvpRedisMsg.class, msgJSON);
                        if (!userSetting.getServerId().equals(wvpRedisMsg.getToId())) {
                            continue;
                        }
                        if (WvpRedisMsg.isRequest(wvpRedisMsg)) {
                            logger.info("[收到REDIS通知] 请求： {}", new String(msg.getBody()));

                            switch (wvpRedisMsg.getCmd()){
                                case WvpRedisMsgCmd.GET_SEND_ITEM:
                                    RequestSendItemMsg content = JSON.to(RequestSendItemMsg.class, wvpRedisMsg.getContent());
                                    requestSendItemMsgHand(content, wvpRedisMsg.getFromId(), wvpRedisMsg.getSerial());
                                    break;
                                case WvpRedisMsgCmd.REQUEST_PUSH_STREAM:
                                    RequestPushStreamMsg param = JSON.to(RequestPushStreamMsg.class, wvpRedisMsg.getContent());
                                    requestPushStreamMsgHand(param, wvpRedisMsg.getFromId(), wvpRedisMsg.getSerial());

                                    break;
                                default:
                                    break;
                            }

                        }else {
                            logger.info("[收到REDIS通知] 回复： {}", new String(msg.getBody()));
                            switch (wvpRedisMsg.getCmd()){
                                case WvpRedisMsgCmd.GET_SEND_ITEM:

                                   WVPResult content  = JSON.to(WVPResult.class, wvpRedisMsg.getContent());

                                    String key = wvpRedisMsg.getSerial();
                                    switch (content.getCode()) {
                                        case 0:
                                           ResponseSendItemMsg responseSendItemMsg =JSON.to(ResponseSendItemMsg.class, content.getData());
                                            PlayMsgCallback playMsgCallback = callbacks.get(key);
                                            if (playMsgCallback != null) {
                                                callbacksForError.remove(key);
                                                try {
                                                    playMsgCallback.handler(responseSendItemMsg);
                                                } catch (ParseException e) {
                                                    logger.error("[REDIS消息处理异常] ", e);
                                                }
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
                                    WVPResult wvpResult  = JSON.to(WVPResult.class, wvpRedisMsg.getContent());
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
                    }catch (Exception e) {
                        logger.warn("[RedisGbPlayMsg] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[RedisGbPlayMsg] 异常内容： ", e);
                    }
                }
            });
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
        JSONObject jsonObject = zlmServerFactory.startSendRtpStream(mediaInfo, param);
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
        redisTemplate.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
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
            redisTemplate.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
            return;
        }
        // 确定流是否在线
        Boolean streamReady = zlmServerFactory.isStreamReady(mediaServerItem, content.getApp(), content.getStream());
        if (streamReady != null && streamReady) {
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
                redisTemplate.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
            }, userSetting.getPlatformPlayTimeout());

            // 添加订阅
            HookSubscribeForStreamChange hookSubscribe = HookSubscribeFactory.on_stream_changed(content.getApp(), content.getStream(), true, "rtsp", mediaServerItem.getId());

            subscribe.addSubscribe(hookSubscribe, (mediaServerItemInUse, hookParam)->{
                        dynamicTask.stop(taskKey);
                        responseSendItem(mediaServerItem, content, toId, serial);
                    });

            MessageForPushChannel messageForPushChannel = MessageForPushChannel.getInstance(1, content.getApp(), content.getStream(),
                    content.getChannelId(), content.getPlatformId(), content.getPlatformName(), content.getServerId(),
                    content.getMediaServerId());

            String key = VideoManagerConstants.VM_MSG_STREAM_PUSH_REQUESTED;
            logger.info("[redis发送通知] 推流被请求 {}: {}/{}", key, messageForPushChannel.getApp(), messageForPushChannel.getStream());
            redisTemplate.convertAndSend(key, JSON.toJSON(messageForPushChannel));
        }
    }

    /**
     * 将获取到的sendItem发送出去
     */
    private void responseSendItem(MediaServerItem mediaServerItem, RequestSendItemMsg content, String toId, String serial) {
        SendRtpItem sendRtpItem = zlmServerFactory.createSendRtpItem(mediaServerItem, content.getIp(),
                content.getPort(), content.getSsrc(), content.getPlatformId(),
                content.getApp(), content.getStream(), content.getChannelId(),
                content.getTcp(), content.getRtcp());

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
        redisTemplate.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
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
                        String platformId, String channelId, boolean isTcp, boolean rtcp, String platformName, PlayMsgCallback callback, PlayMsgErrorCallback errorCallback) {
        RequestSendItemMsg requestSendItemMsg = RequestSendItemMsg.getInstance(
                serverId, mediaServerId, app, stream, ip, port, ssrc, platformId, channelId, isTcp, rtcp, platformName);
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
        redisTemplate.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
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
        redisTemplate.convertAndSend(WVP_PUSH_STREAM_KEY, jsonObject);
    }

    private SendRtpItem querySendRTPServer(String platformGbId, String channelId, String streamId, String callId) {
        if (platformGbId == null) {
            platformGbId = "*";
        }
        if (channelId == null) {
            channelId = "*";
        }
        if (streamId == null) {
            streamId = "*";
        }
        if (callId == null) {
            callId = "*";
        }
        String key = VideoManagerConstants.PLATFORM_SEND_RTP_INFO_PREFIX
                + userSetting.getServerId() + "_*_"
                + platformGbId + "_"
                + channelId + "_"
                + streamId + "_"
                + callId;
        List<Object> scan = RedisUtil.scan(redisTemplate, key);
        if (scan.size() > 0) {
            return (SendRtpItem)redisTemplate.opsForValue().get(scan.get(0));
        }else {
            return null;
        }
    }
}
