package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Component
public class ZLMRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRESTfulUtils.class);

    @Value("${media.ip}")
    private String mediaIp;

    @Value("${media.port}")
    private int mediaPort;

    @Value("${media.secret}")
    private String mediaSecret;

    public JSONObject sendPost(String api, Map<String, Object> param) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/%s",  mediaIp, mediaPort, api);
        JSONObject responseJSON = null;
        logger.debug(url);

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("secret",mediaSecret);
        if (param != null) {
            for (String key : param.keySet()){
                builder.add(key, param.get(key).toString());
            }
        }

        FormBody body = builder.build();

        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseStr = response.body().string();
                if (responseStr != null) {
                    responseJSON = JSON.parseObject(responseStr);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseJSON;
    }

    public JSONObject getMediaList(String app, String schema){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("schema",schema);
        param.put("vhost","__defaultVhost__");
        return sendPost("getMediaList",param);
    }

    public JSONObject getRtpInfo(String stream_id){
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id",stream_id);
        return sendPost("getRtpInfo",param);
    }

    public JSONObject getMediaServerConfig(){
        return sendPost("getServerConfig",null);
    }

    public JSONObject setServerConfig(Map<String, Object> param){
        return sendPost("setServerConfig",param);
    }
}
