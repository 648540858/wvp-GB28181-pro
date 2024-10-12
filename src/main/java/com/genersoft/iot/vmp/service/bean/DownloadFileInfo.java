package com.genersoft.iot.vmp.service.bean;

public class DownloadFileInfo {

    private String httpPath;
    private String httpsPath;
    private String httpDomainPath;
    private String httpsDomainPath;

    public String getHttpPath() {
        return httpPath;
    }

    public void setHttpPath(String httpPath) {
        this.httpPath = httpPath;
    }

    public String getHttpsPath() {
        return httpsPath;
    }

    public void setHttpsPath(String httpsPath) {
        this.httpsPath = httpsPath;
    }

    public String getHttpDomainPath() {
        return httpDomainPath;
    }

    public void setHttpDomainPath(String httpDomainPath) {
        this.httpDomainPath = httpDomainPath;
    }

    public String getHttpsDomainPath() {
        return httpsDomainPath;
    }

    public void setHttpsDomainPath(String httpsDomainPath) {
        this.httpsDomainPath = httpsDomainPath;
    }
}
