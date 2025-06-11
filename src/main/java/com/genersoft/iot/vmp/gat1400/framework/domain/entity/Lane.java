package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 车道
 */
@Data
@TableName(value = "viid_lanes")
public class Lane {

    @ApiModelProperty(value = "主键", notes = "主键为 tollgate_id + lane_id")
    @TableId("id")
    private String id;
    @ApiModelProperty(value = "卡口编号", notes = "R必填")
    @TableField("tollgate_id")
    private String tollgateId;
    @ApiModelProperty(value = "车道ID", notes = "R必填 卡口内车道唯一编号,从1开始")
    @TableField("lane_id")
    private Integer laneId;
    @ApiModelProperty(value = "车道编号", notes = "R必填 车辆行驶方向最左车道为1,由左向右顺序编号,与方向有关")
    @TableField("lane_no")
    private Integer laneNo;
    @ApiModelProperty(value = "车道名称", notes = "R必填")
    @TableField("name")
    private String name;
    @ApiModelProperty(value = "车道方向", notes = "R必填")
    @TableField("direction")
    private String direction;
    @ApiModelProperty(value = "车道描述", notes = "O非必填 车道补充说明")
    @TableField("`desc`")
    private String desc;
    @ApiModelProperty(value = "限速", notes = "O非必填 限速,单位km/h(公里/小时)")
    @TableField("max_speed")
    private Integer maxSpeed;
    @ApiModelProperty(value = "车道出入城", notes = "O非必填 1=进城,2=出城,3=非进出城,4=进出城混合")
    @TableField("city_pass")
    private Integer cityPass;
    @ApiModelProperty(value = "设备编号", notes = "O非必填 车道上对应的采集处理设备ID")
    @TableField("ape_id")
    private String apeId;
}
