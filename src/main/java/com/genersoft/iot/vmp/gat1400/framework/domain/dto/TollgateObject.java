package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TollgateObject {

    @ApiModelProperty("卡口ID")
    @JsonProperty("TollgateID")
    private String TollgateID;
    @ApiModelProperty("名称")
    @JsonProperty("Name")
    private String Name;
    @ApiModelProperty("经度")
    @JsonProperty("Longitude")
    private Double Longitude;
    @ApiModelProperty("纬度")
    @JsonProperty("Latitude")
    private Double Latitude;
    @ApiModelProperty("安装地点行政区划代码")
    @JsonProperty("PlaceCode")
    private String PlaceCode;
    @ApiModelProperty("位置名")
    @JsonProperty("Place")
    private String Place;
    @ApiModelProperty(value = "卡口状态", notes = "1正常,2停用,9其他")
    @JsonProperty("Status")
    private String Status;
    @ApiModelProperty("卡口类型")
    @JsonProperty("TollgateCat")
    private String TollgateCat;
    @ApiModelProperty("卡口用途")
    @JsonProperty("TollgateUsage")
    private Integer TollgateUsage;
    @ApiModelProperty("卡口车道数")
    @JsonProperty("LaneNum")
    private Integer LaneNum;
    @ApiModelProperty("管辖单位代码")
    @JsonProperty("OrgCode")
    private String OrgCode;
    @ApiModelProperty("卡口启用时间")
    @JsonProperty("ActiveTime")
    private String ActiveTime;
    @ApiModelProperty("能力集")
    @JsonProperty("FunctionType")
    private String FunctionType;
}
