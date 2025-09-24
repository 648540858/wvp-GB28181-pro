package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTDeviceConnectionControl;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 终端控制
 */
@Getter
@MsgId(id = "8105")
public class J8105 extends Rs {

    private JTDeviceConnectionControl connectionControl;

    /**
     * 终端复位
     */
    private Boolean reset;

    /**
     * 终端恢复出厂设置
     */
    private Boolean factoryReset;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        if (reset != null) {
            byteBuf.writeByte(4);
        }else if (factoryReset != null) {
            byteBuf.writeByte(5);
        }else if (connectionControl != null) {
            byteBuf.writeByte(2);
            StringBuffer stringBuffer = new StringBuffer();
            if (connectionControl.getSwitchOn() != null) {
                if (connectionControl.getSwitchOn()) {
                    stringBuffer.append("1");
                }else {
                    stringBuffer.append("0");
                    stringBuffer.append(";" + connectionControl.getAuthentication())
                            .append(";" + connectionControl.getName())
                            .append(";" + connectionControl.getUsername())
                            .append(";" + connectionControl.getPassword())
                            .append(";" + connectionControl.getAddress())
                            .append(";" + connectionControl.getTcpPort())
                            .append(";" + connectionControl.getUdpPort())
                            .append(";" + connectionControl.getTimeLimit());
                }
            }
            byteBuf.writeCharSequence(stringBuffer.toString(), Charset.forName("GBK"));
        }
        return byteBuf;
    }

    public void setConnectionControl(JTDeviceConnectionControl connectionControl) {
        this.connectionControl = connectionControl;
    }

    public void setReset(Boolean reset) {
        this.reset = reset;
    }

    public void setFactoryReset(Boolean factoryReset) {
        this.factoryReset = factoryReset;
    }

    @Override
    public String toString() {
        return "J8105{" +
                "connectionControl=" + connectionControl +
                ", reset=" + reset +
                ", factoryReset=" + factoryReset +
                '}';
    }
}
