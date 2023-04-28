package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 实时消息上报
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0200")
public class J0200 extends Re {
    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session) {
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        j8001.setResult(J8001.SUCCESS);
        return j8001;
    }
}
