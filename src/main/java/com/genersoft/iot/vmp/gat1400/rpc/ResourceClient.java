package com.genersoft.iot.vmp.gat1400.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

import feign.Response;

/**
 * 用户图片资源下载
 */
@FeignClient(name = "ResourceClient", url = "http://127.0.0.254")
public interface ResourceClient {

    @GetMapping
    Response getResource(URI uri);
}
