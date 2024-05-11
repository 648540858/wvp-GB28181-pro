package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 摄像头立即拍摄命令应答
 */
@MsgId(id = "0805")
public class J0805 extends Re {

    private int respNo;
    /**
     * 0：成功/确认；1：失败；2：消息有误；3：不支持
     */
    private int result;

    /**
     * 表示拍摄成功的多媒体个数
     */
    private List<Long> ids;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        respNo = buf.readUnsignedShort();
        result = buf.readUnsignedByte();
        int length = buf.readUnsignedByte();
        for (int i = 0; i < length; i++) {
            ids.add(buf.readUnsignedInt());
        }
        SessionManager.INSTANCE.response(header.getTerminalId(), "0805", (long) respNo, ids);
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        SessionManager.INSTANCE.response(header.getTerminalId(), "0001", (long) respNo, result);
        return null;
    }

    public int getRespNo() {
        return respNo;
    }

    public void setRespNo(int respNo) {
        this.respNo = respNo;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
