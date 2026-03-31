package com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.impl;

import com.genersoft.iot.vmp.common.SubscribeCallback;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.SipTransactionInfo;
import com.genersoft.iot.vmp.gb28181.task.deviceSubscribe.SubscribeTask;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubscribeTaskForAlarm extends SubscribeTask {

    public static final String name = "alarm";

    public static SubscribeTask getInstance(Device device, SubscribeCallback callback, SipTransactionInfo transactionInfo) {
        if (device.getSubscribeCycleForAlarm() <= 0) {
            return null;
        }
        SubscribeTaskForAlarm subscribeTaskForAlarm = new SubscribeTaskForAlarm();
        subscribeTaskForAlarm.setDelayTime((device.getSubscribeCycleForAlarm() * 1000L - 500L) + System.currentTimeMillis());
        subscribeTaskForAlarm.setDeviceId(device.getDeviceId());
        subscribeTaskForAlarm.setCallback(callback);
        subscribeTaskForAlarm.setTransactionInfo(transactionInfo);
        return subscribeTaskForAlarm;
    }

    @Override
    public void expired() {
        if (super.getCallback() == null) {
            log.info("[设备订阅到期] 报警订阅 未找到到期处理回调， 编号： {}", getDeviceId());
            return;
        }
        getCallback().run(getDeviceId(), getTransactionInfo());
    }

    @Override
    public String getKey() {
        return String.format("%s_%s", name, getDeviceId());
    }

    @Override
    public String getName() {
        return name;
    }

    public static String getKey(Device device) {
        return String.format("%s_%s", SubscribeTaskForAlarm.name, device.getDeviceId());
    }
}
