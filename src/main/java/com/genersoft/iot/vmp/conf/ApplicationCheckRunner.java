package com.genersoft.iot.vmp.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 *  对配置文件进行校验
 */
@Component
@Order(value=2)
public class ApplicationCheckRunner implements CommandLineRunner {

    private Logger logger = LoggerFactory.getLogger("ApplicationCheckRunner");

    @Value("${sip.ip}")
    private String sipIp;

    @Value("${media.ip}")
    private String mediaIp;

    @Value("${media.wanIp}")
    private String mediaWanIp;

    @Value("${media.hookIp}")
    private String mediaHookIp;

    @Value("${media.port}")
    private int mediaPort;

    @Value("${media.secret}")
    private String mediaSecret;

    @Value("${media.streamNoneReaderDelayMS}")
    private String streamNoneReaderDelayMS;

    @Value("${sip.ip}")
    private String sipIP;

    @Value("${server.port}")
    private String serverPort;

    @Value("${media.autoConfig}")
    private boolean autoConfig;


    @Override
    public void run(String... args) throws Exception {
        if (sipIP.equals("localhost") || sipIP.equals("127.0.0.1")) {
            logger.error("sip.ip不能使用 {} ,请使用类似192.168.1.44这样的来自网卡的IP!!!", sipIP );
            System.exit(1);
        }

        if (mediaIp.equals("localhost") || mediaIp.equals("127.0.0.1")) {
            logger.warn("mediaIp.ip使用 {} ,将无法收到网络内其他设备的推流!!!", mediaIp );
        }

    }
}
