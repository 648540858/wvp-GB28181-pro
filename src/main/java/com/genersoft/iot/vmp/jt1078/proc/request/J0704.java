package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 定位数据批量上传
 *
 */
@MsgId(id = "0704")
public class J0704 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0704.class);
    private List<JTPositionBaseInfo> positionBaseInfoList = new ArrayList<>();
    private int type;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        int length = buf.readUnsignedShort();
        type = buf.readUnsignedByte();
        for (int i = 0; i < length; i++) {
            int dateLength = buf.readUnsignedShort();
            ByteBuf byteBuf = buf.readBytes(dateLength);
            JTPositionBaseInfo positionInfo = JTPositionBaseInfo.decode(byteBuf);
            byteBuf.release();
            positionBaseInfoList.add(positionInfo);
        }
        log.info("[JT-定位数据批量上传]: 共{}条", positionBaseInfoList.size());
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
