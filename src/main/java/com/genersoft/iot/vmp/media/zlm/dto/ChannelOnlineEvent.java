package com.genersoft.iot.vmp.media.zlm.dto;

/**
 * @author lin
 */
public interface ChannelOnlineEvent {

    void run(String app, String stream, String serverId);
}
