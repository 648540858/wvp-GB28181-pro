package com.genersoft.iot.vmp.media.zlm.dto.hook;

import lombok.Getter;
import lombok.Setter;

/**
 * zlm hook事件中的on_publish事件的参数
 * @author lin
 */

public class OnPublishHookParam extends HookParam{

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String app;

    @Getter
    @Setter
    private String stream;

    @Getter
    @Setter
    private String ip;

    @Getter
    @Setter
    private String params;

    @Getter
    @Setter
    private int port;

    @Getter
    @Setter
    private String schema;

    @Getter
    @Setter
    private String vhost;


    @Override
    public String toString() {
        return "OnPublishHookParam{" +
                "id='" + id + '\'' +
                ", app='" + app + '\'' +
                ", stream='" + stream + '\'' +
                ", ip='" + ip + '\'' +
                ", params='" + params + '\'' +
                ", port=" + port +
                ", schema='" + schema + '\'' +
                ", vhost='" + vhost + '\'' +
                '}';
    }
}
