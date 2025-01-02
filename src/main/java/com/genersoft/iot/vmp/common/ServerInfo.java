package com.genersoft.iot.vmp.common;

import lombok.Data;

@Data
public class ServerInfo {

    private String ip;
    private int port;
    /**
     * 现在使用的线程数
     */
    private int threadNumber;

    public static ServerInfo create(String ip, int port, int threadNumber) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setIp(ip);
        serverInfo.setPort(port);
        serverInfo.setThreadNumber(threadNumber);
        return serverInfo;
    }
}
