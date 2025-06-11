package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SubscribeNotificationObject {

    @JsonProperty("NotificationID")
    private String NotificationID;
    @JsonProperty("SubscribeID")
    private String SubscribeID;
    @JsonProperty("Title")
    private String Title;
    @JsonProperty("TriggerTime")
    private String TriggerTime;
    @JsonProperty("InfoIDs")
    private String InfoIDs;

    //自动采集的车辆信息订阅接受对象
    @JsonProperty("MotorVehicleObjectList")
    private MotorVehicleListObject MotorVehicleObjectList;
    //自动采集的非机动车辆信息订阅接收对象
    @JsonProperty("NonMotorVehicleObjectList")
    private cz.data.viid.framework.domain.dto.NonMotorVehicleObjectList NonMotorVehicleObjectList;
    //视频卡口目录订阅接受对象
    @JsonProperty("TollgateObjectList")
    private TollgateObjectList TollgateObjectList;
    //采集设备订阅接受对象 人脸设备
    @JsonProperty("DeviceList")
    private DeviceObjectList DeviceList;
    //人脸抓拍数据订阅接受对象
    @JsonProperty("FaceObjectList")
    private FaceObjectList FaceObjectList;
    //自动采集的人员信息订阅接收对象
    @JsonProperty("PersonObjectList")
    private PersonListObject PersonObjectList;

    @ApiModelProperty(value = "车道数据", notes = "卡口车道订阅接受对象")
    @JsonProperty("LaneObjectList")
    private LaneObjectList laneObjectList;
}
