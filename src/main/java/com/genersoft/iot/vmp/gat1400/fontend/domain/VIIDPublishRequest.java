package com.genersoft.iot.vmp.gat1400.fontend.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

import cz.data.viid.framework.domain.core.PaginationRequest;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VIIDPublishRequest extends PaginationRequest {

    private String serverId;
    private String title;
    private String subscribeId;
    private String subscribeDetail;
    private List<String> resourceUri;

    private String receiveAddr;
    private Integer reportInterval;
    private String applicationName;
    private String applicationOrg;
    private String reason;

    private Integer resourceClass;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date beginTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    @ApiModelProperty(value = "图片格式")
    private String resultImageDeclare;
}
