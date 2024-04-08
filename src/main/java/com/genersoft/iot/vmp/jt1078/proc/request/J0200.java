package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import com.genersoft.iot.vmp.jt1078.util.BCDUtil;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final static Logger log = LoggerFactory.getLogger(J0100.class);
    private JTPositionBaseInfo positionInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        positionInfo = new JTPositionBaseInfo();
        int alarmSignInt = buf.readInt();
        positionInfo.setAlarmSign(new JTAlarmSign(alarmSignInt));

        int statusInt = buf.readInt();
        positionInfo.setStatus(new JTStatus(statusInt));

        positionInfo.setLatitude(buf.readInt() * 0.000001D);
        positionInfo.setLongitude(buf.readInt() *  0.000001D);
        positionInfo.setAltitude(buf.readUnsignedShort());
        positionInfo.setSpeed(buf.readUnsignedShort());
        positionInfo.setDirection(buf.readUnsignedShort());
        byte[] timeBytes = new byte[6];
        buf.readBytes(timeBytes);
        positionInfo.setTime(BCDUtil.transform(timeBytes));
        boolean readable = buf.isReadable();
        // 读取附加信息
        if (buf.isReadable()) {
            // 支持1078的视频报警上报
            int alarm = buf.readInt();
            int loss = buf.readInt();
            int occlusion = buf.readInt();
            short storageFault = buf.readShort();
            short driving = buf.readShort();
            JTVideoAlarm videoAlarm = JTVideoAlarm.getInstance(alarm, loss, occlusion, storageFault, driving);
            positionInfo.setVideoAlarm(videoAlarm);
        }
        log.info("[JT-位置汇报]: {}", positionInfo.toString());
        return null;
    }

    @Override
    protected Rs handler(Header header, Session session, Ijt1078Service service) {
        JTDevice deviceInDb = service.getDevice(header.getTerminalId());
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        if (deviceInDb == null) {
            j8001.setResult(J8001.FAIL);
        }else {
            deviceInDb.setLongitude(positionInfo.getLongitude());
            deviceInDb.setLatitude(positionInfo.getLatitude());
            service.updateDevice(deviceInDb);
            j8001.setResult(J8001.SUCCESS);
        }
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
