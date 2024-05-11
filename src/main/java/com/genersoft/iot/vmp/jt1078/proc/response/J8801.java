package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTShootingCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.util.List;

/**
 * 摄像头立即拍摄命令
 */
@MsgId(id = "8801")
public class J8801 extends Rs {

    JTShootingCommand command;

    @Override
    public ByteBuf encode() {
        return command.decode();
    }

    public JTShootingCommand getCommand() {
        return command;
    }

    public void setCommand(JTShootingCommand command) {
        this.command = command;
    }
}
