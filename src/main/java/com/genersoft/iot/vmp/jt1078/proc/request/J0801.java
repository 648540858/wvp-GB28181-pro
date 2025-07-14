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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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


        try {
            int width = 800;
            int height = 600;
            BufferedImage image1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            File file1 = new File("/home/lin/1.jpg");
            ImageIO.write(image1, "jpg", file1);


            BufferedImage image2 = new BufferedImage(width, height, 2);
            File file2 = new File("/home/lin/2.jpg");
            ImageIO.write(image2, "jpg", file2);


            BufferedImage image3 = new BufferedImage(width, height, 3);
            File file3 = new File("/home/lin/3.jpg");
            ImageIO.write(image3, "jpg", file3);


            BufferedImage image4 = new BufferedImage(width, height, 4);
            File file4 = new File("/home/lin/4.jpg");
            ImageIO.write(image4, "jpg", file4);


            BufferedImage image5 = new BufferedImage(width, height, 5);
            File file5 = new File("/home/lin/5.jpg");
            ImageIO.write(image5, "jpg", file5);


            BufferedImage image6 = new BufferedImage(width, height, 6);
            File file6 = new File("/home/lin/6.jpg");
            ImageIO.write(image6, "jpg", file6);


            BufferedImage image7 = new BufferedImage(width, height, 7);
            File file7 = new File("/home/lin/7.jpg");
            ImageIO.write(image7, "jpg", file7);


            BufferedImage image8 = new BufferedImage(width, height, 8);
            File file8 = new File("/home/lin/8.jpg");
            ImageIO.write(image8, "jpg", file8);

            File file = new File("/home/lin/source.jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(file);
//            fileOutputStream.write(0xFF);
//            fileOutputStream.write(0xD8);
//            fileOutputStream.write(0xFF);
            fileOutputStream.write(mediaEventInfo.getMediaData());
            fileOutputStream.flush();
            fileOutputStream.close();
        }catch (Exception e) {
            log.error("[JT-多媒体数据上传] 写入文件异常", e);
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
