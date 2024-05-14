package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.netty.buffer.ByteBuf;

/**
 * 存储多媒体数据检索
 */
@MsgId(id = "8802")
public class J8802 extends Rs {

    JTQueryMediaDataCommand command;

    @Override
    public ByteBuf encode() {
        return command.decode();
    }

    public JTQueryMediaDataCommand getCommand() {
        return command;
    }

    public void setCommand(JTQueryMediaDataCommand command) {
        this.command = command;
    }
}
