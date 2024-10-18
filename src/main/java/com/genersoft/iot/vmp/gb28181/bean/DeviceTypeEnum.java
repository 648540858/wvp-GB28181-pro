package com.genersoft.iot.vmp.gb28181.bean;

/**
 * 收录行业编码
 */
public enum DeviceTypeEnum {
    DVR("111", "DVR编码", "前端主设备"),
    VIDEO_SERVER("112", "视频服务器编码", "前端主设备"),
    ENCODER("113", "编码器编码", "前端主设备"),
    DECODER("114", "解码器编码", "前端主设备"),
    VIDEO_SWITCHING_MATRIX("115", "视频切换矩阵编码", "前端主设备"),
    AUDIO_SWITCHING_MATRIX("116", "音频切换矩阵编码", "前端主设备"),
    ALARM_CONTROLLER("117", "报警控制器编码", "前端主设备"),
    NVR("118", "网络视频录像机（NVR）编码", "前端主设备"),
    RESERVE("119", "预留", "前端主设备"),
    ONLINE_VIDEO_IMAGE_INFORMATION_ACQUISITION_SYSTEM("120", "在线视频图像信息采集系统编码", "前端主设备"),
    VIDEO_CHECKPOINT("121", "视频卡口编码", "前端主设备"),
    MULTI_CAMERA_DEVICE("122", "多目设备编码", "前端主设备"),
    PARKING_LOT_ENTRANCE_AND_EXIT_CONTROL_EQUIPMENT("123", "停车场出入口控制设备编码", "前端主设备"),
    PERSONNEL_ACCESS_CONTROL_EQUIPMENT("124", "人员出入口控制设备编码", "前端主设备"),
    SECURITY_INSPECTION_EQUIPMENT("125", "安检设备编码", "前端主设备"),
    HVR("130", "混合硬盘录像机（HVR）编码", "前端主设备"),
    CAMERA("131", "摄像机编码", "前端外围设备"),
    IPC("132", "网络摄像机（IPC）/在线视频图像信息采集设备编码", "前端外围设备"),
    MONITOR("133", "显示器编码", "前端外围设备"),
    ALARM_INPUT_DEVICE("134", "报警输入设备编码（如红外、烟感、门禁等报警设备）", "前端外围设备"),
    ALARM_OUTPUT_DEVICE("135", "报警输出设备编码(如警灯、警铃等设备)", "前端外围设备"),
    VOICE_INPUT_DEVICE("136", "语音输入设备编码", "前端外围设备"),
    VOICE_OUTPUT_DEVICE("137", "语音输出设备", "前端外围设备"),
    MOBILE_TRANSMISSION_EQUIPMENT("138", "移动传输设备编码", "前端外围设备"),
    OTHER_PERIPHERAL_DEVICES("139", "其他外围设备编码", "前端外围设备"),
    ALARM_OUTPUT_DEVICE2("140", "报警输出设备编码(如继电器或触发器控制的设备)", "前端外围设备"),
    BARRIER_GATE("141", "道闸(控制车辆通行)", "前端外围设备"),
    SMART_DOOR("142", "智能门(控制人员通行)", "前端外围设备"),
    VOUCHER_RECOGNITION_UNIT("143", "凭证识别单元", "前端外围设备"),
    CENTRAL_SIGNALING_CONTROL_SERVER("200", "中心信令控制服务器编码", "平台设备"),
    WEB_APPLICATION_SERVER("201", "Web应用服务器编码", "平台设备"),
    PROXY_SERVER("203", "代理服务器编码", "平台设备"),
    SECURITY_SERVER("204", "安全服务器编码", "平台设备"),
    ALARM_SERVER("205", "报警服务器编码", "平台设备"),
    DATABASE_SERVER("206", "数据库服务器编码", "平台设备"),
    GIS_SERVER("207", "GIS服务器编码", "平台设备"),
    MANAGER_SERVER("208", "管理服务器编码", "平台设备"),
    ACCESS_GATEWAY("209", "接入网关编码", "平台设备"),
    MEDIA_STORAGE_SERVER("210", "媒体存储服务器编码", "平台设备"),
    SIGNALING_SECURITY_ROUTING_GATEWAY("211", "信令安全路由网关编码", "平台设备"),
    BUSINESS_GROUP("215", "业务分组编码", "平台设备"),
    VIRTUAL_ORGANIZATION("216", "虚拟组织编码", "平台设备"),
    CENTRAL_USER("300", "中心用户", "中心用户"),
    END_USER("400", "终端用户", "终端用户"),
    VIDEO_IMAGE_INFORMATION_SYNTHESIS("500", "视频图像信息综合应用平台", "平台外接服务器"),
    VIDEO_IMAGE_INFORMATION_OPERATION_AND_MAINTENANCE_MANAGEMENT("501", "视频图像信息运维管理平台", "平台外接服务器"),
    VIDEO_IMAGE_ANALYSIS("502", "视频图像分析系统", "平台外接服务器"),
    VIDEO_IMAGE_INFORMATION_DATABASE("503", "视频图像信息数据库", "平台外接服务器"),
    VIDEO_IMAGE_ANALYSIS_EQUIPMENT("505", "视频图像分析设备", "平台外接服务器"),
    ;

    /**
     * 编号
     */
    private final String name;

    /**
     * 名称
     */
    private String code;

    /**
     * 归属名称
     */
    private String ownerName;

    DeviceTypeEnum(String code, String name, String ownerName) {
        this.name = name;
        this.code = code;
        this.ownerName = ownerName;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
