package com.genersoft.iot.vmp.media.zlm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.media.zlm.dto.MediaServer;
import com.genersoft.iot.vmp.utils.SSLSocketClientUtil;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class AssistRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(AssistRESTfulUtils.class);


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
            // 设置连接超时时间
            httpClientBuilder.connectTimeout(8, TimeUnit.SECONDS);
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
            X509TrustManager manager = SSLSocketClientUtil.getX509TrustManager();
            // 设置ssl
            httpClientBuilder.sslSocketFactory(SSLSocketClientUtil.getSocketFactory(manager), manager);
            httpClientBuilder.hostnameVerifier(SSLSocketClientUtil.getHostnameVerifier());//忽略校验
            client = httpClientBuilder.build();
        }
        return client;

    }


    public JSONObject sendGet(MediaServer mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        OkHttpClient client = getClient();

        if (mediaServerItem == null) {
            return null;
        }
        if (mediaServerItem.getRecordAssistPort() <= 0) {
            logger.warn("未启用Assist服务");
            return null;
        }
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append(api);
        JSONObject responseJSON = null;

        if (param != null && !param.keySet().isEmpty()) {
            stringBuffer.append("?");
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
        logger.info("[访问assist]： {}", url);
        Request request = new Request.Builder()
                .get()
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
                } catch (ConnectException e) {
                    logger.error(String.format("连接Assist失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
                    logger.info("请检查media配置并确认Assist已启动...");
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

                        }else {
                            response.close();
                            Objects.requireNonNull(response.body()).close();
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        logger.error(String.format("连接Assist失败: %s, %s", e.getCause().getMessage(), e.getMessage()));
                        logger.info("请检查media配置并确认Assist已启动...");
                    }
                });
            }



        return responseJSON;
    }

    public JSONObject sendPost(MediaServer mediaServerItem, String url,
                               JSONObject param, ZLMRESTfulUtils.RequestCallback callback,
                               Integer readTimeOut) {
        OkHttpClient client = getClient(readTimeOut);

        if (mediaServerItem == null) {
            return null;
        }
        logger.info("[访问assist]： {}, 参数： {}", url, param);
        JSONObject responseJSON = new JSONObject();
        //-2自定义流媒体 调用错误码
        responseJSON.put("code",-2);
        responseJSON.put("msg","ASSIST调用失败");

        RequestBody requestBodyJson = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), param.toString());

        Request request = new Request.Builder()
                .post(requestBodyJson)
                .url(url)
                .addHeader("Content-Type", "application/json")
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
                logger.error(String.format("[ %s ]ASSIST请求失败: %s", url, e.getMessage()));

                if(e instanceof SocketTimeoutException){
                    //读取超时超时异常
                    logger.error(String.format("读取ASSIST数据失败: %s, %s", url, e.getMessage()));
                }
                if(e instanceof ConnectException){
                    //判断连接异常，我这里是报Failed to connect to 10.7.5.144
                    logger.error(String.format("连接ASSIST失败: %s, %s", url, e.getMessage()));
                }

            }catch (Exception e){
                logger.error(String.format("访问ASSIST失败: %s, %s", url, e.getMessage()));
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

    public JSONObject getInfo(MediaServer mediaServerItem, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        return sendGet(mediaServerItem, "api/record/info",param, callback);
    }

    public JSONObject addTask(MediaServer mediaServerItem, String app, String stream, String startTime,
                              String endTime, String callId, List<String> filePathList, String remoteHost) {

        JSONObject videoTaskInfoJSON = new JSONObject();
        videoTaskInfoJSON.put("app", app);
        videoTaskInfoJSON.put("stream", stream);
        videoTaskInfoJSON.put("startTime", startTime);
        videoTaskInfoJSON.put("endTime", endTime);
        videoTaskInfoJSON.put("callId", callId);
        videoTaskInfoJSON.put("filePathList", filePathList);
        if (!ObjectUtils.isEmpty(remoteHost)) {
            videoTaskInfoJSON.put("remoteHost", remoteHost);
        }
        String urlStr = String.format("%s/api/record/file/download/task/add",  remoteHost);;
        return sendPost(mediaServerItem, urlStr, videoTaskInfoJSON, null, 30);
    }

    public JSONObject queryTaskList(MediaServer mediaServerItem, String app, String stream, String callId,
                                    String taskId, Boolean isEnd, String scheme) {
        Map<String, Object> param = new HashMap<>();
        if (!ObjectUtils.isEmpty(app)) {
            param.put("app", app);
        }
        if (!ObjectUtils.isEmpty(stream)) {
            param.put("stream", stream);
        }
        if (!ObjectUtils.isEmpty(callId)) {
            param.put("callId", callId);
        }
        if (!ObjectUtils.isEmpty(taskId)) {
            param.put("taskId", taskId);
        }
        if (!ObjectUtils.isEmpty(isEnd)) {
            param.put("isEnd", isEnd);
        }
        String urlStr = String.format("%s://%s:%s/api/record/file/download/task/list",
                scheme, mediaServerItem.getIp(), mediaServerItem.getRecordAssistPort());;
        return sendGet(mediaServerItem, urlStr, param, null);
    }
}
