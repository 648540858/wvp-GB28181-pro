package com.genersoft.iot.vmp.gat1400.rpc;

import com.genersoft.iot.vmp.gat1400.framework.domain.vo.KeepaliveRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.RegisterRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.UnRegisterRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDBaseResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.net.URI;

import feign.Response;


@FeignClient(name = "SystemClient", url = "http://127.0.0.254")
public interface SystemClient {

    @PostMapping("/VIID/System/Register")
    Response register(URI uri, @RequestBody RegisterRequest request,
                      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);

    @PostMapping("/VIID/System/UnRegister")
    VIIDBaseResponse unRegister(URI uri, @RequestBody UnRegisterRequest request);

    @PostMapping("/VIID/System/Keepalive")
    VIIDBaseResponse keepalive(URI uri, @RequestBody KeepaliveRequest keepaliveRequest);

}
