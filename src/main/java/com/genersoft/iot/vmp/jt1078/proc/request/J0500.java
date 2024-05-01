package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

/**
 * 车辆控制应答
 *
 */
@MsgId(id = "0500")
public class J0500 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0100.class);
    private JTPositionBaseInfo positionInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        int respNo = buf.readUnsignedShort();
        positionInfo = J0200.getPositionInfo(buf);
        log.info("[JT-车辆控制应答]: {}", positionInfo.toString());
        SessionManager.INSTANCE.response(header.getTerminalId(), "0500", (long) respNo, positionInfo);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        JTDevice deviceInDb = service.getDevice(header.getTerminalId());
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        if (deviceInDb == null) {
            j8001.setResult(J8001.FAIL);
        }else {
            // TODO 优化为发送异步事件，定时读取队列写入数据库
            deviceInDb.setLongitude(positionInfo.getLongitude());
            deviceInDb.setLatitude(positionInfo.getLatitude());
            service.updateDevice(deviceInDb);
            j8001.setResult(J8001.SUCCESS);
        }
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
