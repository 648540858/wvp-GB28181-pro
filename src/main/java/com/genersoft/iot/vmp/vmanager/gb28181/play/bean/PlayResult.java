package com.genersoft.iot.vmp.vmanager.gb28181.play.bean;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public class PlayResult {

    private DeferredResult<WVPResult<String>> result;
    private String uuid;

    private Device device;

    public DeferredResult<WVPResult<String>> getResult() {
        return result;
    }

    public void setResult(DeferredResult<WVPResult<String>> result) {
        this.result = result;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }
}
