package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import cz.data.viid.framework.config.Constants;
import cz.data.viid.framework.domain.entity.VIIDPublish;
import cz.data.viid.kafka.PusherMetric;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VIIDPublishVo {

    @ApiModelProperty("订阅标识符")
    private String subscribeId;
    @ApiModelProperty("订阅标题")
    private String title;
    @ApiModelProperty("订阅类型")
    private String subscribeDetail;
    @ApiModelProperty("资源ID")
    private String resourceUri;
    @ApiModelProperty("申请人")
    private String applicationName;
    @ApiModelProperty("申请单位")
    private String applicationOrg;
    @ApiModelProperty("开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;
    @ApiModelProperty("结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    @ApiModelProperty("订阅回调地址")
    private String receiveAddr;
    @ApiModelProperty("数据上报间隔")
    private Integer reportInterval;
    @ApiModelProperty("理由")
    private String reason;
    @ApiModelProperty(value = "操作类型",notes = "0=订阅,1=取消订阅")
    private Integer operateType;
    @ApiModelProperty(value = "订阅状态", notes = "0=订阅中,1=已取消订阅,2=订阅到期,9=未订阅")
    private Integer subscribeStatus;
    @ApiModelProperty(value = "资源类型")
    private Integer resourceClass;
    @ApiModelProperty(value = "图片格式")
    private String resultImageDeclare;
    @ApiModelProperty(value = "发布节点ID")
    private String serverId;
    @ApiModelProperty(value = "创建时间")
    private Date createTime;
    @ApiModelProperty(value = "推送任务描述")
    private String description;
    @ApiModelProperty("上级推送指标")
    private PusherMetric metric;

    public static VIIDPublishVo formList(VIIDPublish publish) {
        if (publish == null)
            return null;
        VIIDPublishVo vo = new VIIDPublishVo();
        BeanUtils.copyProperties(publish, vo);
        String[] details = StringUtils.split(publish.getSubscribeDetail(), ",");
        List<String> detailsDescribe = new ArrayList<>();
        for (String detail : details) {
            Constants.SubscribeDetail subscribeDetail = Constants.SubscribeDetail.match(detail);
            if (Objects.nonNull(subscribeDetail)) {
                detailsDescribe.add(subscribeDetail.getDescribe());
            }
        }
        vo.setSubscribeDetail(String.join(",", detailsDescribe));
        return vo;
    }

    public static VIIDPublishVo formInfo(VIIDPublish publish) {
        if (publish == null)
            return null;
        VIIDPublishVo vo = new VIIDPublishVo();
        BeanUtils.copyProperties(publish, vo);
        return vo;
    }
}
