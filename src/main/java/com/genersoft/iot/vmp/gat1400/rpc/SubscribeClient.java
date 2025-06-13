package com.genersoft.iot.vmp.gat1400.rpc;

import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribeNotificationRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.SubscribesRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.util.List;


@FeignClient(name = "SubscribeClient", url = "http://127.0.0.254")
public interface SubscribeClient {

    @PostMapping("/VIID/Subscribes")
    VIIDResponseStatusObject addSubscribes(URI uri, @RequestBody SubscribesRequest request);

    @PutMapping("/VIID/Subscribes")
    VIIDResponseStatusObject updateSubscribes(URI uri, @RequestBody SubscribesRequest request);

    @DeleteMapping("/VIID/Subscribes")
    VIIDResponseStatusObject cancelSubscribes(URI uri, @RequestParam("IDList") List<String> resourceIds);

    //@PostMapping("/VIID/SubscribeNotifications")
    @PostMapping
    VIIDResponseStatusObject subscribeNotifications(URI uri, @RequestBody SubscribeNotificationRequest request);
}
