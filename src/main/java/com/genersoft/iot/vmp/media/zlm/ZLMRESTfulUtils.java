package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServerItem;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class ZLMRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(ZLMRESTfulUtils.class);

    private OkHttpClient client;



    public interface RequestCallback{
        void run(JSONObject response);
    }

    private OkHttpClient getClient(){
        if (client == null) {
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            //todo 暂时写死超时时间 均为5s
            // 设置连接超时时间
            httpClientBuilder.connectTimeout(5,TimeUnit.SECONDS);
            // 设置读取超时时间
            httpClientBuilder.readTimeout(10,TimeUnit.SECONDS);
            // 设置连接池
            httpClientBuilder.connectionPool(new ConnectionPool(16, 5, TimeUnit.MINUTES));
            if (logger.isDebugEnabled()) {
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                    logger.debug("http请求参数：" + message);
                });
                logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
                // OkHttp進行添加攔截器loggingInterceptor
                httpClientBuilder.addInterceptor(logging);
            }
            client = httpClientBuilder.build();
        }
        return client;

    }


    public JSONObject sendPost(MediaServerItem mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        OkHttpClient client = getClient();

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
                    logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));

                    if(e instanceof SocketTimeoutException){
                        //读取超时超时异常
                        logger.error(String.format("读取ZLM数据失败: %s, %s", url, e.getMessage()));
                    }
                    if(e instanceof ConnectException){
                        //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                        logger.error(String.format("连接ZLM失败: %s, %s", url, e.getMessage()));
                    }

                }catch (Exception e){
                    logger.error(String.format("访问ZLM失败: %s, %s", url, e.getMessage()));
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

                        }else {
                            response.close();
                            Objects.requireNonNull(response.body()).close();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        logger.error(String.format("连接ZLM失败: %s, %s", call.request().toString(), e.getMessage()));

                        if(e instanceof SocketTimeoutException){
                            //读取超时超时异常
                            logger.error(String.format("读取ZLM数据失败: %s, %s", call.request().toString(), e.getMessage()));
                        }
                        if(e instanceof ConnectException){
                            //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                            logger.error(String.format("连接ZLM失败: %s, %s", call.request().toString(), e.getMessage()));
                        }
                    }
                });
            }



        return responseJSON;
    }

    public void sendGetForImg(MediaServerItem mediaServerItem, String api, Map<String, Object> params, String targetPath, String fileName) {
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
        logger.info(request.toString());
        try {
            OkHttpClient client = getClient();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                if (targetPath != null) {
                    File snapFolder = new File(targetPath);
                    if (!snapFolder.exists()) {
                        if (!snapFolder.mkdirs()) {
                            logger.warn("{}路径创建失败", snapFolder.getAbsolutePath());
                        }

                    }
                    File snapFile = new File(targetPath + File.separator + fileName);
                    FileOutputStream outStream = new FileOutputStream(snapFile);

                    outStream.write(Objects.requireNonNull(response.body()).bytes());
                    outStream.flush();
                    outStream.close();
                } else {
                    logger.error(String.format("[ %s ]请求失败: %s %s", url, response.code(), response.message()));
                }
            } else {
                logger.error(String.format("[ %s ]请求失败: %s %s", url, response.code(), response.message()));
            }
            Objects.requireNonNull(response.body()).close();
        } catch (ConnectException e) {
            logger.error(String.format("连接ZLM失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            logger.info("请检查media配置并确认ZLM已启动...");
        } catch (IOException e) {
            logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }
    }

    public JSONObject getMediaList(MediaServerItem mediaServerItem, String app, String stream, String schema, RequestCallback callback){
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

    public JSONObject getMediaList(MediaServerItem mediaServerItem, String app, String stream){
        return getMediaList(mediaServerItem, app, stream,null,  null);
    }

    public JSONObject getMediaList(MediaServerItem mediaServerItem, RequestCallback callback){
        return sendPost(mediaServerItem, "getMediaList",null, callback);
    }

    public JSONObject getMediaInfo(MediaServerItem mediaServerItem, String app, String schema, String stream){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("schema",schema);
        param.put("stream",stream);
        param.put("vhost","__defaultVhost__");
        return sendPost(mediaServerItem, "getMediaInfo",param, null);
    }

    public JSONObject getRtpInfo(MediaServerItem mediaServerItem, String stream_id){
        Map<String, Object> param = new HashMap<>();
        param.put("stream_id",stream_id);
        return sendPost(mediaServerItem, "getRtpInfo",param, null);
    }

    public JSONObject addFFmpegSource(MediaServerItem mediaServerItem, String src_url, String dst_url, String timeout_ms,
                                      boolean enable_audio, boolean enable_mp4, String ffmpeg_cmd_key){
        logger.info(src_url);
        logger.info(dst_url);
        Map<String, Object> param = new HashMap<>();
        param.put("src_url", src_url);
        param.put("dst_url", dst_url);
        param.put("timeout_ms", timeout_ms);
        param.put("enable_mp4", enable_mp4);
        param.put("ffmpeg_cmd_key", ffmpeg_cmd_key);
        return sendPost(mediaServerItem, "addFFmpegSource",param, null);
    }

    public JSONObject delFFmpegSource(MediaServerItem mediaServerItem, String key){
        Map<String, Object> param = new HashMap<>();
        param.put("key", key);
        return sendPost(mediaServerItem, "delFFmpegSource",param, null);
    }

    public JSONObject getMediaServerConfig(MediaServerItem mediaServerItem){
        return sendPost(mediaServerItem, "getServerConfig",null, null);
    }

    public JSONObject setServerConfig(MediaServerItem mediaServerItem, Map<String, Object> param){
        return sendPost(mediaServerItem,"setServerConfig",param, null);
    }

    public JSONObject openRtpServer(MediaServerItem mediaServerItem, Map<String, Object> param){
        return sendPost(mediaServerItem, "openRtpServer",param, null);
    }

    public JSONObject closeRtpServer(MediaServerItem mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "closeRtpServer",param, null);
    }

    public void closeRtpServer(MediaServerItem mediaServerItem, Map<String, Object> param, RequestCallback callback) {
        sendPost(mediaServerItem, "closeRtpServer",param, callback);
    }

    public JSONObject listRtpServer(MediaServerItem mediaServerItem) {
        return sendPost(mediaServerItem, "listRtpServer",null, null);
    }

    public JSONObject startSendRtp(MediaServerItem mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "startSendRtp",param, null);
    }

    public JSONObject startSendRtpPassive(MediaServerItem mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "startSendRtpPassive",param, null);
    }

    public JSONObject stopSendRtp(MediaServerItem mediaServerItem, Map<String, Object> param) {
        return sendPost(mediaServerItem, "stopSendRtp",param, null);
    }

    public JSONObject restartServer(MediaServerItem mediaServerItem) {
        return sendPost(mediaServerItem, "restartServer",null, null);
    }

    public JSONObject addStreamProxy(MediaServerItem mediaServerItem, String app, String stream, String url, boolean enable_audio, boolean enable_mp4, String rtp_type) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("url", url);
        param.put("enable_mp4", enable_mp4?1:0);
        param.put("enable_audio", enable_audio?1:0);
        param.put("rtp_type", rtp_type);
        return sendPost(mediaServerItem, "addStreamProxy",param, null);
    }

    public JSONObject closeStreams(MediaServerItem mediaServerItem, String app, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("force", 1);
        return sendPost(mediaServerItem, "close_streams",param, null);
    }

    public JSONObject getAllSession(MediaServerItem mediaServerItem) {
        return sendPost(mediaServerItem, "getAllSession",null, null);
    }

    public void kickSessions(MediaServerItem mediaServerItem, String localPortSStr) {
        Map<String, Object> param = new HashMap<>();
        param.put("local_port", localPortSStr);
        sendPost(mediaServerItem, "kick_sessions",param, null);
    }

    public void getSnap(MediaServerItem mediaServerItem, String streamUrl, int timeout_sec, int expire_sec, String targetPath, String fileName) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("url", streamUrl);
        param.put("timeout_sec", timeout_sec);
        param.put("expire_sec", expire_sec);
        sendGetForImg(mediaServerItem, "getSnap", param, targetPath, fileName);
    }

    public JSONObject pauseRtpCheck(MediaServerItem mediaServerItem, String streamId) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("stream_id", streamId);
        return sendPost(mediaServerItem, "pauseRtpCheck",param, null);
    }

    public JSONObject resumeRtpCheck(MediaServerItem mediaServerItem, String streamId) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("stream_id", streamId);
        return sendPost(mediaServerItem, "resumeRtpCheck",param, null);
    }

    public JSONObject connectRtpServer(MediaServerItem mediaServerItem, String dst_url, int dst_port, String stream_id) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("dst_url", dst_url);
        param.put("dst_port", dst_port);
        param.put("stream_id", stream_id);
        return sendPost(mediaServerItem, "connectRtpServer",param, null);
    }

    public JSONObject updateRtpServerSSRC(MediaServerItem mediaServerItem, String streamId, String ssrc) {
        Map<String, Object> param = new HashMap<>(1);
        param.put("ssrc", ssrc);
        param.put("stream_id", streamId);
        return sendPost(mediaServerItem, "updateRtpServerSSRC",param, null);
    }
}
