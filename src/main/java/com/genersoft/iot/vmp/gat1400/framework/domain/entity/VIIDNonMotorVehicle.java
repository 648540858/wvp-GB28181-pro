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
@TableName(value = "viid_non_motor_vehicle", autoResultMap = true)
public class VIIDNonMotorVehicle {

    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.NONE)
    private String id;
    @ApiModelProperty("数据时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "data_time")
    private LocalDateTime dataTime;
    @ApiModelProperty("车辆标识")
    @TableField("non_motor_vehicle_id")
    private String nonMotorVehicleId;
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
    @ApiModelProperty("有无车牌")
    @TableField("has_plate")
    private Boolean hasPlate;
    @ApiModelProperty("号牌种类")
    @TableField("plate_class")
    private String plateClass;
    @ApiModelProperty("车牌颜色")
    @TableField("plate_color")
    private String plateColor;
    @ApiModelProperty("车牌号")
    @TableField("plate_no")
    private String plateNo;
    @ApiModelProperty("挂车牌号")
    @TableField("plate_no_attach")
    private String plateNoAttach;
    @ApiModelProperty("车牌描述")
    @TableField("plate_describe")
    private String plateDescribe;
    @ApiModelProperty("是否套牌")
    @TableField("is_decked")
    private Boolean isDecked;
    @ApiModelProperty("是否涂改")
    @TableField("is_altered")
    private Boolean isAltered;
    @ApiModelProperty("是否遮挡")
    @TableField("is_covered")
    private Boolean isCovered;
    @ApiModelProperty("行驶速度")
    @TableField("speed")
    private Double speed;
    @ApiModelProperty("行驶状态代码")
    @TableField("driving_status_code")
    private String drivingStatusCode;
    @ApiModelProperty("车辆使用性质代码")
    @TableField("using_properties_code")
    private String usingPropertiesCode;
    @ApiModelProperty("车辆品牌")
    @TableField("vehicle_brand")
    private String vehicleBrand;
    @ApiModelProperty("车辆款型")
    @TableField("vehicle_type")
    private String vehicleType;
    @ApiModelProperty("车辆长度")
    @TableField("vehicle_length")
    private String vehicleLength;
    @ApiModelProperty("车辆宽度")
    @TableField("vehicle_width")
    private String vehicleWidth;
    @ApiModelProperty("车辆高度")
    @TableField("vehicle_height")
    private String vehicleHeight;
    @ApiModelProperty("车身颜色")
    @TableField("vehicle_color")
    private String vehicleColor;
    @ApiModelProperty("车前盖")
    @TableField("vehicle_hood")
    private String vehicleHood;
    @ApiModelProperty("车后盖")
    @TableField("vehicle_trunk")
    private String vehicleTrunk;
    @ApiModelProperty("车轮")
    @TableField("vehicle_wheel")
    private String VehicleWheel;
    @ApiModelProperty("车轮印花纹")
    @TableField("wheel_printed_pattern")
    private String wheelPrintedPattern;
    @ApiModelProperty("车窗")
    @TableField("vehicle_window")
    private String vehicleWindow;
    @ApiModelProperty("车顶")
    @TableField("vehicle_roof")
    private String vehicleRoof;
    @ApiModelProperty("车门")
    @TableField("vehicle_door")
    private String vehicleDoor;
    @ApiModelProperty("车侧")
    @TableField("side_of_vehicle")
    private String sideOfVehicle;
    @ApiModelProperty("车厢")
    @TableField("car_of_vehicle")
    private String carOfVehicle;
    @ApiModelProperty("后视镜")
    @TableField("rearview_mirror")
    private String rearviewMirror;
    @ApiModelProperty("底盘")
    @TableField("vehicle_chassis")
    private String ehicleChassis;
    @ApiModelProperty("遮挡")
    @TableField("vehicle_shielding")
    private String vehicleShielding;
    @ApiModelProperty("贴膜颜色")
    @TableField("film_color")
    private Integer filmColor;
    @ApiModelProperty("改装标志")
    @TableField("is_modified")
    private Integer isModified;
    @ApiModelProperty("图片列表")
    @TableField(value = "sub_image_list", typeHandler = JacksonTypeHandler.class)
    private SubImageList subImageList;
}
