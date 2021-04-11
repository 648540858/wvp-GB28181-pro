package com.genersoft.iot.vmp.vmanager.gb28181.play.bean;

import com.genersoft.iot.vmp.gb28181.bean.Device;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

public class PlayResult {

    private DeferredResult<ResponseEntity<String>> result;
    private String uuid;

    private Device device;

    public DeferredResult<ResponseEntity<String>> getResult() {
        return result;
    }

    public void setResult(DeferredResult<ResponseEntity<String>> result) {
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
