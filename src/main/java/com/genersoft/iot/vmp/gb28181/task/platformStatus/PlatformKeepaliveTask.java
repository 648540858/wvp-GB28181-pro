package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.gb28181.bean.PlatformKeepaliveCallback;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 平台心跳任务
 */
@Slf4j
public class PlatformKeepaliveTask implements Delayed {

    @Getter
    private String platformServerId;

    /**
     * 超时时间(单位： 毫秒)
     */
    @Getter
    @Setter
    private long delayTime;

    /**
     * 到期回调
     */
    @Getter
    private PlatformKeepaliveCallback callback;

    /**
     * 心跳发送失败次数
     */
    @Getter
    @Setter
    private int failCount;

    public PlatformKeepaliveTask(String platformServerId, long delayTime, PlatformKeepaliveCallback callback) {
        this.platformServerId = platformServerId;
        this.delayTime = System.currentTimeMillis() + delayTime;
        this.callback = callback;
    }

    public void expired() {
        if (callback == null) {
            log.info("[平台心跳到期] 未找到到期处理回调， 平台上级编号： {}", platformServerId);
            return;
        }
        getCallback().run(platformServerId, failCount);
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
