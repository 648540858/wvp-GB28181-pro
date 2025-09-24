package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/**
 * 数据压缩上报
 */
@Setter
@Getter
@MsgId(id = "0901")
public class J0901 extends Re {

    /**
     * 平台 RSA公钥{e ,n}中的 e
     */
    private Long e;

    /**
     * RSA公钥{e ,n}中的 n
     */
    private byte[] n;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        e = buf.readUnsignedInt();
        byte[] content = new byte[buf.readableBytes()];
        buf.readBytes(content);
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
