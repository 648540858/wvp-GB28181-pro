package com.genersoft.iot.vmp.gb28181.transmit.callback;

import com.genersoft.iot.vmp.gb28181.bean.RecordInfo;
import com.genersoft.iot.vmp.gb28181.bean.RecordItem;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.response.cmd.RecordInfoResponseMessageHandler;
import com.genersoft.iot.vmp.utils.redis.RedisUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class CheckForAllRecordsThread extends Thread {

    private String key;

    private RecordInfo recordInfo;

    private RedisUtil redis;

    private Logger logger;

    private DeferredResultHolder deferredResultHolder;

    public CheckForAllRecordsThread(String key, RecordInfo recordInfo) {
        this.key = key;
        this.recordInfo = recordInfo;
    }

    @Override
    public void run() {

        String cacheKey = this.key;

        for (long stop = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); stop > System.nanoTime();) {
            List<Object> cacheKeys = redis.scan(cacheKey + "_*");
            List<RecordItem> totalRecordList = new ArrayList<RecordItem>();
            for (int i = 0; i < cacheKeys.size(); i++) {
                totalRecordList.addAll((List<RecordItem>) redis.get(cacheKeys.get(i).toString()));
            }
            if (totalRecordList.size() < this.recordInfo.getSumNum()) {
                logger.info("已获取" + totalRecordList.size() + "项录像数据，共" + this.recordInfo.getSumNum() + "项");
            } else {
                logger.info("录像数据已全部获取，共 {} 项", this.recordInfo.getSumNum());
                this.recordInfo.setRecordList(totalRecordList);
                for (int i = 0; i < cacheKeys.size(); i++) {
                    redis.del(cacheKeys.get(i).toString());
                }
                break;
            }
        }
        // 自然顺序排序, 元素进行升序排列
        this.recordInfo.getRecordList().sort(Comparator.naturalOrder());
        RequestMessage msg = new RequestMessage();
        msg.setKey(DeferredResultHolder.CALLBACK_CMD_RECORDINFO + recordInfo.getDeviceId() + recordInfo.getSn());
        msg.setData(recordInfo);
        deferredResultHolder.invokeAllResult(msg);
        logger.info("处理完成，返回结果");
        RecordInfoResponseMessageHandler.threadNameList.remove(cacheKey);
    }
    
	public void setRedis(RedisUtil redis) {
		this.redis = redis;
	}

	public void setDeferredResultHolder(DeferredResultHolder deferredResultHolder) {
		this.deferredResultHolder = deferredResultHolder;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

}
