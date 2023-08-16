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

import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AssistRESTfulUtils {

    private final static Logger logger = LoggerFactory.getLogger(AssistRESTfulUtils.class);


    public interface RequestCallback{
        void run(JSONObject response);
    }

    private OkHttpClient getClient(){
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        if (logger.isDebugEnabled()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(message -> {
                logger.debug("http请求参数：" + message);
            });
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            // OkHttp進行添加攔截器loggingInterceptor
            httpClientBuilder.addInterceptor(logging);
        }
        return httpClientBuilder.build();
    }


    public JSONObject sendGet(MediaServerItem mediaServerItem, String api, Map<String, Object> param, RequestCallback callback) {
        OkHttpClient client = getClient();

        if (mediaServerItem == null) {
            return null;
        }
        if (mediaServerItem.getRecordAssistPort() <= 0) {
            logger.warn("未启用Assist服务");
            return null;
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.format("http://%s:%s/%s",  mediaServerItem.getIp(), mediaServerItem.getRecordAssistPort(), api));
        JSONObject responseJSON = null;

        if (param != null && param.keySet().size() > 0) {
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


    public JSONObject fileDuration(MediaServerItem mediaServerItem, String app, String stream, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("stream",stream);
        param.put("recordIng",true);
        return sendGet(mediaServerItem, "api/record/file/duration",param, callback);
    }

    public JSONObject getInfo(MediaServerItem mediaServerItem, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        return sendGet(mediaServerItem, "api/record/info",param, callback);
    }

    public JSONObject addStreamCallInfo(MediaServerItem mediaServerItem, String app, String stream, String callId, RequestCallback callback){
        Map<String, Object> param = new HashMap<>();
        param.put("app",app);
        param.put("stream",stream);
        param.put("callId",callId);
        return sendGet(mediaServerItem, "api/record/addStreamCallInfo",param, callback);
    }

    public JSONObject getDateList(MediaServerItem mediaServerItem, String app, String stream, int year, int month) {
        Map<String, Object> param = new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("year", year);
        param.put("month", month);
        return sendGet(mediaServerItem, "api/record/date/list", param, null);
    }

    public JSONObject getFileList(MediaServerItem mediaServerItem, int page, int count, String app, String stream,
                                  String startTime, String endTime) {
        Map<String, Object> param = new HashMap<>();
        param.put("app", app);
        param.put("stream", stream);
        param.put("page", page);
        param.put("count", count);
        param.put("startTime", startTime);
        param.put("endTime", endTime);
        return sendGet(mediaServerItem, "api/record/file/listWithDate", param, null);
    }

}
