package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaConfig;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ZLMRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRESTfulUtils.class);

    @Autowired
    MediaConfig mediaConfig;

    public JSONObject sendPost(String mediaServerIp, String api, Map<String, Object> param) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/%s", mediaServerIp, mediaConfig.getMediaPort(), api);
        JSONObject responseJSON = null;
        logger.debug(url);

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("secret", mediaConfig.getMediaSecret());
        if (param != null) {
            for (String key : param.keySet()) {
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
        } catch (ConnectException e) {
            logger.error(String.format("连接ZLM失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            logger.info("请检查media配置并确认ZLM已启动...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return responseJSON;
    }

//    public JSONObject getMediaList(String app, String schema) {
//        Map<String, Object> param = new HashMap<>();
//        param.put("app", app);
//        param.put("schema", schema);
//        param.put("vhost", "__defaultVhost__");
//        return sendPost("getMediaList", param);
//    }
//
//    public JSONObject getMediaInfo(String app, String schema, String stream) {
//        Map<String, Object> param = new HashMap<>();
//        param.put("app", app);
//        param.put("schema", schema);
//        param.put("stream", stream);
//        param.put("vhost", "__defaultVhost__");
//        return sendPost("getMediaInfo", param);
//    }

    public JSONObject getRtpInfo(String mediaServerIp, String stream_id) {
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id", stream_id);
        return sendPost(mediaServerIp, "getRtpInfo", param);
    }

    public JSONObject addFFmpegSource(String mediaServerIp, String src_url, String dst_url, String timeout_ms) {
        System.out.println(src_url);
        System.out.println(dst_url);
        Map<String, Object> param = new HashMap<>();
        param.put("src_url", src_url);
        param.put("dst_url", dst_url);
        param.put("timeout_ms", timeout_ms);
        return sendPost(mediaServerIp, "addFFmpegSource", param);
    }

    public JSONObject delFFmpegSource(String mediaServerIp, String key) {
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);
        return sendPost(mediaServerIp, "delFFmpegSource", param);
    }

    public JSONObject getMediaServerConfig(String mediaServerIp) {
        return sendPost(mediaServerIp, "getServerConfig", null);
    }

    public JSONObject setServerConfig(String mediaServerIp, Map<String, Object> param) {
        return sendPost(mediaServerIp, "setServerConfig", param);
    }

    public JSONObject openRtpServer(String mediaServerIp, Map<String, Object> param) {
        return sendPost(mediaServerIp, "openRtpServer", param);
    }

    public JSONObject closeRtpServer(String mediaServerIp, Map<String, Object> param) {
        return sendPost(mediaServerIp, "closeRtpServer", param);
    }

    /**
     * 截图
     *
     * @param mediaServerIp
     * @param url
     * @return
     */
    public JSONObject getSnap(String mediaServerIp, String url) {
        Map<String, Object> param = new HashMap<>();
        param.put("url", url);
        param.put("timeout_sec", 10);
        param.put("expire_sec", 1);
        param.put("url", url);
        return sendPost(mediaServerIp, "getSnap", param);
    }

    /**
     * 开始录制
     *
     * @param mediaServerIp
     * @param stream
     * @return
     */
    public JSONObject startRecord(String mediaServerIp, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("type", 1);
        param.put("app", "rtp");
        param.put("stream", stream);
        return sendPost(mediaServerIp, "startRecord", param);
    }

    /**
     * 结束录制
     *
     * @param mediaServerIp
     * @param stream
     * @return
     */
    public JSONObject stopRecord(String mediaServerIp, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("type", 1);
        param.put("app", "rtp");
        param.put("stream", stream);
        return sendPost(mediaServerIp, "stopRecord", param);
    }
}
