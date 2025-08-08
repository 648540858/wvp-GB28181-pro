package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTMediaEventInfo;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.session.SessionManager;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEvent;

/**
 * 多媒体数据上传
 */
@Slf4j
@MsgId(id = "0801")
public class J0801 extends Re {

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        JTMediaEventInfo mediaEventInfo = JTMediaEventInfo.decode(buf);
        log.info("[JT-多媒体数据上传]: {}", mediaEventInfo);
//        try {
//            if (mediaEventInfo.getMediaData() != null) {
//                File file = new File("./source.jpg");
//                if (file.exists()) {
//                    file.delete();
//                }
//                FileOutputStream fileOutputStream = new FileOutputStream(file);
//                fileOutputStream.write(mediaEventInfo.getMediaData());
//                fileOutputStream.flush();
//                fileOutputStream.close();
//            }
//        }catch (Exception e) {
//            log.error("[JT-多媒体数据上传] 写入文件异常", e);
//        }
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
