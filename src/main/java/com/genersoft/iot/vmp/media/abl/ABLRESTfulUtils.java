package com.genersoft.iot.vmp.media.abl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.bean.MediaServer;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Component
public class ABLRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(ABLRESTfulUtils.class);

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
                        System.out.println( 2222);
                        System.out.println( response.code());
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



        return responseJSON;
    }

    public JSONObject sendGet(MediaServer mediaServerItem, String api, Map<String, Object> param) {
        OkHttpClient client = getClient();

        if (mediaServerItem == null) {
            return null;
        }
        JSONObject responseJSON = null;
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
                    String responseStr = responseBody.string();
                    responseJSON = JSON.parseObject(responseStr);
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


    public Integer openRtpServer(MediaServer mediaServer, String app, String stream, int payload, Integer port, Integer tcpMode, Integer disableAudio, Boolean record) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "_defaultVhost_");
        param.put("app", app);
        param.put("stream_id", stream);
        param.put("payload", payload);
        if (port != null) {
            param.put("port", port);
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

        JSONObject jsonObject = sendPost(mediaServer, "openRtpServer", param, null);
        if (jsonObject.getInteger("code") == 0) {
            return jsonObject.getInteger("port");
        }else {
            return 0;
        }
    }

    public JSONObject closeStreams(MediaServer mediaServerItem, String app, String stream) {
        Map<String, Object> param = new HashMap<>();
        param.put("vhost", "__defaultVhost__");
        param.put("app", app);
        param.put("stream", stream);
        param.put("force", 1);
        return sendPost(mediaServerItem, "close_streams",param, null);
    }

    public JSONObject getServerConfig(MediaServer mediaServerItem){
        return sendPost(mediaServerItem, "getServerConfig",null, null);
    }

    public JSONObject setConfigParamValue(MediaServer mediaServerItem, String key, Object value){
        Map<String, Object> param =  new HashMap<>();
        param.put("key", key);
        param.put("value", value);
        return sendGet(mediaServerItem,"setConfigParamValue", param);
    }

    public void stopSendRtp(MediaServer mediaServer,String key) {
        Map<String, Object> param =  new HashMap<>();
        param.put("key", key);
        sendPost(mediaServer,"stopSendRtp", param, null);
    }

    public JSONObject getMediaList(MediaServer mediaServer, String app, String stream) {
        Map<String, Object> param =  new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        return sendPost(mediaServer,"getMediaList", param, null);
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

}
