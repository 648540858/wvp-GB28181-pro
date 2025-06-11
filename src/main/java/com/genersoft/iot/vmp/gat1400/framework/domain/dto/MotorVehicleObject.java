package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MotorVehicleObject {

    @ApiModelProperty("全局ID")
    @JsonProperty("MotorVehicleID")
    private String MotorVehicleID;
    @JsonProperty("InfoKind")
    private String InfoKind;
    @JsonProperty("SourceID")
    private String SourceID;
    @ApiModelProperty("卡口编号")
    @JsonProperty("TollgateID")
    private String TollgateID;
    @ApiModelProperty("设备ID")
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
    @ApiModelProperty("车道号")
    @JsonProperty("LaneNo")
    private Integer LaneNo;
    @ApiModelProperty("有无车牌")
    @JsonProperty("HasPlate")
    private Boolean HasPlate;
    @ApiModelProperty("号牌种类")
    @JsonProperty("PlateClass")
    private String PlateClass;
    @ApiModelProperty("号牌颜色")
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
    @ApiModelProperty("速度")
    @JsonProperty("Speed")
    private Double Speed;
    @ApiModelProperty("行驶方向")
    @JsonProperty("Direction")
    private String Direction;
    @ApiModelProperty("行驶状态代码")
    @JsonProperty("DrivingStatusCode")
    private String DrivingStatusCode;
    @ApiModelProperty("车辆使用性质代码")
    @JsonProperty("UsingPropertiesCode")
    private String UsingPropertiesCode;
    @ApiModelProperty("车辆类型")
    @JsonProperty("VehicleClass")
    private String VehicleClass;
    @ApiModelProperty("车辆品牌")
    @JsonProperty("VehicleBrand")
    private String VehicleBrand;
    @ApiModelProperty("车辆型号")
    @JsonProperty("VehicleModel")
    private String VehicleModel;
    @ApiModelProperty("车辆年款")
    @JsonProperty("VehicleStyles")
    private String VehicleStyles;
    @ApiModelProperty("车辆长度")
    @JsonProperty("VehicleLength")
    private Integer VehicleLength;
    @ApiModelProperty("车辆宽度")
    @JsonProperty("VehicleWidth")
    private Integer VehicleWidth;
    @ApiModelProperty("车辆高度")
    @JsonProperty("VehicleHeight")
    private Integer VehicleHeight;
    @ApiModelProperty("车身颜色")
    @JsonProperty("VehicleColor")
    private String VehicleColor;
    @ApiModelProperty("颜色深浅")
    @JsonProperty("VehicleColorDepth")
    private String VehicleColorDepth;
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
    private String FilmColor;
    @ApiModelProperty("改装标志")
    @JsonProperty("IsModified")
    private Boolean IsModified;
    @ApiModelProperty("撞痕信息")
    @JsonProperty("HitMarkInfo")
    private String HitMarkInfo;
    @ApiModelProperty("车身描述")
    @JsonProperty("VehicleBodyDesc")
    private String VehicleBodyDesc;
    @ApiModelProperty("车前部物品")
    @JsonProperty("VehicleFrontItem")
    private String VehicleFrontItem;
    @ApiModelProperty("车前部物品描述")
    @JsonProperty("DescOfFrontItem")
    private String DescOfFrontItem;
    @ApiModelProperty("车后部物品")
    @JsonProperty("VehicleRearItem")
    private String VehicleRearItem;
    @ApiModelProperty("车后部物品描述")
    @JsonProperty("DescOfRearItem")
    private String DescOfRearItem;
    @ApiModelProperty("车内人数")
    @JsonProperty("NumOfPassenger")
    private Integer NumOfPassenger;
    @ApiModelProperty("过车时间")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    @JsonProperty("PassTime")
    private String PassTime;
    @ApiModelProperty("经过道路名称")
    @JsonProperty("NameOfPassedRoad")
    private String NameOfPassedRoad;
    @ApiModelProperty("是否可疑车")
    @JsonProperty("IsSuspicious")
    private Boolean IsSuspicious;
    @ApiModelProperty(value = "遮阳板状态", notes = "0=收起 1=放下")
    @JsonProperty("Sunvisor")
    private String Sunvisor;
    @ApiModelProperty(value = "安全带状态", notes = "0=未系 1=有系")
    @JsonProperty("SafetyBelt")
    private String SafetyBelt;
    @ApiModelProperty(value = "打电话状态", notes = "0=未打电话 1=打电话中")
    @JsonProperty("Calling")
    private String Calling;
    @ApiModelProperty(value = "号牌识别可信度", notes = "整个号牌号码的识别可信度,以0~100数值表示百分比,数值越大可信度越高")
    @JsonProperty("PlateReliability")
    private String PlateReliability;
    @ApiModelProperty(value = "每位号牌号码可信度")
    @JsonProperty("PlateCharReliability")
    private String PlateCharReliability;
    @ApiModelProperty(value = "品牌标志识别可信度")
    @JsonProperty("BrandReliability")
    private String BrandReliability;
    @ApiModelProperty("近景照片")
    @JsonProperty("StorageUrl1")
    private String StorageUrl1;
    @ApiModelProperty("车牌照片")
    @JsonProperty("StorageUrl2")
    private String StorageUrl2;
    @ApiModelProperty("远景照片")
    @JsonProperty("StorageUrl3")
    private String StorageUrl3;
    @ApiModelProperty("合成图")
    @JsonProperty("StorageUrl4")
    private String StorageUrl4;
    @ApiModelProperty("缩略图")
    @JsonProperty("StorageUrl5")
    private String StorageUrl5;
    @ApiModelProperty("图片列表")
    @JsonProperty("SubImageList")
    private SubImageList SubImageList;

    public MotorVehicleObject validateMotorVehicle() {
        String id = getMotorVehicleID();
        if (StringUtils.length(id) == 48) {
            String type = id.substring(20, 22);
            if (!"02".equals(type)) {
                String start = StringUtils.substring(id, 0, 20);
                String end = StringUtils.substring(id, 22, id.length());
                this.setMotorVehicleID(start + "02" + end);
            }
            if (Objects.isNull(this.getLaneNo())) {
                setLaneNo(1);
            }
            if (Objects.isNull(this.getSpeed())) {
                setSpeed(0d);
            }
            if (Objects.isNull(this.getVehicleLength())) {
                setVehicleLength(0);
            }
            if (Objects.isNull(this.getDirection())) {
                //9=其它
                setDirection("9");
            }
            // fix 车辆信息ID前缀为0
            id = getMotorVehicleID();
            String deviceID = getDeviceID();
            if (!StringUtils.startsWith(id, deviceID)) {
                String subId = StringUtils.substring(id, deviceID.length());
                setMotorVehicleID(deviceID + subId);
            }
            return this;
        } else {
            return null;
        }
    }
}
