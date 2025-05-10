package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ZLMRESTfulUtils {

    private OkHttpClient client;


    public interface RequestCallback{
        void run(JSONObject response);
    }

    private OkHttpClient getClient(){
        return getClient(null);
    }

    private OkHttpClient getClient(Integer readTimeOut){
        if (client == null) {
            if (readTimeOut == null) {
                readTimeOut = 10;
            }
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            //todo 暂时写死超时时间 均为5s
            // 设置连接超时时间
            httpClientBuilder.connectTimeout(8,TimeUnit.SECONDS);
            // 设置读取超时时间
            httpClientBuilder.readTimeout(readTimeOut,TimeUnit.SECONDS);
            // 设置连接池
            httpClientBuilder.connectionPool(new ConnectionPool(16, 5, TimeUnit.MINUTES));
            if (log.isDebugEnabled()) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                    log.debug("http请求参数：" + message);
                });
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                // OkHttp進行添加攔截器loggingInterceptor
                httpClientBuilder.addInterceptor(logging);
            }
            client = httpClientBuilder.build();
        }
        return client;

    }

    public JSONObject sendPost(MediaServer mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        return sendPost(mediaServerItem, api, param, callback, null);
    }


    public JSONObject sendPost(MediaServer mediaServerItem, String api, Map<String, Object> param, RequestCallback callback, Integer readTimeOut) {
        OkHttpClient client = getClient(readTimeOut);

        if (mediaServerItem == null) {
            return null;
        }
        String url = String.format("http://%s:%s/index/api/%s",  mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api);
        JSONObject responseJSON = new JSONObject();
        //-2自定义流媒体 调用错误码
        responseJSON.put("code",-2);
        responseJSON.put("msg","流媒体调用失败");

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
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String responseStr = responseBody.string();
                            responseJSON = JSON.parseObject(responseStr);
                        }
                    }else {
                        response.close();
                        Objects.requireNonNull(response.body()).close();
                    }
                }catch (IOException e) {
                    log.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));

                    if(e instanceof SocketTimeoutException){
                        //读取超时超时异常
                        log.error(String.format("读取ZLM数据超时失败: %s, %s", url, e.getMessage()));
                    }
                    if(e instanceof ConnectException){
                        //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                        log.error(String.format("连接ZLM连接失败: %s, %s", url, e.getMessage()));
                    }

                }catch (Exception e){
                    log.error(String.format("访问ZLM失败: %s, %s", url, e.getMessage()));
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
                                log.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
                            }

                        }else {
                            response.close();
                            Objects.requireNonNull(response.body()).close();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        log.error(String.format("连接ZLM失败: %s, %s", call.request().toString(), e.getMessage()));

                        if(e instanceof SocketTimeoutException){
                            //读取超时超时异常
                            log.error(String.format("读取ZLM数据失败: %s, %s", call.request().toString(), e.getMessage()));
                        }
                        if(e instanceof ConnectException){
                            //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                            log.error(String.format("连接ZLM失败: %s, %s", call.request().toString(), e.getMessage()));
                        }
                    }
                });
            }



        return responseJSON;
    }

    public void sendGetForImg(MediaServer mediaServerItem, String api, Map<String, Object> params, String targetPath, String fileName) {
        String url = String.format("http://%s:%s/index/api/%s", mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api);
        HttpUrl parseUrl = HttpUrl.parse(url);
        if (parseUrl == null) {
            return;
        }
        HttpUrl.Builder httpBuilder = parseUrl.newBuilder();

        httpBuilder.addQueryParameter("secret", mediaServerItem.getSecret());
        if (params != null) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                httpBuilder.addQueryParameter(param.getKey(), param.getValue().toString());
            }
        }

        Request request = new Request.Builder()
                .url(httpBuilder.build())
                .build();
        if (log.isDebugEnabled()){
            log.debug(request.toString());
        }
        try {
            OkHttpClient client = getClient();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                if (targetPath != null) {
                    File snapFolder = new File(targetPath);
                    if (!snapFolder.exists()) {
                        if (!snapFolder.mkdirs()) {
                            log.warn("{}路径创建失败", snapFolder.getAbsolutePath());
                        }

                    }
                    File snapFile = new File(targetPath + File.separator + fileName);
                    FileOutputStream outStream = new FileOutputStream(snapFile);

                    outStream.write(Objects.requireNonNull(response.body()).bytes());
                    outStream.flush();
                    outStream.close();
                } else {
                    log.error(String.format("[ %s ]请求失败: %s %s", url, response.code(), response.message()));
                }
            } else {
                log.error(String.format("[ %s ]请求失败: %s %s", url, response.code(), response.message()));
            }
            Objects.requireNonNull(response.body()).close();
        } catch (ConnectException e) {
            log.error(String.format("连接ZLM失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            log.info("请检查media配置并确认ZLM已启动...");
        } catch (IOException e) {
            log.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }
    }

    public JSONObject isMediaOnline(MediaServer mediaServerItem, String app, String stream, String schema){
        Map<String, Object> param = new HashMap<>();
        if (app != null) {
            param.put("app",app);
        }
        if (stream != null) {
            param.put("stream",stream);
        }
        if (schema != null) {
            param.put("schema",schema);
        }
        param.put("vhost","__defaultVhost__");
        return sendPost(mediaServerItem, "isMediaOnline", param, null);
    }

    public JSONObject getMediaList(MediaServer mediaServerItem, String app, String stream, String schema, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        if (app != null) {
            param.put("app",app);
        }
        if (stream != null) {
            param.put("stream",stream);
        }
        if (schema != null) {
            param.put("schema",schema);
        }
        param.put("vhost","__defaultVhost__");
        return sendPost(mediaServerItem, "getMediaList",param, callback);
    }

    public JSONObject getMediaList(MediaServer mediaServerItem, String app, String stream){
        return getMediaList(mediaServerItem, app, stream,null,  null);
    }

    public JSONObject getMediaList(MediaServer mediaServerItem, RequestCallback callback){
        return sendPost(mediaServerItem, "getMediaList",null, callback);
    }

    public JSONObject getMediaInfo(MediaServer mediaServerItem, String app, String schema, String stream){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("schema",schema);
        param.put("stream",stream);
        param.put("vhost","__defaultVhost__");
        return sendPost(mediaServerItem, "getMediaInfo",param, null);
    }

    public JSONObject getRtpInfo(MediaServer mediaServerItem, String stream_id){
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id",stream_id);
        return sendPost(mediaServerItem, "getRtpInfo",param, null);
    }

    public JSONObject addFFmpegSource(MediaServer mediaServerItem, String src_url, String dst_url, Integer timeout_sec,
                                      boolean enable_audio, boolean enable_mp4, String ffmpeg_cmd_key){
        log.info(src_url);
        log.info(dst_url);
        Map<String, Object> param = new HashMap<>();
        param.put("src_url", src_url);
        param.put("dst_url", dst_url);
        param.put("timeout_ms", timeout_sec*1000);
        param.put("enable_mp4", enable_mp4);
        param.put("ffmpeg_cmd_key", ffmpeg_cmd_key);
        return sendPost(mediaServerItem, "addFFmpegSource",param, null);
    }

    public JSONObject delFFmpegSource(MediaServer mediaServerItem, String key){
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);
        return sendPost(mediaServerItem, "delFFmpegSource",param, null);
    }

    public JSONObject delStreamProxy(MediaServer mediaServerItem, String key){
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);
        return sendPost(mediaServerItem, "delStreamProxy",param, null);
    }

    public JSONObject getMediaServerConfig(MediaServer mediaServerItem){
        return sendPost(mediaServerItem, "getServerConfig",null, null);
    }

    public JSONObject setServerConfig(MediaServer mediaServerItem, Map<String, Object> param){
        return sendPost(mediaServerItem,"setServerConfig",param, null);
    }

    public JSONObject openRtpServer(MediaServer mediaServerItem, Map<String, Object> param){
        return sendPost(mediaServerItem, "openRtpServer",param, null);
    }

    public JSONObject closeRtpServer(MediaServer mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "closeRtpServer",param, null);
    }

    public void closeRtpServer(MediaServer mediaServerItem, Map<String, Object> param, RequestCallback callback) {
        sendPost(mediaServerItem, "closeRtpServer",param, callback);
    }

    public JSONObject listRtpServer(MediaServer mediaServerItem) {
        return sendPost(mediaServerItem, "listRtpServer",null, null);
    }

    public JSONObject startSendRtp(MediaServer mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "startSendRtp",param, null);
    }

    public JSONObject startSendRtpPassive(MediaServer mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "startSendRtpPassive",param, null);
    }

    public JSONObject startSendRtpPassive(MediaServer mediaServerItem, Map<String, Object> param, RequestCallback callback) {
        return sendPost(mediaServerItem, "startSendRtpPassive",param, callback);
    }

    public JSONObject stopSendRtp(MediaServer mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "stopSendRtp",param, null);
    }

    public JSONObject restartServer(MediaServer mediaServerItem) {
        return sendPost(mediaServerItem, "restartServer",null, null);
    }

    public JSONObject addStreamProxy(MediaServer mediaServerItem, String app, String stream, String url, boolean enable_audio, boolean enable_mp4, String rtp_type, Integer timeOut) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("url", url);
        param.put("enable_mp4", enable_mp4?1:0);
        param.put("enable_audio", enable_audio?1:0);
        param.put("rtp_type", rtp_type);
        param.put("timeout_sec", timeOut);
        // 拉流重试次数,默认为3
        param.put("retry_count", 3);
        return sendPost(mediaServerItem, "addStreamProxy",param, null, 20);
    }

    public JSONObject closeStreams(MediaServer mediaServerItem, String app, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("force", 1);
        return sendPost(mediaServerItem, "close_streams",param, null);
    }

    public JSONObject getAllSession(MediaServer mediaServerItem) {
        return sendPost(mediaServerItem, "getAllSession",null, null);
    }

    public void kickSessions(MediaServer mediaServerItem, String localPortSStr) {
        Map<String, Object> param = new HashMap<>();
        param.put("local_port", localPortSStr);
        sendPost(mediaServerItem, "kick_sessions",param, null);
    }

    public void getSnap(MediaServer mediaServerItem, String streamUrl, int timeout_sec, int expire_sec, String targetPath, String fileName) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("url", streamUrl);
        param.put("timeout_sec", timeout_sec);
        param.put("expire_sec", expire_sec);
        sendGetForImg(mediaServerItem, "getSnap", param, targetPath, fileName);
    }

    public JSONObject pauseRtpCheck(MediaServer mediaServerItem, String streamId) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("stream_id", streamId);
        return sendPost(mediaServerItem, "pauseRtpCheck",param, null);
    }

    public JSONObject resumeRtpCheck(MediaServer mediaServerItem, String streamId) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("stream_id", streamId);
        return sendPost(mediaServerItem, "resumeRtpCheck",param, null);
    }

    public JSONObject connectRtpServer(MediaServer mediaServerItem, String dst_url, int dst_port, String stream_id) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("dst_url", dst_url);
        param.put("dst_port", dst_port);
        param.put("stream_id", stream_id);
        return sendPost(mediaServerItem, "connectRtpServer",param, null);
    }

    public JSONObject updateRtpServerSSRC(MediaServer mediaServerItem, String streamId, String ssrc) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("ssrc", ssrc);
        param.put("stream_id", streamId);
        return sendPost(mediaServerItem, "updateRtpServerSSRC",param, null);
    }

    public JSONObject deleteRecordDirectory(MediaServer mediaServerItem, String app, String stream, String date, String fileName) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("period", date);
        param.put("name", fileName);
        return sendPost(mediaServerItem, "deleteRecordDirectory",param, null);
    }

    public JSONObject loadMP4File(MediaServer mediaServer, String app, String stream, String datePath) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("file_path", datePath);
        param.put("file_repeat", "0");
        return sendPost(mediaServer, "loadMP4File",param, null);
    }

    public JSONObject setRecordSpeed(MediaServer mediaServer, String app, String stream, int speed, String schema) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("speed", speed);
        param.put("schema", schema);
        return sendPost(mediaServer, "setRecordSpeed",param, null);
    }

    public JSONObject seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("stamp", stamp);
        param.put("schema", schema);
        return sendPost(mediaServer, "seekRecordStamp",param, null);
    }
}
