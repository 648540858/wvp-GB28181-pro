package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTPositionInfo;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import org.springframework.context.ApplicationEvent;

/**
 * 实时消息上报
 *
 * @author QingtaiJiang
 * @date 2023/4/27 18:06
 * @email qingtaij@163.com
 */
@MsgId(id = "0200")
public class J0200 extends Re {

    private JTPositionInfo positionInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        positionInfo = new JTPositionInfo();
        int alarmSignInt = buf.readInt();
        int statusInt = buf.readInt();
        int latitudeInt = buf.readInt();
        int longitudeInt = buf.readInt();
        int altitudeInt = buf.readUnsignedShort();
        int speedInt = buf.readUnsignedShort();
        int directionInt = buf.readUnsignedShort();
        byte[] timeBytes = new byte[6];
        buf.readBytes(timeBytes);
        System.out.println(alarmSignInt);
        System.out.println(statusInt);
        System.out.println(latitudeInt);
        System.out.println(longitudeInt);
        System.out.println(altitudeInt);
        System.out.println(speedInt);
        System.out.println(directionInt);
        // TODO 解析时间
//        for (byte timeByte : timeBytes) {
//            for (int i = 0; i < 8; i++) {
//                System.out.print(timeByte>>i & 1);
//            }
//        }
        StringBuffer temp = new StringBuffer(timeBytes.length * 2);
        for (int i = 0; i < timeBytes.length; i++) {
            temp.append((byte) ((timeBytes[i] & 0xf0) >>> 4));
            temp.append((byte) (timeBytes[i] & 0x0f));
        }

        System.out.println(temp.toString().substring(0, 1).equalsIgnoreCase("0") ? temp
                .toString().substring(1) : temp.toString());
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

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
