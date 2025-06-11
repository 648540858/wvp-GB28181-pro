package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 车道
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LaneObject {

    @ApiModelProperty(value = "数据库主键", notes = "仅在内部使用")
    private String id;
    @ApiModelProperty(value = "卡口编号", notes = "R必填")
    @NotBlank(message = "卡口编号不能为空")
    @JsonProperty("TollgateID")
    private String tollgateId;
    @ApiModelProperty(value = "车道ID", notes = "R必填 卡口内车道唯一编号,从1开始")
    @NotNull(message = "车道ID不能为空")
    @JsonProperty("LaneId")
    private Integer laneId;
    @ApiModelProperty(value = "车道编号", notes = "R必填 车辆行驶方向最左车道为1,由左向右顺序编号,与方向有关")
    @NotNull(message = "车道编号不能为空")
    @JsonProperty("LaneNo")
    private Integer laneNo;
    @ApiModelProperty(value = "车道名称", notes = "R必填")
    @NotBlank(message = "车道名称不能为空")
    @JsonProperty("Name")
    private String name;
    @ApiModelProperty(value = "车道方向", notes = "R必填")
    @NotBlank(message = "车道方向不能为空")
    @JsonProperty("Direction")
    private String direction;
    @ApiModelProperty(value = "车道描述", notes = "O非必填 车道补充说明")
    @JsonProperty("Desc")
    private String desc;
    @ApiModelProperty(value = "限速", notes = "O非必填 限速,单位km/h(公里/小时)")
    @JsonProperty("MaxSpeed")
    private Integer maxSpeed;
    @ApiModelProperty(value = "车道出入城", notes = "O非必填 1=进城,2=出城,3=非进出城,4=进出城混合")
    @JsonProperty("CityPass")
    private Integer cityPass;
    @ApiModelProperty(value = "设备ID", notes = "O非必填 车道上对应的采集处理设备ID")
    @JsonProperty("ApeID")
    private String apeId;
}
