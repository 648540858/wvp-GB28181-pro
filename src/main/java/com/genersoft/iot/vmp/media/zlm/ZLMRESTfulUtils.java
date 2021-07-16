package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.genersoft.iot.vmp.conf.MediaConfig;
import com.genersoft.iot.vmp.media.zlm.dto.IMediaServerItem;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class ZLMRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRESTfulUtils.class);

    public interface RequestCallback{
        void run(JSONObject response);
    }

    public JSONObject sendPost(IMediaServerItem mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/%s",  mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api);
        JSONObject responseJSON = null;
        logger.debug(url);

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("secret",mediaServerItem.getSecret());
        if (param != null && param.keySet().size() > 0) {
            for (String key : param.keySet()){
                if (param.get(key) != null) {
                    builder.add(key, param.get(key).toString());
                }
            }
        }

        FormBody body = builder.build();

        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();
            if (callback == null) {
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
                }catch (IOException e) {
                    logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
                }
            }else {
                client.newCall(request).enqueue(new Callback(){

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response){
                        if (response.isSuccessful()) {
                            try {
                                String responseStr = Objects.requireNonNull(response.body()).string();
                                callback.run(JSON.parseObject(responseStr));
                            } catch (IOException e) {
                                logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        logger.error(String.format("连接ZLM失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
                        logger.info("请检查media配置并确认ZLM已启动...");
                    }
                });
            }



        return responseJSON;
    }


    public void sendPostForImg(IMediaServerItem mediaServerItem, String api, Map<String, Object> param, String targetPath, String fileName) {
        OkHttpClient client = new OkHttpClient();
        String url = String.format("http://%s:%s/index/api/%s",  mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api);
        JSONObject responseJSON = null;
        logger.debug(url);

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("secret",mediaServerItem.getSecret());
        if (param != null && param.keySet().size() > 0) {
            for (String key : param.keySet()){
                if (param.get(key) != null) {
                    builder.add(key, param.get(key).toString());
                }
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
                if (targetPath != null) {
                    File snapFolder = new File(targetPath);
                    if (!snapFolder.exists()) {
                        snapFolder.mkdirs();
                    }
                    File snapFile = new File(targetPath + "/" + fileName);
                    FileOutputStream outStream = new FileOutputStream(snapFile);
                    outStream.write(response.body().bytes());
                    outStream.close();
                }
            }
        } catch (ConnectException e) {
            logger.error(String.format("连接ZLM失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            logger.info("请检查media配置并确认ZLM已启动...");
        }catch (IOException e) {
            logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }

    }


    public JSONObject getMediaList(IMediaServerItem mediaServerItem,String app, String stream, String schema, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        if (app != null) param.put("app",app);
        if (stream != null) param.put("stream",stream);
        if (schema != null) param.put("schema",schema);
        param.put("vhost","__defaultVhost__");
        return sendPost(mediaServerItem, "getMediaList",param, callback);
    }

    public JSONObject getMediaList(IMediaServerItem mediaServerItem,String app, String stream){
        return getMediaList(mediaServerItem, app, stream,null,  null);
    }

    public JSONObject getMediaList(IMediaServerItem mediaServerItem,RequestCallback callback){
        return sendPost(mediaServerItem, "getMediaList",null, callback);
    }

    public JSONObject getMediaInfo(IMediaServerItem mediaServerItem,String app, String schema, String stream){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("schema",schema);
        param.put("stream",stream);
        param.put("vhost","__defaultVhost__");
        return sendPost(mediaServerItem, "getMediaInfo",param, null);
    }

    public JSONObject getRtpInfo(IMediaServerItem mediaServerItem,String stream_id){
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id",stream_id);
        return sendPost(mediaServerItem, "getRtpInfo",param, null);
    }

    public JSONObject addFFmpegSource(IMediaServerItem mediaServerItem,String src_url, String dst_url, String timeout_ms,
                                      boolean enable_hls, boolean enable_mp4, String ffmpeg_cmd_key){
        logger.info(src_url);
        logger.info(dst_url);
        Map<String, Object> param = new HashMap<>();
        param.put("src_url", src_url);
        param.put("dst_url", dst_url);
        param.put("timeout_ms", timeout_ms);
        param.put("enable_hls", enable_hls);
        param.put("enable_mp4", enable_mp4);
        param.put("ffmpeg_cmd_key", ffmpeg_cmd_key);
        return sendPost(mediaServerItem, "addFFmpegSource",param, null);
    }

    public JSONObject delFFmpegSource(IMediaServerItem mediaServerItem,String key){
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);
        return sendPost(mediaServerItem, "delFFmpegSource",param, null);
    }

    public JSONObject getMediaServerConfig(IMediaServerItem mediaServerItem){
        return sendPost(mediaServerItem, "getServerConfig",null, null);
    }

    public JSONObject setServerConfig(IMediaServerItem mediaServerItem, Map<String, Object> param){
        return sendPost(mediaServerItem,"setServerConfig",param, null);
    }

    public JSONObject openRtpServer(IMediaServerItem mediaServerItem,Map<String, Object> param){
        return sendPost(mediaServerItem, "openRtpServer",param, null);
    }

    public JSONObject closeRtpServer(IMediaServerItem mediaServerItem,Map<String, Object> param) {
        return sendPost(mediaServerItem, "closeRtpServer",param, null);
    }

    public JSONObject listRtpServer(IMediaServerItem mediaServerItem) {
        return sendPost(mediaServerItem, "listRtpServer",null, null);
    }

    public JSONObject startSendRtp(IMediaServerItem mediaServerItem,Map<String, Object> param) {
        return sendPost(mediaServerItem, "startSendRtp",param, null);
    }

    public JSONObject stopSendRtp(IMediaServerItem mediaServerItem,Map<String, Object> param) {
        return sendPost(mediaServerItem, "stopSendRtp",param, null);
    }

    public JSONObject addStreamProxy(IMediaServerItem mediaServerItem,String app, String stream, String url, boolean enable_hls, boolean enable_mp4, String rtp_type) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("url", url);
        param.put("enable_hls", enable_hls?1:0);
        param.put("enable_mp4", enable_mp4?1:0);
        param.put("rtp_type", rtp_type);
        return sendPost(mediaServerItem, "addStreamProxy",param, null);
    }

    public JSONObject closeStreams(IMediaServerItem mediaServerItem,String app, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("force", 1);
        return sendPost(mediaServerItem, "close_streams",param, null);
    }

    public JSONObject getAllSession(IMediaServerItem mediaServerItem) {
        return sendPost(mediaServerItem, "getAllSession",null, null);
    }

    public void kickSessions(IMediaServerItem mediaServerItem, String localPortSStr) {
        Map<String, Object> param = new HashMap<>();
        param.put("local_port", localPortSStr);
        sendPost(mediaServerItem, "kick_sessions",param, null);
    }

    public void getSnap(IMediaServerItem mediaServerItem, String flvUrl, int timeout_sec, int expire_sec, String targetPath, String fileName) {
        Map<String, Object> param = new HashMap<>();
        param.put("url", flvUrl);
        param.put("timeout_sec", timeout_sec);
        param.put("expire_sec", expire_sec);
        sendPostForImg(mediaServerItem, "getSnap",param, targetPath, fileName);
    }
}
