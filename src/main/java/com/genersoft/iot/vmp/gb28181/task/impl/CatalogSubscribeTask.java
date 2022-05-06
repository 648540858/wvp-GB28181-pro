package com.genersoft.iot.vmp.gb28181.task.impl;

import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.ResponseEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 目录订阅任务
 * @author lin
 */
public class CatalogSubscribeTask implements ISubscribeTask {
    private final Logger logger = LoggerFactory.getLogger(CatalogSubscribeTask.class);
    private Device device;
    private final ISIPCommander sipCommander;
    private Dialog dialog;

    private DynamicTask dynamicTask;

    private String taskKey = "catalog-subscribe-timeout";


    public CatalogSubscribeTask(Device device, ISIPCommander sipCommander, DynamicTask dynamicTask) {
        this.device = device;
        this.sipCommander = sipCommander;
        this.dynamicTask = dynamicTask;
    }

    @Override
    public void run() {
        if (dynamicTask.get(taskKey) != null) {
            dynamicTask.stop(taskKey);
        }
        sipCommander.catalogSubscribe(device, dialog, eventResult -> {
            if (eventResult.dialog != null || eventResult.dialog.getState().equals(DialogState.CONFIRMED)) {
                dialog = eventResult.dialog;
            }
            ResponseEvent event = (ResponseEvent) eventResult.event;
            if (event.getResponse().getRawContent() != null) {
                // 成功
                logger.info("[目录订阅]成功： {}", device.getDeviceId());
            }else {
                // 成功
                logger.info("[目录订阅]成功： {}", device.getDeviceId());
            }
        },eventResult -> {
            dialog = null;
            // 失败
            logger.warn("[目录订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
            dynamicTask.startDelay(taskKey, CatalogSubscribeTask.this, 2000);
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
        logger.info("取消目录订阅时dialog状态为{}", DialogState.CONFIRMED);
        if (dynamicTask.get(taskKey) != null) {
            dynamicTask.stop(taskKey);
        }
        if (dialog != null && dialog.getState().equals(DialogState.CONFIRMED)) {
            device.setSubscribeCycleForCatalog(0);
            sipCommander.catalogSubscribe(device, dialog, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                if (event.getResponse().getRawContent() != null) {
                    // 成功
                    logger.info("[取消目录订阅订阅]成功： {}", device.getDeviceId());
                }else {
                    // 成功
                    logger.info("[取消目录订阅订阅]成功： {}", device.getDeviceId());
                }
            },eventResult -> {
                // 失败
                logger.warn("[取消目录订阅订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        }
    }

    @Override
    public DialogState getDialogState() {
        if (dialog == null) {
            return null;
        }
        return dialog.getState();
    }
}
