package com.genersoft.iot.vmp.gat1400.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;

import cz.data.viid.be.domain.vo.MotorVehicleRequest;
import cz.data.viid.framework.domain.vo.VIIDResponseStatusObject;

@FeignClient(name = "MotorVehicleClient", url = "http://127.0.0.254")
public interface MotorVehicleClient {

    @PostMapping("/VIID/MotorVehicles")
    VIIDResponseStatusObject addMotorVehicle(URI uri, @RequestBody MotorVehicleRequest request);
}
