package com.genersoft.iot.vmp.gat1400.backend.task.action;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

import cz.data.viid.be.security.DigestData;
import cz.data.viid.framework.domain.dto.DeviceIdObject;
import cz.data.viid.framework.domain.dto.ResponseStatusObject;
import cz.data.viid.framework.domain.entity.VIIDServer;
import cz.data.viid.framework.domain.vo.KeepaliveRequest;
import cz.data.viid.framework.domain.vo.RegisterRequest;
import cz.data.viid.framework.domain.vo.UnRegisterRequest;
import cz.data.viid.framework.domain.vo.VIIDBaseResponse;
import cz.data.viid.framework.exception.VIIDRuntimeException;
import cz.data.viid.rpc.SystemClient;
import feign.Response;

@Component
public class VIIDServerAction {

    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    SystemClient systemClient;

    public VIIDBaseResponse register(VIIDServer domain) throws IOException {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setRegisterObject(new DeviceIdObject(KeepaliveAction.CURRENT_SERVER_ID));
        Response response = null;
        try {
            response = systemClient.register(URI.create(domain.httpUrlBuilder()), registerRequest, "");
            if (response.status() == HttpStatus.UNAUTHORIZED.value()) {
                Collection<String> headers = response.headers().get(HttpHeaders.WWW_AUTHENTICATE);
                String header = Optional.ofNullable(headers)
                        .filter(ele -> !ele.isEmpty())
                        .map(LinkedList::new)
                        .map(LinkedList::getFirst)
                        .orElseThrow(() -> new VIIDRuntimeException("未返回正确的 WWW-Authenticate"));
                DigestData digestData = new DigestData(header);
                String authenticate = digestData.toDigestHeader(domain.getUsername(), domain.getAuthenticate(), "POST");
                response = systemClient.register(URI.create(domain.httpUrlBuilder()), registerRequest, authenticate);
                if (response.status() == HttpStatus.UNAUTHORIZED.value()) {
                    return new VIIDBaseResponse(new ResponseStatusObject(null, null, "1", "摘要认证第二次请求返回401"));
                }
            }
            VIIDBaseResponse gaBaseResponse = objectMapper.readValue(response.body().asReader(StandardCharsets.UTF_8), VIIDBaseResponse.class);
            Optional.ofNullable(gaBaseResponse)
                    .map(VIIDBaseResponse::getResponseStatusObject)
                    .map(ResponseStatusObject::getStatusCode)
                    .filter("0"::equals)
                    .orElseThrow(() -> new RuntimeException("注册失败"));
            return gaBaseResponse;
        } finally {
            if (Objects.nonNull(response))
                response.close();
        }
    }

    public VIIDBaseResponse unRegister(VIIDServer domain) {
        DeviceIdObject deviceObject = new DeviceIdObject();
        deviceObject.setDeviceId(KeepaliveAction.CURRENT_SERVER_ID);
        UnRegisterRequest request = new UnRegisterRequest();
        request.setUnRegisterObject(deviceObject);
        return systemClient.unRegister(URI.create(domain.httpUrlBuilder()), request);
    }

    public VIIDBaseResponse keepalive(VIIDServer domain) {
        DeviceIdObject deviceObject = new DeviceIdObject();
        deviceObject.setDeviceId(KeepaliveAction.CURRENT_SERVER_ID);
        KeepaliveRequest request = KeepaliveRequest.builder(deviceObject);
        return systemClient.keepalive(URI.create(domain.httpUrlBuilder()), request);
    }


}
