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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

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

        // 读取附加信息
//        JTPositionAdditionalInfo positionAdditionalInfo = new JTPositionAdditionalInfo();
//        Map<Integer, byte[]> additionalMsg = new HashMap<>();
//        getAdditionalMsg(buf, positionAdditionalInfo);
        log.info("[JT-位置汇报]: {}", positionInfo.toString());
        return null;
    }

    private void getAdditionalMsg(ByteBuf buf, JTPositionAdditionalInfo additionalInfo) {

        if (buf.isReadable()) {
            int msgId = buf.readUnsignedByte();
            int length = buf.readUnsignedByte();
            ByteBuf byteBuf = buf.readBytes(length);
            switch (msgId) {
                case 1:
                    // 里程
                    long mileage = byteBuf.readUnsignedInt();
                    log.info("[JT-位置汇报]: 里程： {} km", (double)mileage/10);
                    break;
                case 2:
                    // 油量
                    int oil = byteBuf.readUnsignedShort();
                    log.info("[JT-位置汇报]: 油量： {} L", (double)oil/10);
                    break;
                case 3:
                    // 速度
                    int speed = byteBuf.readUnsignedShort();
                    log.info("[JT-位置汇报]: 速度： {} km/h", (double)speed/10);
                    break;
                case 4:
                    // 需要人工确认报警事件的 ID
                    int alarmId = byteBuf.readUnsignedShort();
                    log.info("[JT-位置汇报]: 需要人工确认报警事件的 ID： {}", alarmId);
                    break;
                case 5:
                    byte[] tirePressureBytes = new byte[30];
                    // 胎压
                    byteBuf.readBytes(tirePressureBytes);
                    log.info("[JT-位置汇报]: 胎压 {}", tirePressureBytes);
                    break;
                case 6:
                    // 车厢温度
                    short carriageTemperature = byteBuf.readShort();
                    log.info("[JT-位置汇报]: 车厢温度 {}摄氏度", carriageTemperature);
                    break;
                case 11:
                    // 超速报警
                    short positionType = byteBuf.readUnsignedByte();
                    long positionId = byteBuf.readUnsignedInt();
                    log.info("[JT-位置汇报]: 超速报警, 位置类型: {}, 区域或路段 ID: {}", positionType, positionId);
                    break;
                default:
                    log.info("[JT-位置汇报]: 附加消息ID： {}， 消息长度： {}", msgId, length);
                    break;

            }
            getAdditionalMsg(buf, additionalInfo);
        }
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
