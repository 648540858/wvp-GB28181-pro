package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTMediaAttribute;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import org.springframework.context.ApplicationEvent;

/**
 * 终端上传音视频属性
 *
 */
@MsgId(id = "1003")
public class J1003 extends Re {

    JTMediaAttribute mediaAttribute;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        mediaAttribute = JTMediaAttribute.decode(buf);
        SessionManager.INSTANCE.response(header.getPhoneNumber(), "1003", null, mediaAttribute);
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
        return null;
    }
}
