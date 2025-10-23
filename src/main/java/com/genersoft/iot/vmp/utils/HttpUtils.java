package com.genersoft.iot.vmp.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipOutputStream;

@Slf4j
public class HttpUtils {

    public static boolean downLoadFile(String url, ZipOutputStream zos) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("下载失败，HTTP 状态码: {}, URL: {}", response.code(), url);
                return false;
            }

            // 获取响应体的输入流
            InputStream inputStream = null;
            if (response.body() != null) {
                inputStream = response.body().byteStream();
            }
            if (inputStream == null) {
                log.error("响应体为空，无法下载文件: {}", url);
                return false;
            }

            // 将输入流写入zip文件
            byte[] buffer = new byte[8192]; // 8KB 缓冲区，提高性能
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            log.debug("成功下载文件: {}, 大小: {} bytes", url, response.body().contentLength());
            return true;
        } catch (IOException e) {
            log.error("下载过程中出错: {}, URL: {}", e.getMessage(), url);
            return false;
        }
    }
}
