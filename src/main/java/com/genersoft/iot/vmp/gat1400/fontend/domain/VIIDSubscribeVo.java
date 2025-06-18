package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.genersoft.iot.vmp.gat1400.framework.config.Constants;
import com.genersoft.iot.vmp.gat1400.framework.domain.entity.VIIDSubscribe;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VIIDSubscribeVo {

    @ApiModelProperty("订阅标识符")
    private String subscribeId;
    @ApiModelProperty("订阅标题")
    private String title;
    @ApiModelProperty(value = "订阅类型", notes = "7=视频卡口设备,13=车辆信息(卡口过车记录)")
    private String subscribeDetail;
    @ApiModelProperty(value = "资源ID", notes = "卡口ID 视图库ID")
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
    @ApiModelProperty(value = "操作类型", notes = "(0=订阅,1=取消订阅)")
    private Integer operateType;
    @ApiModelProperty(value = "订阅状态", notes = "(0=订阅中,1=已取消订阅,2=订阅到期,9=未订阅)")
    private Integer subscribeStatus;
    @ApiModelProperty("资源类型")
    private Integer resourceClass;
    @ApiModelProperty("未知")
    private String resultImageDeclare;
    @ApiModelProperty("未知")
    private Integer resultFeatureDeclare;
    @ApiModelProperty("节点ID")
    private String serverId;
    @ApiModelProperty("创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public static VIIDSubscribeVo formList(VIIDSubscribe subscribe) {
        if (subscribe == null)
            return null;
        VIIDSubscribeVo vo = new VIIDSubscribeVo();
        BeanUtils.copyProperties(subscribe, vo);
        String[] details = StringUtils.split(subscribe.getSubscribeDetail(), ",");
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

    public static VIIDSubscribeVo formInfo(VIIDSubscribe subscribe) {
        if (subscribe == null)
            return null;
        VIIDSubscribeVo vo = new VIIDSubscribeVo();
        BeanUtils.copyProperties(subscribe, vo);
        return vo;
    }
}
