package com.genersoft.iot.vmp.utils;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author gqs
 */
@Service
public class RequestSendUtil {

    @Value("${web.ip}")
    private String ip ;

    @Value("${web.port}")
    private String port = "8088";

    public Boolean send(Integer type, String device, String channel) {
        StringBuffer url = new StringBuffer();
        url.append("http://");
        url.append(ip);
        url.append(":");
        url.append(port);
        url.append("/zzjp/log/info/");
        url.append(type);
        url.append("/");
        url.append(device);
        url.append("/");
        url.append(channel);
        HttpResponse execute = HttpUtil.createGet(url.toString()).execute();
        String body = execute.body();
        JSONObject entries = JSONUtil.parseObj(body);
        Integer code = (Integer) entries.get("code");
        if (code != 200) {
            return false;
        }
        return true;
    }
}
