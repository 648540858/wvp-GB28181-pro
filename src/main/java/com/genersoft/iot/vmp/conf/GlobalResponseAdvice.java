package com.genersoft.iot.vmp.conf;

import com.alibaba.fastjson2.JSON;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import com.genersoft.iot.vmp.vmanager.bean.WVPResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;

/**
 * 全局统一返回结果
 * @author lin
 */
@RestControllerAdvice
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {


    @Override
    public boolean supports(@NotNull MethodParameter returnType, @NotNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }


    @Override
    public Object beforeBodyWrite(Object body, @NotNull MethodParameter returnType, @NotNull MediaType selectedContentType, @NotNull Class<? extends HttpMessageConverter<?>> selectedConverterType, @NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response) {
        // 排除api文档的接口，这个接口不需要统一
        String[] excludePath = {"/v3/api-docs","/api/v1","/index/hook","/api/video-"};
        for (String path : excludePath) {
            if (request.getURI().getPath().startsWith(path)) {
                return body;
            }
        }

        if (body instanceof WVPResult) {
            return body;
        }

        if (body instanceof ErrorCode errorCode) {
            return new WVPResult<>(errorCode.getCode(), errorCode.getMsg(), null);
        }

        if (body instanceof String) {
            return JSON.toJSONString(WVPResult.success(body));
        }

        if (body instanceof LinkedHashMap ) {
            LinkedHashMap<String, Object> bodyMap = (LinkedHashMap<String, Object>) body;
            if (bodyMap.get("status") != null && (Integer)bodyMap.get("status") != 200) {
                return body;
            }
        }

        return WVPResult.success(body);
    }

    /**
     * 防止返回string时出错
     * @return
     */
    /*@Bean
    public HttpMessageConverters custHttpMessageConverter() {
        return new HttpMessageConverters(new FastJsonHttpMessageConverter());
    }*/
}
