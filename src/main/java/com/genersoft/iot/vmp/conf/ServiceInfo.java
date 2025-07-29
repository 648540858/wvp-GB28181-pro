package com.genersoft.iot.vmp.conf;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceInfo implements ApplicationListener<WebServerInitializedEvent> {

    @Getter
    private static int serverPort;

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // 项目启动获取启动的端口号
        ServiceInfo.serverPort = event.getWebServer().getPort();
        log.info("项目启动获取启动的端口号:  {}", ServiceInfo.serverPort);
    }

    public void setServerPort(int serverPort) {
        ServiceInfo.serverPort = serverPort;
    }
}
