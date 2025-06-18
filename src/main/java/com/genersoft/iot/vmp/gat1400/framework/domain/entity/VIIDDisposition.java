package com.genersoft.iot.vmp.gat1400.framework.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.genersoft.iot.vmp.gat1400.framework.domain.dto.SubImageList;

import java.util.Date;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 布控信息
 */
@Data
@EqualsAndHashCode
@TableName(value = "viid_disposition", autoResultMap = true)
public class VIIDDisposition {

    @ApiModelProperty("主键")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;
    @ApiModelProperty(value = "布控ID")
    @TableField(value = "disposition_id")
    private String dispositionId;
    @ApiModelProperty("布控标题")
    @TableField("title")
    private String title;
    @ApiModelProperty(value = "布控类别", notes = "1=人,2=机动车,3=非机动车,4=关键字")
    @TableField("disposition_category")
    private String dispositionCategory;
    @ApiModelProperty("目标特征")
    @TableField("target_feature")
    private String targetFeature;
    @ApiModelProperty("目标图片路径")
    @TableField("target_image_uri")
    private String targetImageUri;
    @ApiModelProperty("优先等级")
    @TableField("priority_level")
    private Integer priorityLevel;
    @ApiModelProperty("应用名称")
    @TableField("applicant_name")
    private String applicantName;
    @ApiModelProperty("应用信息")
    @TableField("applicant_info")
    private String applicantInfo;
    @ApiModelProperty("应用组织机构")
    @TableField("applicant_org")
    private String applicantOrg;
    @ApiModelProperty("布控开始时间")
    @TableField("begin_time")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date beginTime;
    @ApiModelProperty("布控结束时间")
    @TableField("end_time")
    @JsonFormat(pattern = "yyyyMMddHHmmss")
    private Date endTime;
    @ApiModelProperty(value = "操作类型", notes = "0=布控,1=撤控")
    @TableField("operate_type")
    private String operateType;
    @ApiModelProperty(value = "布控状态", notes = "0=布控中,1=已撤控,2=布控到期,9=未布控")
    @TableField(value = "disposition_status")
    private String dispositionStatus;
    @ApiModelProperty(value = "布控范围", notes = "1=卡口,2=区域布控")
    @TableField("disposition_range")
    private String dispositionRange;
    @ApiModelProperty(value = "布控卡口", notes = "卡口布控时使用,多个卡口ID使用','分隔")
    @TableField("tollgate_list")
    private String tollgateList;
    @ApiModelProperty(value = "布控行政区域", notes = "区域布控时使用,县际联动填6位行政区号码GB/T2260规定 地市联动填4位行政区号码 省际联动填2位行政区号码")
    @TableField("disposition_area")
    private String dispositionArea;
    @ApiModelProperty(value = "告警信息接收地址", notes = "告警信息接收地址URL 级联接口时该地址控制直接接收还是逐级转发")
    @TableField("receive_addr")
    private String receiveAddr;
    @ApiModelProperty("告警信息接收手机号")
    @TableField("receive_mobile")
    private String receiveMobile;
    @ApiModelProperty("布控理由")
    @TableField("reason")
    private String reason;
    @ApiModelProperty("图片信息")
    @TableField(value = "sub_image_list", typeHandler = JacksonTypeHandler.class)
    private SubImageList subImageList;

    @ApiModelProperty("节点ID")
    @TableField("server_id")
    private String serverId;
}
