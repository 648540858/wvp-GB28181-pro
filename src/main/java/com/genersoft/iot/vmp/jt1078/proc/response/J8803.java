package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import io.netty.buffer.ByteBuf;

/**
 * 存储多媒体数据上传命令
 */
@MsgId(id = "8803")
public class J8803 extends Rs {

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
