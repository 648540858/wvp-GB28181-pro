package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDevice;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import org.springframework.context.ApplicationEvent;

import java.nio.charset.Charset;

/**
 * 查询终端参数应答
 *
 */
@MsgId(id = "0104")
public class J0104 extends Re {

    Integer respNo;
    Integer paramLength;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        paramLength = (int)buf.readUnsignedByte();
        System.out.println(respNo);
        System.out.println(paramLength);

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
