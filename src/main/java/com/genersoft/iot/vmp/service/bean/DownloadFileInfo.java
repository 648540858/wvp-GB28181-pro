package com.genersoft.iot.vmp.service.bean;

import lombok.Data;

@Data
public class DownloadFileInfo {

    private String httpPath;
    private String httpsPath;
    private String httpDomainPath;
    private String httpsDomainPath;

}
