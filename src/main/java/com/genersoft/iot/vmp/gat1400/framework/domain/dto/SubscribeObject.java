package com.genersoft.iot.vmp.gat1400.framework.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode
@ToString
public class SubscribeObject {

    @ApiModelProperty("订阅标识符")
    @JsonProperty("SubscribeID")
    private String subscribeId;
    @ApiModelProperty("订阅标题")
    @JsonProperty("Title")
    private String title;
    @ApiModelProperty(value = "订阅类型", notes = "")
    @JsonProperty("SubscribeDetail")
    private String subscribeDetail;
    @ApiModelProperty("资源ID(卡口ID)")
    @JsonProperty("ResourceURI")
    private String resourceUri;
    @ApiModelProperty("申请人")
    @JsonProperty("ApplicantName")
    private String applicationName;
    @ApiModelProperty("申请单位")
    @JsonProperty("ApplicantOrg")
    private String applicationOrg;
    @ApiModelProperty("开始时间")
    @JsonProperty("BeginTime")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date beginTime;
    @ApiModelProperty("结束时间")
    @JsonProperty("EndTime")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date endTime;
    @ApiModelProperty("订阅回调地址")
    @JsonProperty("ReceiveAddr")
    private String receiveAddr;
    @ApiModelProperty("数据上报间隔")
    @JsonProperty("ReportInterval")
    private Integer reportInterval;
    @ApiModelProperty("理由")
    @JsonProperty("Reason")
    private String reason;
    @ApiModelProperty(value = "操作类型", notes = "0=订阅,1=取消订阅")
    @JsonProperty("OperateType")
    private Integer operateType;
    @ApiModelProperty(value = "订阅状态", notes = "0=订阅中,1=已取消订阅,2=订阅到期,9=未订阅")
    @JsonProperty("SubscribeStatus")
    private Integer subscribeStatus;
    @JsonProperty("ResourceClass")
    private Integer resourceClass;
    @JsonIgnore
    @JsonProperty("ResultImageDeclare")
    private String resultImageDeclare;
    @JsonIgnore
    @JsonProperty("ResultFeatureDeclare")
    private Integer resultFeatureDeclare;
}
