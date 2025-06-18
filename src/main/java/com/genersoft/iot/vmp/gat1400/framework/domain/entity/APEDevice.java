package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@TableName(value = "viid_ape_device")
public class APEDevice {

    @ApiModelProperty("设备标识")
    @TableId(value = "ape_id", type = IdType.NONE)
    private String apeId;
    @ApiModelProperty("设备名称")
    @TableField("name")
    private String name;
    @ApiModelProperty("设备型号")
    @TableField("model")
    private String model;
    @ApiModelProperty("设备地址")
    @TableField("ip_addr")
    private String ipAddr;
    @ApiModelProperty("设备IPV6地址")
    @TableField("ipv6_addr")
    private String ipv6Addr;
    @ApiModelProperty("设备端口")
    @TableField("port")
    private String port;
    @ApiModelProperty("经度")
    @TableField("longitude")
    private String longitude;
    @ApiModelProperty("纬度")
    @TableField("latitude")
    private String latitude;
    @ApiModelProperty("地区编码")
    @TableField("place_code")
    private String placeCode;
    @ApiModelProperty("位置名")
    @TableField("place")
    private String place;
    @ApiModelProperty("管辖单位代码")
    @TableField("org_code")
    private String orgCode;
    @ApiModelProperty(value = "车辆抓拍方向", notes = "0=拍车头,1=拍车尾,兼容无视频卡口信息设备")
    @TableField("cap_direction")
    private String capDirection;
    @ApiModelProperty("监视方向")
    @TableField("monitor_direction")
    private String monitorDirection;
    @ApiModelProperty("监视区域说明")
    @TableField("monitor_area_desc")
    private String monitorAreaDesc;
    @ApiModelProperty("所属采集系统")
    @TableField("owner_aps_id")
    private String ownerApsId;
    @ApiModelProperty("是否在线")
    @TableField("is_online")
    private String isOnline;
    @ApiModelProperty("用户标识")
    @TableField("user_id")
    private String userId;
    @ApiModelProperty("口令")
    @TableField("password")
    private String password;
    @ApiModelProperty("功能集")
    @TableField("function_type")
    private String functionType;
    @ApiModelProperty("关联外部设备ID")
    @TableField("ext_id")
    private String extId;
}
