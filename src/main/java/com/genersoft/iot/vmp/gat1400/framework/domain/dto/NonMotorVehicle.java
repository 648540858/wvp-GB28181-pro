package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import cz.data.viid.utils.DateUtil;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class NonMotorVehicle {

    @ApiModelProperty("车辆标识")
    @JsonProperty("NonMotorVehicleID")
    private String NonMotorVehicleID;
    @ApiModelProperty("信息分类")
    @JsonProperty("InfoKind")
    private String InfoKind;
    @ApiModelProperty("来源标识")
    @JsonProperty("SourceID")
    private String SourceID;
    @ApiModelProperty("设备编码")
    @JsonProperty("DeviceID")
    private String DeviceID;
    @ApiModelProperty("左上角X坐标")
    @JsonProperty("LeftTopX")
    private Integer LeftTopX;
    @ApiModelProperty("左上角Y坐标")
    @JsonProperty("LeftTopY")
    private Integer LeftTopY;
    @ApiModelProperty("右下角X坐标")
    @JsonProperty("RightBtmX")
    private Integer RightBtmX;
    @ApiModelProperty("右下角Y坐标")
    @JsonProperty("RightBtmY")
    private Integer RightBtmY;
    @ApiModelProperty("位置标记时间")
    @JsonProperty("MarkTime")
    private String MarkTime;
    @ApiModelProperty("车辆出现时间")
    @JsonProperty("AppearTime")
    private String AppearTime;
    @ApiModelProperty("车辆消失时间")
    @JsonProperty("DisappearTime")
    private String DisappearTime;
    @ApiModelProperty("有无车牌")
    @JsonProperty("HasPlate")
    private Boolean HasPlate;
    @ApiModelProperty("号牌种类")
    @JsonProperty("PlateClass")
    private String PlateClass;
    @ApiModelProperty("车牌颜色")
    @JsonProperty("PlateColor")
    private String PlateColor;
    @ApiModelProperty("车牌号")
    @JsonProperty("PlateNo")
    private String PlateNo;
    @ApiModelProperty("挂车牌号")
    @JsonProperty("PlateNoAttach")
    private String PlateNoAttach;
    @ApiModelProperty("车牌描述")
    @JsonProperty("PlateDescribe")
    private String PlateDescribe;
    @ApiModelProperty("是否套牌")
    @JsonProperty("IsDecked")
    private Boolean IsDecked;
    @ApiModelProperty("是否涂改")
    @JsonProperty("IsAltered")
    private Boolean IsAltered;
    @ApiModelProperty("是否遮挡")
    @JsonProperty("IsCovered")
    private Boolean IsCovered;
    @ApiModelProperty("行驶速度")
    @JsonProperty("Speed")
    private Double Speed;
    @ApiModelProperty("行驶状态代码")
    @JsonProperty("DrivingStatusCode")
    private String DrivingStatusCode;
    @ApiModelProperty("车辆使用性质代码")
    @JsonProperty("UsingPropertiesCode")
    private String UsingPropertiesCode;
    @ApiModelProperty("车辆品牌")
    @JsonProperty("VehicleBrand")
    private String VehicleBrand;
    @ApiModelProperty("车辆款型")
    @JsonProperty("VehicleType")
    private String VehicleType;
    @ApiModelProperty("车辆长度")
    @JsonProperty("VehicleLength")
    private String VehicleLength;
    @ApiModelProperty("车辆宽度")
    @JsonProperty("VehicleWidth")
    private String VehicleWidth;
    @ApiModelProperty("车辆高度")
    @JsonProperty("VehicleHeight")
    private String VehicleHeight;
    @ApiModelProperty("车身颜色")
    @JsonProperty("VehicleColor")
    private String VehicleColor;
    @ApiModelProperty("车前盖")
    @JsonProperty("VehicleHood")
    private String VehicleHood;
    @ApiModelProperty("车后盖")
    @JsonProperty("VehicleTrunk")
    private String VehicleTrunk;
    @ApiModelProperty("车轮")
    @JsonProperty("VehicleWheel")
    private String VehicleWheel;
    @ApiModelProperty("车轮印花纹")
    @JsonProperty("WheelPrintedPattern")
    private String WheelPrintedPattern;
    @ApiModelProperty("车窗")
    @JsonProperty("VehicleWindow")
    private String VehicleWindow;
    @ApiModelProperty("车顶")
    @JsonProperty("VehicleRoof")
    private String VehicleRoof;
    @ApiModelProperty("车门")
    @JsonProperty("VehicleDoor")
    private String VehicleDoor;
    @ApiModelProperty("车侧")
    @JsonProperty("SideOfVehicle")
    private String SideOfVehicle;
    @ApiModelProperty("车厢")
    @JsonProperty("CarOfVehicle")
    private String CarOfVehicle;
    @ApiModelProperty("后视镜")
    @JsonProperty("RearviewMirror")
    private String RearviewMirror;
    @ApiModelProperty("底盘")
    @JsonProperty("VehicleChassis")
    private String VehicleChassis;
    @ApiModelProperty("遮挡")
    @JsonProperty("VehicleShielding")
    private String VehicleShielding;
    @ApiModelProperty("贴膜颜色")
    @JsonProperty("FilmColor")
    private Integer FilmColor;
    @ApiModelProperty("改装标志")
    @JsonProperty("IsModified")
    private Integer IsModified;
    @ApiModelProperty("图片列表")
    @JsonProperty("SubImageList")
    private SubImageList SubImageList;

    public NonMotorVehicle validateDataFormat() {
        String id = getNonMotorVehicleID();
        String deviceID = getDeviceID();
        if (StringUtils.isNotBlank(id)
                && id.length() > deviceID.length()
                && !StringUtils.startsWith(id, deviceID)) {
            String subId = StringUtils.substring(id, deviceID.length());
            setNonMotorVehicleID(deviceID + subId);
        }
        if (Objects.isNull(this.getSpeed())) {
            setSpeed(0d);
        }
        if (Objects.isNull(this.getVehicleLength())) {
            setVehicleLength("0");
        }
        if (Objects.isNull(this.getVehicleBrand())) {
            setVehicleBrand("0");
        }
        if (StringUtils.isBlank(getAppearTime())) {
            String dateTime = DateUtil.extractIdDateTime(getNonMotorVehicleID());
            if (dateTime != null) {
                setAppearTime(dateTime);
            }
        }
        return this;
    }

}
