package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.event.DeviceUpdateEvent;
import com.genersoft.iot.vmp.jt1078.event.JTPositionEvent;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

/**
 * 位置信息查询应答
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0201")
public class J0201 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0100.class);
    private JTPositionBaseInfo positionInfo;
    private String phoneNumber;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        phoneNumber = header.getPhoneNumber();
        int respNo = buf.readUnsignedShort();
        positionInfo = JTPositionBaseInfo.decode(buf);
        log.info("[JT-位置信息查询应答]: {}", positionInfo);
        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0201", (long) respNo, positionInfo);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        j8001.setResult(J8001.SUCCESS);
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        if (positionInfo == null || phoneNumber == null ) {
            return null;
        }
        JTPositionEvent registerEvent = new JTPositionEvent(this);
        registerEvent.setPhoneNumber(phoneNumber);
        registerEvent.setPositionInfo(positionInfo);
        return registerEvent;
    }
}
