package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;

/**
 * 文本信息下发
 */
@MsgId(id = "8300")
public class J8300 extends Rs {

    /**
     * 标志
     */
    private JTTextSign sign;

    /**
     * 文本类型1 = 通知 ，2 = 服务
     */
    private int textType;

    /**
     * 文本信息
     */
    private String content;

    @Override
    public ByteBuf encode() {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(sign.encode());
        buffer.writeByte(textType);
        buffer.writeCharSequence(content, Charset.forName("GBK"));
        return buffer;
    }

    public JTTextSign getSign() {
        return sign;
    }

    public void setSign(JTTextSign sign) {
        this.sign = sign;
    }

    public int getTextType() {
        return textType;
    }

    public void setTextType(int textType) {
        this.textType = textType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
