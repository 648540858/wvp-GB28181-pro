package com.genersoft.iot.vmp.jt1078.proc.request;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

/**
 * 终端通用应答
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:04
 * @email qingtaij@163.com
 */
@MsgId(id = "0001")
public class J0001 extends Re {
    int respNo;
    String respId;
    int result;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        respId = ByteBufUtil.hexDump(buf.readSlice(2));
        result = buf.readUnsignedByte();
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session) {
        SessionManager.INSTANCE.response(header.getDevId(), "0001", (long) respNo, JSON.toJSONString(this));
        return null;
    }

    public int getRespNo() {
        return respNo;
    }

    public String getRespId() {
        return respId;
    }

    public int getResult() {
        return result;
    }
}
