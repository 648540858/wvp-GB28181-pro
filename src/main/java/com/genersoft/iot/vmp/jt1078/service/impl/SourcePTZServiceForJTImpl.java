package com.genersoft.iot.vmp.jt1078.service.impl;

import com.genersoft.iot.vmp.common.enums.ChannelDataType;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.service.IPTZService;
import com.genersoft.iot.vmp.gb28181.service.ISourcePTZService;
import com.genersoft.iot.vmp.service.bean.ErrorCallback;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service(ChannelDataType.PTZ_SERVICE + ChannelDataType.JT_1078)
public class SourcePTZServiceForJTImpl implements ISourcePTZService {


    @Override
    public void ptz(CommonGBChannel channel, FrontEndControlCodeForPTZ frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void preset(CommonGBChannel channel, FrontEndControlCodeForPreset frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void fi(CommonGBChannel channel, FrontEndControlCodeForFI frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void tour(CommonGBChannel channel, FrontEndControlCodeForTour frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void scan(CommonGBChannel channel, FrontEndControlCodeForScan frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void auxiliary(CommonGBChannel channel, FrontEndControlCodeForAuxiliary frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void wiper(CommonGBChannel channel, FrontEndControlCodeForWiper frontEndControlCode, ErrorCallback<String> callback) {

    }

    @Override
    public void queryPreset(CommonGBChannel channel, ErrorCallback<List<Preset>> callback) {

    }
}
