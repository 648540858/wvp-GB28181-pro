package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTMediaEventInfo;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 多媒体数据上传
 *
 */
@MsgId(id = "0801")
public class J0801 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0801.class);

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        JTMediaEventInfo mediaEventInfo = JTMediaEventInfo.decode(buf);
        log.info("[JT-多媒体数据上传]: {}", mediaEventInfo);
        File file = new File("/home/lin/" + header.getSn() + ".jpg");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(mediaEventInfo.getMediaData());
            fileOutputStream.flush();
            fileOutputStream.close();
        }catch (Exception e) {

        }

        SessionManager.INSTANCE.response(header.getPhoneNumber(), "0801", null, mediaEventInfo);
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
