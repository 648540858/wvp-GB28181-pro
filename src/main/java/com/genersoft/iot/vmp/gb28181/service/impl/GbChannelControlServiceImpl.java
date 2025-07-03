package com.genersoft.iot.vmp.gb28181.service.impl;

import com.genersoft.iot.vmp.gb28181.bean.CommonGBChannel;
import com.genersoft.iot.vmp.gb28181.bean.FrontEndControlCodeForPTZ;
import com.genersoft.iot.vmp.gb28181.service.IGbChannelControlService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GbChannelControlServiceImpl implements IGbChannelControlService {

    @Override
    public void ptz(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 云台控制， 通道： {}", channel.getGbId());
    }

    @Override
    public void preset(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] 预置位， 通道： {}", channel.getGbId());
    }

    @Override
    public void fi(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {
        log.info("[通用通道] FI指令， 通道： {}", channel.getGbId());
    }

    @Override
    public void tour(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void scan(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void auxiliary(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {

    }
}
