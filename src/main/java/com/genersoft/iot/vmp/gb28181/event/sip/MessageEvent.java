package com.genersoft.iot.vmp.gb28181.event.sip;

import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class MessageEvent<T> implements Delayed {
    /**
     * 超时时间(单位： 毫秒)
     */
    private long delay;

    private String cmdType;

    private String sn;

    private String deviceId;

    private String result;

    private T t;

    private ErrorCallback<T> callback;

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }

    public String getKey(){
        return cmdType + sn;
    }

    public static <T> MessageEvent<T> getInstance(String cmdType, String sn, String deviceId, Long delay, ErrorCallback<T> callback){
        MessageEvent<T> messageEvent = new MessageEvent<>();
        messageEvent.cmdType = cmdType;
        messageEvent.sn = sn;
        messageEvent.deviceId = deviceId;
        messageEvent.callback = callback;
        if (delay == null) {
            messageEvent.delay = System.currentTimeMillis() + 1000;
        }else {
            messageEvent.delay = System.currentTimeMillis() + delay;
        }
        return messageEvent;
    }
}
