package com.genersoft.iot.vmp.gb28181.task.deviceStatus;

import com.genersoft.iot.vmp.common.DeviceStatusCallback;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public abstract class DeviceStatusTask implements Delayed {

    private String deviceId;

    private DeviceStatusCallback callback;

    private SipTransactionInfo transactionInfo;

    /**
     * 超时时间(单位： 毫秒)
     */
    private long delayTime;

    public abstract void expired();

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public DeviceStatusTaskInfo getInfo(){
        DeviceStatusTaskInfo taskInfo = new DeviceStatusTaskInfo();
        taskInfo.setTransactionInfo(transactionInfo);
        taskInfo.setDeviceId(deviceId);
        return taskInfo;
    }
}
