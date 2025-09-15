package com.genersoft.iot.vmp.media.zlm.hookCallback;

import com.alibaba.fastjson2.JSONObject;
import com.genersoft.iot.vmp.conf.UserSetting;
import com.genersoft.iot.vmp.utils.SSLSocketClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

/**
 * @description: 针对 ZLMediaServer的hook事件回调
 * @author: 尚昊
 * @date: 2025年9月13日18:06:22
 */
@Slf4j
@Component
public class ZLMHttpHookCallback {

    // 默认连接超时时间（单位：秒）
    // 超过客户端与服务器建立连接的最大等待时间
    private static final int DEFAULT_CONNECT_TIMEOUT = 8;

    // 默认读取超时时间（单位：秒）
    // 指从服务器读取数据的最大等待时间
    private static final int DEFAULT_READ_TIMEOUT = 10;

    // 连接池最大连接数
    // 控制同时保持的最大HTTP连接数量，避免连接过多导致资源耗尽
    private static final int CONNECTION_POOL_SIZE = 16;

    // 连接池保持存活时间（单位：分钟）
    // 超过此时间的空闲连接将被自动关闭，合理设置可减少资源浪费
    private static final int CONNECTION_KEEP_ALIVE = 5;
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    @Autowired
    private UserSetting userSetting;

    private OkHttpClient client;

    // 初始化方法，替代懒加载，避免线程安全问题
    @PostConstruct
    public void init() {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(CONNECTION_POOL_SIZE,
                        CONNECTION_KEEP_ALIVE,
                        TimeUnit.MINUTES));

        // 配置日志拦截器
        if (log.isDebugEnabled()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                    message -> log.debug("http请求参数：{}", message)
            );
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            httpClientBuilder.addInterceptor(logging);
        }

        // 配置SSL
        X509TrustManager trustManager = SSLSocketClientUtil.getX509TrustManager();
        httpClientBuilder.sslSocketFactory(SSLSocketClientUtil.getSocketFactory(trustManager), trustManager)
                .hostnameVerifier(SSLSocketClientUtil.getHostnameVerifier());

        this.client = httpClientBuilder.build();
    }

    /**
     * 获取自定义超时时间的OkHttpClient
     * 注意：这里创建新的客户端实例而非修改原有实例，避免线程安全问题
     */
    private OkHttpClient getClientWithCustomTimeout() {
        if ((Integer) ZLMHttpHookCallback.DEFAULT_READ_TIMEOUT == null || ZLMHttpHookCallback.DEFAULT_READ_TIMEOUT <= 0) {
            return client;
        }

        return client.newBuilder()
                .readTimeout(ZLMHttpHookCallback.DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送POST请求到指定钩子事件
     */
    @Async("taskExecutor")
    public void sendPost(String hookEvent, Object param) {

        String mediaHookWebUrl = userSetting.getMediaHookWebUrl();
        if (!StringUtils.hasText(mediaHookWebUrl)) {
            log.debug("媒体钩子URL未配置，不发送事件: {}", hookEvent);
            return;
        }

        // 参数校验
        if (!StringUtils.hasText(hookEvent)) {
            log.error("钩子事件名称不能为空");
            return;
        }

        // 构建URL，处理可能的URL拼接问题
        String url = buildUrl(mediaHookWebUrl, hookEvent);
        OkHttpClient client = getClientWithCustomTimeout();

        log.info("[hook] 发送事件: {}, URL: {}", hookEvent, url);

        try {
            // 创建请求体
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(CONTENT_TYPE_JSON),
                    JSONObject.toJSONString(param)
            );

            // 构建请求
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Content-Type", CONTENT_TYPE_JSON)
                    .build();

            // 异步发送请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    try (Response res = response) {  // try-with-resources自动关闭资源
                        if (res.isSuccessful()) {
                            handleSuccessResponse(hookEvent, res);
                        } else {
                            handleErrorResponse(hookEvent, res);
                        }
                    } catch (IOException e) {
                        log.error("[{}] 处理响应失败", hookEvent, e);
                    }
                }

                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    handleRequestFailure(hookEvent, call, e);
                }
            });
        } catch (Exception e) {
            log.error("[{}] 构建请求失败", hookEvent, e);
        }
    }

    /**
     * 处理URL拼接，确保不会出现双斜杠问题
     */
    private String buildUrl(String baseUrl, String path) {
        if (baseUrl.endsWith("/")) {
            return baseUrl + path;
        } else {
            return baseUrl + "/" + path;
        }
    }

    /**
     * 处理成功响应
     */
    private void handleSuccessResponse(String hookEvent, Response response) throws IOException {
        try (ResponseBody body = response.body()) {  // 自动关闭响应体
            String responseStr = body != null ? body.string() : "";
            log.debug("[{}] 响应成功: {}", hookEvent, responseStr);
        }
    }

    /**
     * 处理错误响应
     */
    private void handleErrorResponse(String hookEvent, Response response) {
        log.error("[{}] 响应失败: 状态码={}", hookEvent, response.code());
    }

    /**
     * 处理请求失败
     */
    private void handleRequestFailure(String hookEvent, Call call, IOException e) {
        String requestUrl = call.request().url().toString();

        if (e instanceof SocketTimeoutException) {
            log.error("[{}] 读取数据超时: {}", hookEvent, requestUrl, e);
        } else if (e instanceof ConnectException) {
            log.error("[{}] 连接失败: {}", hookEvent, requestUrl, e);
        } else {
            log.error("[{}] 请求失败: {}", hookEvent, requestUrl, e);
        }
    }
}

