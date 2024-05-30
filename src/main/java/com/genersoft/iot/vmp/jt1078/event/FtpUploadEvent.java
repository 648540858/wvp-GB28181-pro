package com.genersoft.iot.vmp.jt1078.event;

import org.springframework.context.ApplicationEvent;

public class FtpUploadEvent extends ApplicationEvent {

    public FtpUploadEvent(Object source) {
        super(source);
    }

    private String fileName;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
