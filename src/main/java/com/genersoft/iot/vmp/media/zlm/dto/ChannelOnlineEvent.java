package com.genersoft.iot.vmp.media.zlm.dto;

import com.genersoft.iot.vmp.gb28181.bean.SendRtpItem;

import java.text.ParseException;

/**
 * @author lin
 */
public interface ChannelOnlineEvent {

    void run(SendRtpItem sendRtpItem) throws ParseException;
}
