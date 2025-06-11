package com.genersoft.iot.vmp.gat1400.rpc.interceptor;

import org.springframework.stereotype.Component;

import cz.data.viid.be.task.action.KeepaliveAction;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * VIID请求拦截器
 * 补充用户标识符
 */
@Component
public class VIIDRequestInterceptor implements RequestInterceptor {
    private static final String User_Key = "User-Identify";

    @Override
    public void apply(RequestTemplate template) {
        template.header(User_Key, KeepaliveAction.CURRENT_SERVER_ID);
    }
}
