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
    // 生产商
    private String manufacturer;
    // 设备型号
    private String model;
    // 摄像机类型
    private Integer ptzType;

    public StreamPush buildstreamPush() {
        StreamPush push = new StreamPush();
        push.setApp(app);
        push.setStream(stream);
        push.setGbName(name);
        push.setGbDeviceId(gbId);
        push.setStartOfflinePush(true);
        push.setGbManufacturer(manufacturer);
        push.setGbModel(model);
        push.setGbPtzType(ptzType);
        push.setGbStatus(status?"ON":"OFF");
        return push;
    }
}
