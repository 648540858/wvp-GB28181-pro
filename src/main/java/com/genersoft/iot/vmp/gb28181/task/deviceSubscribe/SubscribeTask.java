package com.genersoft.iot.vmp.gb28181.task.deviceSubscribe;

import com.genersoft.iot.vmp.common.SubscribeCallback;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public abstract class SubscribeTask implements Delayed {

    private String deviceId;

    private SubscribeCallback callback;

    private SipTransactionInfo transactionInfo;

    /**
     * 超时时间(单位： 毫秒)
     */
    private long delayTime;

    public abstract void expired();

    public abstract String getKey();

    public abstract String getName();

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public SubscribeTaskInfo getInfo(){
        SubscribeTaskInfo subscribeTaskInfo = new SubscribeTaskInfo();
        subscribeTaskInfo.setName(getName());
        subscribeTaskInfo.setTransactionInfo(transactionInfo);
        subscribeTaskInfo.setDeviceId(deviceId);
        subscribeTaskInfo.setKey(getKey());
        return subscribeTaskInfo;
    }
}
