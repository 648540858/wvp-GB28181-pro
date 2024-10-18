package com.genersoft.iot.vmp.gb28181.task.impl;

import com.genersoft.iot.vmp.common.CommonCallback;
import com.genersoft.iot.vmp.conf.DynamicTask;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.task.ISubscribeTask;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.ISIPCommander;
import gov.nist.javax.sip.message.SIPRequest;
import lombok.extern.slf4j.Slf4j;

import javax.sip.DialogState;
import javax.sip.InvalidArgumentException;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.header.ToHeader;
import java.text.ParseException;

/**
 * 目录订阅任务
 * @author lin
 */
@Slf4j
public class CatalogSubscribeTask implements ISubscribeTask {
    private final Device device;
    private final ISIPCommander sipCommander;
    private SIPRequest request;

    private final DynamicTask dynamicTask;

    private final String taskKey = "catalog-subscribe-timeout";


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
        SIPRequest sipRequest = null;
        try {
            sipRequest = sipCommander.catalogSubscribe(device, request, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                // 成功
                log.info("[目录订阅]成功： {}", device.getDeviceId());
                ToHeader toHeader = (ToHeader)event.getResponse().getHeader(ToHeader.NAME);
                try {
                    this.request.getToHeader().setTag(toHeader.getTag());
                } catch (ParseException e) {
                    log.info("[目录订阅]成功： 但为request设置ToTag失败");
                    this.request = null;
                }
            },eventResult -> {
                this.request = null;
                // 失败
                log.warn("[目录订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
                dynamicTask.startDelay(taskKey, CatalogSubscribeTask.this, 2000);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 目录订阅: {}", e.getMessage());

        }
        if (sipRequest != null) {
            this.request = sipRequest;
        }
    }

    @Override
    public void stop(CommonCallback<Boolean> callback) {
        /**
         * dialog 的各个状态
         * EARLY-> Early state状态-初始请求发送以后，收到了一个临时响应消息
         * CONFIRMED-> Confirmed Dialog状态-已确认
         * COMPLETED-> Completed Dialog状态-已完成
         * TERMINATED-> Terminated Dialog状态-终止
         */
        log.info("取消目录订阅时dialog状态为{}", DialogState.CONFIRMED);
        if (dynamicTask.get(taskKey) != null) {
            dynamicTask.stop(taskKey);
        }
        device.setSubscribeCycleForCatalog(0);
        try {
            sipCommander.catalogSubscribe(device, request, eventResult -> {
                ResponseEvent event = (ResponseEvent) eventResult.event;
                if (event.getResponse().getRawContent() != null) {
                    // 成功
                    log.info("[取消目录订阅]成功： {}", device.getDeviceId());
                }else {
                    // 成功
                    log.info("[取消目录订阅]成功： {}", device.getDeviceId());
                }
                if (callback != null) {
                    callback.run(event.getResponse().getRawContent() != null);
                }
            },eventResult -> {
                // 失败
                log.warn("[取消目录订阅]失败，信令发送失败： {}-{} ", device.getDeviceId(), eventResult.msg);
            });
        } catch (InvalidArgumentException | SipException | ParseException e) {
            log.error("[命令发送失败] 取消目录订阅: {}", e.getMessage());
        }
    }
}
