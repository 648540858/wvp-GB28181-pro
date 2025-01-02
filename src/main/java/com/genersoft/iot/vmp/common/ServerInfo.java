package com.genersoft.iot.vmp.common;

import com.genersoft.iot.vmp.utils.DateUtil;
import lombok.Data;

@Data
public class ServerInfo {

    private String ip;
    private int port;
    /**
     * 现在使用的线程数
     */
    private String createTime;

    public static ServerInfo create(String ip, int port) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setIp(ip);
        serverInfo.setPort(port);
        serverInfo.setCreateTime(DateUtil.getNow());
        return serverInfo;
    }
}
