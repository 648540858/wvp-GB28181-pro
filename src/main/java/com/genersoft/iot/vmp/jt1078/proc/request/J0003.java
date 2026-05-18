package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.event.DeviceUpdateEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

/**
 * 终端注销
 */
@Slf4j
@Getter
@MsgId(id = "0003")
public class J0003 extends Re {

    private JTDevice deviceForUpdate;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        log.info("[JT-注销] 设备： {}", header.getPhoneNumber());
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
//        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0001", (long) respNo, result);
        JTDevice device = service.getDevice(header.getPhoneNumber());
        if (device != null && device.isStatus()) {
            deviceForUpdate = device;
            deviceForUpdate.setStatus(false);
            service.updateDevice(device);
        }
        return null;
    }

    @Override
    public ApplicationEvent getEvent() {
        DeviceUpdateEvent registerEvent = new DeviceUpdateEvent(this);
        registerEvent.setDevice(deviceForUpdate);
        return registerEvent;
    }
}
