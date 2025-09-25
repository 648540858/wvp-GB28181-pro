package com.genersoft.iot.vmp.streamPush.bean;

import lombok.Data;

@Data
public class RedisPushStreamMessage {

    private String gbId;
    private String app;
    private String stream;
    private String name;
    private boolean status;
    // 终端所属的虚拟组织
    private String groupGbId;
    // 终端所属的虚拟组织别名 可选，可作为地方同步组织结构到wvp时的关联关系
    private String groupAlias;

    public StreamPush buildstreamPush() {
        StreamPush push = new StreamPush();
        push.setApp(app);
        push.setStream(stream);
        push.setGbName(name);
        push.setGbDeviceId(gbId);
        push.setStartOfflinePush(true);
        push.setGbStatus(status?"ON":"OFF");
        return push;
    }
}
