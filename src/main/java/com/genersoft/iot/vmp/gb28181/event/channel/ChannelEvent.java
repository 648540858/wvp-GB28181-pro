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



    enum ChannelEventMessageType {
        ADD, UPDATE, DELETE, ONLINE, OFFLINE, OFFLINE_LIST
    }

    public static ChannelEvent getInstanceForAdd(Object source, CommonGBChannel channel) {
        return getInstance(source, ChannelEventMessageType.ADD, channel);
    }

    public static ChannelEvent getInstanceForAddList(Object source, List<CommonGBChannel> channels) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(ChannelEventMessageType.ADD);
        channelEvent.setChannels(channels);
        return channelEvent;
    }

    public static ChannelEvent getInstanceForUpdate(Object source, CommonGBChannel channel) {
        return getInstance(source, ChannelEventMessageType.UPDATE, channel);
    }

    public static ChannelEvent getInstanceForUpdateList(Object source, List<CommonGBChannel> channels) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(ChannelEventMessageType.UPDATE);
        channelEvent.setChannels(channels);
        return channelEvent;
    }

    public static ChannelEvent getInstanceForDelete(Object source, CommonGBChannel channel) {
        return getInstance(source, ChannelEventMessageType.DELETE, channel);
    }

    public static ChannelEvent getInstanceForOnline(Object source, CommonGBChannel channel) {
        return getInstance(source, ChannelEventMessageType.ONLINE, channel);
    }

    public static ChannelEvent getInstanceForOnlineList(Object source, List<CommonGBChannel> channels) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(ChannelEventMessageType.ONLINE);
        channelEvent.setChannels(channels);
        return channelEvent;
    }

    public static ChannelEvent getInstanceForOffline(Object source, CommonGBChannel channel) {
        return getInstance(source, ChannelEventMessageType.OFFLINE, channel);
    }

    public static ChannelEvent getInstanceForOfflineList(Object source, List<CommonGBChannel> channel) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(ChannelEventMessageType.OFFLINE);
        channelEvent.setChannels(channel);
        return channelEvent;
    }

    public static Object getInstanceForDeleteList(Object source, List<CommonGBChannel> commonGBChannels) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(ChannelEventMessageType.DELETE);
        channelEvent.setChannels(commonGBChannels);
        return channelEvent;
    }

    private static ChannelEvent getInstance(Object source, ChannelEventMessageType messageType, CommonGBChannel channel) {
        ChannelEvent channelEvent = new ChannelEvent(source);
        channelEvent.setMessageType(messageType);
        channelEvent.setChannels(Collections.singletonList(channel));
        return channelEvent;
    }

}
