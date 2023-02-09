package com.genersoft.iot.vmp.gb28181.session;

import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.record.RecordEndEventListener;
import com.genersoft.iot.vmp.gb28181.transmit.callback.DeferredResultHolder;
import com.genersoft.iot.vmp.gb28181.transmit.callback.RequestMessage;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author lin
 */
@Component
public class RecordDataCatch {

    public static Map<String, RecordInfo> data = new ConcurrentHashMap<>();

    @Autowired
    private DeferredResultHolder deferredResultHolder;
    @Autowired
    private RecordEndEventListener recordEndEventListener;


    public int put(String deviceId,String channelId, String sn, int sumNum, List<RecordItem> recordItems) {
        String key = deviceId + sn;
        RecordInfo recordInfo = data.get(key);
        if (recordInfo == null) {
            recordInfo = new RecordInfo();
            recordInfo.setDeviceId(deviceId);
            recordInfo.setChannelId(channelId);
            recordInfo.setSn(sn.trim());
            recordInfo.setSumNum(sumNum);
            recordInfo.setRecordList(Collections.synchronizedList(new ArrayList<>()));
            recordInfo.setLastTime(Instant.now());
            recordInfo.getRecordList().addAll(recordItems);
            data.put(key, recordInfo);
        }else {
            // 同一个设备的通道同步请求只考虑一个，其他的直接忽略
            if (!Objects.equals(sn.trim(), recordInfo.getSn())) {
                return 0;
            }
            recordInfo.getRecordList().addAll(recordItems);
            recordInfo.setLastTime(Instant.now());
        }
        return recordInfo.getRecordList().size();
    }

    @Scheduled(fixedRate = 5 * 1000)   //每5秒执行一次, 发现数据5秒未更新则移除数据并认为数据接收超时
    private void timerTask(){
        Set<String> keys = data.keySet();
        // 获取五秒前的时刻
        Instant instantBefore5S = Instant.now().minusMillis(TimeUnit.SECONDS.toMillis(5));
        for (String key : keys) {
            RecordInfo recordInfo = data.get(key);
            // 超过五秒收不到消息任务超时， 只更新这一部分数据
            if ( recordInfo.getLastTime().isBefore(instantBefore5S)) {
                // 处理录像数据， 返回给前端
                String msgKey = DeferredResultHolder.CALLBACK_CMD_RECORDINFO + recordInfo.getDeviceId() + recordInfo.getSn();

                // 对数据进行排序
                Collections.sort(recordInfo.getRecordList());

                RequestMessage msg = new RequestMessage();
                msg.setKey(msgKey);
                msg.setData(recordInfo);
                deferredResultHolder.invokeAllResult(msg);
                recordEndEventListener.delEndEventHandler(recordInfo.getDeviceId(),recordInfo.getChannelId());
                data.remove(key);
            }
        }
    }

    public boolean isComplete(String deviceId, String sn) {
        RecordInfo recordInfo = data.get(deviceId + sn);
        return recordInfo != null && recordInfo.getRecordList().size() == recordInfo.getSumNum();
    }

    public RecordInfo getRecordInfo(String deviceId, String sn) {
        return data.get(deviceId + sn);
    }

    public void remove(String deviceId, String sn) {
        data.remove(deviceId + sn);
    }
}
