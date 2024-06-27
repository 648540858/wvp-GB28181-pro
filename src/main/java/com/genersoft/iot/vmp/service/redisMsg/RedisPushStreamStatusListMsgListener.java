package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.service.IMediaServerService;
import com.genersoft.iot.vmp.streamPush.bean.StreamPush;
import com.genersoft.iot.vmp.service.IGbStreamService;
import com.genersoft.iot.vmp.streamPush.service.IStreamPushService;
import com.genersoft.iot.vmp.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @Auther: JiangFeng
 * @Date: 2022/8/16 11:32
 * @Description: 接收redis发送的推流设备列表更新通知
 */
@Component
public class RedisPushStreamStatusListMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisPushStreamStatusListMsgListener.class);
    @Resource
    private IMediaServerService mediaServerService;

    @Resource
    private IStreamPushService streamPushService;
    @Resource
    private IGbStreamService gbStreamService;


    private ConcurrentLinkedQueue<Message> taskQueue = new ConcurrentLinkedQueue<>();

    @Qualifier("taskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.info("[REDIS消息-推流设备列表更新]： {}", new String(message.getBody()));
        boolean isEmpty = taskQueue.isEmpty();
        taskQueue.offer(message);
        if (isEmpty) {
            taskExecutor.execute(() -> {
                while (!taskQueue.isEmpty()) {
                    Message msg = taskQueue.poll();
                    try {
                        List<StreamPush> streamPushItems = JSON.parseArray(new String(msg.getBody()), StreamPush.class);
                        //查询全部的app+stream 用于判断是添加还是修改
                        Map<String, StreamPush> allAppAndStream = streamPushService.getAllAppAndStreamMap();
                        Map<String, StreamPush> allGBId = streamPushService.getAllGBId();

                        /**
                         * 用于存储更具APP+Stream过滤后的数据，可以直接存入stream_push表与gb_stream表
                         */
                        List<StreamPush> streamPushItemForSave = new ArrayList<>();
                        List<StreamPush> streamPushItemForUpdate = new ArrayList<>();
                        for (StreamPush streamPush : streamPushItems) {
                            String app = streamPush.getApp();
                            String stream = streamPush.getStream();
                            boolean contains = allAppAndStream.containsKey(app + stream);
                            //不存在就添加
                            if (!contains) {
                                if (allGBId.containsKey(streamPush.getGbDeviceId())) {
                                    StreamPush streamPushInDb = allGBId.get(streamPush.getGbDeviceId());
                                    logger.warn("[REDIS消息-推流设备列表更新-INSERT] 国标编号重复: {}, 已分配给{}/{}",
                                            streamPushInDb.getGbDeviceId(), streamPushInDb.getApp(), streamPushInDb.getStream());
                                    continue;
                                }
                                streamPush.setCreateTime(DateUtil.getNow());
                                streamPush.setMediaServerId(mediaServerService.getDefaultMediaServer().getId());
                                streamPushItemForSave.add(streamPush);
                                allGBId.put(streamPush.getGbDeviceId(), streamPush);
                            } else {
                                if (allGBId.containsKey(streamPush.getGbDeviceId())
                                        && (!allGBId.get(streamPush.getGbDeviceId()).getApp().equals(streamPush.getApp())
                                        || !allGBId.get(streamPush.getGbDeviceId()).getStream().equals(streamPush.getStream()))) {
                                    StreamPush streamPushInDb = allGBId.get(streamPush.getGbDeviceId());
                                    logger.warn("[REDIS消息-推流设备列表更新-UPDATE] 国标编号重复: {}, 已分配给{}/{}",
                                            streamPush.getGbDeviceId(), streamPushInDb.getApp(), streamPushInDb.getStream());
                                    continue;
                                }
                                //存在就只修改 name和gbId
                                streamPushItemForUpdate.add(streamPush);
                            }
                        }
                        if (!streamPushItemForSave.isEmpty()) {
                            logger.info("添加{}条",streamPushItemForSave.size());
                            logger.info(JSONObject.toJSONString(streamPushItemForSave));
                            streamPushService.batchAdd(streamPushItemForSave);

                        }
                        if(!streamPushItemForUpdate.isEmpty()){
                            logger.info("修改{}条",streamPushItemForUpdate.size());
                            logger.info(JSONObject.toJSONString(streamPushItemForUpdate));
                            gbStreamService.updateGbIdOrName(streamPushItemForUpdate);
                        }
                    }catch (Exception e) {
                        logger.warn("[REDIS消息-推流设备列表更新] 发现未处理的异常, \r\n{}", new String(message.getBody()));
                        logger.error("[REDIS消息-推流设备列表更新] 异常内容： ", e);
                    }
                }
            });
        }
    }
}
