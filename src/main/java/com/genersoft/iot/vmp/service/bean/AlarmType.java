package com.genersoft.iot.vmp.service.bean;

public enum AlarmType {

    // 视频丢失报警
    VideoLoss("视频丢失报警"),
    // 设备防拆报警
    DeviceTamper("设备防拆报警"),
    // 存储设备磁盘满报警
    StorageFull("存储设备磁盘满报警"),
    // 设备高温报警
    DeviceHighTemperature("设备高温报警"),
    // 设备低温报警
    DeviceLowTemperature("设备低温报警"),
    // 人工视频报警
    ManualVideo("人工视频报警"),
    // 运动目标检测报警
    MotionDetection("运动目标检测报警"),
    // 遗留物检测报警
    LeftObjectDetection("遗留物检测报警"),
    // 物体移除检测报警
    ObjectRemovalDetection("物体移除检测报警"),
    // 绊线检测报警
    TripwireDetection("绊线检测报警"),
    // 入侵检测报警
    IntrusionDetection("入侵检测报警"),
    // 移动侦测报警
    MobileDetection("移动侦测报警"),
    // 视频遮挡报警
    VideoOcclusion("视频遮挡报警"),
    // 逆行检测报警
    ReverseDetection("逆行检测报警"),
    // 徘徊检测报警
    LoiteringDetection("徘徊检测报警"),
    // 流量统计报警
    FlowStatistics("流量统计报警"),
    // 密度检测报警
    DensityDetection("密度检测报警"),
    // 视频异常检测报警
    VideoAbnormal("视频异常检测报警"),
    // 快速移动报警
    RapidMovement("快速移动报警"),
    // 存储设备磁盘故障报警
    StorageFault("存储设备磁盘故障报警"),
    // 存储设备风扇故障报警
    StorageFanFault("存储设备风扇故障报警"),
    // 声音异常报警
    SoundAbnormal("声音异常报警"),
    // 信号量异常报警
    SignalAbnormal("信号量异常报警"),
    // 非法访问报警
    IllegalAccess("非法访问报警"),
    // 虚焦报警
    Defocus("虚焦报警"),
    // 场景变更报警
    SceneChange("场景变更报警"),
    // 人员聚集报警
    CrowdGathering("人员聚集报警"),
    // 停车侦测报警
    ParkingDetection("停车侦测报警"),
    // 其他报警
    Other("其他报警");

    private String description;

    AlarmType(String description) {
        this.description = description;
    }
}
