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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 多媒体数据上传
 *
 */
@MsgId(id = "0801")
public class J0801 extends Re {

    private final static Logger log = LoggerFactory.getLogger(J0801.class);

    private JTMediaEventInfo mediaEventInfo;
    private JTPositionBaseInfo positionBaseInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        mediaEventInfo = JTMediaEventInfo.decode(buf);
        System.out.println(mediaEventInfo.getId());
        ByteBuf byteBuf = buf.readSlice(28);
        positionBaseInfo = JTPositionBaseInfo.decode(byteBuf);
        String fileName = "mediaEvent/" + mediaEventInfo.getId() + ".";
        File mediaEventFile = new File("mediaEvent");
        if (!mediaEventFile.exists()) {
            mediaEventFile.mkdirs();
        }
        switch (mediaEventInfo.getCode()){
            case 0:
                fileName += "jpg";
                break;
            case 1:
                fileName += "tif";
                break;
            case 2:
                fileName += "mp3";
                break;
            case 3:
                fileName += "wav";
                break;
            case 4:
                fileName += "wmv";
                break;
        }
        try {
            File file = new File(fileName);
            file.deleteOnExit();
            file.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (IOException e) {
            log.info("[JT-多媒体数据上传] 写入文件失败", e);
        }
        log.info("[JT-多媒体数据上传]: {}", mediaEventInfo);
//        SessionManager.INSTANCE.response(header.getTerminalId(), "0801", null, mediaEventInfo);
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
