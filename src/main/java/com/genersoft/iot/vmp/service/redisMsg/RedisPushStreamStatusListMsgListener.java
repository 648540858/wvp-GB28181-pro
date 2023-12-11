package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPush;
import com.genersoft.iot.vmp.service.IMediaServerService;
import com.genersoft.iot.vmp.service.IStreamPushService;
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
                        List<StreamPush> streamPushList = JSON.parseArray(new String(msg.getBody()), StreamPush.class);
                        //查询全部的app+stream 用于判断是添加还是修改
                        Map<String, StreamPush> allAppAndStream = streamPushService.getAllAppAndStream();

                        /**
                         * 用于存储更具APP+Stream过滤后的数据，可以直接存入stream_push表与gb_stream表
                         */
                        List<StreamPush> streamPushItemForSave = new ArrayList<>();
                        List<StreamPush> streamPushItemForUpdate = new ArrayList<>();
                        for (StreamPush streamPush : streamPushList) {

                            streamPush.setUpdateTime(DateUtil.getNow());
                            //不存在就添加
                            if (!allAppAndStream.containsKey(streamPush.getApp() + streamPush.getStream())) {
                                streamPush.setCreateTime(DateUtil.getNow());
                                streamPush.setMediaServerId(mediaServerService.getDefaultMediaServer().getId());
                                streamPushItemForSave.add(streamPush);
                            } else {
                                StreamPush streamPushBoInDB = allAppAndStream.get(streamPush.getApp() + streamPush.getStream());
                                // 涉及可以变化的内容为名称，国标Id， 状态
                                if (!streamPush.getName().equals(streamPushBoInDB.getName())
                                || !streamPush.getGbId().equals(streamPushBoInDB.getGbId())
                                || !streamPush.isStatus() == streamPushBoInDB.isStatus()) {
                                    streamPushBoInDB.setName(streamPush.getName());
                                    streamPushBoInDB.setGbId(streamPush.getGbId());
                                    streamPushBoInDB.setStatus(streamPush.isStatus());

                                    streamPushItemForUpdate.add(streamPushBoInDB);
                                }
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
                            streamPushService.batchUpdate(streamPushItemForUpdate);
                        }
                    }catch (Exception e) {
                        logger.warn("[REDIS消息-推流设备列表更新] 发现未处理的异常, \r\n{}", JSON.toJSONString(message));
                        logger.error("[REDIS消息-推流设备列表更新] 异常内容： ", e);
                    }
                }
            });
        }
    }
}
