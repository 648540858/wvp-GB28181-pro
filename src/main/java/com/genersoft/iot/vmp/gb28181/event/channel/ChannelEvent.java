package com.genersoft.iot.vmp.gb28181.event.channel;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

/**
 * 通道事件
 */

@Setter
@Getter
public class ChannelEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public ChannelEvent(Object source) {
        super(source);
    }

    private List<CommonGBChannel> channels;

    private ChannelEventMessageType messageType;



    public enum ChannelEventMessageType {
        ADD, UPDATE, DELETE, ONLINE, OFFLINE, VLOST, DEFECT
    }

    public static ChannelEvent getInstance(Object source, ChannelEventMessageType messageType, List<CommonGBChannel> channelList) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(messageType);
        channelEvent.setChannels(channelList);
        return channelEvent;
    }

}
