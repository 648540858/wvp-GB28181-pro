package com.genersoft.iot.vmp.jt1078.proc.request;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.springframework.context.ApplicationEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传完成通知
 *
 */
@MsgId(id = "1206")
public class J1206 extends Re {
    Integer respNo;

    // 0：成功； 1：失败
    private int result;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        result = buf.readUnsignedByte();
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


    public Integer getRespNo() {
        return respNo;
    }

    public void setRespNo(Integer respNo) {
        this.respNo = respNo;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }


    @Override
    public String toString() {
        return "J1206{" +
                "respNo=" + respNo +
                ", result=" + result +
                '}';
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
