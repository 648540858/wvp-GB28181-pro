package com.genersoft.iot.vmp.gb28181.event.record;

import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: 录像查询结束事件
 * @author: pan
 * @data: 2022-02-23
 */

@Component
public class RecordEndEventListener implements ApplicationListener<RecordEndEvent> {

    private final static Logger logger = LoggerFactory.getLogger(RecordEndEventListener.class);

    private Map<String, RecordEndEventHandler> handlerMap = new ConcurrentHashMap<>();
    public interface RecordEndEventHandler{
        void  handler(RecordInfo recordInfo);
    }

    @Override
    public void onApplicationEvent(RecordEndEvent event) {
        String deviceId = event.getRecordInfo().getDeviceId();
        String channelId = event.getRecordInfo().getChannelId();
        int count = event.getRecordInfo().getCount();
        int sumNum = event.getRecordInfo().getSumNum();
        logger.info("录像查询完成事件触发，deviceId：{}, channelId: {}, 录像数量{}/{}条", event.getRecordInfo().getDeviceId(),
                event.getRecordInfo().getChannelId(), count,sumNum);
        if (!handlerMap.isEmpty()) {
            RecordEndEventHandler handler = handlerMap.get(deviceId + channelId);
            if (handler !=null){
                handler.handler(event.getRecordInfo());
                if (count ==sumNum){
                    handlerMap.remove(deviceId + channelId);
                }
            }
        }else {
            logger.info("录像查询完成事件触发, 但是订阅为空，取消发送，deviceId：{}, channelId: {}",
                    event.getRecordInfo().getDeviceId(), event.getRecordInfo().getChannelId());
        }
    }

    /**
     * 添加
     * @param device
     * @param channelId
     * @param recordEndEventHandler
     */
    public void addEndEventHandler(String device, String channelId, RecordEndEventHandler recordEndEventHandler) {
        logger.info("录像查询事件添加监听，deviceId：{}, channelId: {}", device, channelId);
        handlerMap.put(device + channelId, recordEndEventHandler);
    }
    /**
     * 添加
     * @param device
     * @param channelId
     */
    public void delEndEventHandler(String device, String channelId) {
        logger.info("录像查询事件移除监听，deviceId：{}, channelId: {}", device, channelId);
        handlerMap.remove(device + channelId);
    }

}
