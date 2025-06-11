package com.genersoft.iot.vmp.gat1400.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration(proxyBeanMethods = false)
public class OSSProperties {

    @Value("${VIID_OSS_ENABLE}")
    @ApiModelProperty(value = "是否开启")
    private Boolean enable = false;
    @Value("${VIID_OSS_ENDPOINT}")
    @ApiModelProperty(value = "OSS端点")
    private String endpoint;
    @Value("${VIID_OSS_AK}")
    @ApiModelProperty(value = "访问key")
    private String accessKey;
    @Value("${VIID_OSS_AS}")
    @ApiModelProperty(value = "访问秘钥")
    private String accessSecret;
    @Value("${VIID_OSS_BUCKET}")
    @ApiModelProperty(value = "桶名称")
    private String bucket;
    @Value("${VIID_OSS_REGION}")
    @ApiModelProperty(value = "区域名称")
    private String region = "s3";
    @Value("${VIID_OSS_DOMAIN}")
    @ApiModelProperty(value = "OSS访问端点")
    private String domain;
}
