package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.event.DeviceUpdateEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

import java.nio.charset.Charset;

/**
 * 终端鉴权
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@Slf4j
@MsgId(id = "0102")
public class J0102 extends Re {

    private String authenticationCode;
    private JTDevice deviceForUpdate;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        if (header.is2019Version()) {
            int lenCode = buf.readUnsignedByte();
            authenticationCode = buf.readCharSequence(lenCode, Charset.forName("GBK")).toString();
        }else {
            authenticationCode = buf.readCharSequence(buf.readableBytes(), Charset.forName("GBK")).toString();
        }
        log.info("设备鉴权： authenticationCode： " + authenticationCode);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        if (session.getAuthenticationCode() == null ||
                !session.getAuthenticationCode().equals(authenticationCode)) {
            j8001.setResult(J8001.FAIL);
        }else {
            j8001.setResult(J8001.SUCCESS);
            JTDevice device = service.getDevice(header.getPhoneNumber());
            if (device != null && !device.isStatus()) {
                deviceForUpdate = device;
                deviceForUpdate.setStatus(true);
                service.updateDevice(device);
            }
        }
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        DeviceUpdateEvent registerEvent = new DeviceUpdateEvent(this);
        registerEvent.setDevice(deviceForUpdate);
        return registerEvent;
    }

}
