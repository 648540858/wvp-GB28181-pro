package com.genersoft.iot.vmp.media.abl;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.media.abl.bean.ABLResult;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class ABLRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(ABLRESTfulUtils.class);

    private OkHttpClient client;

    public interface RequestCallback{
        void run(String response);
    }
    public interface ResultCallback{
        void run(ABLResult response);
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
            client = httpClientBuilder.build();
        }
        return client;

    }

    public String sendPost(MediaServer mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        return sendPost(mediaServerItem, api, param, callback, null);
    }


    public String sendPost(MediaServer mediaServerItem, String api, Map<String, Object> param, RequestCallback callback, Integer readTimeOut) {
        OkHttpClient client = getClient(readTimeOut);

        if (mediaServerItem == null) {
            return null;
        }
        String url = String.format("http://%s:%s/index/api/%s",  mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api);
        String result = null;

        FormBody.Builder builder = new FormBody.Builder();
        builder.add("secret",mediaServerItem.getSecret());
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
                    logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));

                    if(e instanceof SocketTimeoutException){
                        //读取超时超时异常
                        logger.error(String.format("读取ABL数据超时失败: %s, %s", url, e.getMessage()));
                    }
                    if(e instanceof ConnectException){
                        //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                        logger.error(String.format("连接ABL连接失败: %s, %s", url, e.getMessage()));
                    }

                }catch (Exception e){
                    logger.error(String.format("访问ABL失败: %s, %s", url, e.getMessage()));
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
                                logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
                            }

                        }else {
                            response.close();
                            Objects.requireNonNull(response.body()).close();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        logger.error(String.format("连接ABL失败: %s, %s", call.request().toString(), e.getMessage()));

                        if(e instanceof SocketTimeoutException){
                            //读取超时超时异常
                            logger.error(String.format("读取ABL数据失败: %s, %s", call.request().toString(), e.getMessage()));
                        }
                        if(e instanceof ConnectException){
                            //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                            logger.error(String.format("连接ABL失败: %s, %s", call.request().toString(), e.getMessage()));
                        }
                    }
                });
            }
        return result;
    }

    public String sendGet(MediaServer mediaServerItem, String api, Map<String, Object> param) {
        OkHttpClient client = getClient();

        if (mediaServerItem == null) {
            return null;
        }
        String result = null;
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append(String.format("http://%s:%s/index/api/%s",  mediaServerItem.getIp(), mediaServerItem.getHttpPort(), api));
        if (param != null && !param.keySet().isEmpty()) {
            stringBuffer.append("?secret=").append(mediaServerItem.getSecret()).append("&");
            int index = 1;
            for (String key : param.keySet()){
                if (param.get(key) != null) {
                    stringBuffer.append(key + "=" + param.get(key));
                    if (index < param.size()) {
                        stringBuffer.append("&");
                    }
                }
                index++;
            }
        }
        String url = stringBuffer.toString();
        logger.info("[访问ABL]： {}", url);
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();
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
        } catch (ConnectException e) {
            logger.error(String.format("连接ABL失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            logger.info("请检查media配置并确认ABL已启动...");
        }catch (IOException e) {
            logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }
        return result;
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
            logger.error(String.format("连接ABL失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            logger.info("请检查media配置并确认ABL已启动...");
        } catch (IOException e) {
            logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }
    }

    public void sendGetForImgForUrl(String url,  String targetPath, String fileName) {
        HttpUrl parseUrl = HttpUrl.parse(url);
        if (parseUrl == null) {
            return;
        }
        HttpUrl.Builder httpBuilder = parseUrl.newBuilder();

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
            logger.error(String.format("连接ABL失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
            logger.info("请检查media配置并确认ABL已启动...");
        } catch (IOException e) {
            logger.error(String.format("[ %s ]请求失败: %s", url, e.getMessage()));
        }
    }

    public Integer openRtpServer(MediaServer mediaServer, String app, String stream, int payload, Integer port, Integer tcpMode, Integer disableAudio, Boolean record, Boolean isJtt) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "_defaultVhost_");
        param.put("app", app);
        param.put("stream_id", stream);
        param.put("payload", payload);
        if (isJtt) {
            // 1 PS 国标gb28181, 默认为1、
            // 2 ES 视频支持 H246\H265，音频只支持G711A、G711U 、AAC
            // 3 XHB (一家公司的打包格式) 只支持视频，音频不能加入打包
            // 4 、Jt1078（2016版本）码流接入
            param.put("RtpPayloadDataType", 4);
            param.put("jtt1078_version", "2019");
        }
        if (port != null) {
            param.put("port", port);
        }else {
            param.put("port", 0);
        }
        if (tcpMode != null) {
            param.put("enable_tcp", tcpMode);
        }
        if (disableAudio != null) {
            param.put("disableAudio", disableAudio);
        }
        if (record != null && record) {
            param.put("enable_mp4", 1);
        }

        String response = sendPost(mediaServer, "openRtpServer", param, null);
        if (response == null) {
            return 0;
        }else {
            ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
            if (ablResult.getCode() == 0) {
                return ablResult.getPort();
            }else {
                return 0;
            }
        }
    }

    public ABLResult closeStreams(MediaServer mediaServerItem, String app, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("force", 1);
        String response = sendPost(mediaServerItem, "close_streams", param, null);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult getServerConfig(MediaServer mediaServerItem){
        String response = sendPost(mediaServerItem, "getServerConfig", null, null);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult setConfigParamValue(MediaServer mediaServerItem, String key, Object value){
        Map<String, Object> param =  new HashMap<>();
        param.put("key", key);
        param.put("value", value);
        String response = sendGet(mediaServerItem, "setConfigParamValue", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public void stopSendRtp(MediaServer mediaServer,String key) {
        Map<String, Object> param =  new HashMap<>();
        param.put("key", key);
        sendPost(mediaServer,"stopSendRtp", param, null);
    }

    public ABLResult getMediaList(MediaServer mediaServer, String app, String stream) {
        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        if (stream != null) {
            param.put("stream", stream);
        }

        String response = sendGet(mediaServer, "getMediaList", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult queryRecordList(MediaServer mediaServer, String app, String stream, String startTime, String endTime) {
        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("starttime", startTime);
        param.put("endtime", endTime);
        String response = sendGet(mediaServer, "queryRecordList", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public void getSnap(MediaServer mediaServer, String app, String stream, int timeoutSec, String path, String fileName) {
        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("timeout_sec", timeoutSec);
        param.put("vhost", "_defaultVhost_");
//        JSONObject jsonObject = sendPost(mediaServer, "getSnap", param, null);
//        if (jsonObject != null && jsonObject.getInteger("code") == 0) {
//            String url = jsonObject.getString("url");
//            sendGetForImgForUrl(url, path, fileName);
//        }
        sendGetForImg(mediaServer, "getSnap", param, path, fileName);

    }

    public ABLResult addStreamProxy(MediaServer mediaServer, String app, String stream, String url, boolean disableAudio, boolean enableMp4, String rtpType, Integer timeout) {
        try {
            url = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"url编码失败");
        }

        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("url", url);
        param.put("disableAudio", disableAudio? "1" : "0");
        param.put("enable_mp4", enableMp4 ? "1" : "0");
        // TODO rtpType timeout 尚不支持
        String response = sendGet(mediaServer, "addStreamProxy", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult addFFmpegProxy(MediaServer mediaServer, String app, String stream, String url, boolean disableAudio, boolean enableMp4, String rtpType, Integer timeout) {
        try {
            url = URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(),"url编码失败");
        }
        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("url", url);
        param.put("disableAudio", disableAudio);
        param.put("enable_mp4", enableMp4);
        // TODO rtpType timeout 尚不支持
        String response = sendGet(mediaServer, "addFFmpegProxy", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult delStreamProxy(MediaServer mediaServer, String streamKey) {
        Map<String, Object> param =  new HashMap<>();
        param.put("key", streamKey);
        String response = sendGet(mediaServer, "delStreamProxy", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult delFFmpegProxy(MediaServer mediaServer, String streamKey) {
        Map<String, Object> param =  new HashMap<>();
        param.put("key", streamKey);
        String response = sendGet(mediaServer, "delFFmpegProxy", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult pauseRtpServer(MediaServer mediaServer, String streamKey) {
        Map<String, Object> param =  new HashMap<>();
        param.put("key", streamKey);
        String response = sendGet(mediaServer, "pauseRtpServer", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult resumeRtpServer(MediaServer mediaServer, String streamKey) {
        Map<String, Object> param =  new HashMap<>();
        param.put("key", streamKey);
        String response = sendGet(mediaServer, "resumeRtpServer", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

    public ABLResult controlRecordPlay(MediaServer mediaServer, String app, String stream, String command, String value) {
        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("command", command);
        param.put("value", value);
        String response = sendGet(mediaServer, "controlRecordPlay", param);
        ABLResult ablResult = JSON.parseObject(response, ABLResult.class);
        if (ablResult == null) {
            return ABLResult.getFailForMediaServer();
        }else {
            return ablResult;
        }
    }

}
