package com.genersoft.iot.vmp.gat1400.rpc;

import com.genersoft.iot.vmp.gat1400.backend.domain.vo.DispositionRequest;
import com.genersoft.iot.vmp.gat1400.framework.domain.vo.VIIDResponseStatusObject;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.URI;


/**
 * 布控API远程调用
 */
@FeignClient(name = "VIIDDispositionClient", url = "http://127.0.0.254")
public interface DispositionClient {

    @PostMapping("/VIID/Dispositions")
    VIIDResponseStatusObject createDisposition(URI uri, @RequestBody DispositionRequest request);

    @PutMapping("/VIID/Dispositions")
    VIIDResponseStatusObject updateDisposition(URI uri, @RequestBody DispositionRequest request);

    @PutMapping("/VIID/Dispositions/{id}")
    VIIDResponseStatusObject revokeDisposition(URI uri, @PathVariable("id") String id);

    @DeleteMapping("/VIID/Dispositions/{id}")
    VIIDResponseStatusObject deleteDisposition(URI uri, @PathVariable("id") String id);
}
