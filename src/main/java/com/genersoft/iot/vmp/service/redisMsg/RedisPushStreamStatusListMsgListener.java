package com.genersoft.iot.vmp.service.redisMsg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.StreamPushItem;
import com.genersoft.iot.vmp.service.IGbStreamService;
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
                        List<StreamPushItem> streamPushItems = JSON.parseArray(new String(msg.getBody()), StreamPushItem.class);
                        //查询全部的app+stream 用于判断是添加还是修改
                        List<String> allAppAndStream = streamPushService.getAllAppAndStream();

                        /**
                         * 用于存储更具APP+Stream过滤后的数据，可以直接存入stream_push表与gb_stream表
                         */
                        List<StreamPushItem> streamPushItemForSave = new ArrayList<>();
                        List<StreamPushItem> streamPushItemForUpdate = new ArrayList<>();
                        for (StreamPushItem streamPushItem : streamPushItems) {
                            String app = streamPushItem.getApp();
                            String stream = streamPushItem.getStream();
                            boolean contains = allAppAndStream.contains(app + stream);
                            //不存在就添加
                            if (!contains) {
                                streamPushItem.setStreamType("push");
                                streamPushItem.setCreateTime(DateUtil.getNow());
                                streamPushItem.setMediaServerId(mediaServerService.getDefaultMediaServer().getId());
                                streamPushItem.setOriginType(2);
                                streamPushItem.setOriginTypeStr("rtsp_push");
                                streamPushItem.setTotalReaderCount("0");
                                streamPushItemForSave.add(streamPushItem);
                            } else {
                                //存在就只修改 name和gbId
                                streamPushItemForUpdate.add(streamPushItem);
                            }
                        }
                        if (streamPushItemForSave.size() > 0) {

                            logger.info("添加{}条",streamPushItemForSave.size());
                            logger.info(JSONObject.toJSONString(streamPushItemForSave));
                            streamPushService.batchAdd(streamPushItemForSave);

                        }
                        if(streamPushItemForUpdate.size()>0){
                            logger.info("修改{}条",streamPushItemForUpdate.size());
                            logger.info(JSONObject.toJSONString(streamPushItemForUpdate));
                            gbStreamService.updateGbIdOrName(streamPushItemForUpdate);
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
