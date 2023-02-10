package com.genersoft.iot.vmp.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class ServiceInfo implements ApplicationListener<WebServerInitializedEvent> {

    private final Logger logger = LoggerFactory.getLogger(ServiceInfo.class);

    private static int serverPort;

    public static int getServerPort() {
        return serverPort;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        // 项目启动获取启动的端口号
        ServiceInfo.serverPort = event.getWebServer().getPort();
        logger.info("项目启动获取启动的端口号:  " + ServiceInfo.serverPort);
    }

    public void setServerPort(int serverPort) {
        ServiceInfo.serverPort = serverPort;
    }
}
