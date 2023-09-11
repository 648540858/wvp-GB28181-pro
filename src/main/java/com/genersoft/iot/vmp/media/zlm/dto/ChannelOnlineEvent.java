package com.genersoft.iot.vmp.media.zlm.dto;

import java.text.ParseException;

/**
 * @author lin
 */
public interface ChannelOnlineEvent {

    void run(String app, String stream, String serverId) throws ParseException;
}
