package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName(value = "viid_tollgate_device")
public class TollgateDevice {

    @ApiModelProperty("设备标识")
    @TableId(value = "tollgate_id", type = IdType.NONE)
    private String tollgateId;
    @ApiModelProperty("卡口名称")
    @TableField("name")
    private String name;
    @ApiModelProperty("经度")
    @TableField("longitude")
    private String longitude;
    @ApiModelProperty("纬度")
    @TableField("latitude")
    private String latitude;
    @ApiModelProperty(value = "位置编码", notes = "安装地点行政区划代码,街道编号")
    @TableField("place_code")
    private String placeCode;
    @ApiModelProperty("状态")
    @TableField("status")
    private String status;
    @ApiModelProperty(value = "卡口类型", notes = "10国际,20省际,30市际,31市区,40县际,41县区,99其他")
    @TableField("tollgate_cat")
    private String tollgateCat;
    @ApiModelProperty(value = "卡口用途", notes = "80治安卡口,81交通卡口,82其他")
    @TableField("tollgate_usage")
    private String tollgateUsage;
    @ApiModelProperty("车道数量")
    @TableField("lane_num")
    private Integer laneNum;
    @ApiModelProperty("管辖单位代码")
    @TableField("org_code")
    private String orgCode;
    @ApiModelProperty(value = "设备ID")
    @TableField("device_id")
    private String deviceId;

}
