package com.genersoft.iot.vmp.jt1078.bean.config;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

/**
 * 单独视频通道参数设置
 */
public class ChannelParam implements JTDeviceSubConfig {

    /**
     * 单独通道视频参数设置列表
     */
    private List<JTAloneChanel> jtAloneChanelList;

    public List<JTAloneChanel> getJtAloneChanelList() {
        return jtAloneChanelList;
    }

    public void setJtAloneChanelList(List<JTAloneChanel> jtAloneChanelList) {
        this.jtAloneChanelList = jtAloneChanelList;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(jtAloneChanelList.size());
        for (JTAloneChanel jtAloneChanel : jtAloneChanelList) {
            byteBuf.writeBytes(jtAloneChanel.encode());
        }
        return byteBuf;
    }

    public static ChannelParam decode(ByteBuf byteBuf) {
        ChannelParam channelParam = new ChannelParam();
        int length = byteBuf.readUnsignedByte();
        List<JTAloneChanel> jtAloneChanelList = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            jtAloneChanelList.add(JTAloneChanel.decode(byteBuf));
        }
        channelParam.setJtAloneChanelList(jtAloneChanelList);
        return channelParam;
    }
}
