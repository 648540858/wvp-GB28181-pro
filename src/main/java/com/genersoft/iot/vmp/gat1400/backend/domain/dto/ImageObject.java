package com.genersoft.iot.vmp.gat1400.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.FaceObjectList;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.MotorVehicleListObject;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.NonMotorVehicleObjectList;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.PersonListObject;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class ImageObject {

    @ApiModelProperty("人脸")
    @JsonProperty("FaceList")
    private FaceObjectList faceList;
    @ApiModelProperty("人员")
    @JsonProperty("PersonList")
    private PersonListObject personList;
    @ApiModelProperty("车辆")
    @JsonProperty("MotorVehicleList")
    private MotorVehicleListObject motorVehicleList;
    @ApiModelProperty("非机动车辆")
    @JsonProperty("NonMotorVehicleList")
    private NonMotorVehicleObjectList nonMotorVehicleList;
    @ApiModelProperty("图片信息")
    @JsonProperty("ImageInfo")
    private ImageInfo imageInfo;
    @ApiModelProperty("图片数据(base64)")
    @JsonProperty("Data")
    private String data;
}
