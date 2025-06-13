package com.genersoft.iot.vmp.gat1400.rpc;

import com.genersoft.iot.vmp.gat1400.backend.domain.vo.MotorVehicleRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;


@FeignClient(name = "MotorVehicleClient", url = "http://127.0.0.254")
public interface MotorVehicleClient {

    @PostMapping("/VIID/MotorVehicles")
    VIIDResponseStatusObject addMotorVehicle(URI uri, @RequestBody MotorVehicleRequest request);
}
