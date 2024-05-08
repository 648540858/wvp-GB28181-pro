package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.bean.JTDriverInformation;
import com.genersoft.iot.vmp.jt1078.bean.JTPositionBaseInfo;
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
 * 驾驶员身份信息采集上报
 *
 */
@MsgId(id = "0702")
public class J0702 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0100.class);
    private JTDriverInformation driverInformation;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        driverInformation = JTDriverInformation.decode(buf);
        log.info("[JT-驾驶员身份信息采集上报]: {}", driverInformation.toString());
        SessionManager.INSTANCE.response(header.getTerminalId(), "0702", null, driverInformation);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
