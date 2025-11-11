package com.genersoft.iot.vmp.web.custom.conf;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * 自定义请求包装器，用于缓存请求体内容
 * 解决流只能读取一次的问题
 */
@Slf4j
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    
    private byte[] cachedBody;
    private String cachedBodyString;

    public CachedBodyHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBody == null) {
            cacheInputStream();
        }
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (cachedBodyString == null) {
            if (cachedBody == null) {
                cacheInputStream();
            }
            cachedBodyString = new String(cachedBody, StandardCharsets.UTF_8);
        }
        return new BufferedReader(new StringReader(cachedBodyString));
    }

    /**
     * 获取缓存的请求体内容
     */
    public String getCachedBody() {
        if (cachedBodyString == null) {
            if (cachedBody == null) {
                try {
                    cacheInputStream();
                } catch (IOException e) {
                    log.warn("缓存请求体失败: {}", e.getMessage());
                    return "";
                }
            }
            cachedBodyString = new String(cachedBody, StandardCharsets.UTF_8);
        }
        return cachedBodyString;
    }

    /**
     * 获取缓存的请求体字节数组
     */
    public byte[] getCachedBodyBytes() {
        if (cachedBody == null) {
            try {
                cacheInputStream();
            } catch (IOException e) {
                log.warn("缓存请求体失败: {}", e.getMessage());
                return new byte[0];
            }
        }
        return cachedBody;
    }

    private void cacheInputStream() throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             InputStream inputStream = super.getInputStream()) {
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            cachedBody = baos.toByteArray();
            log.debug("成功缓存请求体，长度: {}", cachedBody.length);
        }
    }

    /**
     * 自定义 ServletInputStream 实现
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 不需要实现
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }
    }
}



