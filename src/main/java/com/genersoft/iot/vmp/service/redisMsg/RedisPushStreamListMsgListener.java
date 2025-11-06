package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.streamPush.bean.RedisPushStreamMessage;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Auther: JiangFeng
 * @Date: 2022/8/16 11:32
 * @Description: 接收redis发送的推流设备列表更新通知
 * 监听：  SUBSCRIBE VM_MSG_PUSH_STREAM_LIST_CHANGE
 * 发布 PUBLISH VM_MSG_PUSH_STREAM_LIST_CHANGE '[{"app":1000,"stream":10000000,"gbId":"12345678901234567890","name":"A6","status":false},{"app":1000,"stream":10000021,"gbId":"24212345671381000021","name":"终端9273","status":false},{"app":1000,"stream":10000022,"gbId":"24212345671381000022","name":"终端9434","status":true},{"app":1000,"stream":10000025,"gbId":"24212345671381000025","name":"华为M10","status":false},{"app":1000,"stream":10000051,"gbId":"11111111111381111122","name":"终端9720","status":false}]'
 */
@Slf4j
@Component
public class RedisPushStreamListMsgListener implements MessageListener {

    @Resource
    private IMediaServerService mediaServerService;

    @Resource
    private IStreamPushService streamPushService;

    private final ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message message, byte[] bytes) {
        log.info("[REDIS: 推流设备列表更新]： {}", new String(message.getBody()));
        taskQueue.offer(message);
    }

    @Scheduled(fixedDelay = 100)
    public void executeTaskQueue() {
        if (taskQueue.isEmpty()) {
            return;
        }
        List<Message> messageDataList = new ArrayList<>();
        int size = taskQueue.size();
        for (int i = 0; i < size; i++) {
            Message msg = taskQueue.poll();
            if (msg != null) {
                messageDataList.add(msg);
            }
        }
        if (messageDataList.isEmpty()) {
            return;
        }
        for (Message msg : messageDataList) {
            try {
                List<RedisPushStreamMessage> streamPushItems = JSON.parseArray(new String(msg.getBody()), RedisPushStreamMessage.class);
                //查询全部的app+stream 用于判断是添加还是修改
                Map<String, StreamPush> allAppAndStream = streamPushService.getAllAppAndStreamMap();
                Map<String, StreamPush> allGBId = streamPushService.getAllGBId();

                // 用于存储更具APP+Stream过滤后的数据，可以直接存入stream_push表与gb_stream表
                List<StreamPush> streamPushItemForSave = new ArrayList<>();
                List<StreamPush> streamPushItemForUpdate = new ArrayList<>();
                for (RedisPushStreamMessage pushStreamMessage : streamPushItems) {
                    String app = pushStreamMessage.getApp();
                    String stream = pushStreamMessage.getStream();
                    boolean contains = allAppAndStream.containsKey(app + stream);
                    //不存在就添加
                    if (!contains) {
                        if (allGBId.containsKey(pushStreamMessage.getGbId())) {
                            StreamPush streamPushInDb = allGBId.get(pushStreamMessage.getGbId());
                            log.warn("[REDIS消息-推流设备列表更新-INSERT] 国标编号重复: {}, 已分配给{}/{}",
                                    streamPushInDb.getGbDeviceId(), streamPushInDb.getApp(), streamPushInDb.getStream());
                            continue;
                        }
                        StreamPush streamPush = pushStreamMessage.buildstreamPush();
                        streamPush.setCreateTime(DateUtil.getNow());
                        streamPush.setUpdateTime(DateUtil.getNow());
                        streamPush.setMediaServerId(mediaServerService.getDefaultMediaServer().getId());
                        streamPushItemForSave.add(streamPush);
                        allGBId.put(streamPush.getGbDeviceId(), streamPush);
                    } else {
                        StreamPush streamPushForGbDeviceId = allGBId.get(pushStreamMessage.getGbId());
                        if (streamPushForGbDeviceId != null
                                && (!streamPushForGbDeviceId.getApp().equals(pushStreamMessage.getApp())
                                || !streamPushForGbDeviceId.getStream().equals(pushStreamMessage.getStream()))) {
                            StreamPush streamPushInDb = allGBId.get(pushStreamMessage.getGbId());
                            log.warn("[REDIS消息-推流设备列表更新-UPDATE] 国标编号重复: {}, 已分配给{}/{}",
                                    pushStreamMessage.getGbId(), streamPushInDb.getApp(), streamPushInDb.getStream());
                            continue;
                        }
                        StreamPush streamPush = allAppAndStream.get(app + stream);
                        streamPush.setUpdateTime(DateUtil.getNow());
                        streamPush.setGbDeviceId(pushStreamMessage.getGbId());
                        streamPush.setGbName(pushStreamMessage.getName());
                        if (pushStreamMessage.getStatus() != null) {
                            streamPush.setGbStatus(pushStreamMessage.getStatus() ? "ON" : "OFF");
                        }
                        //存在就只修改 name和gbId
                        streamPushItemForUpdate.add(streamPush);
                    }
                }
                if (!streamPushItemForSave.isEmpty()) {
                    log.info("添加{}条", streamPushItemForSave.size());
                    log.info(JSONObject.toJSONString(streamPushItemForSave));
                    streamPushService.batchAdd(streamPushItemForSave);

                }
                if (!streamPushItemForUpdate.isEmpty()) {
                    log.info("修改{}条", streamPushItemForUpdate.size());
                    log.info(JSONObject.toJSONString(streamPushItemForUpdate));
                    streamPushService.batchUpdate(streamPushItemForUpdate);
                }
            } catch (Exception e) {
                log.warn("[REDIS消息-推流设备列表更新] 发现未处理的异常, \r\n{}", new String(msg.getBody()));
                log.error("[REDIS消息-推流设备列表更新] 异常内容： ", e);
            }
        }

    }
}
