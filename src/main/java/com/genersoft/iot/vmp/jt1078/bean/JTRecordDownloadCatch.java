package com.genersoft.iot.vmp.jt1078.bean;

import com.genersoft.iot.vmp.jt1078.proc.response.J9206;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class JTRecordDownloadCatch implements Delayed {

    @Getter
    @Setter
    private String phoneNumber;

    @Getter
    @Setter
    private String path;

    @Getter
    @Setter
    private J9206 j9206;

    /**
     * 超时时间(单位： 毫秒)
     */
    @Getter
    @Setter
    private long delayTime;

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
