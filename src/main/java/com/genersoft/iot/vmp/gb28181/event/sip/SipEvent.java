package com.genersoft.iot.vmp.gb28181.event.sip;

import com.genersoft.iot.vmp.gb28181.event.SipSubscribe;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Data
public class SipEvent implements Delayed {

    private String key;

    /**
     * 成功的回调
     */
    private SipSubscribe.Event okEvent;

    /**
     * 错误的回调,包括超时
     */
    private SipSubscribe.Event errorEvent;

    /**
     * 超时时间(单位： 毫秒)
     */
    private long delay;

    public static SipEvent getInstance(String key, SipSubscribe.Event okEvent, SipSubscribe.Event errorEvent, long delay) {
        SipEvent sipEvent = new SipEvent();
        sipEvent.setKey(key);
        sipEvent.setOkEvent(okEvent);
        sipEvent.setErrorEvent(errorEvent);
        sipEvent.setDelay(System.currentTimeMillis() + delay);
        return sipEvent;
    }

    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return unit.convert(delay - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return (int) (this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS));
    }
}
