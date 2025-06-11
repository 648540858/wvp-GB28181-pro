package com.genersoft.iot.vmp.gat1400.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import cz.data.viid.framework.domain.dto.MotorVehicleObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class DispositionNotificationObject {

    @ApiModelProperty("告警标识")
    @JsonProperty("NotificationID")
    private String NotificationID;
    @ApiModelProperty("布控标识")
    @JsonProperty("DispositionID")
    private String DispositionID;
    @ApiModelProperty("布控标题")
    @JsonProperty("Title")
    private String Title;
    @ApiModelProperty("触发时间")
    @JsonProperty("TriggerTime")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date TriggerTime;
    @ApiModelProperty(value = "信息标识", notes = "自动采集过车或过人记录ID")
    @JsonProperty("CntObjectID")
    private String CntObjectID;
    @ApiModelProperty(value = "人员", notes = "自动采集的人员数据")
    @JsonProperty("PersonObject")
    private cz.data.viid.framework.domain.dto.PersonObject PersonObject;
    @ApiModelProperty(value = "车辆", notes = "自动采集的车辆信息")
    @JsonProperty("MotorVehicleObject")
    private MotorVehicleObject MotorVehicleObject;
}
