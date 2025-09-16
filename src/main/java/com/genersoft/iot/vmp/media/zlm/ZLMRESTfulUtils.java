package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.media.zlm.dto.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ZLMRESTfulUtils {

    private OkHttpClient client;


    public interface RequestCallback{
        void run(String response);
    }
    public interface ResultCallback{
        void run(ZLMResult<?> response);
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

    public String sendPost(MediaServer mediaServer, String api, Map<String, Object> param, RequestCallback callback) {
        return sendPost(mediaServer, api, param, callback, null);
    }


    public String sendPost(MediaServer mediaServer, String api, Map<String, Object> param, RequestCallback callback, Integer readTimeOut) {
        OkHttpClient client = getClient(readTimeOut);

        if (mediaServer == null) {
            return null;
        }
        String url = String.format("http://%s:%s/index/api/%s",  mediaServer.getIp(), mediaServer.getHttpPort(), api);
        String result = null;
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("secret",mediaServer.getSecret());
        if (param != null && !param.isEmpty()) {
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
                            result = responseBody.string();
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
                                callback.run(responseStr);
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

        return result;
    }

    public void sendGetForImg(MediaServer mediaServer, String api, Map<String, Object> params, String targetPath, String fileName) {
        String url = String.format("http://%s:%s/index/api/%s", mediaServer.getIp(), mediaServer.getHttpPort(), api);
        HttpUrl parseUrl = HttpUrl.parse(url);
        if (parseUrl == null) {
            return;
        }
        HttpUrl.Builder httpBuilder = parseUrl.newBuilder();

        httpBuilder.addQueryParameter("secret", mediaServer.getSecret());
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

    public ZLMResult<?> isMediaOnline(MediaServer mediaServer, String app, String stream, String schema){
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
        String response = sendPost(mediaServer, "isMediaOnline", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<JSONArray> getMediaList(MediaServer mediaServer, String app, String stream, String schema, ResultCallback callback){
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
        String response = sendPost(mediaServer, "getMediaList",param, (responseStr -> {
            if (callback == null) {
                return;
            }
            if (responseStr == null) {
                callback.run(ZLMResult.getFailForMediaServer());
            }else {
                ZLMResult<JSONArray> zlmResult = JSON.parseObject(responseStr, new TypeReference<ZLMResult<JSONArray>>() {});
                if (zlmResult == null) {
                    callback.run(ZLMResult.getFailForMediaServer());
                }else {
                    callback.run(zlmResult);
                }

            }
        }));
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<JSONArray> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<JSONArray>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<JSONArray> getMediaList(MediaServer mediaServer, String app, String stream){
        return getMediaList(mediaServer, app, stream,null,  null);
    }

    public ZLMResult<JSONObject> getMediaInfo(MediaServer mediaServer, String app, String schema, String stream){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("schema",schema);
        param.put("stream",stream);
        param.put("vhost","__defaultVhost__");

        String response = sendPost(mediaServer, "getMediaInfo",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            JSONObject jsonObject = JSON.parseObject(response);
            if (jsonObject == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                ZLMResult<JSONObject> zlmResult = new ZLMResult<>();
                zlmResult.setCode(0);
                zlmResult.setData(jsonObject);
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> getRtpInfo(MediaServer mediaServer, String stream_id){
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id",stream_id);
        String response = sendPost(mediaServer, "getRtpInfo",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<StreamProxyResult> addFFmpegSource(MediaServer mediaServer, String src_url, String dst_url, Integer timeout_sec,
                                      boolean enable_audio, boolean enable_mp4, String ffmpeg_cmd_key){
        Map<String, Object> param = new HashMap<>();
        param.put("src_url", src_url);
        param.put("dst_url", dst_url);
        param.put("timeout_ms", timeout_sec*1000);
        param.put("enable_mp4", enable_mp4);
        param.put("ffmpeg_cmd_key", ffmpeg_cmd_key);

        String response = sendPost(mediaServer, "addFFmpegSource",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<StreamProxyResult> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<StreamProxyResult>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<FlagData> delFFmpegSource(MediaServer mediaServer, String key){
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);

        String response = sendPost(mediaServer, "delFFmpegSource",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<FlagData> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<FlagData>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<FlagData> delStreamProxy(MediaServer mediaServer, String key){
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);
        String response = sendPost(mediaServer, "delStreamProxy",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<FlagData> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<FlagData>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<List<JSONObject>> getMediaServerConfig(MediaServer mediaServer  ){

        String response = sendPost(mediaServer, "getServerConfig",null, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<List<JSONObject>> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<List<JSONObject>>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> setServerConfig(MediaServer mediaServer, Map<String, Object> param){
        String response = sendPost(mediaServer, "setServerConfig",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> openRtpServer(MediaServer mediaServer, Map<String, Object> param){
        String response = sendPost(mediaServer, "openRtpServer",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> closeRtpServer(MediaServer mediaServer, Map<String, Object> param) {
        String response = sendPost(mediaServer, "closeRtpServer",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public void closeRtpServer(MediaServer mediaServer, Map<String, Object> param, ResultCallback callback) {
        sendPost(mediaServer, "closeRtpServer",param, (response -> {
            if (callback == null) {
                return;
            }
            if (response == null) {
                callback.run(ZLMResult.getFailForMediaServer());
            }else {
                callback.run(JSON.parseObject(response, ZLMResult.class));
            }
        }));

    }

    public ZLMResult<List<RtpServerResult>> listRtpServer(MediaServer mediaServer) {
        String response = sendPost(mediaServer, "listRtpServer",null, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<List<RtpServerResult>> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<List<RtpServerResult>>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> startSendRtp(MediaServer mediaServer, Map<String, Object> param) {
        String response = sendPost(mediaServer, "startSendRtp",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> startSendRtpPassive(MediaServer mediaServer, Map<String, Object> param) {
        String response = sendPost(mediaServer, "startSendRtpPassive",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> startSendRtpPassive(MediaServer mediaServer, Map<String, Object> param, ResultCallback callback) {
        String response = sendPost(mediaServer, "startSendRtpPassive",param, (responseStr -> {
            if (callback == null) {
                return;
            }
            if (responseStr == null) {
                callback.run(ZLMResult.getFailForMediaServer());
            }else {
                ZLMResult<?> zlmResult = JSON.parseObject(responseStr, ZLMResult.class);
                if (zlmResult == null) {
                    callback.run(ZLMResult.getFailForMediaServer());
                }else {
                    callback.run(zlmResult);
                }
            }
        }));
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> startSendRtpTalk(MediaServer mediaServer, Map<String, Object> param, ResultCallback callback) {
        String response = sendPost(mediaServer, "startSendRtpTalk",param, (responseStr -> {
            if (callback == null) {
                return;
            }
            if (responseStr == null) {
                callback.run(ZLMResult.getFailForMediaServer());
            }else {
                callback.run(JSON.parseObject(responseStr, ZLMResult.class));
            }
        }));
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> stopSendRtp(MediaServer mediaServer, Map<String, Object> param) {
        String response = sendPost(mediaServer, "stopSendRtp",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> restartServer(MediaServer mediaServer) {
        String response = sendPost(mediaServer, "restartServer",null, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<StreamProxyResult> addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean enable_audio, boolean enable_mp4, String rtp_type, Integer timeOut) {
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

        String response = sendPost(mediaServer, "addStreamProxy",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<StreamProxyResult> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<StreamProxyResult>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<FlagData> closeStreams(MediaServer mediaServer, String app, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("force", 1);

        String response = sendPost(mediaServer, "close_streams",param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<FlagData> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<FlagData>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<List<SessionData>> getAllSession(MediaServer mediaServer) {
        String response = sendPost(mediaServer, "getAllSession",null, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<List<SessionData>> zlmResult = JSON.parseObject(response, new TypeReference<ZLMResult<List<SessionData>>>() {});
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public void kickSessions(MediaServer mediaServer, String localPortSStr) {
        Map<String, Object> param = new HashMap<>();
        param.put("local_port", localPortSStr);
        sendPost(mediaServer, "kick_sessions",param, null);
    }

    public void getSnap(MediaServer mediaServer, String streamUrl, int timeout_sec, int expire_sec, String targetPath, String fileName) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("url", streamUrl);
        param.put("timeout_sec", timeout_sec);
        param.put("expire_sec", expire_sec);
        param.put("async", 1);
        sendGetForImg(mediaServer, "getSnap", param, targetPath, fileName);
    }

    public ZLMResult<?> pauseRtpCheck(MediaServer mediaServer, String streamId) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("stream_id", streamId);
        String response = sendPost(mediaServer, "pauseRtpCheck", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> resumeRtpCheck(MediaServer mediaServer, String streamId) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("stream_id", streamId);
        String response = sendPost(mediaServer, "resumeRtpCheck", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> connectRtpServer(MediaServer mediaServer, String dst_url, int dst_port, String stream_id) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("dst_url", dst_url);
        param.put("dst_port", dst_port);
        param.put("stream_id", stream_id);
        String response = sendPost(mediaServer, "connectRtpServer", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> updateRtpServerSSRC(MediaServer mediaServer, String streamId, String ssrc) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("ssrc", ssrc);
        param.put("stream_id", streamId);

        String response = sendPost(mediaServer, "updateRtpServerSSRC", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> deleteRecordDirectory(MediaServer mediaServer, String app, String stream, String date, String fileName) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("period", date);
        param.put("name", fileName);
        String response = sendPost(mediaServer, "deleteRecordDirectory", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> loadMP4File(MediaServer mediaServer, String app, String stream, String datePath) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("file_path", datePath);
        param.put("file_repeat", "0");
        String response = sendPost(mediaServer, "loadMP4File", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> setRecordSpeed(MediaServer mediaServer, String app, String stream, int speed, String schema) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("speed", speed);
        param.put("schema", schema);
        String response = sendPost(mediaServer, "setRecordSpeed", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }

    public ZLMResult<?> seekRecordStamp(MediaServer mediaServer, String app, String stream, Double stamp, String schema) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        BigDecimal bigDecimal = new BigDecimal(stamp);
        param.put("stamp", bigDecimal);
        param.put("schema", schema);

        String response = sendPost(mediaServer, "seekRecordStamp", param, null);
        if (response == null) {
            return ZLMResult.getFailForMediaServer();
        }else {
            ZLMResult<?> zlmResult = JSON.parseObject(response, ZLMResult.class);
            if (zlmResult == null) {
                return ZLMResult.getFailForMediaServer();
            }else {
                return zlmResult;
            }
        }
    }
}
