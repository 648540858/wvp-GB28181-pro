package com.genersoft.iot.vmp.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 一个优秀的颓废程序猿（CSDN）
 */
@Getter
@Component
@PropertySource(value = {"classpath:git.properties" }, ignoreResourceNotFound = true)
public class GitUtil {

    @Value("${git.branch:}")
    private String branch;
    @Value("${git.commit.id:}")
    private String gitCommitId;
    @Value("${git.remote.origin.url:}")
    private String gitUrl;
    @Value("${git.build.time:}")
    private String buildDate;

    @Value("${git.build.version:}")
    private String buildVersion;

    @Value("${git.commit.id.abbrev:}")
    private String commitIdShort;

    @Value("${git.commit.time:}")
    private String commitTime;

}
