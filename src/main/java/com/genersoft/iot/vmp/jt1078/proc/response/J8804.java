package com.genersoft.iot.vmp.jt1078.proc.response;

import com.genersoft.iot.vmp.jt1078.annotation.MsgId;
import com.genersoft.iot.vmp.jt1078.bean.JTQueryMediaDataCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * 录音开始/停止命令
 */
@MsgId(id = "8804")
public class J8804 extends Rs {

    /**
     * 录音命令， 0:停止录音；0X01:开始录音
     */
    private int commond;

    /**
     * 录音时长，单位为秒(s) ,0 表示一直录音
     */
    private int duration;

    /**
     * 保存标志， 0:实时上传；1:保存
     */
    private int save;

    /**
     * 音频采样率， 0:8K；1:11K；2:23K；3:32K；其他保留
     */
    private int samplingRate;

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(commond);
        byteBuf.writeShort((short)(duration & 0xffff));
        byteBuf.writeByte(save);
        byteBuf.writeByte(samplingRate);
        return byteBuf;
    }

    public int getCommond() {
        return commond;
    }

    public void setCommond(int commond) {
        this.commond = commond;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getSave() {
        return save;
    }

    public void setSave(int save) {
        this.save = save;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }
}
