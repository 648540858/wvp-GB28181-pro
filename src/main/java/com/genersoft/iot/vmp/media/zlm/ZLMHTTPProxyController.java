package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.storager.IRedisCatchStorage;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

@RestController
@RequestMapping("/zlm")
public class ZLMHTTPProxyController {


    private final static Logger logger = LoggerFactory.getLogger(ZLMHTTPProxyController.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Value("${media.port}")
    private int mediaHttpPort;

    @ResponseBody
    @RequestMapping(value = "/**/**/**", produces = "application/json;charset=UTF-8")
    public Object proxy(HttpServletRequest request, HttpServletResponse response){

        if (redisCatchStorage.getMediaInfo() == null) {
            return "未接入流媒体";
        }
        MediaServerConfig mediaInfo = redisCatchStorage.getMediaInfo();
        String requestURI = String.format("http://%s:%s%s?%s&%s",
                mediaInfo.getLocalIP(),
                mediaHttpPort,
                request.getRequestURI().replace("/zlm",""),
                mediaInfo.getHookAdminParams(),
                request.getQueryString()
        );
        // 发送请求
        RestTemplate restTemplate = new RestTemplate();
        //将指定的url返回的参数自动封装到自定义好的对应类对象中
        Object result = null;
        try {
            result = restTemplate.getForObject(requestURI,Object.class);

        }catch (HttpClientErrorException httpClientErrorException) {
            response.setStatus(httpClientErrorException.getStatusCode().value());
        }
        return result;
    }
}
