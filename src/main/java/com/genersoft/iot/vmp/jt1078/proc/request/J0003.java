package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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

    int respNo;
    String respId;
    int result;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        respId = ByteBufUtil.hexDump(buf.readSlice(2));
        result = buf.readUnsignedByte();
        log.info("[JT-注销] 设备： {}", header.getPhoneNumber());
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0001", (long) respNo, result);
        return null;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
