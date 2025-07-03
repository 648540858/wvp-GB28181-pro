package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;
import lombok.Setter;
import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class MessageResponseTask<T> implements Delayed {

    @Getter
    @Setter
    private Element element;

    @Getter
    @Setter
    private List<T> data;

    @Getter
    @Setter
    private String key;


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
