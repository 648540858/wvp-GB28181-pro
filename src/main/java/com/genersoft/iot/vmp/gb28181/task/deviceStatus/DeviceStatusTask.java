package com.genersoft.iot.vmp.gb28181.task.deviceStatus;

import com.genersoft.iot.vmp.common.DeviceStatusCallback;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Slf4j
@Data
public class DeviceStatusTask implements Delayed {

    private String deviceId;

    private SipTransactionInfo transactionInfo;

    /**
     * 超时时间(单位： 毫秒)
     */
    private long delayTime;

    private DeviceStatusCallback callback;

    public static DeviceStatusTask getInstance(String deviceId, SipTransactionInfo transactionInfo, long delayTime, DeviceStatusCallback callback) {
        DeviceStatusTask deviceStatusTask = new DeviceStatusTask();
        deviceStatusTask.setDeviceId(deviceId);
        deviceStatusTask.setTransactionInfo(transactionInfo);
        deviceStatusTask.setDelayTime((delayTime * 1000L - 500L) + System.currentTimeMillis());
        deviceStatusTask.setCallback(callback);
        return deviceStatusTask;
    }

    public void expired() {
        if (callback == null) {
            log.info("[设备离线] 未找到过期处理回调， {}", deviceId);
            return;
        }
        callback.run(deviceId, transactionInfo);
    }

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
