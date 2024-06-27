package com.genersoft.iot.vmp.streamPush.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用来关联推流数据关联的平台和目录
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StreamPushInfoForUpdateLoad extends StreamPush{

    private String platformId;

    private String catalogId;
}
