package com.genersoft.iot.vmp.gb28181.task.platformStatus;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 平台注册任务
 */
@Slf4j
public class PlatformRegisterTask implements Delayed {

    @Getter
    private String platformServerId;

    /**
     * 超时时间(单位： 毫秒)
     */
    @Getter
    @Setter
    private long delayTime;

    @Getter
    private SipTransactionInfo sipTransactionInfo;

    /**
     * 到期回调
     */
    @Getter
    private CommonCallback<String> callback;


    public PlatformRegisterTask(String platformServerId, long delayTime, SipTransactionInfo sipTransactionInfo, CommonCallback<String> callback) {
        this.platformServerId = platformServerId;
        this.delayTime = delayTime;
        this.callback = callback;
        this.sipTransactionInfo = sipTransactionInfo;
    }

    public void expired() {
        if (callback == null) {
            log.info("[平台注册到期] 未找到到期处理回调， 平台上级编号： {}", platformServerId);
            return;
        }
        getCallback().run(platformServerId);
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public PlatformRegisterTaskInfo getInfo() {
        PlatformRegisterTaskInfo taskInfo = new PlatformRegisterTaskInfo();
        taskInfo.setPlatformServerId(platformServerId);
        taskInfo.setSipTransactionInfo(sipTransactionInfo);
        return taskInfo;
    }
}
