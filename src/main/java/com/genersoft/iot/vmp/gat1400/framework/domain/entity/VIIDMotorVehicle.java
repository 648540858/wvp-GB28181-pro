package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

import cz.data.viid.framework.domain.dto.SubImageList;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName(value = "viid_motor_vehicle", autoResultMap = true)
public class VIIDMotorVehicle {

    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @ApiModelProperty("数据时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "data_time")
    private LocalDateTime dataTime;
    @ApiModelProperty("机动车标识")
    @TableField("motor_vehicle_id")
    private String motorVehicleId;
    @ApiModelProperty("信息分类")
    @TableField("info_kind")
    private String infoKind;
    @ApiModelProperty("来源标识")
    @TableField("source_id")
    private String sourceId;
    @ApiModelProperty("设备ID")
    @TableField("device_id")
    private String deviceId;
    @ApiModelProperty("左上角X坐标")
    @TableField("left_top_x")
    private Integer leftTopX;
    @ApiModelProperty("左上角Y坐标")
    @TableField("left_top_y")
    private Integer leftTopY;
    @ApiModelProperty("右下角X坐标")
    @TableField("right_btm_x")
    private Integer rightBtmX;
    @ApiModelProperty("右下角Y坐标")
    @TableField("right_btm_y")
    private Integer rightBtmY;
    @ApiModelProperty("位置标记时间")
    @TableField("mark_time")
    private String markTime;
    @ApiModelProperty("车辆出现时间")
    @TableField("appear_time")
    private String appearTime;
    @ApiModelProperty("车辆消失时间")
    @TableField("disappear_time")
    private String disappearTime;
    @ApiModelProperty("卡口编号")
    @TableField("tollgate_id")
    private String tollgateId;
    @ApiModelProperty("过车时间")
    @TableField("pass_time")
    private String passTime;
    @ApiModelProperty("车道号")
    @TableField("lane_no")
    private Integer laneNo;
    @ApiModelProperty("有无车牌")
    @TableField("has_plate")
    private Boolean hasPlate;
    @ApiModelProperty("号牌种类")
    @TableField("plate_class")
    private String plateClass;
    @ApiModelProperty("号牌颜色")
    @TableField("plate_color")
    private String plateColor;
    @ApiModelProperty("车牌号")
    @TableField("plate_no")
    private String plateNo;
    @ApiModelProperty("速度")
    @TableField("speed")
    private Double speed;
    @ApiModelProperty("车身颜色")
    @TableField("vehicle_color")
    private String vehicleColor;
    @ApiModelProperty("车辆类型")
    @TableField("vehicle_class")
    private String vehicleClass;
    @ApiModelProperty("车辆品牌")
    @TableField("vehicle_brand")
    private String vehicleBrand;
    @ApiModelProperty("车辆型号")
    @TableField("vehicle_model")
    private String vehicleModel;
    @ApiModelProperty("车辆长度")
    @TableField("vehicle_length")
    private Integer vehicleLength;
    @ApiModelProperty("行驶方向")
    @TableField("direction")
    private String direction;
    @ApiModelProperty("图片1")
    @TableField("storage_url1")
    private String storageUrl1;
    @ApiModelProperty("图片2")
    @TableField("storage_url2")
    private String storageUrl2;
    @ApiModelProperty("图片列表")
    @TableField(value = "sub_image_list", typeHandler = JacksonTypeHandler.class)
    private SubImageList subImageList;
}
