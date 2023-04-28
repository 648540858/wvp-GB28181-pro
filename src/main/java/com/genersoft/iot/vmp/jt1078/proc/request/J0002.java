package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 终端心跳
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:04
 * @email qingtaij@163.com
 */
@MsgId(id = "0002")
public class J0002 extends Re {
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
