package com.genersoft.iot.vmp.jt1078.event;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

@Setter
@Getter
public class FtpUploadEvent extends ApplicationEvent {

    public FtpUploadEvent(Object source) {
        super(source);
    }

    private String fileName;

}
