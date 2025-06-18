package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class APEObject {

    @ApiModelProperty("设备ID")
    @JsonProperty("ApeID")
    private String ApeID;
    @ApiModelProperty("设备名称")
    @JsonProperty("Name")
    private String Name;
    @ApiModelProperty("设备型号")
    @JsonProperty("Model")
    private String Model;
    @ApiModelProperty("IP地址")
    @JsonProperty("IPAddr")
    private String IPAddr;
    @ApiModelProperty("IPV6地址")
    @JsonProperty("IPV6Addr")
    private String IPV6Addr;
    @ApiModelProperty("端口号")
    @JsonProperty("Port")
    private String Port;
    @ApiModelProperty("经度")
    @JsonProperty("Longitude")
    private Double Longitude;
    @ApiModelProperty("纬度")
    @JsonProperty("Latitude")
    private Double Latitude;
    @ApiModelProperty("安装地点行政区号")
    @JsonProperty("PlaceCode")
    private String PlaceCode;
    @ApiModelProperty("位置名")
    @JsonProperty("Place")
    private String Place;
    @ApiModelProperty("管辖单位代码")
    @JsonProperty("OrgCode")
    private String OrgCode;
    @ApiModelProperty(value = "车辆抓拍方向", notes = "0=拍车头,1=拍车尾,兼容无视频卡口信息设备")
    @JsonProperty("CapDirection")
    private String CapDirection;
    @ApiModelProperty("监视方向")
    @JsonProperty("MonitorDirection")
    private String MonitorDirection;
    @ApiModelProperty("监视区域说明")
    @JsonProperty("MonitorAreaDesc")
    private String MonitorAreaDesc;
    @ApiModelProperty("所属采集系统")
    @JsonProperty("OwnerApsID")
    private String OwnerApsID;
    @ApiModelProperty("是否在线")
    @JsonProperty("IsOnline")
    private String IsOnline;
    @ApiModelProperty("用户账户")
    @JsonProperty("UserId")
    private String UserId;
    @ApiModelProperty("口令")
    @JsonProperty("Password")
    private String Password;

}
