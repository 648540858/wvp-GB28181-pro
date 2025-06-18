package com.genersoft.iot.vmp.gat1400.backend.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubImageList;
import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 布控对象
 */
@Data
@EqualsAndHashCode
public class DispositionObject {

    @ApiModelProperty(value = "布控ID", notes = "33位编号")
    @JsonProperty("DispositionID")
    private String DispositionID;
    @ApiModelProperty("通知标题")
    @JsonProperty("Title")
    private String Title;
    @ApiModelProperty(value = "布控类别", notes = "1=人,2=机动车,3=非机动车,4=关键字")
    @JsonProperty("DispositionCategory")
    private String DispositionCategory;
    @ApiModelProperty("目标特征")
    @JsonProperty("TargetFeature")
    private String TargetFeature;
    @ApiModelProperty("目标图片路径")
    @JsonProperty("TargetImageURI")
    private String TargetImageURI;
    @ApiModelProperty("优先等级")
    @JsonProperty("PriorityLevel")
    private Integer PriorityLevel;
    @ApiModelProperty("应用名称")
    @JsonProperty("ApplicantName")
    private String ApplicantName;
    @ApiModelProperty("应用信息")
    @JsonProperty("ApplicantInfo")
    private String ApplicantInfo;
    @ApiModelProperty("应用组织机构")
    @JsonProperty("ApplicantOrg")
    private String ApplicantOrg;
    @ApiModelProperty("布控开始时间")
    @JsonProperty("BeginTime")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date BeginTime;
    @ApiModelProperty("布控结束时间")
    @JsonProperty("EndTime")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date EndTime;
    @ApiModelProperty(value = "操作类型", notes = "0=布控,1=撤控")
    @JsonProperty("OperateType")
    private String OperateType;
    @ApiModelProperty(value = "布控状态", notes = "0=布控中,1=已撤控,2=布控到期,9=未布控")
    @JsonProperty(value = "DispositionStatus")
    private String DispositionStatus;
    @ApiModelProperty(value = "布控范围", notes = "1=卡口,2=区域布控")
    @JsonProperty("DispositionRange")
    private String DispositionRange;
    @ApiModelProperty(value = "布控卡口", notes = "卡口布控时使用,多个卡口ID使用','分隔")
    @JsonProperty("TollgateList")
    private String TollgateList;
    @ApiModelProperty(value = "布控行政区域", notes = "区域布控时使用,县际联动填6位行政区号码GB/T2260规定 地市联动填4位行政区号码 省际联动填2位行政区号码")
    @JsonProperty("DispositionArea")
    private String DispositionArea;
    @ApiModelProperty(value = "告警信息接收地址", notes = "告警信息接收地址URL 级联接口时该地址控制直接接收还是逐级转发")
    @JsonProperty("ReceiveAddr")
    private String ReceiveAddr;
    @ApiModelProperty("告警信息接收手机号")
    @JsonProperty("ReceiveMobile")
    private String ReceiveMobile;
    @ApiModelProperty("布控理由")
    @JsonProperty("Reason")
    private String Reason;
    @ApiModelProperty("图片信息")
    @JsonProperty("SubImageList")
    private SubImageList SubImageList;
}
