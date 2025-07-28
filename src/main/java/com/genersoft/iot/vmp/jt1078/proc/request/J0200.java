package com.genersoft.iot.vmp.jt1078.proc.request;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.*;
import com.genersoft.iot.vmp.jt1078.proc.Header;
import com.genersoft.iot.vmp.jt1078.proc.response.J8001;
import com.genersoft.iot.vmp.jt1078.proc.response.Rs;
import com.genersoft.iot.vmp.jt1078.service.Ijt1078Service;
import com.genersoft.iot.vmp.jt1078.session.Session;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;

/**
 * 位置信息汇报
 *
 */
@Slf4j
@MsgId(id = "0200")
public class J0200 extends Re {

    private JTPositionBaseInfo positionInfo;

    @Override
    protected Rs decode0(ByteBuf buf, Header header, Session session) {
        positionInfo = JTPositionBaseInfo.decode(buf);
        log.info("[JT-位置汇报]: phoneNumber={}  {}", header.getPhoneNumber(), positionInfo.toSimpleString());
        // 读取附加信息
//        JTPositionAdditionalInfo positionAdditionalInfo = new JTPositionAdditionalInfo();
//        Map<Integer, byte[]> additionalMsg = new HashMap<>();
//        getAdditionalMsg(buf, positionAdditionalInfo);
//        log.info("[JT-位置汇报]: phoneNumber={}  {}", header.getPhoneNumber(), positionInfo.toSimpleString());
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
        J8001 j8001 = new J8001();
        j8001.setRespNo(header.getSn());
        j8001.setRespId(header.getMsgId());
        j8001.setResult(J8001.SUCCESS);
        service.updateDevicePosition(header.getPhoneNumber(), positionInfo.getLongitude(), positionInfo.getLatitude());
        return j8001;
    }

    @Override
    public ApplicationEvent getEvent() {
        return null;
    }
}
