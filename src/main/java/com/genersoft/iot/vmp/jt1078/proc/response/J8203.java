package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JConfirmationAlarmMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 人工确认报警消息
 */
@MsgId(id = "8203")
public class J8203 extends Rs {

    /**
     * 报警消息流水号
     */
    private int alarmPackageNo;
    /**
     * 人工确认报警类型
     */
    private JConfirmationAlarmMessageType alarmMessageType;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeShort((short)(alarmPackageNo & 0xffff));
        if (alarmMessageType != null) {
            buffer.writeInt((int) (alarmMessageType.encode() & 0xffffffffL));
        }
        return buffer;
    }

    public int getAlarmPackageNo() {
        return alarmPackageNo;
    }

    public void setAlarmPackageNo(int alarmPackageNo) {
        this.alarmPackageNo = alarmPackageNo;
    }

    public JConfirmationAlarmMessageType getAlarmMessageType() {
        return alarmMessageType;
    }

    public void setAlarmMessageType(JConfirmationAlarmMessageType alarmMessageType) {
        this.alarmMessageType = alarmMessageType;
    }
}
