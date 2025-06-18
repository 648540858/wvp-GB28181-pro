package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
@TableName(value = "viid_publish", autoResultMap = true)
public class VIIDPublish {

    @ApiModelProperty("订阅标识符")
    @TableId(value = "subscribe_id", type = IdType.NONE)
    private String subscribeId;
    @ApiModelProperty("订阅标题")
    @TableField("title")
    private String title;
    @ApiModelProperty("订阅类型")
    @TableField("subscribe_detail")
    private String subscribeDetail;
    @ApiModelProperty("资源ID")
    @TableField(value = "resource_uri")
    private String resourceUri;
    @ApiModelProperty("申请人")
    @TableField("application_name")
    private String applicationName;
    @ApiModelProperty("申请单位")
    @TableField("application_org")
    private String applicationOrg;
    @ApiModelProperty("开始时间")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    @TableField("begin_time")
    private Date beginTime;
    @ApiModelProperty("结束时间")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    @TableField("end_time")
    private Date endTime;
    @ApiModelProperty("订阅回调地址")
    @TableField("receive_addr")
    private String receiveAddr;
    @ApiModelProperty("数据上报间隔")
    @TableField("report_interval")
    private Integer reportInterval = 3;
    @ApiModelProperty("理由")
    @TableField("reason")
    private String reason;
    @ApiModelProperty(value = "操作类型",notes = "0=订阅,1=取消订阅")
    @TableField("operate_type")
    private Integer operateType;
    @ApiModelProperty(value = "订阅状态", notes = "0=订阅中,1=已取消订阅,2=订阅到期,9=未订阅")
    @TableField("subscribe_status")
    private Integer subscribeStatus;
    @ApiModelProperty(value = "资源类型")
    @TableField("resource_class")
    private Integer resourceClass;
    @ApiModelProperty(value = "图片格式")
    @TableField("result_image_declare")
    private String resultImageDeclare;
    @JsonIgnore
    @ApiModelProperty(value = "未知")
    @TableField("result_feature_declare")
    private Integer resultFeatureDeclare;
    @ApiModelProperty(value = "发布节点ID")
    @TableField("server_id")
    private String serverId;
    @JsonIgnore
    @TableField("create_time")
    private Date createTime;
}
