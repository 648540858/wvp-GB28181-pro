package com.genersoft.iot.vmp.gb28181.task.impl;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ResponseEvent;

/**
 * 移动位置订阅的定时更新
 */
public class MobilePositionSubscribeTask implements ISubscribeTask {
    private final Logger logger = LoggerFactory.getLogger(MobilePositionSubscribeTask.class);
    private  Device device;
    private  ISIPCommander sipCommander;
    private Dialog dialog;

    public MobilePositionSubscribeTask(Device device, ISIPCommander sipCommander) {
        this.device = device;
        this.sipCommander = sipCommander;
    }

    @Async
    @Override
    public void run() {
        sipCommander.mobilePositionSubscribe(device, dialog, eventResult -> {
            if (eventResult.dialog != null || eventResult.dialog.getState().equals(DialogState.CONFIRMED)) {
                dialog = eventResult.dialog;
            }
            ResponseEvent event = (ResponseEvent) eventResult.event;
            if (event.getResponse().getRawContent() != null) {
                // 成功
                logger.info("[移动位置订阅]成功： {}", device.getDeviceId());
            }else {
                // 成功
                logger.info("[移动位置订阅]成功： {}", device.getDeviceId());
            }
        },eventResult -> {
            dialog = null;
            // 失败
            logger.warn("[移动位置订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
        });

    }

    @Override
    public void stop() {
        /**
         * dialog 的各个状态
         * EARLY-> Early state状态-初始请求发送以后，收到了一个临时响应消息
         * CONFIRMED-> Confirmed Dialog状态-已确认
         * COMPLETED-> Completed Dialog状态-已完成
         * TERMINATED-> Terminated Dialog状态-终止
         */
        if (dialog != null && dialog.getState().equals(DialogState.CONFIRMED)) {
            logger.info("取消移动订阅时dialog状态为{}", dialog.getState());
            device.setSubscribeCycleForMobilePosition(0);
            sipCommander.mobilePositionSubscribe(device, dialog, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                if (event.getResponse().getRawContent() != null) {
                    // 成功
                    logger.info("[取消移动位置订阅]成功： {}", device.getDeviceId());
                }else {
                    // 成功
                    logger.info("[取消移动位置订阅]成功： {}", device.getDeviceId());
                }
            },eventResult -> {
                // 失败
                logger.warn("[取消移动位置订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        }
    }
    @Override
    public DialogState getDialogState() {
        if (dialog == null) return null;
        return dialog.getState();
    }
}
