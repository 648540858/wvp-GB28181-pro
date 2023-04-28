package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;

/**
 * 查询服务器时间
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0004")
public class J0004 extends Re {
    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session) {
        return null;
    }
}
