package com.genersoft.iot.vmp.gat1400.framework.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import cz.data.viid.framework.domain.dto.DeviceIdObject;
import lombok.Data;

@Data
public class KeepaliveRequest {

    @JsonProperty("KeepaliveObject")
    private DeviceIdObject keepaliveObject;

    public static KeepaliveRequest builder(DeviceIdObject keepaliveObject) {
        KeepaliveRequest keepaliveRequest = new KeepaliveRequest();
        keepaliveRequest.setKeepaliveObject(keepaliveObject);
        return keepaliveRequest;
    }

}
