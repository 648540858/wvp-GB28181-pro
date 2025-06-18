package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SubImageInfoObject {

    @ApiModelProperty("图片ID")
    @JsonProperty("ImageID")
    private String ImageID;
    @ApiModelProperty("事件排序")
    @JsonProperty("EventSort")
    private Integer EventSort;
    @ApiModelProperty("设备ID")
    @JsonProperty("DeviceID")
    private String DeviceID;
    @ApiModelProperty("存储路径")
    @JsonProperty("StoragePath")
    private String StoragePath;
    @ApiModelProperty("图片类型")
    @JsonProperty("Type")
    private String Type;
    @ApiModelProperty("图片格式(Jpeg)")
    @JsonProperty("FileFormat")
    private String FileFormat;
    @ApiModelProperty("图片宽度")
    @JsonProperty("Width")
    private String Width;
    @ApiModelProperty("图片高度")
    @JsonProperty("Height")
    private String Height;
    @ApiModelProperty("拍摄时间")
    @JsonProperty("ShotTime")
    private String ShotTime;
    @ApiModelProperty("图片数据(base64)")
    @JsonProperty("Data")
    private String Data;
}
