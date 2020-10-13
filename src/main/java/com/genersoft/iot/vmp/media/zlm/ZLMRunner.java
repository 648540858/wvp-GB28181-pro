package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaServerConfig;
import com.genersoft.iot.vmp.storager.IVideoManagerStorager;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Component
@Order(value=1)
public class ZLMRunner implements CommandLineRunner {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRunner.class);

    @Autowired
    private IVideoManagerStorager storager;

    @Value("${media.ip}")
    private String mediaIp;

    @Value("${media.port}")
    private int mediaPort;

    @Value("${media.secret}")
    private String mediaSecret;

    @Value("${sip.ip}")
    private String sipIP;

    @Value("${server.port}")
    private String serverPort;

    @Override
    public void run(String... strings) throws Exception {
        // 获取zlm信息
        logger.info("等待zlm接入...");
        MediaServerConfig mediaServerConfig = getMediaServerConfig();
        if (mediaServerConfig != null) {
            logger.info("zlm接入成功...");
            storager.updateMediaInfo(mediaServerConfig);
            logger.info("设置zlm...");
            saveZLMConfig();

        }
    }



    public MediaServerConfig getMediaServerConfig() {
        MediaServerConfig mediaServerConfig = null;
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/getServerConfig?secret=%s", mediaIp, mediaPort, mediaSecret);
        //创建一个Request
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
        //通过client发起请求
        final Call call = client.newCall(request);
        //执行同步请求，获取Response对象
        Response response = null;
        try {
            response = call.execute();
            if (response.isSuccessful()) {
                String responseStr = response.body().string();
                if (responseStr != null) {
                    JSONObject responseJSON = JSON.parseObject(responseStr);
                    JSONArray data = responseJSON.getJSONArray("data");
                    if (data != null && data.size() > 0) {
                        mediaServerConfig = JSON.parseObject(JSON.toJSONString(data.get(0)), MediaServerConfig.class);
                        mediaServerConfig.setLocalIP(mediaIp);
                    }
                }
            }else {
                logger.error("getMediaServerConfig失败, 1s后重试");
                Thread.sleep(1000);
                getMediaServerConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mediaServerConfig;
    }

    private void saveZLMConfig() {
        String hookIP = sipIP;
        if (mediaIp.equals(sipIP)) {
            hookIP = "127.0.0.1";
        }
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/setServerConfig", mediaIp, mediaPort);
        String hookPrex = String.format("http://%s:%s/index/hook", hookIP, serverPort);

        RequestBody body = new FormBody.Builder()
                .add("secret",mediaSecret)
                .add("hook.enable","1")
                .add("hook.on_flow_report","")
                .add("hook.on_http_access","")
                .add("hook.on_publish",String.format("%s/on_publish", hookPrex))
                .add("hook.on_record_mp4","")
                .add("hook.on_record_ts","")
                .add("hook.on_rtsp_auth","")
                .add("hook.on_rtsp_realm","")
                .add("hook.on_server_started",String.format("%s/on_server_started", hookPrex))
                .add("hook.on_shell_login",String.format("%s/on_shell_login", hookPrex))
                .add("hook.on_stream_none_reader",String.format("%s/on_stream_none_reader", hookPrex))
                .add("hook.on_stream_not_found",String.format("%s/on_stream_not_found", hookPrex))
                .add("hook.timeoutSec","20")
                .build();

        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("saveZLMConfig ",e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    if (responseStr != null) {
                        JSONObject responseJSON = JSON.parseObject(responseStr);
                        if (responseJSON.getInteger("code") == 0) {
                            logger.info("设置zlm成功");
                        }else {
                            logger.info("设置zlm失败: " + responseJSON.getString("msg"));
                        }
                    }
                }

            }
        });
    }
}
